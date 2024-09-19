package tech.ammer.sdk.card.apdu

import tech.ammer.sdk.card.apdu.Tags.OFFSET_CLA
import tech.ammer.sdk.card.apdu.Tags.OFFSET_INS
import tech.ammer.sdk.card.apdu.Tags.OFFSET_LC
import tech.ammer.sdk.card.apdu.Tags.OFFSET_P1
import tech.ammer.sdk.card.apdu.Tags.OFFSET_P2

internal class APDUBuilder private constructor() {

    private val header = ByteArray(5)
    var data: ByteArray = byteArrayOf()
        private set

    fun setCLA(cla: Byte): APDUBuilder {
        header[OFFSET_CLA.toInt()] = cla
        return this
    }

    fun setINS(ins: Byte): APDUBuilder {
        header[OFFSET_INS.toInt()] = ins
        return this
    }

    fun setP1(p1: Byte): APDUBuilder {
        header[OFFSET_P1.toInt()] = p1
        return this
    }

    fun setP2(p2: Byte): APDUBuilder {
        header[OFFSET_P2.toInt()] = p2
        return this
    }

    fun setData(data: ByteArray?): APDUBuilder {
        this.data = data ?: byteArrayOf()
        return this
    }

    fun setData(pin: ByteArray, publicKey: ByteArray, privateNonce: ByteArray, publicNonce: ByteArray, payload: ByteArray): APDUBuilder {
        data = byteArrayOf(
            Tags.CARD_PIN, pin.size.toByte(), *pin,
            Tags.ED_CARD_PUBLIC_KEY_ENCODED, publicKey.size.toByte(), *publicKey,
            Tags.ED_PRIVATE_NONCE, privateNonce.size.toByte(), *privateNonce,
            Tags.ED_PUBLIC_NONCE, publicNonce.size.toByte(), *publicNonce,
            Tags.DATA_FOR_SIGN, payload.size.toByte(), *payload
        )

        return this
    }

    fun build(): ByteArray {
        return if (data.isEmpty()) {
            header
        } else {
            val output = ByteArray(header.size + data.size)
            System.arraycopy(header, 0, output, 0, header.size)
            System.arraycopy(data, 0, output, header.size, data.size)

            output[OFFSET_LC.toInt()] = data.size.toByte()
            output
        }
    }

    companion object {
        fun init(): APDUBuilder {
            return APDUBuilder()
        }
    }
}