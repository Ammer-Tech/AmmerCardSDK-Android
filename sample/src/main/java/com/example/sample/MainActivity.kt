package com.example.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.WorkerThread
import com.google.android.material.button.MaterialButton
import org.bouncycastle.util.encoders.Hex
import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.CardSdk
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

class MainActivity : Activity(), CardControllerListener {

    private var cardController: ICardController? = null
    private val pin = "123456"
    private var title: TextView? = null
    private val toSignEC = "3a5cb9040a7088ec25fb4c1a8c18ce29882ea307df0201ba25ac52206ac77a5f"
    private val toSignED = "035c96b5f300f6ab60909a85fc124d24ec32a82e6187f6584c23cb1d3a80c50a"
    private val gatewaySignature =
        "3045022100c595b7b18c73a28024d3bf7c21e1880203ca6c760d06069feb3bafa795365952022048d5dd965f6a326bc7b38a26324efb6c1d9293597c5dcbf293dd76d9bcf664f0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = findViewById(R.id.title)

        cardController = CardSdk.getController(this)
        cardController?.open(this)

        findViewById<MaterialButton>(R.id.start).setOnClickListener {
            title?.text = "Started"
            cardController?.startListening()
        }

        findViewById<MaterialButton>(R.id.stop).setOnClickListener {
            cardController?.stopListening()
            title?.text = "Stopped"
        }
    }

    @SuppressLint("SetTextI18n")
    @WorkerThread
    override fun onCardAttached() {
        val aid = cardController?.select() //Required!!

        runOnUiThread {
            title?.text = "Processing.."
        }

        val needActivation = cardController?.doNeedActivation()
        if (needActivation == true) {
            runOnUiThread {
                title?.text = "Activation..."
            }
            cardController?.activate(pin)
        }

        val availablePinCount = cardController?.countPinAttempts().toString()
        val uuid = cardController?.getCardUUID(pin)
        val cardIssuer = cardController?.getIssuer().toString()
        val pubKey = cardController?.getPublicKeyECDSA(pin)
        val pvkKey = cardController?.getPrivateKey(pin)
        val pubKeyED: String? = cardController?.getPublicKeyEDDSA(pin)

        val signEC: String?
        val signED: String?
        var signByNonceEC: String? = null

        if (cardController?.isNFCPay() == true) {
            cardController?.setTransactionInfoForNFCPay(BigDecimal("0.000001"), "AMR", UUID.randomUUID())

            signEC = cardController?.signDataNFC(Hex.decode(toSignEC), false)
            signED = cardController?.signDataNFC(Hex.decode(toSignED), true)

            cardController?.lock() // show success on phone
            cardController?.rejectedTransaction() // show reject on phone
        } else {
//            val newPin = "123456"
//            cardController?.changePin(pin, newPin)

            signEC = cardController?.signDataEC(toSignEC, pin)
            signED = pubKeyED?.let { cardController?.signDataED(it, toSignED, pin) }

            signByNonceEC = cardController?.signDataByNonce(toSignEC, gatewaySignature)
        }

        runOnUiThread {
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