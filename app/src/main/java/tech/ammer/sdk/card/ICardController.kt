package tech.ammer.sdk.card

import android.app.Activity
import java.util.*

interface ICardController {

    fun open(activity: Activity)

    fun close()

    fun startListening()

    fun stopListening()

    fun getPublicKeyECDSA(pin: String): String?
    fun getPublicKeyEDDSA(pin: String): String?

    fun getPrivateKey(pin: String): String?

    fun getCardUUID(pin: String): UUID?
    fun blockCard(pin: String)

    fun signDataEC(payload: String, pin: String): String?
    fun signDataED(publicKeyEDDSA: String, toSign: String, pin: String): String?

    fun signDataByNonce(toSign: String, gatewaySignature: String): String?

    fun select(): String

    fun doNeedActivation(): Boolean

    fun activate(pin: String): Boolean

    fun changePin(oldPin: String, newPin: String)

    fun countPinAttempts(): Int

    fun getIssuer(): Int

    fun isUnlock(): Boolean

    companion object {
        val AIDs = arrayListOf("63:98:96:00:FF:00:01", "70:6f:72:74:65:42:54:43", "A0:00:00:08:82:00:01", "A0:00:00:08:82:00:02")
    }
}