package com.example.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import org.bouncycastle.util.encoders.Hex
import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.CardSDK
import tech.ammer.sdk.card.ICardController
import tech.ammer.sdk.card.apdu.CardErrors.SW_CARD_BLOCK
import tech.ammer.sdk.card.apdu.CardErrors.SW_CONDITIONS_NOT_SATISFIED
import tech.ammer.sdk.card.apdu.CardErrors.SW_FILE_NOT_FOUND
import tech.ammer.sdk.card.apdu.CardErrors.SW_SECURITY_STATUS_NOT_SATISFIED
import java.math.BigDecimal
import java.util.UUID


class MainActivity : Activity(), CardControllerListener, NfcAdapter.ReaderCallback {

    private var isoDep: IsoDep? = null
    private var cardController: ICardController? = null

    private val pin = "123456"

    private var title: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = findViewById(R.id.title)

        cardController = CardSDK.getController(this)
        val nfc = NfcAdapter.getDefaultAdapter(this)
        if (!nfc.isEnabled) {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }

        findViewById<MaterialButton>(R.id.start).setOnClickListener {
            nfc?.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, null)
            title?.text = "Started"
        }

        findViewById<MaterialButton>(R.id.stop).setOnClickListener {
            nfc?.disableReaderMode(this)
            title?.text = "Stopped"
        }
    }

    override fun processCommand(byteArray: ByteArray): ByteArray? {
        return isoDep?.transceive(byteArray)
    }

    override fun onTagDiscovered(tag: Tag) {
        isoDep = IsoDep.get(tag)
        try {
            isoDep?.connect()
            isoDep?.timeout = 25000

            clearAllValue()
            runOnUiThread {
                title?.text = "Processing.."
            }

            doWork()
        } catch (e: Exception) {
            e.printStackTrace()
            onAppletNotSelected(e.message ?: "Tag not found.")
        }
    }

    private fun doWork() {
        val aid = cardController?.select()

        val needActivation = cardController?.needActivation()
        if (needActivation == true) {
            runOnUiThread {
                title?.text = "Activation..."
            }
            cardController?.activate(pin)
        }

        val countPinAttempts = cardController?.countPinAttempts().toString()
        val uuid = cardController?.getCardUUID()
        val cardIssuer = cardController?.getIssuer().toString()
        val series = cardController?.getSeries().toString()

        val pubKey = cardController?.getPublicKeyECDSA(pin)
        val pubKeyED = cardController?.getPublicKeyEDDSA(pin)

        resultMap["AID"] = aid.toString()
        resultMap["NeedActivation"] = needActivation.toString()
        resultMap["CountPinAttempts"] = countPinAttempts
        resultMap["UUID"] = uuid.toString()
        resultMap["CardIssuer"] = cardIssuer
        resultMap["Series"] = series
        resultMap["EC_PublicKey"] = pubKey.toString()

        val isRealDevice = cardController?.getAID()?.realDevice == true

        if (isRealDevice) {
            cardController?.setTransactionInfoForNFCPay(amount = BigDecimal("0.0005"), assetId = "AMR", orderID = UUID.randomUUID(), isEDKey = false)
            val sign_EC_NFC = cardController?.signData(toSignEC, null, true)
            val sign_ED_NFC = cardController?.signData(toSignED, null, false)

            resultMap["EC_Sign_NFC"] = Hex.toHexString(sign_EC_NFC)
            resultMap["ED_Sign_NFC"] = Hex.toHexString(sign_ED_NFC)
        } else {
            val pvkKey = cardController?.getPrivateKey(pin)

            val signEC = cardController?.signData(toSignEC, pin, true)
            val signED = cardController?.signData(toSignEDWithNonce, pin, false)

            val signByNonceEC = cardController?.signDataByNonce(toSignEC, gatewaySignature, true)
            val signByNonceED = cardController?.signDataByNonce(toSignEDNonce, gatewaySignatureED, false)

            val signByNonceECAnyInput = cardController?.signDataByNonce(toSignECAny, gatewaySignatureECAny, true)
            val signByNonceEDAnyInput = cardController?.signDataByNonce(toSignEDAny, gatewaySignatureEDAny, false)

            resultMap["EC_PrivateKey"] = pvkKey
            resultMap["ED_PublicKey"] = pubKeyED
            resultMap["EC_Sign"] = Hex.toHexString(signEC)
            resultMap["ED_Sign"] = Hex.toHexString(signED)
            resultMap["EC_SignByNonce"] = Hex.toHexString(signByNonceEC)
            resultMap["ED_SignByNonce"] = Hex.toHexString(signByNonceED)

            resultMap["EC_SignByNonceAnyInput"] = signByNonceECAnyInput?.map { "${it.key}:${Hex.toHexString(it.value)}" }?.toList().toString()
            resultMap["ED_SignByNonceAnyInput"] = signByNonceEDAnyInput?.map { "${it.key}:${Hex.toHexString(it.value)}" }?.toList().toString()
        }

        if (isRealDevice) {
            cardController?.statusTransaction(1) // show result status on phone (Only Phone)
        }

//        val newPin = "123456"
//        cardController?.changePin(pin, newPin)
//        cardController?.blockGetPrivateKey(pin) //!!!! It is used only once

        updateTextView()
    }

    @SuppressLint("SetTextI18n")
    override fun onAppletNotSelected(message: String?) {
        updateTextView()
        val error = when (message) {
            SW_SECURITY_STATUS_NOT_SATISFIED.toString(), SW_CONDITIONS_NOT_SATISFIED.toString() -> "Wrong PIN"
            SW_CARD_BLOCK.toString() -> "The Card is Blocked"
            "Tag was lost." -> "Please steadily hold your card near your phone NFC module"
            "Early" -> "The card was attach too early"
            SW_FILE_NOT_FOUND.toString() -> "This card isn't a Ammer Wallet or it is blocked"
            else -> message
        }

        runOnUiThread {
            findViewById<TextView>(R.id.title).append("\nError: $error \n")
        }
    }

    private fun clearAllValue() {
        resultMap.clear()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTextView() {
        runOnUiThread {
            title?.text = resultMap.map { "${it.key}:  ${it.value}\n\n" }.joinToString("\n")
        }
    }

    private val toSignECAny = hashMapOf(
        1 to Hex.decode("5e38ecc5990bdb21fb9a733a94967dd722d59bcc1d23ebbccdbdf5bf704d69e2"),
        2 to Hex.decode("5e38ecc5990bdb21fb9a733a94967dd722d59bcc1d23ebbccdbdf5bf704d69e2")
    )

    private val gatewaySignatureECAny = hashMapOf(
        1 to Hex.decode("3045022079ee236f02a6f83965093ecae1562f22990e812738c3461c987094857aa010be022100e8cb0737db78c76aaa832af63bb4fda7f1c523202e2851fe9aa7510c72610a31"),
        2 to Hex.decode("3045022079ee236f02a6f83965093ecae1562f22990e812738c3461c987094857aa010be022100e8cb0737db78c76aaa832af63bb4fda7f1c523202e2851fe9aa7510c72610a31")
    )
    private val gatewaySignatureEDAny = hashMapOf(
        1 to Hex.decode("3046022100ae48427b872bde3153534d56ce21f0b1dd17d889a3cccf82bb85ed5468e23d9c022100d8acb71f3455330b48fdd289f75b92feb0e798d6e5d9bda2f2354db3b8d8857a"),
        2 to Hex.decode("3046022100ae48427b872bde3153534d56ce21f0b1dd17d889a3cccf82bb85ed5468e23d9c022100d8acb71f3455330b48fdd289f75b92feb0e798d6e5d9bda2f2354db3b8d8857a")
    )

    private val toSignEDAny = hashMapOf(
        1 to Hex.decode("0d2015f0c371220d9b069792ffc8f01e4ba6f359beb406bb525c35fb57b46fe3974d0e2000af1ac9d753bdbc72a456d7fa0c500d320b15e284578fdb87c762409f301ba50f2061e62721d608b13488498f99190f37c2cd22ff2950e641b20cfb05b27e04b1a20a206ccdc70fcb7b0166d76cb5af1fe32a94b69daebb48b223083b9edade9c9ebf9d"),
        2 to Hex.decode("0d2015f0c371220d9b069792ffc8f01e4ba6f359beb406bb525c35fb57b46fe3974d0e2000af1ac9d753bdbc72a456d7fa0c500d320b15e284578fdb87c762409f301ba50f2061e62721d608b13488498f99190f37c2cd22ff2950e641b20cfb05b27e04b1a20a206ccdc70fcb7b0166d76cb5af1fe32a94b69daebb48b223083b9edade9c9ebf9d")
    )

    private val toSignEC = Hex.decode("5e38ecc5990bdb21fb9a733a94967dd722d59bcc1d23ebbccdbdf5bf704d69e2")
    private val toSignED = Hex.decode("891d0d2f431f7b437ac603f1b76954817f499ddd6bb8b85f5c64a6095b2c82a5")

    private val toSignEDWithNonce =
        Hex.decode("0d201f13016122122cf292ee1a073b7bdc25076a7baead8312334cc271e25cc6f29d0e20090825e5b78454da6855d4b0d566f3867624ae54bcded077670e19af5fec2ebc0f205f36fbddbb5bad193a059688b3bb075537e7d7655f41c6a83d24c330a8a7940d0a209c800ac43f7a5f713840f6650af65a980147f8c6ea80d61a5e6556bc2e88c9b2")
    private val gatewaySignature =
        Hex.decode("3045022079ee236f02a6f83965093ecae1562f22990e812738c3461c987094857aa010be022100e8cb0737db78c76aaa832af63bb4fda7f1c523202e2851fe9aa7510c72610a31")

    private val toSignEDNonce =
        Hex.decode("0d2015f0c371220d9b069792ffc8f01e4ba6f359beb406bb525c35fb57b46fe3974d0e200be84e30d4fcfb9d0701c0e56a408dd5c3a6085c12ce4f108911ae7c12169e9b0f206f85f1cfd931b05c3f797a4417da9e1a08d5d46cb6d710ef36872ecfe34d47b50a200d849a3947a3822a56b90fdd84fa31a2dda202bb99ad41e1bc37967ed470e850")
    private val gatewaySignatureED =
        Hex.decode("3046022100fa792843362461a77786615dea1a6f6b241a2c4caf6b6d515d81a8ecb7e94a0b022100d516bc59cd783f45884424affb017627637046b9658890771743fb7c7fb81690")

    private val resultMap = linkedMapOf<String, String?>()
}