package com.example.sample

import com.example.sample.ingenico.common.NfcMan
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.annotation.WorkerThread
import com.example.sample.ingenico.common.DeviceHelper
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.util.encoders.Hex
import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.CardSDK
import tech.ammer.sdk.card.ICardController
import tech.ammer.sdk.card.apdu.CardErrors.SIGN_NO_VERIFY
import tech.ammer.sdk.card.apdu.CardErrors.SW_CONDITIONS_NOT_SATISFIED
import tech.ammer.sdk.card.apdu.CardErrors.SW_FILE_NOT_FOUND
import tech.ammer.sdk.card.apdu.CardErrors.SW_INS_NOT_SUPPORTED
import tech.ammer.sdk.card.apdu.CardErrors.SW_WRONG_DATA
import tech.ammer.sdk.card.apdu.CardErrors.SW_WRONG_P1P2
import tech.ammer.sdk.card.apdu.CardErrors.TAG_WAL_LOST
import java.math.BigDecimal
import java.util.UUID

class MainActivity : Activity(), CardControllerListener, DeviceHelper.ServiceReadyListener {

    private var cardController: ICardController? = null
    private val pin = "123456"
    private var title: TextView? = null
    private val toSignEC = "3a5cb9040a7088ec25fb4c1a8c18ce29882ea307df0201ba25ac52206ac77a5f"
    private val toSignECNonce = "f6eeee536ed83b1de03d484b1217896f5f276b8439c23cf8eabc882921d765f3"
    private val toSignED = "035c96b5f300f6ab60909a85fc124d24ec32a82e6187f6584c23cb1d3a80c50a"
    private val gatewaySignature =
        "3045022100c595b7b18c73a28024d3bf7c21e1880203ca6c760d06069feb3bafa795365952022048d5dd965f6a326bc7b38a26324efb6c1d9293597c5dcbf293dd76d9bcf664f0"
    private val gatewaySignatureNonce =
        "304402203326325d729c7ee0aac2953c100c337cc78af44739f98d359cc56a8362d6f378022072af536e2c6a233d5228d1b4c1985907eab1785428593e88e76e5081983db4c7"

    override fun onCreate(savedInstanceState: Bundle?) {
        DeviceHelper.me().setServiceListener(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = findViewById(R.id.title)


        findViewById<MaterialButton>(R.id.start).setOnClickListener {
            title?.text = "Started"
            NfcMan.nfcOn()
            cardController?.startListening()
        }

        findViewById<MaterialButton>(R.id.stop).setOnClickListener {
            NfcMan.doNfcOff()
            cardController?.stopListening()
            title?.text = "Stopped"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NfcMan.doNfcOff()
        DeviceHelper.me().unregister()
        DeviceHelper.me().setServiceListener(null)
        Log.d("Apdu", "destroy")
    }

    override fun onReady(version: String?) {
        Log.d("Nfc", "$version")
        DeviceHelper.me().register(true)

        NfcMan.start()
        cardController = CardSDK.getController(this)
        cardController?.open(this)
    }

    @SuppressLint("SetTextI18n")
    @WorkerThread
    override fun onCardAttached() {
        val aid = cardController?.select() //Required!!
        Log.d("uuid", "$aid")
        runOnUiThread {
            title?.text = "Processing.."
        }

//        CoroutineScope(Dispatchers.IO).launch {
        val needActivation = cardController?.doNeedActivation()
        if (needActivation == true) {
            runOnUiThread {
                title?.text = "Activation..."
            }
            cardController?.activate(pin)
        }

        val availablePinCount = cardController?.countPinAttempts().toString()
        val uuid = try {
            cardController?.getCardUUID(pin)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        val cardIssuer = cardController?.getIssuer().toString()
        val pubKey = cardController?.getPublicKeyECDSA(pin)
        val pvkKey = cardController?.getPrivateKey(pin)
        Thread.sleep(2000)
        val pubKeyED: String? = cardController?.getPublicKeyEDDSA(pin)

        var signEC: String? = null
        var signED: String? = null
        var signByNonceEC: String? = null

        if (cardController?.isNFCPay() == true) {
            cardController?.setTransactionInfoForNFCPay(BigDecimal("0.000001"), "AMR", UUID.randomUUID())

            signEC = cardController?.signDataNFC(Hex.decode(toSignEC), false)
//            signED = cardController?.signDataNFC(Hex.decode(toSignED), true)

            cardController?.lock() // show success on phone
            cardController?.rejectedTransaction() // show reject on phone
        } else {
//            val newPin = "123456"
//            cardController?.changePin(pin, newPin)

            signEC = cardController?.signDataEC(toSignEC, pin)
            signED = pubKeyED?.let { cardController?.signDataED(it, toSignED, pin) }
            Thread.sleep(2000)
            runCatching {
                signByNonceEC = cardController?.signDataByNonce(toSignECNonce, gatewaySignatureNonce)
            }.onFailure {
                it.printStackTrace()
            }
        }

        NfcMan.doNfcOff()
        cardController?.stopListening()

        runOnUiThread {
            Log.d("nfc", "run draw")
            title?.text = "aid:  $aid\n\n" +
                    "uuid: $uuid\n\n" +
                    "issuer: $cardIssuer\n\n" +
                    "countPinAttempts: $availablePinCount\n\n" +
                    "ecPubKey: $pubKey\n\n" +
                    "edPubKey: ${pubKeyED ?: "Not supported"}\n\n" +
                    "prvKey: $pvkKey\n\n" +
                    "signEC: $signEC\n\n" +
                    "signED: ${signED ?: "Not supported"}\n\n" +
                    "signNonceEC: ${signByNonceEC ?: "Not supported"}\n\n"
        }
//    }
//        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCardError(code: Short) {
        val message = when (code) {
            SW_CONDITIONS_NOT_SATISFIED -> "Condition not satisfied ${cardController?.isUnlock()} ,${cardController?.countPinAttempts()}"
            SW_WRONG_DATA -> "Bad sign data"
            SW_WRONG_P1P2 -> "Wrong PIN, Number of attempts:${cardController?.countPinAttempts()}"
            SW_FILE_NOT_FOUND, SW_INS_NOT_SUPPORTED -> "Card is blocked or the applet isn't found"
            TAG_WAL_LOST -> "The card was detached early"
            SIGN_NO_VERIFY -> "Sign not verify"
            else -> "Error: $code"
        }

        runOnUiThread {
            findViewById<TextView>(R.id.title).append("\nError: $message \n")
        }
    }
}