package com.example.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import tech.ammer.sdk.card.CardControllerFactory
import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.ICardController
import tech.ammer.sdk.card.ReaderMode

class MainActivity : Activity(), CardControllerListener {
    private var cardController: ICardController? = null
    private val pin = "123456"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardController = CardControllerFactory().getController(ReaderMode.ANDROID_DEFAULT, this)
        cardController?.open(this)

        findViewById<MaterialButton>(R.id.start).setOnClickListener {
            findViewById<TextView>(R.id.title).text = "Started"
            cardController?.startListening()
        }

        findViewById<MaterialButton>(R.id.stop).setOnClickListener {
            cardController?.stopListening()
            findViewById<TextView>(R.id.title).text = "Stopped"
        }
    }

    /**
     * Card is attached
     */
    @SuppressLint("SetTextI18n")
    override fun onAppletSelected() {
        cardController?.select() // Required!!

        runOnUiThread {
            findViewById<TextView>(R.id.title).text = "Processing.."
        }

        runOnUiThread {
            findViewById<TextView>(R.id.title).text = "isActivate: ${!cardController?.isNotActivate()!!}\n"
        }

        if (cardController?.isNotActivate() == true) {
            Toast.makeText(this, "Activate the card", Toast.LENGTH_SHORT).show()
//            activate("12345")
            return
        }

        val privateKey = cardController?.getPrivateKeyString(pin)
        val pubKey = cardController?.getPublicKeyString(pin)

        if (pubKey == null) {
            Toast.makeText(this, "Wrong pin", Toast.LENGTH_SHORT).show()
        }

        val uuid = cardController?.getCardUUID(pin)
        val sign = cardController?.signData("bce6d58f7da6c3cd7239cbf5fcc0e323302ff072b20ecf59c501752c0e98906a", pin)

        runOnUiThread {
            findViewById<TextView>(R.id.title).append("\n uuidCard - $uuid\n\npubKey - $pubKey\n\nprivate key - $privateKey\n\nsign - $sign")
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onAppletNotSelected(message: String) {
        findViewById<TextView>(R.id.title).text = "Error: $message"
    }

    override fun tagDiscoverTimeout() {
    }



    private fun activate(pin: String) {
        cardController?.activate(pin)
        val pubKey = cardController?.getPublicKeyString(pin)
        val uuid = cardController?.getCardUUID(pin)

        runOnUiThread {
            findViewById<TextView>(R.id.title).text = "New card\nuuidCard - $uuid\n\npubKey - $pubKey"
        }
    }

    private fun changePin() {
        cardController?.changePin(pin, "11111")
    }
}