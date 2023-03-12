package com.example.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import tech.ammer.sdk.card.CardControllerFactory
import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.ICardController
import tech.ammer.sdk.card.ReaderMode
import tech.ammer.sdk.card.apdu.ERROR_CODES
import tech.ammer.sdk.card.apdu.ERROR_CODES.SIGN_NO_VERIFY
import tech.ammer.sdk.card.apdu.ERROR_CODES.SW_CONDITIONS_NOT_SATISFIED
import tech.ammer.sdk.card.apdu.ERROR_CODES.SW_FILE_NOT_FOUND
import tech.ammer.sdk.card.apdu.ERROR_CODES.SW_WRONG_DATA
import tech.ammer.sdk.card.apdu.ERROR_CODES.SW_WRONG_P1P2
import tech.ammer.sdk.card.apdu.ERROR_CODES.TAG_WAL_LOST

class MainActivity : Activity(), CardControllerListener {
    private var cardController: ICardController? = null
    private val pin = "123456"
    private var title: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = findViewById(R.id.title)

        cardController = CardControllerFactory().getController(ReaderMode.ANDROID_DEFAULT, this)
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

    /**
     * Card is attached
     */
    @SuppressLint("SetTextI18n")
    override fun onCardAttach() {
        cardController?.select() //Required!!

        runOnUiThread {
            title?.text = "Processing.."
        }

        val isNotActivated = cardController?.isNotActivated()
//        if (isNotActivated == true) {
//            runOnUiThread {
//                title?.text = "Activation..."
//            }
////            cardController?.activate(pin)
//        }
//        val availablePinCount = cardController?.getAvailablePinCount().toString()
//        val uuid = cardController?.getCardUUID(pin)
//        val cardIssuer = cardController?.getIssuer().toString()
//        val pubKey = cardController?.getPublicKeyString(pin)

//      Can be called only once
//      val pvkKey = cardController?.getPrivateKeyString(pin)

//        val newPin = "123456"
//        cardController?.changePin(pin, newPin)

        val sign = cardController?.signData("bce6d58f7da6c3cd7239cbf5fcc0e323302ff072b20ecf59c501752c0e98906a", pin)

        val toSign = "3a5cb9040a7088ec25fb4c1a8c18ce29882ea307df0201ba25ac52206ac77a5f"
        val gatewaySignature = "3045022100c595b7b18c73a28024d3bf7c21e1880203ca6c760d06069feb3bafa795365952022048d5dd965f6a326bc7b38a26324efb6c1d9293597c5dcbf293dd76d9bcf664f0"
        val signByNonce = cardController?.signDataByNonce(toSign, gatewaySignature)

        runOnUiThread {
            title?.text =
//                "uuid: $uuid\n\n" +
//                        "issuer: $cardIssuer\n\n" +
//                        "availablePinCount: $availablePinCount\n\n" +
//                        "pubKey: $pubKey\n\n" +
                "sign: $sign\n\n" +
                        "signNonce: ${signByNonce}\n\n"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCardError(code: Short) {
        val message = when (code) {
            SW_CONDITIONS_NOT_SATISFIED -> "Condition not satisfied ${cardController?.isUnlock()} ,${cardController?.getAvailablePinCount()}"
            SW_WRONG_DATA -> "Bad sign data"
            SW_WRONG_P1P2 -> "Wrong PIN, Number of attempts:${cardController?.getAvailablePinCount()}"
            SW_FILE_NOT_FOUND -> "Card is blocked or the applet isn't found"
            TAG_WAL_LOST -> "The card was detached early"
            SIGN_NO_VERIFY -> "Sign not verify"
            else -> "Error $code"
        }

        runOnUiThread {
            findViewById<TextView>(R.id.title).append("\nError: $message \n")
        }
    }
}