package com.example.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import androidx.annotation.WorkerThread
import com.google.android.material.button.MaterialButton
import org.bouncycastle.jce.provider.BouncyCastleProvider
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
import java.security.Security
import java.util.UUID
import kotlin.system.measureTimeMillis

class MainActivity : Activity(), CardControllerListener {


    /*
    NoSecure
    Cold,Hot
    unlock - 49, 47
    sign - 179, 152

    Secure v.3
    Cold,Hot
    unlock - 833, 151
    sign - 442, 215

    Secure v.4
    Cold,Hot
    unlock - 63, 151
    sign - 544, 215

    Secure v.2
    Cold,Hot
    unlock - 871, 159
    sign - 445, 187
     */
    private var cardController: ICardController? = null

    private val pin = "123456"
    //    private val pin = "012345"

    private var title: TextView? = null
    private val toSignEC = "3a5cb9040a7088ec25fb4c1a8c18ce29882ea307df0201ba25ac52206ac77a5f"
    private val toSignED = "035c96b5f300f6ab60909a85fc124d24ec32a82e6187f6584c23cb1d3a80c50a"
    private val gatewaySignature =
        "3045022100c595b7b18c73a28024d3bf7c21e1880203ca6c760d06069feb3bafa795365952022048d5dd965f6a326bc7b38a26324efb6c1d9293597c5dcbf293dd76d9bcf664f0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        setContentView(R.layout.activity_main)
        title = findViewById(R.id.title)

        cardController = CardSDK.getController(this)
        kotlin.runCatching {
            cardController?.open(this)
        }.onFailure {
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }

        findViewById<MaterialButton>(R.id.start).setOnClickListener {
            cardController?.startListening()
            title?.text = "Started"
        }

        findViewById<MaterialButton>(R.id.stop).setOnClickListener {
            cardController?.stopListening()
            title?.text = "Stopped"
        }
    }

    private var aid: String? = null
    private var needActivation: Boolean? = null
    private var availablePinCount: String? = null
    private var signEC: String? = null
    private var signED: String? = null
    private var signByNonceEC: String? = null
    private var pubKey: String? = null
    private var uuid: UUID? = null
    private var cardIssuer: String? = null
    private var series: String? = null
    private var pvkKey: String? = null
    private var pubKeyED: String? = null

    //1472
    // 432

    @SuppressLint("SetTextI18n")
    @WorkerThread
    override fun onCardAttached() {
        clearAllValue()
        val time = measureTimeMillis {
            aid = cardController?.select() //Required!!

//        runOnUiThread {
//            title?.text = "Processing.."
//        }

            needActivation = cardController?.doNeedActivation()
//            if (needActivation == true) {
//                runOnUiThread {
//                    title?.text = "Activation..."
//                }
//                cardController?.activate(pin)
//            }

            availablePinCount = cardController?.countPinAttempts().toString()
            uuid = cardController?.getCardUUID()
            cardIssuer = cardController?.getIssuer().toString()
//            series = cardController?.getSeries().toString()

            pubKey = cardController?.getPublicKeyECDSA(pin)
//            pvkKey = cardController?.getPrivateKey(pin)
//            pubKeyED = cardController?.getPublicKeyEDDSA(pin)
            if (cardController?.isNFCPay() == true) {
                val _time = measureTimeMillis {
                    cardController?.setTransactionInfoForNFCPay(BigDecimal("0.000001"), "AMR", UUID.randomUUID())
                    signEC = cardController?.signDataNFC(Hex.decode(toSignEC), false)
                }
                Log.d("timeSignNFC", _time.toString())
                signED = cardController?.signDataNFC(Hex.decode(toSignED), true)
//            cardController?.lock() // show success on phone
                cardController?.statusTransaction(0) // show reject on phone
            } else {
//            val newPin = "123456"
//            cardController?.changePin(pin, newPin)
                signEC = cardController?.signDataEC(toSignEC, pin)
//                signED = pubKeyED?.let { cardController?.signDataED(it, toSignED, pin) }

//            signByNonceEC = cardController?.signDataByNonce(toSignEC, gatewaySignature)
            }
        }

        Log.d("timeSignSCard", time.toString())

        setTextInfo()
    }

    private fun clearAllValue() {
        aid = null
        needActivation = null
        availablePinCount = null
        signEC = null
        signED = null
        signByNonceEC = null
        pubKey = null
        uuid = null
        cardIssuer = null
        pvkKey = null
        pubKeyED = null
    }

    @SuppressLint("SetTextI18n")
    private fun setTextInfo() {
        runOnUiThread {
            title?.text = "aid:  $aid\n\n" +
                    "uuid: $uuid\n\n" +
                    "issuer: $cardIssuer\n\n" +
                    "series: $series\n\n" +
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
        setTextInfo()
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