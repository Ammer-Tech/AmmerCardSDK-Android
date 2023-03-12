package tech.ammer.sdk.card

import android.app.Activity
import java.util.*

interface ICardController {

    fun open(activity: Activity)

    fun close()

    fun startListening()

    fun stopListening()

    fun getPublicKeyString(pin: String): String?

    @SuppressWarnings("Can be called only once")
    fun getPrivateKeyString(pin: String): String?

    fun getCardUUID(pin: String): UUID?

    fun signData(payload: String, pin: String): String?

    fun signDataByNonce(toSign: String, gatewaySignature: String): String?

    fun select()

    fun isNotActivated(): Boolean

    fun activate(pin: String): Boolean

    fun changePin(oldPin: String, newPin: String)

    fun getAvailablePinCount(): Int

    fun getIssuer(): Int

    fun isUnlock(): Boolean

    companion object {
        val AIDs = arrayListOf("63:98:96:00:FF:00:01", "70:6f:72:74:65:42:54:43", "A0:00:00:08:82:00:01")
    }
}