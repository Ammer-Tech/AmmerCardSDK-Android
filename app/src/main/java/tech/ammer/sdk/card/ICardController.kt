package tech.ammer.sdk.card

import android.app.Activity
import tech.ammer.sdk.card.apdu.APDUData

interface ICardController {

    @Throws(Exception::class)
    fun open(activity: Activity)

    fun getPublicKeyString(pin: String): String?

    fun signData(data: String, pin: String): String?

    @Throws(Exception::class)
    fun select()

    fun isNotActivate(): Boolean

    @Throws(Exception::class)
    fun signData(payload: ByteArray, pin: ByteArray): ByteArray

    companion object {
        const val AID = "70:6f:72:74:65:42:54:43"
    }
}