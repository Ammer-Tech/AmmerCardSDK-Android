package tech.ammer.sdk.card

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import java.lang.Exception
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import kotlin.properties.Delegates.notNull

object ECController {
    private var parameterSpec: ECNamedCurveParameterSpec by notNull()
    private var keyPairGenerator: KeyPairGenerator by notNull()
    private var keyFactory: KeyFactory by notNull()

    fun getPublicKeyString(w: ByteArray?): String? {
        try {
            return Hex.toHexString(getPublicKey(w).encoded)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun getPublicKey(w: ByteArray?): ECPublicKey {
        val cardKeySpec = ECPublicKeySpec(parameterSpec.curve.decodePoint(w), parameterSpec)
        val publicKey = keyFactory.generatePublic(cardKeySpec) as ECPublicKey

        return publicKey
    }

    fun getEDPublicKeyString(pubKeyStr: ByteArray?): String? {
        try {
            return PointEncoder.convert(pubKeyStr)
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

    fun createKeyPairHandshake(): KeyPair {
        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    fun start() {
        keyFactory = KeyFactory.getInstance("EC", "BC")
        parameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC")
        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()
    }
}