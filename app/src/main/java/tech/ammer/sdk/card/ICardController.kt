package tech.ammer.sdk.card

import android.app.Activity
import tech.ammer.sdk.card.apdu.APDUData

interface ICardController {

    fun open(activity: Activity)

    fun listenForCard()

    fun stopListening()

    fun close()

    fun getPublicKeyString(pin: String): String?

    fun signDataWallet(payload: String, pin: String): String?

    fun signDataPos(payload: String, pin: String): String?

    fun select()

    fun isNotActivate(): Boolean

    fun activate(pin: String): Boolean

    companion object {
        const val AID = "70:6f:72:74:65:42:54:43"
    }
}