package com.example.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sample.databinding.ActivityMainBinding
import tech.ammer.sdk.card.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val TAG = "#!MainActivity"
    private var cardController: ICardController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val listener = object : CardControllerListener {
            override fun onCardAttached() {
                TODO("Not yet implemented")
            }

            override fun onAppletSelected() {
                TODO("Not yet implemented")
            }

            override fun onAppletNotSelected(message: String) {
                TODO("Not yet implemented")
            }

            override fun tagDiscoverTimeout() {
                TODO("Not yet implemented")
            }
        }

        val listenerV2 = object : CardControllerListenerV2 {
            override fun onCardAttached(isActivated: Boolean, pubKey: String, cardController: CardControllerV2) {
                if (isActivated) {
                    val uuID = cardController.init("12345").getUUID()
                    Log.d(TAG, "$pubKey, $uuID")
                } else {
                    Log.d(TAG, "Card is not active")
                    cardController.activate("12345")
                    cardController.stopListening()
                }
            }

            //Optional
            override fun onStartListening() {
                Log.d(TAG, "startListener")
            }

            //Optional
            override fun onStopListening() {
                Log.d(TAG, "stopListener")
            }

            override fun onErrorAttach(errorCode: Int) {
                when (errorCode) {
                    ExceptionCode.NFC_NOT_FOUND -> Toast.makeText(this@MainActivity, "No NFC module", Toast.LENGTH_SHORT).show()
                }
                Log.e(TAG, errorCode.toString())
            }
        }

        cardController = TrustodyCardController.Builder(this)
            .addListener(listener)
            .addListenerV2(listenerV2)
            .setType(ReaderMode.ANDROID_DEFAULT)
            .setTimeoutListener(25000L)
            .build()

        binding.btnStart.setOnClickListener {
            cardController?.startListening() // TODO why i still need to use !!
        }
    }
}