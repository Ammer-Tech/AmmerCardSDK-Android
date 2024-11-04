package tech.ammer.sdk.card

import android.app.Activity
import android.nfc.TagLostException
import android.util.Log
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.internal.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import org.bouncycastle.util.encoders.Hex
import tech.ammer.sdk.card.apdu.*
import tech.ammer.sdk.card.apdu.CardErrors.SIGN_NO_VERIFY
import tech.ammer.sdk.card.apdu.CardErrors.SW_FILE_NOT_FOUND
import tech.ammer.sdk.card.apdu.CardErrors.SW_NO_ERROR
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and


class NFCCardController(private val listener: CardControllerListener) : ICardController {

    companion object {
        private var parameterSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")

        val algr = "EC"
        private val cipherAlg = "AES/CBC/ISO7816-4Padding"
        private val keyAlg = "ECDH"

        fun convertError(e: Exception): Short {
            return when {
                (e as? TagLostException) != null -> CardErrors.TAG_WAL_LOST
                (e as? NoSuchAlgorithmException) != null -> CardErrors.OTHER_ALGORITHM
                e.message?.toShortOrNull() != null -> e.message!!.toShort()
                else -> -1
            }
        }
    }

    private var isUnlock: Boolean = false
    private var activity: Activity? = null
    private var selectedAID: ICardController.AIDs? = null

    private val aidsByte = arrayListOf<ByteArray>()

    private var cipher: Cipher? = null
    var secretKeySpec: SecretKeySpec? = null
    var secureRandom = SecureRandom()

    private var hostPublicKey: ECPublicKey? = null
    private var hostPrivateKey: ECPrivateKey? = null

