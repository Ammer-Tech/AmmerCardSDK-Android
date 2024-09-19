package tech.ammer.sdk.card

import java.io.IOException
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.Arrays

class ResponseAPDU(apdu: ByteArray) : Serializable {
    /** @serial
     */
    private var apdu: ByteArray

    /**
     * Constructs a ResponseAPDU from a byte array containing the complete
     * APDU contents (conditional body and trailed).
     *
     *
     * Note that the byte array is cloned to protect against subsequent
     * modification.
     *
     * @param apdu the complete response APDU
     *
     * @throws NullPointerException if apdu is null
     * @throws IllegalArgumentException if apdu.length is less than 2
     */
    init {
        var apdu = apdu
        apdu = apdu.clone()
        check(apdu)
        this.apdu = apdu
    }

    val nr: Int
        /**
         * Returns the number of data bytes in the response body (Nr) or 0 if this
         * APDU has no body. This call is equivalent to
         * `getData().length`.
         *
         * @return the number of data bytes in the response body or 0 if this APDU
         * has no body.
         */
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

    /**
     * Returns a string representation of this response APDU.
     *
     * @return a String representation of this response APDU.
     */
    override fun toString(): String {
        return ("ResponseAPDU: " + apdu.size + " bytes, SW="
                + Integer.toHexString(sW))
    }

    /**
     * Compares the specified object with this response APDU for equality.
     * Returns true if the given object is also a ResponseAPDU and its bytes are
     * identical to the bytes in this ResponseAPDU.
     *
     * @param obj the object to be compared for equality with this response APDU
     * @return true if the specified object is equal to this response APDU
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj is ResponseAPDU == false) {
            return false
        }
        return Arrays.equals(this.apdu, obj.apdu)
    }

    /**
     * Returns the hash code value for this response APDU.
     *
     * @return the hash code value for this response APDU.
     */
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
