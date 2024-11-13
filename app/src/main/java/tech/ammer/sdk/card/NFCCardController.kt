package tech.ammer.sdk.card

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
import tech.ammer.sdk.card.apdu.CardErrors.SW_FILE_NOT_FOUND
import tech.ammer.sdk.card.apdu.CardErrors.SW_NO_ERROR
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.HashMap
import kotlin.experimental.and


open class NFCCardController(private val listener: CardControllerListener) : ICardController {

    companion object {
        private var parameterSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val algr = "EC"
        private val cipherAlg = "AES/CBC/ISO7816-4Padding"
        private val keyAlg = "ECDH"
    }

    private var isUnlock: Boolean = false

    private var selectedAID: ICardController.AID? = null

    private val aidsByte = arrayListOf<ByteArray>()

    private var cipher: Cipher? = null
    private var secretKeySpec: SecretKeySpec? = null
    private var secureRandom = SecureRandom()

    private var hostPublicKey: ECPublicKey? = null
    private var hostPrivateKey: ECPrivateKey? = null

    init {
        createKeys()

        ICardController
            .AID
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
            val pubKey = ECController.getPublicKeyString(publicKey())
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
            val pubKey = ECController.getEDPublicKeyString(_key)

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
            return ECController.getPrivateKeyString(privKey)
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


    private fun processCommand(commandName: String, command: APDUBuilder): ByteArray {
        Log.d("Send Command:", "$commandName: ${command.build().toList()}")
        if (selectedAID?.isSecure == true && commandName != "Select") {
            createSecure()
            val cmdDecode = if (command.data.isEmpty()) command else encodeMsg(command)

            val responseByteArray = listener?.processCommand(cmdDecode.build())
            val response = ResponseAPDU(responseByteArray)
            Log.d("ResultCommand", "response(decoded): $commandName, ${response.sW.toShort()}, ${Hex.toHexString(responseByteArray)}")

            if (response.sW.toShort() != SW_NO_ERROR) {
                Log.e("ResultCommand", "Error: $commandName:${response.sW}")
                throw Exception("${response.sW}")
            }

            if (response.data.isNotEmpty()) {
                val _response = decodeMsg(response)

                Log.d("ResultCommand", "response(encoded): $commandName, ${Hex.toHexString(_response)}")
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
        val keyPair = ECController.createKeyPairHandshake()

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
            val command = APDUBuilder
                .init()
                .setCLA(ISO7816.CLA_ISO7816)
                .setINS(Instructions.INS_LOCK)

            kotlin.runCatching {
                processCommand("Lock", command)
            }.onFailure {
                it.printStackTrace()
            }
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
                    selectedAID = ICardController.AID.values().find { it.aid.replace(":", "") == result.uppercase() }
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

    override fun needActivation(): Boolean {
        val command = APDUBuilder.init().setINS(Instructions.INS_GET_STATE)
        val status = processCommand("isNotActivated:", command)

        return status.last() == State.INITED
    }

    private fun signDataEC(data: ByteArray, pin: String?): ByteArray {
        if (pin?.isEmpty() == true && !isRealDevice()) {
            throw Exception("Without a Pin, only for mobile NFC payments")
        }

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

        return processCommand("SignData", command)

    }

    private fun signDataED(toSign: ByteArray, pin: String?): ByteArray {
        if (pin?.isEmpty() == true && !isRealDevice()) {
            throw Exception("Without a Pin, only for mobile NFC payments")
        }

        val pinBytes = pinGetBytes(pin ?: "")

        if (!isUnlock) {
            unlock(pinBytes)
        }

        val data = byteArrayOf(
            Tags.CARD_PIN,
            pinBytes.size.toByte(),
            *pinBytes,
            *toSign
        )

        val command = APDUBuilder.init().setINS(Instructions.INS_ED_SIGN_DATA).setData(data)
        val response = processCommand("SignDataED", command)

//                verifyED(payload, response, publicKeyEDDSA)
        return response
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

    override fun getAID(): ICardController.AID {
        return selectedAID ?: run {
            listener.onAppletNotSelected("Early")
            ICardController.AID.AID_2
        }
    }

    private fun isRealDevice(): Boolean {
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


    override fun signData(data: HashMap<Int, ByteArray>, pin: String?, isECKey: Boolean): HashMap<Int, ByteArray>? {
        val resultMap = hashMapOf<Int, ByteArray>()

        try {
            data.forEach { (i, p) ->
                resultMap[i] = if (isECKey) signDataEC(p, pin) else signDataED(p, pin)
            }

            return resultMap
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return null
    }

    override fun signDataByNonce(
        data: HashMap<Int, ByteArray>,
        gatewaySignatures: HashMap<Int, ByteArray>,
        isECKey: Boolean
    ): HashMap<Int, ByteArray>? {
        try {

            val resultMap = hashMapOf<Int, ByteArray>()
            data.forEach { (i, p) ->
                val gatewaySignature = gatewaySignatures[i]!!
                resultMap[i] = if (isECKey) signDataByNonceEC(p, gatewaySignature)!! else signDataByNonceED(p, gatewaySignature)!!
            }

            return resultMap
        } catch (e: Throwable) {
            e.printStackTrace()
        }


        return null
    }

    override fun signData(data: ByteArray, pin: String?, isECKey: Boolean): ByteArray? {
        try {
            val result = if (isECKey) {
                signDataEC(data, pin)
            } else {
                signDataED(data, pin)
            }

            return result
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return null
    }

    override fun signDataByNonce(data: ByteArray, gatewaySignature: ByteArray, isECKey: Boolean): ByteArray? {
        try {
            return if (isECKey) {
                signDataByNonceEC(data, gatewaySignature)
            } else {
                signDataByNonceED(data, gatewaySignature)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return null
    }

    private fun signDataByNonceEC(data: ByteArray, gatewaySignature: ByteArray?): ByteArray? {
        if (isRealDevice())
            return null

        val tlvSign = TLVBuilder.init(Tags.DATA_FOR_SIGN, Tags.DATA_FOR_SIGN_LENGTH).build(data);
        val tlvSignature = TLVBuilder.init(Tags.DATA_SIGNATURE, gatewaySignature!!.size.toByte()).build(gatewaySignature)

        val buffer = ByteBuffer.allocate(tlvSign.size + tlvSignature.size)
        buffer.put(tlvSign)
        buffer.put(tlvSignature)

        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_SIGN_PROCESSING_DATA)
            .setData(buffer.array())

        val result = processCommand("SignProcessingDataNonceEC", command)
        return result
    }

    private fun signDataByNonceED(data: ByteArray, gatewaySignature: ByteArray): ByteArray? {
        if (isRealDevice())
            return null

        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_ED_SIGN_PROCESSING_DATA)
            .setData(
                byteArrayOf(*data, Tags.DATA_SIGNATURE, gatewaySignature.size.toByte(), *gatewaySignature)
            )

        val result = processCommand("SignProcessingDataNonceED", command)
        return result
    }

    override fun unlock(pin: String) {
        unlock(pinGetBytes(pin))
    }

    private fun unlock(pinBytes: ByteArray) {
        if (!isUnlock) {
            if (!isRealDevice()) {
                val command =
                    APDUBuilder.init().setINS(Instructions.INS_UNLOCK).setData(byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes))
                processCommand("Unlock", command)
            }
            isUnlock = true
        }
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