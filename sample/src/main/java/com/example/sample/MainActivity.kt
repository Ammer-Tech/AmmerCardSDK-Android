package com.example.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.CardSDK
import tech.ammer.sdk.card.ICardController
import tech.ammer.sdk.card.NFCCardController
import tech.ammer.sdk.card.apdu.CardErrors.SIGN_NO_VERIFY
import tech.ammer.sdk.card.apdu.CardErrors.SW_CONDITIONS_NOT_SATISFIED
import tech.ammer.sdk.card.apdu.CardErrors.SW_FILE_NOT_FOUND
import tech.ammer.sdk.card.apdu.CardErrors.SW_INS_NOT_SUPPORTED
import tech.ammer.sdk.card.apdu.CardErrors.SW_WRONG_DATA
import tech.ammer.sdk.card.apdu.CardErrors.SW_WRONG_P1P2
import tech.ammer.sdk.card.apdu.CardErrors.TAG_WAL_LOST
import tech.ammer.sdk.card.pax.PaxEventListener
import tech.ammer.sdk.card.pax.PaxInterface
import tech.ammer.sdk.card.pax.PaxWrapper
import tech.ammer.sdk.card.pax.ReaderWrapper
import java.math.BigDecimal
import java.util.UUID

class MainActivity : Activity(), CardControllerListener, PaxEventListener {
    private var cardController: ICardController? = null

    private val pin = "123456"

    private var title: TextView? = null
    private val toSignEC = "5e38ecc5990bdb21fb9a733a94967dd722d59bcc1d23ebbccdbdf5bf704d69e2"
    private val toSignED = "891d0d2f431f7b437ac603f1b76954817f499ddd6bb8b85f5c64a6095b2c82a5"
    private val toSignEDNonce = "d40d0c3b7b691babe3fee25655e4e78d6149440d61e6009a55fef39952bb5f1c"

    private val gatewaySignature =
        "3045022079ee236f02a6f83965093ecae1562f22990e812738c3461c987094857aa010be022100e8cb0737db78c76aaa832af63bb4fda7f1c523202e2851fe9aa7510c72610a31"
    private val gatewaySignatureED =
        "304402207e3ce81f3e3abf68e959ef7be77f985c2dea30a68a154d13027aaddcd258558102206b22058e3a276330d8393e8af26c364cc9b9788b5e60457798b62d24676124f5"

    private val resultMap = linkedMapOf<String, String?>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = findViewById(R.id.title)

        cardController = CardSDK.getController(this)
        PaxWrapper.setEventListener(this)

        findViewById<MaterialButton>(R.id.start).setOnClickListener {
            PaxWrapper.enable()
            title?.text = "Started"
        }

