package tech.ammer.sdk.card.apdu

import java.nio.ByteBuffer


class TLVBuilder(tag: Byte, length: Byte) {
    private var tlv: ByteBuffer = ByteBuffer.allocate(TLV.HEADER_BYTES_COUNT + length)

    init {
        tlv.put(tag)
        tlv.put(length)
    }

    fun build(value: Byte): ByteArray {
        tlv.put(value)
        return tlv.array()
    }

    fun build(value: ByteArray): ByteArray {
        tlv.put(value)
        return tlv.array()
    }

    companion object {
        fun init(tag: Byte, length: Byte): TLVBuilder {
            return TLVBuilder(tag, length)
        }
    }
}


