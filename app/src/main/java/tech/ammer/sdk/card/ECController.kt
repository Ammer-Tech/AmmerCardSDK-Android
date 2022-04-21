package tech.ammer.sdk.card

import android.util.Log
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.lang.Exception
import java.math.BigInteger
import java.security.KeyFactory
import java.security.Security
import java.util.*

internal class ECController private constructor() {

    private val parameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    private val curve = SecP256K1Curve()

    fun getPublicKeyString(w: ByteArray?): String? {
        try {
            val cardKeySpec = ECPublicKeySpec(parameterSpec.curve.decodePoint(w), parameterSpec)
            val publicKey = KeyFactory.getInstance("EC", "BC").generatePublic(cardKeySpec) as ECPublicKey
            return Hex.toHexString(publicKey.encoded)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getPrivateKeyString(pin: ByteArray): String? {
        kotlin.runCatching {
            val cardKeySpec = ECPrivateKeySpec(BigInteger(1, pin), parameterSpec)
            val privateKey = KeyFactory.getInstance("EC", "BC").generatePrivate(cardKeySpec) as ECPrivateKey
            return Hex.toHexString(privateKey.encoded)
        }.onFailure { it.printStackTrace() }

        return null
    }

    companion object {
        private var controller: ECController? = null
        val instance: ECController?
            get() {
                if (controller == null) {
                    Security.removeProvider("BC")
                    Security.insertProviderAt(BouncyCastleProvider(), 1)
                    controller = ECController()
                    return controller
                }
                return controller
            }
    }

    init {
        val start = System.currentTimeMillis()
        try {
            val w = byteArrayOf(
                4, -11, -41, 13, 21, 100, -38, 88, -62, -14, -61, 88, 29, -73, -107, 124, 104, -36, -118, -22, 108, -72,
                42, 43, 124, 81, 96, 40, 124, -44, 79, 102, -92, -14, -32, -55, -27, -1, -27, 35, -114, 52, -109, 5, 91,
                77, 95, -40, 36, 55, -40, -8, -31, -100, 115, -85, 106, -44, 8, 99, 30, 109, 100, -90, 51
            )
            val wPoint = curve.decodePoint(w)
            val cardKeySpec = ECPublicKeySpec(parameterSpec.curve.decodePoint(w), parameterSpec)
            val cardKey =
                KeyFactory.getInstance("EC", "BC").generatePublic(cardKeySpec) as ECPublicKey
            val fromCard = Hex.toHexString(cardKey.encoded)
            Log.d("INIT", fromCard)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("init ", (Calendar.getInstance().time.time - start).toString() + "ms")
    }
}