package tech.ammer.sdk.card

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.util.AndroidException
import android.util.Log
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import org.bouncycastle.util.encoders.Hex
import tech.ammer.sdk.card.apdu.*
import tech.ammer.sdk.card.apdu.CardErrors.SW_FILE_NOT_FOUND
import tech.ammer.sdk.card.apdu.CardErrors.SW_NO_ERROR
import tech.ammer.sdk.card.apdu.Tags.CLA_ISO7816
import tech.ammer.sdk.card.apdu.Tags.INS_SELECT
import tech.ammer.sdk.card.apdu.Tags.VALUE_OFFSET
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Permission
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime


class NFCCardController(private val listener: CardControllerListener) : ReaderCallback, ICardController {

    companion object {
        private const val CONNECT_TIMEOUT = 25000
        private var parameterSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")

        val algr = "EC"

        //    private val cipherAlg = "AES/GCM/NoPadding"
        val cipherAlg = "AES/CBC/ISO7816-4Padding"

        //    private val keyAlg = "DH"
        val keyAlg = "ECDH"
    }

    init {
        createKeys()
    }

    private var isUnlock: Boolean = false
    private var isoDep: IsoDep? = null
    private var nfc: NfcAdapter? = null
    private var activity: Activity? = null
    private var selectedAID: ICardController.AIDs? = null
    private val aidsByte = arrayListOf<ByteArray>()

    private var cipher: Cipher? = null
    var secretKeySpec: SecretKeySpec? = null
    var secureRandom = SecureRandom()
    private var hostPublicKey: ECPublicKey? = null
    private var hostPrivateKey: ECPrivateKey? = null

    init {
        ICardController
            .AIDs
            .values()
            .forEachIndexed { index, it ->
                val aidHex = it.aid.split(":").toTypedArray()
                aidsByte.add(index, ByteArray(aidHex.size))

                for (i in aidsByte[index].indices) {
                    val ii = aidHex[i].toInt(16)
                    aidsByte[index][i] = (if (ii > 127) ii - 256 else ii).toByte()
                }
            }
    }

    override fun onTagDiscovered(tag: Tag) {
        isoDep = IsoDep.get(tag)
        try {
            isoDep?.connect()
            isoDep?.timeout = CONNECT_TIMEOUT
            listener.onCardAttached()
        } catch (e: Exception) {
            e.printStackTrace()
            listener.onCardError(convertError(e))
        }
    }

    private fun convertError(e: Exception): Short {
        return when {
            (e as? TagLostException) != null -> CardErrors.TAG_WAL_LOST
            (e as? NoSuchAlgorithmException) != null -> CardErrors.OTHER_ALGORITHM
            e.message?.toShortOrNull() != null -> e.message!!.toShort()
            else -> -1
        }
    }

    private fun pinGetBytes(pin: String): ByteArray {
        val pinBytes = ByteArray(pin.length)
        for (i in pin.indices) {
            pinBytes[i] = (pin[i].code - 48).toByte()
        }
        return pinBytes
    }

    private fun publicKeyObj(): ECPublicKey {
        val w = publicKey()
        Log.d("publicKey ", w.toList().toString())

        val cardKeySpec = ECPublicKeySpec(parameterSpec.curve.decodePoint(w), parameterSpec)
        val cardKey = KeyFactory.getInstance("EC", "BC").generatePublic(cardKeySpec) as ECPublicKey
        Log.d("getPublicKeyObj ", Hex.toHexString(cardKey.encoded))

        return cardKey
    }

    // TODO remove saving reference to activity for `listenForCard` and `stopListening` (find better way to do it)
    override fun open(activity: Activity) {
        Log.d("open activity is ", activity.toString())
        this.activity = activity

        nfc = NfcAdapter.getDefaultAdapter(activity)
        if (nfc == null) throw Exception("NFC module not found")
        if (!nfc!!.isEnabled) throw Exception("NFC not enabled")
        Log.d("NfcAdapter looks ok ", nfc.toString())
    }

