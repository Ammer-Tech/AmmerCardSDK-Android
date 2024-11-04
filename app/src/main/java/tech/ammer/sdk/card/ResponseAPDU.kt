package tech.ammer.sdk.card

import java.io.IOException
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.Arrays

class ResponseAPDU(_apdu: ByteArray?) : Serializable {
    private var apdu: ByteArray

    init {
        apdu = _apdu?.clone() ?: byteArrayOf()
        check(apdu)
    }

    val nr: Int
        get() = apdu.size - 2

    val data: ByteArray
        get() {
            val data = ByteArray(apdu.size - 2)
            System.arraycopy(apdu, 0, data, 0, data.size)
            return data
        }

    val sW1: Int
        get() = apdu[apdu.size - 2].toInt() and 0xff

    val sW2: Int
        get() = apdu[apdu.size - 1].toInt() and 0xff

    val sW: Int
        get() = (sW1 shl 8) or this.sW2

    val bytes: ByteArray
        get() = apdu.clone()
    override fun toString(): String {
        return ("ResponseAPDU: " + apdu.size + " bytes, SW="
                + Integer.toHexString(sW))
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj !is ResponseAPDU) {
            return false
        }
        return this.apdu.contentEquals(obj.apdu)
    }


    override fun hashCode(): Int {
        return apdu.contentHashCode()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        apdu = `in`.readUnshared() as ByteArray
        check(apdu)
    }

    companion object {
        private const val serialVersionUID = 6962744978375594225L

        private fun check(apdu: ByteArray) {
            require(apdu.size >= 2) { "apdu must be at least 2 bytes long" }
        }
    }
}