        findViewById<MaterialButton>(R.id.stop).setOnClickListener {
            PaxWrapper.destroy()
            title?.text = "Stopped"
        }
    }

    private fun doWork() {
        println("start: ${Thread.currentThread()}")

        runOnUiThread {
            title?.text = "Processing..."
        }

        val aid = cardController?.select()
        val needActivation = cardController?.needActivation()
        if (needActivation == true) {
            runOnUiThread {
                title?.text = "Activation..."
            }

            cardController?.activate(pin)
        }

        val uuid = cardController?.getCardUUID()
        val cardIssuer = cardController?.getIssuer().toString()
        val availablePinCount = cardController?.countPinAttempts().toString()
        val series = cardController?.getSeries().toString()
        val pubKey = cardController?.getPublicKeyECDSA(pin)
        val pubKeyED = cardController?.getPublicKeyEDDSA(pin)

        resultMap["AID"] = aid.toString()
        resultMap["NeedActivation"] = needActivation.toString()
        resultMap["AvailablePinCount"] = availablePinCount
        resultMap["UUID"] = uuid.toString()
        resultMap["CardIssuer"] = cardIssuer
        resultMap["Series"] = series
        resultMap["EC_PublicKey"] = pubKey.toString()

        val isRealDevice = cardController?.isRealDevice() ?: false
        if (isRealDevice) {
            cardController?.setTransactionInfoForNFCPay(amount = BigDecimal("0.0005"), assetId = "AMR", orderID = UUID.randomUUID(), isEDKey = false)
            val sign_EC_NFC = cardController?.signDataEC(toSignEC, null)
            val sign_ED_NFC = cardController?.signDataED(toSignED, null, null)

            resultMap["EC_Sign_NFC"] = sign_EC_NFC.toString()
            resultMap["ED_Sign_NFC"] = sign_ED_NFC.toString()
        } else {
            val pvkKey = cardController?.getPrivateKey(pin)
            val signEC = cardController?.signDataEC(toSignEC, pin)
            val signED = cardController?.signDataED(toSignED, pubKeyED, pin)
            val signByNonceEC = cardController?.signDataByNonceEC(toSignEC, gatewaySignature)
            val signByNonceED = cardController?.signDataByNonceED(toSignEDNonce, gatewaySignatureED)

            resultMap["EC_PrivateKey"] = pvkKey.toString()
            resultMap["ED_PublicKey"] = pubKeyED.toString()
            resultMap["EC_Sign"] = signEC.toString()
            resultMap["ED_Sign"] = signED.toString()
            resultMap["EC_SignByNonce"] = signByNonceEC.toString()
            resultMap["ED_SignByNonce"] = signByNonceED.toString()
        }

        if (isRealDevice) {
            cardController?.statusTransaction(1) // show result status on phone (Only Phone)
        }

//        val newPin = "123456"
//        cardController?.changePin(pin, newPin)
//        cardController?.blockGetPrivateKey(pin) //!!!! It is used only once


        updateTextView()
    }

    private suspend fun testThread() {
        println("MyT0 ${Thread.currentThread()}")
//        CoroutineScope(Dispatchers.IO).launch {
//        PaxWrapper.getScopeNfc()?.launch {
//            withContext(Dispatchers.Main) {
//                title?.append("processing...")
//            }
        println("MyT1 ${Thread.currentThread()}")

        cardController?.select()
        val uuid = cardController?.getCardUUID()
        println("u $uuid")

        val cardIssuer = cardController?.getIssuer().toString()
        println("c $cardIssuer")

        val availablePinCount = cardController?.countPinAttempts().toString()
        println("a $availablePinCount")

//        CoroutineScope(Dispatchers.IO).async {
//            println("Thread start")
//            delay(3000)
//        }.await()
//        println("Thread finish")

        val signByNonceED = cardController?.signDataByNonceED(toSignEDNonce, gatewaySignatureED)
        println("s $signByNonceED")

        println()
        PaxWrapper.pause()
        withContext(Dispatchers.IO) {
            println("Thread start")
            delay(6000)
        }

        PaxWrapper.enable()
        cardController?.select()
        val pubKeyED = cardController?.getPublicKeyEDDSA(pin)
        println("a $pubKeyED")

        withContext(Dispatchers.IO) {
            println("Thread start")
            delay(2000)
        }

        val dd = cardController?.signDataED(toSignED, pubKeyED, pin)
        println("sPas $dd")
        println("Thread finish")

        PaxWrapper.pause()
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

    override fun processCommand(byteArray: ByteArray): ByteArray? {
        Log.d("Wrap", "${Thread.currentThread()}")
        return rWrapper?.processCommand(byteArray)
    }

    @SuppressLint("SetTextI18n")
    override fun onCardError(code: Short) {
        updateTextView()
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

    private var rWrapper: ReaderWrapper? = null

    override suspend fun cardAttached(iccWrapper: ReaderWrapper, paxInterfaceContact: PaxInterface) {
        try {
            this.rWrapper = iccWrapper
//            doWork()
            testThread()
        } catch (e: Exception) {
            e.printStackTrace()
            onCardError(NFCCardController.convertError(e))
        }
    }


    override fun cardDetached(paxInterfaceContact: PaxInterface?) {
        rWrapper = null
    }


    val CONNECT_TIMEOUT = 25000
}