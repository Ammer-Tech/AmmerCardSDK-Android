package tech.ammer.sdk.card

import android.app.Activity
import java.math.BigDecimal
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

    fun lock()

    fun signDataNFC(data: ByteArray, isEDKey: Boolean): String?

    fun setTransactionInfoForNFCPay(amount: BigDecimal, assetId: String, orderID: UUID)
    fun isNFCPay(): Boolean

    fun rejectedTransaction()

    enum class AIDs(val aid: String) {
        AID_1("63:98:96:00:FF:00:01"),
        AID_2("70:6f:72:74:65:42:54:43"),
        AID_3("A0:00:00:08:82:00:01"),
        AID_4("A0:00:00:08:82:00:02"),
        AID_5("A7:77:77:77:77:77:77")
    }

    enum class ALGORITHMS {
        EDDSA, NONE_WITH_ECDSA;
    }
}
