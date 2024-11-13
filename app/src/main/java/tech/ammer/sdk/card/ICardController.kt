package tech.ammer.sdk.card

import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashMap

interface ICardController {

    fun getPublicKeyECDSA(pin: String): String?

    fun getPublicKeyEDDSA(pin: String): String?

    fun getPrivateKey(pin: String): String?

    fun getCardUUID(): UUID?

    @SuppressWarnings("It is used only once and has no reverse action.")
    fun blockGetPrivateKey(pin: String)

    fun signData(data: ByteArray, pin: String?, isECKey: Boolean = true): ByteArray?

    fun signDataByNonce(data: ByteArray, gatewaySignature: ByteArray, isECKey: Boolean = true): ByteArray?

    fun signData(data: HashMap<Int, ByteArray>, pin: String?, isECKey: Boolean = true): HashMap<Int, ByteArray>?

    fun signDataByNonce(data: HashMap<Int, ByteArray>, gatewaySignatures: HashMap<Int, ByteArray>, isECKey: Boolean = true): HashMap<Int, ByteArray>?

    fun select(): String

    fun needActivation(): Boolean

    fun activate(pin: String): Boolean

    fun changePin(oldPin: String, newPin: String)

    fun countPinAttempts(): Int

    fun getIssuer(): Int

    fun getSeries(): Int

    fun unlock(pin: String)

    fun lock()

    fun setTransactionInfoForNFCPay(amount: BigDecimal, assetId: String, orderID: UUID, isEDKey: Boolean = false)

    fun getAID(): AID

    /**
     * @param type 0 - reject status, 1 - success status
     */
    fun statusTransaction(type: Int)


    enum class AID(
        val aid: String,
        val realDevice: Boolean = false,
        val hasTon: Boolean = true,
        val countAttemptsPin: Int = 10,
        val isSecure: Boolean = false,
        val lengthPin: Int = 6,
    ) {
        AID_1("A0:00:00:08:82:00:03", isSecure = true),
        AID_2("A0:00:00:08:82:00:02"),
        AID_3("A7:77:77:77:77:77:77", realDevice = true),
        AID_4("A0:00:00:08:82:00:01", hasTon = false, countAttemptsPin = 3),
        AID_5("63:98:96:00:FF:00:01", hasTon = false, countAttemptsPin = 3),
        AID_6("70:6f:72:74:65:42:54:43", hasTon = false, countAttemptsPin = 3, lengthPin = 5);
    }

    enum class ALGORITHMS {
        EDDSA, NONE_WITH_ECDSA;
    }
}