    override fun startListening() {
        Log.d("NfcAdapter is:", nfc.toString())
        nfc?.enableReaderMode(activity, this, NfcAdapter.FLAG_READER_NFC_A, null)
    }

    override fun stopListening() {
        Log.d("NfcAdapter is:", nfc.toString())
        nfc?.disableReaderMode(activity)
        createKeys()
    }

    override fun close() {
        Log.d("close NfcAdapter is ", nfc.toString())
        activity = null
        isUnlock = false
    }

    override fun getCardUUID(): UUID {

        val command = APDUBuilder.init().setINS(Instructions.INS_GET_CARD_GUID)
        val uuidCardBytes = processCommand("GetMeta", command)
        val buffer = ByteBuffer.allocate(8)
        buffer.put(uuidCardBytes, 0, 8)
        buffer.flip()

        val mostSigBits = buffer.long
        buffer.clear()
        buffer.put(uuidCardBytes, 8, 8)
        buffer.flip()

        val leastSigBits = buffer.long
        return UUID(mostSigBits, leastSigBits)
    }

    override fun getPublicKeyECDSA(pin: String): String? {
        try {
            unlock(pinGetBytes(pin))
            val pubKey = ECController.instance?.getPublicKeyString(publicKey())
            return pubKey
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("${CardErrors.SW_WRONG_P1P2}")
        }
    }

    override fun getPublicKeyEDDSA(pin: String): String? {
        try {
            unlock(pinGetBytes(pin))
            val _key = publicKeyED()
            val pubKey = ECController.instance?.getEDPublicKeyString(_key)

            Log.d("", ">>> edPublicKeyStringHex: $pubKey")
            return pubKey

        } catch (it: Exception) {
            it.printStackTrace()
        }

        return null
    }

    private fun publicKeyED(): ByteArray {
        val command = APDUBuilder.init().setINS(Instructions.INS_ED_GET_PUBLIC_KEY)
        val cmd = processCommand("GetPublicKeyED", command)
        return cmd
    }

    override fun getPrivateKey(pin: String): String? {
        kotlin.runCatching {
            val pinBytes = pinGetBytes(pin)

            unlock(pinBytes)
            val privKey = privateKey(pinBytes)
            return ECController.instance?.getPrivateKeyString(privKey)
        }.onFailure {
            it.printStackTrace()
        }

        return null
    }

    override fun blockCard(pin: String) {
        val _pin = pinGetBytes(pin)
        unlock(_pin)
        blockGetPrivateKey(pin)
    }

    private fun blockGetPrivateKey(pin: String) {
        val pinBytes = pinGetBytes(pin)
        val _pin = byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes)