    init {
        createKeys()

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

    override fun close() {
//        Log.d("close NfcAdapter is ", nfc.toString())
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

            Log.d("", ">>> edPublicKeyStringHex: ${Hex.toHexString(_key)}")
            Log.d("", ">>> edPublicKeyStringHex(Short): $pubKey")

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

    override fun blockGetPrivateKey(pin: String) {
        val _pin = pinGetBytes(pin)
        unlock(_pin)
        val command = APDUBuilder.init().setINS(Instructions.INS_DISABLE_PRIVATE_KEY_EXPORT).setData(_pin)
        processCommand("Block get private key", command)
    }

    private fun verify(data: ByteArray, signedData: String): Boolean {
        val signature = Signature.getInstance("NONEwithECDSA")
        signature.initVerify(publicKeyObj())
        signature.update(data)
        val isVerify = signature.verify(Hex.decode(signedData))

        return isVerify
    }

    private var secureMessage = false

    private fun processCommand(commandName: String, command: APDUBuilder): ByteArray {
//        Log.d("Send Command:", "$commandName: ${command.build().toList()}")
        if (secureMessage) {
            createSecure()
            val cmdDecode = if (command.data.isEmpty()) command else encodeMsg(command)

            val ss = listener.processCommand(cmdDecode.build())
            val response = ResponseAPDU(ss)
//            Log.d("ResultCommand", "response(decoded): $commandName, ${response.sW.toShort()}, ${Hex.toHexString(ss)}")

            if (response.sW.toShort() != SW_NO_ERROR) {
//                Log.e("ResultCommand","Error: $commandName:${response.sW}")
                throw Exception("${response.sW}")
            }

            if (response.data.isNotEmpty()) {
                val _response = decodeMsg(response)

//                Log.d("ResultCommand", "response(encoded): $commandName, ${Hex.toHexString(_response)}")
                return if (_response.size > 2)
                    _response.copyOfRange(TLV.OFFSET_VALUE.toInt(), _response.size)
                else
                    _response
            }

            return response.data
        } else {
            val response = ResponseAPDU(listener.processCommand(command.build()))
            if (response.sW.toShort() != SW_NO_ERROR) {
                throw Exception("Error: $commandName:${response.sW}")
            }
            val data = response.data
            Log.d("Result Command:", "$commandName: ${data.toList()}")
            return if (data.size > 2)
                data.copyOfRange(TLV.OFFSET_VALUE.toInt(), data.size) else data
        }
    }

    private fun createSecure() {

        if (hostPublicKey == null || hostPrivateKey == null) {
            createKeys()
        }

        if (secretKeySpec == null || cipher == null) {
            val encoded = SecP256K1Curve().createPoint(
                hostPublicKey?.q?.affineXCoord?.toBigInteger(),
                hostPublicKey?.q?.affineYCoord?.toBigInteger()
            ).getEncoded(false)

            val tlv = TLVBuilder
                .init(Tags.CARD_PUBLIC_KEY, Tags.CARD_PUBLIC_KEY_LENGTH)
                .build(encoded)

            val cmd = APDUBuilder
                .init()
                .setINS(Instructions.INS_ECDH_HANDSHAKE)
                .setData(tlv)

            val rsp = ResponseAPDU(listener.processCommand(cmd.build()))
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
        val ecdhCardPublicKeyBytes =
            Arrays.copyOfRange(response.data, TLV.OFFSET_VALUE.toInt(), TLV.HEADER_BYTES_COUNT + Tags.CARD_PUBLIC_KEY_LENGTH)
        val ecdhNonceBytes =
            Arrays.copyOfRange(response.data, TLV.OFFSET_VALUE + TLV.HEADER_BYTES_COUNT + Tags.CARD_PUBLIC_KEY_LENGTH, response.data.size)

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

    override fun lock() {
        if (!isRealDevice()) {
            val command = APDUBuilder.init().setCLA(ISO7816.CLA_ISO7816).setINS(Instructions.INS_LOCK)
            processCommand("Lock", command)
            isUnlock = false
        }
    }

    override fun activate(pin: String): Boolean {
        val pinBytes = pinGetBytes(pin)
        val _pin = byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes)

        val command = APDUBuilder.init().setINS(Instructions.INS_ACTIVATE).setData(_pin)

        Log.d("activate", command.toString())
        processCommand("Activate", command)
        return true
    }

    override fun select(): String {
        isUnlock = false
        cipher = null

        aidsByte.forEachIndexed { index, aid ->
            try {
                val command = APDUBuilder
                    .init()
                    .setCLA(ISO7816.CLA_ISO7816)
                    .setINS(ISO7816.INS_SELECT)
                    .setP1(0x04.toByte())
                    .setP2(0x00.toByte())
                    .setData(aid)

                processCommand("Select", command)
                return Hex.toHexString(aid).also { result ->
                    selectedAID = ICardController.AIDs.values().find { it.aid.replace(":", "") == result.uppercase() }
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

    override fun signDataEC(payload: String, pin: String?): String? {
        if (pin?.isEmpty() == true && !isRealDevice()) {
            throw Exception("Without a Pin, only for mobile NFC payments")
        }

        try {
            val data = Hex.decode(payload)
            val _pinData = pinGetBytes(pin ?: "")

            val command = APDUBuilder
                .init()
                .setINS(Instructions.INS_SIGN_DATA)
                .setData(
                    byteArrayOf(
                        Tags.CARD_PIN,
                        _pinData.size.toByte(),
                        *_pinData,
                        Tags.DATA_FOR_SIGN,
                        data.size.toByte(),
                        *data
                    )
                )

            val fullSignResponse = processCommand("SignData", command)
            val hexSign = Hex.toHexString(fullSignResponse)
            if (!verify(data, hexSign)) {
                Log.d("VERIFY_EC: ", "verify EC false")
                throw Exception(SIGN_NO_VERIFY.toString())
            } else {
                Log.d("VERIFY_EC: ", "verify EC true")
            }

//                lock()
            return hexSign
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun signDataED(toSign: String, publicKeyEDDSA: String?, pin: String?): String? {
        try {
            if (isRealDevice()) {
                val payload = Hex.decode(toSign)

                val command = APDUBuilder.init().setINS(Instructions.INS_ED_SIGN_DATA).setData(payload)
                val response = processCommand("SignDataED", command)
                val signedData = Hex.toHexString(response)

                verifyED(payload, response, publicKeyEDDSA)
                return signedData
            } else {
                val pinBytes = pinGetBytes(pin!!)

                if (!isUnlock) {
                    unlock(pinBytes)
                }

                val publicKey = Hex.decode(publicKeyEDDSA!!)
                val privateNonce = PointEncoder.privateNonce
                val publicNonce = PointEncoder.getPublicNonce(privateNonce)
                val payload = Hex.decode(toSign)

                val data = byteArrayOf(
                    Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes,
                    Tags.ED_CARD_PUBLIC_KEY_ENCODED, publicKey.size.toByte(), *publicKey,
                    Tags.ED_PRIVATE_NONCE, privateNonce.size.toByte(), *privateNonce,
                    Tags.ED_PUBLIC_NONCE, publicNonce.size.toByte(), *publicNonce,
                    Tags.DATA_FOR_SIGN, payload.size.toByte(), *payload
                )

                val command = APDUBuilder.init().setINS(Instructions.INS_ED_SIGN_DATA).setData(data)
                val response = processCommand("SignDataED", command)
                val signedData = Hex.toHexString(response)

                verifyED(payload, response, publicKeyEDDSA)
                return signedData
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }


    override fun changePin(oldPin: String, newPin: String) {
        if (isRealDevice()) return

        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_CHANGE_PIN)
            .setData(
                byteArrayOf(
                    Tags.CARD_PIN,
                    oldPin.length.toByte(),
                    *pinGetBytes(oldPin),
                    Tags.CARD_PIN,
                    newPin.length.toByte(),
                    *pinGetBytes(newPin)
                )
            )

        processCommand("ChangePin", command)
    }

    override fun getIssuer(): Int {
        val command = APDUBuilder.init().setINS(Instructions.INS_GET_CARD_ISSUER)
        val issue = processCommand("getIssue:", command)
        return ((issue[0].toShort() and 0xFF.toShort()).toInt().shl(8) + (issue[1].toShort() and 0xFF.toShort()))
    }

    override fun countPinAttempts(): Int {
        if (isRealDevice())
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

    private fun signDataNFC(apdu: APDUBuilder, isEDKey: Boolean): String? {
        kotlin.runCatching {
            val command = APDUBuilder.init().setINS(Instructions.INS_SIGN_DATA).setData(apdu.data)

            val fullInfoSign = processCommand("SignDataNFC:", command)
            val hSign = Hex.toHexString(fullInfoSign)
            if (isEDKey)
                verifyED(apdu.data, fullInfoSign)
            else
                verify(apdu.data, hSign)

            lock()

            return hSign
        }.onFailure {
            it.printStackTrace()
        }
        return null
    }

    override fun setTransactionInfoForNFCPay(amount: BigDecimal, assetId: String, orderID: UUID, isEDKey: Boolean) {
        if (isRealDevice()) {
            kotlin.runCatching {
                val assetIdBytes = assetId.toByteArray()
                val orderIdBytes = convertUUIDToBytes(UUID.fromString(orderID.toString()))
                val amountB = amount.toPlainString().toByteArray()
                val isEdKeyBytes = isEDKey.toString().toByteArray()

                val data = APDUBuilder
                    .init()
                    .setINS(Instructions.INS_TRANSACTION_INFO)
                    .setData(
                        byteArrayOf(
                            Tags.ASSET_ID, assetIdBytes.size.toByte(), *assetIdBytes,
                            Tags.AMOUNT_TX, amountB.size.toByte(), *amountB,
                            Tags.ORDER_ID, orderIdBytes.size.toByte(), *orderIdBytes,
                            Tags.IS_ED_KEY, isEdKeyBytes.size.toByte(), *isEdKeyBytes,
                        )
                    )

                processCommand("setTxInfo", data)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun isRealDevice(): Boolean {
        return selectedAID?.realDevice == true
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

    override fun signDataByNonceEC(data: String, gatewaySignature: String): String? {
        if (isRealDevice())
            return null

        try {
            val toSign = Hex.decode(data)
            val _gatewaySignature = Hex.decode(gatewaySignature)

            val tlvSign = TLVBuilder.init(Tags.DATA_FOR_SIGN, Tags.DATA_FOR_SIGN_LENGTH).build(toSign);
            val tlvSignature = TLVBuilder.init(Tags.DATA_SIGNATURE, _gatewaySignature.size.toByte()).build(_gatewaySignature)

            val buffer = ByteBuffer.allocate(tlvSign.size + tlvSignature.size)
            buffer.put(tlvSign)
            buffer.put(tlvSignature)

            val command = APDUBuilder
                .init()
                .setINS(Instructions.INS_SIGN_PROCESSING_DATA)
                .setData(buffer.array())

            val signedDataArray = processCommand("SignProcessingDataNonceEC", command)
            if (!verify(toSign, Hex.toHexString(signedDataArray))) {
                Log.d("Verify", "Verify nonce false")
                listener.onCardError(SIGN_NO_VERIFY)
            } else {
                Log.d("Verify", "Verify nonce true")
            }
            return Hex.toHexString(signedDataArray)
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }

    override fun signDataByNonceED(data: String, gatewaySignature: String, publicKeyED: ByteArray): String? {
        if (isRealDevice())
            return null
        try {

            val dataSignature = Hex.decode(data)
            val payload = Hex.decode(gatewaySignature)

            val privateNonce: ByteArray = PointEncoder.privateNonce
            val publicNonce: ByteArray = PointEncoder.getPublicNonce(privateNonce)

            val dataBuffer = ByteBuffer.allocate(
                TLV.HEADER_BYTES_COUNT + Tags.ED_CARD_PUBLIC_KEY_ENCODED_LENGTH +
                        TLV.HEADER_BYTES_COUNT + Tags.ED_PRIVATE_NONCE_LENGTH +
                        TLV.HEADER_BYTES_COUNT + Tags.ED_PUBLIC_NONCE_LENGTH +
                        TLV.HEADER_BYTES_COUNT + Tags.DATA_FOR_SIGN_LENGTH +
                        TLV.HEADER_BYTES_COUNT + payload.size
            )

            dataBuffer.put(TLVBuilder.init(Tags.ED_CARD_PUBLIC_KEY_ENCODED, Tags.ED_CARD_PUBLIC_KEY_ENCODED_LENGTH).build(publicKeyED))
            dataBuffer.put(TLVBuilder.init(Tags.ED_PRIVATE_NONCE, Tags.ED_PRIVATE_NONCE_LENGTH).build(privateNonce))
            dataBuffer.put(TLVBuilder.init(Tags.ED_PUBLIC_NONCE, Tags.ED_PUBLIC_NONCE_LENGTH).build(publicNonce))
            dataBuffer.put(TLVBuilder.init(Tags.DATA_FOR_SIGN, Tags.DATA_FOR_SIGN_LENGTH).build(dataSignature))
            dataBuffer.put(TLVBuilder.init(Tags.DATA_SIGNATURE, payload.size.toByte()).build(payload))

            val cmd = APDUBuilder.init().setINS(Instructions.INS_ED_SIGN_PROCESSING_DATA).setData(dataBuffer.array())

            val signedDataArray = processCommand("SignProcessingDataNonceED", cmd)

            return Hex.toHexString(signedDataArray)
        } catch (e: Throwable) {
            e.printStackTrace()

            return null
        }
    }

    override fun unlock(pin: String) {
        unlock(pinGetBytes(pin))
    }

    private fun unlock(pinBytes: ByteArray) {
        if (!isUnlock) {
            if (!isRealDevice()) {
                secureMessage = selectedAID?.security ?: false
                val command =
                    APDUBuilder.init().setINS(Instructions.INS_UNLOCK).setData(byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes))
                processCommand("Unlock", command)
            }
            isUnlock = true
        }
    }

    override fun isUnlock(): Boolean {
        return isUnlock
    }

    private fun verifyED(data: ByteArray, signedData: ByteArray, publicKey: String? = null) {
        try {
            val keyFactory = KeyFactory.getInstance("Ed25519", "BC")
            val subjectPublicKeyInfo = SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), Hex.decode(publicKey))
            val _publicKey = keyFactory.generatePublic(X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded()))

            val signature = Signature.getInstance("Ed25519", "BC")
            signature.initVerify(_publicKey)
            signature.update(data)
            Log.d("VERIFY_ED: ", "Verify ed " + signature.verify(signedData).toString())
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