package tech.ammer.sdk.card.apdu

internal class APDUBuilder private constructor() {
    private val header = ByteArray(5)
    private var data: ByteArray? = null

    fun setCLA(cla: Byte): APDUBuilder {
        header[ISO7816.OFFSET_CLA.toInt()] = cla
        return this
    }

    fun setINS(ins: Byte): APDUBuilder {
        header[ISO7816.OFFSET_INS.toInt()] = ins
        return this
    }

    fun setP1(p1: Byte): APDUBuilder {
        header[ISO7816.OFFSET_P1.toInt()] = p1
        return this
    }

    fun setP2(p2: Byte): APDUBuilder {
        header[ISO7816.OFFSET_P2.toInt()] = p2
        return this
    }

    fun setData(data: APDUData): APDUBuilder {
        return setData(data.bytes)
    }

    fun setData(data: ByteArray?): APDUBuilder {
        this.data = data
        return this
    }

    fun build(): ByteArray {
        return if (data == null) {
            header
        } else {
            val output = ByteArray(header.size + data!!.size)
            System.arraycopy(header, 0, output, 0, header.size)
            System.arraycopy(data, 0, output, header.size, data!!.size)

            output[ISO7816.OFFSET_LC.toInt()] = data!!.size.toByte()
            output
        }
    }

    companion object {
        fun init(): APDUBuilder {
            return APDUBuilder()
        }
    }
}