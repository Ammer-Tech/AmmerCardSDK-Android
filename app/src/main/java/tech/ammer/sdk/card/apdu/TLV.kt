package tech.ammer.sdk.card.apdu

import tech.ammer.sdk.card.apdu.CardErrors.SW_WRONG_DATA

abstract class TLV @JvmOverloads constructor(
    val tag: Byte,
    private val minLength: Byte,
    private val maxLength: Byte,
    val isTransientTLV: Boolean = false
) {
    var tlv: ByteArray

    constructor(tag: Byte, length: Byte) : this(tag, length, length, false)

    constructor(tag: Byte, length: Byte, transientTLV: Boolean) : this(tag, length, length, transientTLV)

    init {
        tlv = ByteArray((maxLength + HEADER_BYTES_COUNT).toShort().toInt())
    }

    fun build(buffer: ByteArray?, offset: Short): Short {
        val length = (tlv[OFFSET_LENGTH.toInt()] + HEADER_BYTES_COUNT).toShort()
//        Util.arrayCopy(tlv, 0.toShort(), buffer, offset, length)
        return length
    }

    fun getMinLength(): Short {
        return minLength.toShort()
    }

    fun getMaxLength(): Short {
        return maxLength.toShort()
    }

    protected fun verifyTag(tag: Byte): Boolean {
        return this.tag == tag
    }

    protected fun verifyLength(length: Short): Boolean {
        return length >= this.minLength && length <= this.maxLength
    }

    protected abstract fun verifyValue(buffer: ByteArray?, offset: Short, valueLength: Short): Boolean

    @Throws(Exception::class)
    fun verifyAndSet(tag: Byte, length: Short, value: ByteArray?, offset: Short): ByteArray? {
        if (!verifyTag(tag) || !verifyLength(length) || !verifyValue(value, offset, length)) {
            throw Exception((SW_WRONG_DATA).toString())
        }
        tlv[OFFSET_TAG.toInt()] = tag
        tlv[OFFSET_LENGTH.toInt()] = length.toByte()
//        return Util.arrayCopy(value, offset, tlv, OFFSET_VALUE, length)
        return value
    }

    companion object {
        const val HEADER_BYTES_COUNT: Byte = 0x02
        const val OFFSET_TAG: Byte = 0x00
        const val OFFSET_LENGTH: Byte = 0x01
        const val OFFSET_VALUE: Byte = 0x02
    }
}
