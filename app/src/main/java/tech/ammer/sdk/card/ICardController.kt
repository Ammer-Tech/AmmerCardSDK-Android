package tech.ammer.sdk.card

import java.math.BigDecimal
import java.util.UUID

interface ICardController {

    fun close()

    fun getPublicKeyECDSA(pin: String): String?

    fun getPublicKeyEDDSA(pin: String): String?

    fun getPrivateKey(pin: String): String?

    fun getCardUUID(): UUID?

    @SuppressWarnings("It is used only once and has no reverse action.")
    fun blockGetPrivateKey(pin: String)

    fun signDataEC(payload: String, pin: String?): String?

    fun signDataED(toSign: String, publicKeyEDDSA: String?, pin: String?): String?

    fun signDataByNonceEC(data: String, gatewaySignature: String): String?

    fun signDataByNonceED(data: String, gatewaySignature: String): String?

    fun select(): String

    fun needActivation(): Boolean

    fun activate(pin: String): Boolean

    fun changePin(oldPin: String, newPin: String)

    fun countPinAttempts(): Int

    fun getIssuer(): Int

    fun getSeries(): Int

    fun isUnlock(): Boolean

    fun unlock(pin: String)

    fun lock()

    fun haveTon(): Boolean

    fun setTransactionInfoForNFCPay(amount: BigDecimal, assetId: String, orderID: UUID, isEDKey: Boolean = false)

    fun isRealDevice(): Boolean

    /**
     * @param type 0 - reject status, 1 - success status
     */
    fun statusTransaction(type: Int)


    enum class AIDs(val aid: String, val security: Boolean = false, val realDevice: Boolean = false, val haveTon: Boolean = true) {
        AID_3("A0:00:00:08:82:00:03", security = true),
        AID_2("A0:00:00:08:82:00:02"),
        AID_4("A7:77:77:77:77:77:77", realDevice = true),
        AID_1("A0:00:00:08:82:00:01",haveTon = false),
        AID_5("63:98:96:00:FF:00:01", haveTon = false),
        AID_6("70:6f:72:74:65:42:54:43", haveTon = false)
    }

    enum class ALGORITHMS {
        EDDSA, NONE_WITH_ECDSA;
    }
}
