package tech.ammer.sdk.card

import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.nfc.tech.IsoDep
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.util.encoders.Hex
import tech.ammer.sdk.card.apdu.*
import timber.log.Timber
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.Signature
import java.util.*
import kotlin.experimental.and


class CardController(private val listener: CardControllerListener) : ReaderCallback {

    companion object {
        private const val AID = "70:6f:72:74:65:42:54:43"
        private const val CONNECT_TIMEOUT = 25000
        private var parameterSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    }

    private var isoDep: IsoDep? = null

    private val aidBytes: ByteArray

    init {
        val aidHex = AID.split(":").toTypedArray()
        aidBytes = ByteArray(aidHex.size)
        for (i in aidHex.indices) {
            aidBytes[i] = aidHex[i].toByte(16)
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        isoDep = IsoDep.get(tag)
        try {
            isoDep?.connect()
            isoDep?.timeout = CONNECT_TIMEOUT
            checkCardState()
            listener.onAppletSelected()
        } catch (e: Exception) {
            e.printStackTrace()
            listener.onAppletNotSelected(e.message)
        }
    }

    @Throws(Exception::class)
    private fun checkCardState() {

    }

    private fun pinGetBytes(pin: String): ByteArray {
        val pinBytes = ByteArray(pin.length)
        for (i in pin.indices) {
            pinBytes[i] = (pin[i].toInt() - 48).toByte()
        }
        return pinBytes
    }


    private fun publicKeyObj(): ECPublicKey {
        val w = publicKey()
        Timber.d("GET_PUB_KEY: ${w.toList()}")

        val cardKeySpec = ECPublicKeySpec(parameterSpec.curve.decodePoint(w), parameterSpec)
        val cardKey = KeyFactory.getInstance("EC", "BC").generatePublic(cardKeySpec) as ECPublicKey

        val fromCard = Hex.toHexString(cardKey.encoded)
        Timber.d("getPublicKeyObj: $fromCard")

        return cardKey
    }

    fun getPublicKeyString(pin: String): String? {
        try {
            val start = System.currentTimeMillis()
            unlock(pin)

            val pubKey = ECController.instance?.getPublicKeyString(publicKey())

            Timber.d(">>> getPublicKeyString: " + (System.currentTimeMillis() - start))
            return pubKey
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(Exception::class)
    private fun verify(data: String, signedData: String) {
        val signature = Signature.getInstance("NONEwithECDSA")

        signature.initVerify(publicKeyObj())
        signature.update(Hex.decode(data))
        Timber.d("VERIFY: " + signature.verify(Hex.decode(signedData)))
    }

    fun signData(data: String, pin: String): String? {
        try {
            val pinBytes = pinGetBytes(pin)
            //unlock(pinBytes);
            val apduData = APDUData
                .init()
                .setPIN(pinBytes)
                .setPayload(Hex.decode(data))

            Timber.d("Sign Data1 $data")
            Timber.d("Sign Data2 " + apduData.getBytes().toList().toString())

            val fullInfoSign = signData(apduData)
            val _sign = fullInfoSign.takeLast(fullInfoSign.size - 2)
            val signedDataHex = Hex.toHexString(_sign.toByteArray())
            verify(data, signedDataHex)
            lock()
            return signedDataHex
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(Exception::class)
    private fun processCommand(commandName: String, command: ByteArray): ByteArray {
        val result = isoDep!!.transceive(command)
        val resultLength = result.size
        val sw: Short = ByteBuffer.wrap(byteArrayOf(result[resultLength - 2], result[resultLength - 1])).short and 0xFFFF.toShort()

        if (sw != ISO7816.SW_NO_ERROR and 0xFFFF.toShort()) {
            throw Exception("" + sw)
        }
        return if (resultLength > 2) Arrays.copyOf(result, resultLength - 2) else byteArrayOf()
    }

    private fun publicKey(): ByteArray {
        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_GET_PUBLIC_KEY)
            .build()
        val cmd = processCommand("GetPublicKey", command)

        return cmd.takeLast(cmd.size - 2).toByteArray()
    }

    @Throws(Exception::class)
    fun lock() {
        val command = APDUBuilder.init().setCLA(ISO7816.CLA_ISO7816)
            .setINS(Instructions.INS_LOCK)
            .build()
        processCommand("Lock", command)
    }

    @Throws(Exception::class)
    fun select() {
        val command = APDUBuilder
            .init()
            .setCLA(ISO7816.CLA_ISO7816)
            .setINS(ISO7816.INS_SELECT)
            .setP1(0x04.toByte())
            .setP2(0x00.toByte())
            .setData(aidBytes)
            .build()
        processCommand("Select", command)
    }

    fun  isNotActivate(): Boolean {
            val command = APDUBuilder
                .init()
                .setINS(Instructions.INS_GET_STATE)
                .build()
            val status = processCommand("isNotActivated:", command)
            return status[2] == State.INITED
        }

    @Throws(Exception::class)
    fun signData(data: APDUData?): ByteArray {
        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_SIGN_DATA)
            .setData(data!!)
            .build()
        return processCommand("SignData", command)
    }

    @Throws(Exception::class)
    fun unlock(pin: ByteArray?) {
        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_UNLOCK)
            .setData(pin)
            .build()

        Timber.d(command.toList().toString())
        processCommand("Unlock", command)
    }

    @Throws(Exception::class)
    fun unlock(pin: String) {
        val pinBytes = pinGetBytes(pin)
        unlock(byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes))
    }

}