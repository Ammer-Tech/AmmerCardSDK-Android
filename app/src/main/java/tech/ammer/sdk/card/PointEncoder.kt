package tech.ammer.sdk.card

import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.SecureRandom
import kotlin.experimental.or
object PointEncoder {
    val p = byteArrayOf(0x7f.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xff.toByte(),
        0xed.toByte())
    val a = byteArrayOf(0x2a.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0x98.toByte(),
        0x49.toByte(),
        0x14.toByte(),
        0xa1.toByte(),
        0x44.toByte())
    val b = byteArrayOf(0x7b.toByte(),
        0x42.toByte(),
        0x5e.toByte(),
        0xd0.toByte(),
        0x97.toByte(),
        0xb4.toByte(),
        0x25.toByte(),
        0xed.toByte(),
        0x09.toByte(),
        0x7b.toByte(),
        0x42.toByte(),
        0x5e.toByte(),
        0xd0.toByte(),
        0x97.toByte(),
        0xb4.toByte(),
        0x25.toByte(),
        0xed.toByte(),
        0x09.toByte(),
        0x7b.toByte(),
        0x42.toByte(),
        0x5e.toByte(),
        0xd0.toByte(),
        0x97.toByte(),
        0xb4.toByte(),
        0x26.toByte(),
        0x0b.toByte(),
        0x5e.toByte(),
        0x9c.toByte(),
        0x77.toByte(),
        0x10.toByte(),
        0xc8.toByte(),
        0x64.toByte())
    val r = byteArrayOf(0x10.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x14.toByte(),
        0xde.toByte(),
        0xf9.toByte(),
        0xde.toByte(),
        0xa2.toByte(),
        0xf7.toByte(),
        0x9c.toByte(),
        0xd6.toByte(),
        0x58.toByte(),
        0x12.toByte(),
        0x63.toByte(),
        0x1a.toByte(),
        0x5c.toByte(),
        0xf5.toByte(),
        0xd3.toByte(),
        0xed.toByte())
    val TRANSFORM_A3 = byteArrayOf(0x2a.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xad.toByte(),
        0x24.toByte(),
        0x51.toByte())
    val TRANSFORM_C = byteArrayOf(0x70.toByte(),
        0xd9.toByte(),
        0x12.toByte(),
        0x0b.toByte(),
        0x9f.toByte(),
        0x5f.toByte(),
        0xf9.toByte(),
        0x44.toByte(),
        0x2d.toByte(),
        0x84.toByte(),
        0xf7.toByte(),
        0x23.toByte(),
        0xfc.toByte(),
        0x03.toByte(),
        0xb0.toByte(),
        0x81.toByte(),
        0x3a.toByte(),
        0x5e.toByte(),
        0x2c.toByte(),
        0x2e.toByte(),
        0xb4.toByte(),
        0x82.toByte(),
        0xe5.toByte(),
        0x7d.toByte(),
        0x33.toByte(),
        0x91.toByte(),
        0xfb.toByte(),
        0x55.toByte(),
        0x00.toByte(),
        0xba.toByte(),
        0x81.toByte(),
        0xe7.toByte())

   private val G = byteArrayOf(
        0x04.toByte(),
        0x2a.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xaa.toByte(),
        0xad.toByte(),
        0x24.toByte(),
        0x5a.toByte(),
        0x20.toByte(),
        0xae.toByte(),
        0x19.toByte(),
        0xa1.toByte(),
        0xb8.toByte(),
        0xa0.toByte(),
        0x86.toByte(),
        0xb4.toByte(),
        0xe0.toByte(),
        0x1e.toByte(),
        0xdd.toByte(),
        0x2c.toByte(),
        0x77.toByte(),
        0x48.toByte(),
        0xd1.toByte(),
        0x4c.toByte(),
        0x92.toByte(),
        0x3d.toByte(),
        0x4d.toByte(),
        0x7e.toByte(),
        0x6d.toByte(),
        0x7c.toByte(),
        0x61.toByte(),
        0xb2.toByte(),
        0x29.toByte(),
        0xe9.toByte(),
        0xc5.toByte(),
        0xa2.toByte(),
        0x7e.toByte(),
        0xce.toByte(),
        0xd3.toByte(),
        0xd9.toByte())

    val curve: ECCurve = ECCurve.Fp(BigInteger(1, p),
        BigInteger(1, a),
        BigInteger(1, b),
        BigInteger(1, r),
        BigInteger.valueOf(8))

    fun convert(pubKey: ByteArray?): String {
        return Hex.toHexString(encodeEd25519(curve.decodePoint(pubKey).multiply(BigInteger.valueOf(8))))
    }

    private fun encodeEd25519(point: ECPoint): ByteArray {
        val p = BigInteger(1, p)
        val a3 = BigInteger(1, TRANSFORM_A3)
        val c = BigInteger(1, TRANSFORM_C)
        val x = point.normalize().affineXCoord.toBigInteger()
        val y = point.normalize().affineYCoord.toBigInteger()
        val tmp = x.subtract(a3).mod(p)
        val x_bit = tmp.multiply(c).mod(p).multiply(y.modInverse(p)).mod(p).testBit(0)

        // Compute Y
        val result = tmp.subtract(BigInteger.ONE).multiply(tmp.add(BigInteger.ONE).modInverse(p)).mod(p).toByteArray()
        val output = ByteArray(32)
        val diff = output.size - result.size
        for (i in diff until output.size) {
            output[i] = result[i - diff]
        }
        output[0] = output[0] or if (x_bit) 0x80.toByte() else 0x00.toByte()
        for (i in 0 until (output.size / 2).toShort()) {
            val t = output[output.size - i - 1]
            output[(output.size - i - 1).toShort().toInt()] = output[i]
            output[i] = t
        }
        return output
    }

    val privateNonce: ByteArray
        get() {
            val privateNonce = ByteArray(32)
            SecureRandom().nextBytes(privateNonce)
            return BigInteger(privateNonce).mod(BigInteger(r)).toByteArray()
        }

    fun getPublicNonce(privateNonce: ByteArray?): ByteArray {
        var publicNonce = curve.decodePoint(G)
        publicNonce = publicNonce.multiply(BigInteger(privateNonce))
        return encodeEd25519(publicNonce)
    }
}