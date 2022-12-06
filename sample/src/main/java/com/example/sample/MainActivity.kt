package com.example.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import tech.ammer.sdk.card.CardControllerFactory
import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.ICardController
import tech.ammer.sdk.card.ReaderMode
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

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
    override fun onAppletSelected() {
        Log.d("Applet", "attach ${Thread.currentThread()}")

        cardController?.select() //Required!!

        runOnUiThread {
            title?.text = "Processing.."
        }

        val isNotActivate = cardController?.isNotActivate()

        runOnUiThread {
            title?.text = "isActivated: ${isNotActivate == false}\nActivation..."
        }

        if (isNotActivate == true) {
            cardController?.activate(pin)
        }

        val uuid = cardController?.getCardUUID(pin)

//      Can be called only once
//      val pvkKey = cardController?.getPrivateKeyString(pin)

        val pubKey = cardController?.getPublicKeyString(pin)

        val newPin = "123456"
        cardController?.changePin(pin, newPin)

        val sign = cardController?.signData("bce6d58f7da6c3cd7239cbf5fcc0e323302ff072b20ecf59c501752c0e98906a", pin)

        runOnUiThread {
            title?.text = "uuid: $uuid\n\npubKey: $pubKey\n\nsign: $sign\n\n"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onAppletNotSelected(message: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.title).text = "Error: $message"
        }
    }

    override fun tagDiscoverTimeout() {
    }

    private fun changePin() {
        cardController?.changePin(pin, "11111")
    }
}