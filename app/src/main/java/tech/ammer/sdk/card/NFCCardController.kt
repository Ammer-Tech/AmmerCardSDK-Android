package tech.ammer.sdk.card

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.util.encoders.Hex
import tech.ammer.sdk.card.ICardController.Companion.AIDs
import tech.ammer.sdk.card.apdu.*
import tech.ammer.sdk.card.apdu.ISO7816.Companion.SW_FILE_NOT_FOUND
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.Signature
import java.util.*
import kotlin.experimental.and

class NFCCardController(private val listener: CardControllerListener) :
    ReaderCallback,
    ICardController {

    companion object {
        private const val CONNECT_TIMEOUT = 25000
        private var parameterSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    }

    private var isUnlock: Boolean = false
    private var isoDep: IsoDep? = null
    private var nfc: NfcAdapter? = null
    private var activity: Activity? = null

    private val aidsByte = arrayListOf<ByteArray>()

    init {
        AIDs.forEachIndexed { index, it ->
            val aidHex = it.split(":").toTypedArray()
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
            listener.onAppletSelected()
        } catch (e: Exception) {
            e.printStackTrace()
            listener.onAppletNotSelected(e.message ?: "") //TODO FIX ME, add codes
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
        Log.d("listenForCard NfcAdapter is ", nfc.toString())
        nfc?.enableReaderMode(activity, this, NfcAdapter.FLAG_READER_NFC_A, null)
    }

    override fun stopListening() {
        Log.d("stopListening NfcAdapter is ", nfc.toString())
        nfc?.disableReaderMode(activity)
    }

    override fun close() {
        Log.d("close NfcAdapter is ", nfc.toString())
        activity = null
        isUnlock = false
    }

    override fun getCardUUID(pin: String): UUID {
        unlock(pin)

        val command: ByteArray = APDUBuilder
            .init()
            .setINS(Instructions.INS_GET_CARD_GUID)
            .build()

        val uuidCardBytes = processCommand("GetMeta", command)
        val buffer = ByteBuffer.allocate(8)
        buffer.put(uuidCardBytes, 2, 8)
        buffer.flip()

        val mostSigBits = buffer.long
        buffer.clear()
        buffer.put(uuidCardBytes, 10, 8)
        buffer.flip()

        val leastSigBits = buffer.long
        return UUID(mostSigBits, leastSigBits)
    }

    override fun getPublicKeyString(pin: String): String? {
        try {
            unlock(pin)
            val pubKey = ECController.instance?.getPublicKeyString(publicKey())
            return pubKey
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun getPrivateKeyString(pin: String): String? {
        kotlin.runCatching {
            unlock(pin)
            val privKey = privateKey(pin)
            blockGetPrivateKey(pin)
            return ECController.instance?.getPrivateKeyString(privKey)
        }.onFailure {
            it.printStackTrace()
        }

        return null
    }

    private fun blockGetPrivateKey(pin: String) {
        val pinBytes = pinGetBytes(pin)
        val _pin = byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes)

        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_DISABLE_PRIVATE_KEY_EXPORT)
            .setData(_pin)
            .build()

        processCommand("Block get private key", command)
    }

    private fun verify(data: String, signedData: String): Boolean {
        val signature = Signature.getInstance("NONEwithECDSA")
        signature.initVerify(publicKeyObj())
        signature.update(Hex.decode(data))
        return signature.verify(Hex.decode(signedData))
    }

    // TODO add card answer processing like if it [0x90,0x00] this is good and all other is error
    private fun processCommand(commandName: String, command: ByteArray): ByteArray {
        Log.d("processCommand($commandName)(lock:${!isUnlock})", command.toList().toString())
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

    private fun privateKey(pin: String): ByteArray {
        val pinBytes = pinGetBytes(pin)
        val _pin = byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes)

        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_EXPORT_PRIVATE_KEY)
            .setData(_pin)
            .build()

        val cmd = processCommand("GetPrivatKey", command)

        return cmd.takeLast(cmd.size - 2).toByteArray()
    }

    // TODO check when it needed
    private fun lock() {
        val command = APDUBuilder
            .init()
            .setCLA(ISO7816.CLA_ISO7816)
            .setINS(Instructions.INS_LOCK)
            .build()
        processCommand("Lock", command)
        isUnlock = false
    }

    override fun activate(pin: String): Boolean {
        val pinBytes = pinGetBytes(pin)
        val _pin = byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes)

        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_ACTIVATE)
            .setData(_pin)
            .build()

        Log.d("activate", command.toString())
        processCommand("Activate", command)
        return true // TODO FIX ME
    }


    override fun select() {
        aidsByte.forEachIndexed { index, aid ->
            try {
                val command = APDUBuilder
                    .init()
                    .setCLA(ISO7816.CLA_ISO7816)
                    .setINS(ISO7816.INS_SELECT)
                    .setP1(0x04.toByte())
                    .setP2(0x00.toByte())
                    .setData(aid)
                    .build()

                processCommand("Select", command)
//                return AIDs.get(index)
                return
            } catch (e: Throwable) {
                if (e.message != SW_FILE_NOT_FOUND.toString() || aidsByte.size - 1 == index) {
                    throw e
                }
            }
        }

    }

    override fun isNotActivate(): Boolean {
        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_GET_STATE)
            .build()
        val status = processCommand("isNotActivated:", command)

        return status[2] == State.INITED
    }

    override fun signData(payload: String, pin: String): String? {
        try {
            val pinBytes = pinGetBytes(pin)
            //unlock(pinBytes);
            val apduData = APDUData(Hex.decode(payload), pinBytes)

            val apduCommand = APDUBuilder
                .init()
                .setINS(Instructions.INS_SIGN_DATA)
                .setData(apduData)
                .build()

            val fullSignResponse = processCommand("SignData", apduCommand)
            val mySign = fullSignResponse.takeLast(fullSignResponse.size - 2)
            val hexSign = Hex.toHexString(mySign.toByteArray())
            if (!verify(payload, hexSign)) throw java.lang.Exception("Data was not verified payload $payload signedDataHex $hexSign")
            lock()
            return hexSign
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun unlock(pin: ByteArray?) {
        Log.d("unlock", "pin $pin")
        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_UNLOCK)
            .setData(pin)
            .build()

        processCommand("Unlock", command)
        isUnlock = true
    }

    override fun changePin(oldPin: String, newPin: String) {
        val data = byteArrayOf(Tags.CARD_PIN, oldPin.length.toByte(), *pinGetBytes(oldPin), Tags.CARD_PIN, newPin.length.toByte(), *pinGetBytes(newPin))

        val command = APDUBuilder
            .init()
            .setINS(Instructions.INS_CHANGE_PIN)
            .setData(data)
            .build()

        processCommand("ChangePin", command)
    }

    private fun unlock(pin: String) {
        if (isUnlock) return

        val pinBytes = pinGetBytes(pin)
        unlock(byteArrayOf(Tags.CARD_PIN, pinBytes.size.toByte(), *pinBytes))
    }
}
