package tech.ammer.sdk.card

import android.app.Activity
import java.util.*

interface ICardController {

    fun open(activity: Activity)
    fun close()

    fun startListening()

    fun stopListening()

    fun getPublicKeyString(pin: String): String?

    /**
     *  Can be called only once
     */
    fun getPrivateKeyString(pin: String): String?

    fun getCardUUID(pin: String): UUID?

    fun signData(payload: String, pin: String): String?

    fun select()

    fun isNotActivate(): Boolean

    fun activate(pin: String): Boolean

    fun changePin(oldPin: String, newPin: String)

    companion object {
        const val AID = "70:6f:72:74:65:42:54:43"
    }
}