        val command = APDUBuilder.init().setINS(Instructions.INS_DISABLE_PRIVATE_KEY_EXPORT).setData(_pin)
        processCommand("Block get private key", command)
    }

    private fun verify(data: String, signedData: String): Boolean {
        val signature = Signature.getInstance("NONEwithECDSA")
        signature.initVerify(publicKeyObj())
        signature.update(Hex.decode(data))

        return signature.verify(Hex.decode(signedData))
    }

    private var secureMessage = false

    private fun processCommand(commandName: String, command: APDUBuilder): ByteArray {
        Log.d("Send Command:", "$commandName: ${command.build().toList()}")
        if (secureMessage) {
            createSecure()
            val cmdDecode = if (command.data.isEmpty()) command else encodeMsg(command)
            println("send(decoded): ($commandName): ${Hex.toHexString(cmdDecode.data)}")
            val response = ResponseAPDU(isoDep!!.transceive(cmdDecode.build()))
            Log.d("ResultCommand", "response(decoded): $commandName, ${response.sW.toShort()}, ${Hex.toHexString(response.data)}")

            if (response.sW.toShort() != SW_NO_ERROR) {
                println("Error: $commandName:${response.sW}")
                throw Exception("${response.sW}")
            }

            if (response.data.isNotEmpty()) {
                val _response = decodeMsg(response)
                Log.d("ResultCommand", "response(encoded): $commandName, ${Hex.toHexString(_response)}")
                return if (_response.size > 2)
                    _response.takeLast(_response.size - 2).toByteArray()
                else
                    _response
            }

            return response.data
        } else {
            val response = ResponseAPDU(isoDep!!.transceive(command.build()))
            if (response.sW.toShort() != SW_NO_ERROR) {
                throw Exception("Error: $commandName:${response.sW}")
            }
            val data = response.data
            Log.d("Result Command:", "$commandName: ${data.toList()}")
            return if (data.size > 2) data.takeLast(data.size - 2).toByteArray() else data
        }
    }

    private fun createSecure() {
        if (isoDep == null) throw Exception("Tag was lost")

        if (hostPublicKey == null || hostPrivateKey == null) {
            createKeys()
        }

        if (secretKeySpec == null || cipher == null) {
            val encoded = SecP256K1Curve().createPoint(
                hostPublicKey?.q?.affineXCoord?.toBigInteger(),
                hostPublicKey?.q?.affineYCoord?.toBigInteger()
            ).getEncoded(false)

            val tlv = TLVBuilder
                .init(Tags.CARD_PUBLIC_KEY, Tags.CARD_PUBLIC_KEY_MAX_LENGTH.toByte())
                .build(encoded)

            val cmd = APDUBuilder
                .init()
                .setINS(Instructions.INS_ECDH_HANDSHAKE)
                .setData(tlv)

            println("send HANDSHAKE:  ${cmd.data.toList()}")
            val rsp = ResponseAPDU(isoDep!!.transceive(cmd.build()))
            println("response HANDSHAKE: ${rsp.sW.toShort()}")
            initCipher(rsp)
        }
    }

    private fun decodeMsg(response: ResponseAPDU): ByteArray {
        val data: ByteArray = response.data
        val iv = Arrays.copyOfRange(data, 0, 16)

        val ivParameterSpec = IvParameterSpec(iv)
        cipher?.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
        val uncipheredData = cipher!!.doFinal(data, 16, data.size - 16)

        val buffer = ByteBuffer.allocate(uncipheredData.size + 2)
        buffer.put(uncipheredData)
        buffer.put(response.sW1.toByte())
        buffer.put(response.sW2.toByte())

        return ResponseAPDU(buffer.array()).data
    }

    private fun initCipher(response: ResponseAPDU) {
        /* Get ECDH public data from card */
        val ecdhCardPublicKeyBytes =
            Arrays.copyOfRange(response.data, TLV.OFFSET_VALUE.toInt(), TLV.HEADER_BYTES_COUNT + Tags.CARD_PUBLIC_KEY_MAX_LENGTH)
        val ecdhNonceBytes =
            Arrays.copyOfRange(response.data, TLV.OFFSET_VALUE + TLV.HEADER_BYTES_COUNT + Tags.CARD_PUBLIC_KEY_MAX_LENGTH, response.data.size)

        val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")

        val keyFactory = KeyFactory.getInstance(algr, "BC")
        val cardPoint = SecP256K1Curve().decodePoint(ecdhCardPublicKeyBytes)
        val ecPublicKeySpec = ECPublicKeySpec(cardPoint, ecSpec)
        val ecdhCardPublicKey = keyFactory.generatePublic(ecPublicKeySpec) as ECPublicKey

        /* Calculate shared secret */
        val keyAgreement = KeyAgreement.getInstance(keyAlg, "BC")
        keyAgreement.init(hostPrivateKey)
        keyAgreement.doPhase(ecdhCardPublicKey, true)

        val secret = keyAgreement.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(secret)
        digest.update(ecdhNonceBytes)
        val finalSecret = digest.digest()

        /* Prepare cipher */
        cipher = Cipher.getInstance(cipherAlg, "BC")
        secretKeySpec = SecretKeySpec(finalSecret, "AES")
        secureRandom = SecureRandom()
    }

    private fun createKeys() {
        val keyPair = ECController.instance?.createKeyPairHandshake() ?: throw Exception("No create key pair handshake")

        hostPublicKey = keyPair.public as ECPublicKey
        hostPrivateKey = keyPair.private as ECPrivateKey
        cipher = null
        secretKeySpec = null
    }

    private fun publicKey(): ByteArray {
        val command = APDUBuilder.init().setINS(Instructions.INS_GET_PUBLIC_KEY)
        val cmd = processCommand("GetPublicKey", command)

        return cmd
    }

    private fun privateKey(pinBytes: ByteArray): ByteArray {
        val _pin = byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes)

        val command = APDUBuilder.init().setINS(Instructions.INS_EXPORT_PRIVATE_KEY).setData(_pin)
        val cmd = processCommand("GetPrivateKey", command)
        return cmd
    }

    // TODO check when it needed
    override fun lock() {
        val command = APDUBuilder.init().setCLA(CLA_ISO7816).setINS(Instructions.INS_LOCK)
        processCommand("Lock", command)
        isUnlock = false
    }

    override fun activate(pin: String): Boolean {
        val pinBytes = pinGetBytes(pin)
        val _pin = byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes)

        val command = APDUBuilder.init().setINS(Instructions.INS_ACTIVATE).setData(_pin)

        Log.d("activate", command.toString())
        processCommand("Activate", command)
        return true // TODO FIX ME
    }

    override fun select(): String {
        isUnlock = false
        secureMessage = false
        aidsByte.forEachIndexed { index, aid ->
            try {
                val command = APDUBuilder
                    .init()
                    .setCLA(CLA_ISO7816)
                    .setINS(INS_SELECT)
                    .setP1(0x04.toByte())
                    .setP2(0x00.toByte())
                    .setData(aid)

                processCommand("Select", command)
                return Hex.toHexString(aid).also { result ->
                    selectedAID = ICardController.AIDs.values().find { it.aid.replace(":", "") == result.uppercase() }
//                    secureMessage = selectedAID?.security ?: false
                }
            } catch (e: Throwable) {
//                e.printStackTrace()
            }
        }

        throw Exception(SW_FILE_NOT_FOUND.toString())
    }

    override fun getSeries(): Int {
        try {
            val command = APDUBuilder
                .init()
                .setINS(Instructions.INS_GET_CARD_SERIES)

            val issue = processCommand("getSeries:", command)
            return ((issue[0].toShort() and 0xFF.toShort()).toInt().shl(8) + (issue[1].toShort() and 0xFF.toShort()))
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return 1
    }

    private fun encodeMsg(command: APDUBuilder): APDUBuilder {
        val iv = ByteArray(16)
        secureRandom = SecureRandom()
        secureRandom.nextBytes(iv)
        val ivParameterSpec = IvParameterSpec(iv)
        cipher!!.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        val cipheredData = cipher!!.doFinal(command.data)
        val buffer = ByteBuffer.allocate(iv.size + cipheredData.size)
        buffer.put(iv)
        buffer.put(cipheredData)

        return command.setData(buffer.array())
    }

    override fun doNeedActivation(): Boolean {
        val command = APDUBuilder.init().setINS(Instructions.INS_GET_STATE)
        val status = processCommand("isNotActivated:", command)

        return status.last() == State.INITED
    }

    override fun signDataEC(payload: String, pin: String): String? {
        if (isNFCPay()) return null
        try {
            val pinBytes = pinGetBytes(pin)
            unlock(pinBytes)
            val apduData = APDUDataBuilder.createSignData(pinBytes, Hex.decode(payload))
            val apduCommand = APDUBuilder.init().setINS(Instructions.INS_SIGN_DATA).setData(apduData)

            val fullSignResponse = processCommand("SignData", apduCommand)
            val hexSign = Hex.toHexString(fullSignResponse)
            if (!verify(payload, hexSign)) {
                throw Exception(CardErrors.SIGN_NO_VERIFY.toString())
            }
            lock()
            return hexSign
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun signDataED(publicKeyEDDSA: String, toSign: String, _pin: String): String? {
        if (isNFCPay()) return null
        try {

            val pin = pinGetBytes(_pin)
            if (!isUnlock) {
                unlock(pin)
            }

            val publicKey = Hex.decode(publicKeyEDDSA)
            val privateNonce = PointEncoder.privateNonce
            val publicNonce = PointEncoder.getPublicNonce(privateNonce)
            val payload = Hex.decode(toSign)

            val data = byteArrayOf(
                Tags.CARD_PIN, pin.size.toByte(), *pin,
                Tags.ED_CARD_PUBLIC_KEY_ENCODED, publicKey.size.toByte(), *publicKey,
                Tags.ED_PRIVATE_NONCE, privateNonce.size.toByte(), *privateNonce,
                Tags.ED_PUBLIC_NONCE, publicNonce.size.toByte(), *publicNonce,
                Tags.DATA_FOR_SIGN, payload.size.toByte(), *payload
            )
            val fullInfoSign = signDataED(data)
            val signedData = Hex.toHexString(fullInfoSign)
//            KeyStoreHelper.verify(Hex.decode(data), publicKeyObj(), Hex.decode(signedData))
//            lock()
            return signedData
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    @Throws(Exception::class)
    private fun signDataED(data: ByteArray): ByteArray {
        val command = APDUBuilder.init().setINS(Instructions.INS_ED_SIGN_DATA).setData(data)
        return processCommand("SignDataED", command)
    }

    override fun changePin(oldPin: String, newPin: String) {
        if (isNFCPay()) return

        val data = APDUDataBuilder.createChangePinData(oldPin, newPin)
        val command = APDUBuilder.init().setINS(Instructions.INS_CHANGE_PIN).setData(data)
        processCommand("ChangePin", command)
    }

    override fun getIssuer(): Int {
        val command = APDUBuilder.init().setINS(Instructions.INS_GET_CARD_ISSUER)
        val issue = processCommand("getIssue:", command)
        return ((issue[0].toShort() and 0xFF.toShort()).toInt().shl(8) + (issue[1].toShort() and 0xFF.toShort()))
    }

    override fun countPinAttempts(): Int {
        if (isNFCPay())
            return 0
        try {
            val command = APDUBuilder.init().setINS(Instructions.INS_GET_PIN_RETRIES)
            val cmd = processCommand("GetIncorrectPinCount", command)
            return cmd.lastOrNull()?.toInt() ?: 10
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return -1
    }

    override fun signDataNFC(data: ByteArray, isEDKey: Boolean): String? {
        kotlin.runCatching {
            val command = APDUBuilder.init().setINS(Instructions.INS_SIGN_DATA).setData(data)

            val fullInfoSign = processCommand("SignDataNFC:", command)
            val hSign = Hex.toHexString(fullInfoSign)
            if (isEDKey)
                verifyED(data, fullInfoSign)
            else
                verify(Hex.toHexString(data), hSign)

            lock()

            return hSign
        }.onFailure {
            it.printStackTrace()
        }
        return null
    }

    override fun setTransactionInfoForNFCPay(amount: BigDecimal, assetId: String, orderID: UUID) {
        if (isNFCPay()) {
            kotlin.runCatching {
                val assetIdBytes = assetId.toByteArray()
                val orderIdBytes = convertUUIDToBytes(UUID.fromString(orderID.toString()))
                val amountB = amount.toPlainString().toByteArray()

                val data = APDUBuilder
                    .init()
                    .setINS(Instructions.INS_TRANSACTION_INFO)
                    .setData(
                        byteArrayOf(
                            Tags.ASSET_ID,
                            assetIdBytes.size.toByte(),
                            *assetIdBytes,
                            Tags.AMOUNT_TX,
                            amountB.size.toByte(),
                            *amountB,
                            Tags.ORDER_ID,
                            orderIdBytes.size.toByte(),
                            *orderIdBytes
                        )
                    )

                processCommand("setTxInfo", data)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun isNFCPay(): Boolean {
        return selectedAID == ICardController.AIDs.AID_5
    }

    override fun statusTransaction(type: Int) {
        try {
            val command = APDUBuilder
                .init()
                .setINS(Instructions.INS_STATUS_TRANSACTION)
                .setData(byteArrayOf(type.toByte()))

            processCommand("StatusNFC", command)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun signDataByNonce(toSign: String, gatewaySignature: String): String? {
        if (isNFCPay()) return null
        try {
            val dataTLV = APDUDataBuilder.createSignByNonceData(Hex.decode(toSign), Hex.decode(gatewaySignature))
            val command = APDUBuilder.init().setCLA(CLA_ISO7816).setINS(Instructions.INS_SIGN_PROCESSING_DATA).setData(dataTLV)
            var signedDataArray = processCommand("SignProcessingDataNoPin", command)
            signedDataArray = signedDataArray.copyOfRange(VALUE_OFFSET.toInt(), signedDataArray.size)

//            if (!verify(Hex.toHexString(dataTLV), Hex.toHexString(signedDataArray))) {
//                listener.onCardError(ERROR_CODES.SIGN_NO_VERIFY)
//            }
            return Hex.toHexString(signedDataArray)
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }

    override fun unlock(pin: String) {
//        if (!isUnlock) {
//            if (!isNFCPay()) {
////                val time = measureTimeMillis {
//                unlock(pinGetBytes(pin))
////                }
////                Log.d("timeUnlcok", time.toString())
//            }
//            isUnlock = true
//        }
    }

    override fun unlock(pinBytes: ByteArray) {
        if (!isUnlock) {
            if (!isNFCPay()) {
                secureMessage = selectedAID?.security ?: false
                val command =APDUBuilder.init().setINS(Instructions.INS_UNLOCK).setData(byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes))
                processCommand("Unlock", command)
            }
            isUnlock = true
        }
    }

    override fun isUnlock(): Boolean {
        return isUnlock
    }

    private fun convertPrivateKeyToIOSStyle(private: String?): String? {
        private ?: return null
        val keypair = recoveryAndroid(private)
        return (keypair.private as ECPrivateKey).s.toString(16)
    }

    private fun recoveryAndroid(priv: String): KeyPair {
        val keyFactory = KeyFactory.getInstance("EC", "BC")
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(Hex.decode(priv))
        val privateKey = keyFactory.generatePrivate(privateKeySpec)

        val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val q: ECPoint = ecSpec.g.multiply((privateKey as org.bouncycastle.jce.interfaces.ECPrivateKey).d)

        val pubSpec = ECPublicKeySpec(q, ecSpec)
        val publicKeyGenerated = keyFactory.generatePublic(pubSpec) as java.security.interfaces.ECPublicKey

        return KeyPair(publicKeyGenerated, privateKey)
    }

    private fun convertBytesToUUID(bytes: ByteArray): UUID {
        val byteBuffer: ByteBuffer = ByteBuffer.wrap(bytes)
        val high: Long = byteBuffer.long
        val low: Long = byteBuffer.long
        return UUID(high, low)
    }

    private fun verifyED(data: ByteArray, signedData: ByteArray) {
        try {
            val signature = Signature.getInstance("Ed25519", "BC")
            signature.initVerify(publicKeyObj())
            signature.update(data)
            Log.d("VERIFY_ED: ", signature.verify(signedData).toString())
        } catch (e: Throwable) {
            e.printStackTrace()
            Log.d("VERIFY_ED", "No verify ed")
        }
    }

    private fun convertUUIDToBytes(uuid: UUID): ByteArray {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}
