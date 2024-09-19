package tech.ammer.sdk.card.apdu
import tech.ammer.sdk.card.ResponseAPDU
import java.io.IOException
import java.io.ObjectInputStream
import java.io.Serializable
import java.nio.ByteBuffer


class CommandAPDU(apdu: ByteArray) {
    private var apdu: ByteArray
    @Transient
    var nc: Int = 0
        private set

    @Transient
    var ne: Int = 0
        private set

    // index of start of data within the apdu array
    @Transient
    private var dataOffset = 0

    init {
        this.apdu = apdu.clone()
        parse()
    }


    private fun parse() {
        if (apdu.size < 4) {
            throw IllegalArgumentException("apdu must be at least 4 bytes long")
        }
        if (apdu.size == 4) {
            // case 1
            return
        }
        val l1 = apdu[4].toInt() and 0xff
        if (apdu.size == 5) {
            // case 2s
            this.ne = if ((l1 == 0)) 256 else l1
            return
        }
        if (l1 != 0) {
            if (apdu.size == 4 + 1 + l1) {
                // case 3s
                this.nc = l1
                this.dataOffset = 5
                return
            } else if (apdu.size == 4 + 2 + l1) {
                // case 4s
                this.nc = l1
                this.dataOffset = 5
                val l2 = apdu[apdu.size - 1].toInt() and 0xff
                this.ne = if ((l2 == 0)) 256 else l2
                return
            } else {
                throw IllegalArgumentException("Invalid APDU: length=" + apdu.size + ", b1=" + l1)
            }
        }
        if (apdu.size < 7) {
            throw IllegalArgumentException("Invalid APDU: length=" + apdu.size + ", b1=" + l1)
        }
        val l2 = ((apdu[5].toInt() and 0xff) shl 8) or (apdu[6].toInt() and 0xff)
        if (apdu.size == 7) {
            // case 2e
            this.ne = if ((l2 == 0)) 65536 else l2
            return
        }
        if (l2 == 0) {
            throw IllegalArgumentException(
                "Invalid APDU: length="
                        + apdu.size + ", b1=" + l1 + ", b2||b3=" + l2
            )
        }
        if (apdu.size == 4 + 3 + l2) {
            // case 3e
            this.nc = l2
            this.dataOffset = 7
            return
        } else if (apdu.size == 4 + 5 + l2) {
            // case 4e
            this.nc = l2
            this.dataOffset = 7
            val leOfs = apdu.size - 2
            val l3 = ((apdu[leOfs].toInt() and 0xff) shl 8) or (apdu[leOfs + 1].toInt() and 0xff)
            this.ne = if ((l3 == 0)) 65536 else l3
        } else {
            throw IllegalArgumentException(
                (("Invalid APDU: length="
                        + apdu.size + ", b1=" + l1 + ", b2||b3=" + l2))
            )
        }
    }

    private fun setHeader(cla: Int, ins: Int, p1: Int, p2: Int) {
        apdu[0] = cla.toByte()
        apdu[1] = ins.toByte()
        apdu[2] = p1.toByte()
        apdu[3] = p2.toByte()
    }

    val cLA: Int
        /**
         * Returns the value of the class byte CLA.
         *
         * @return the value of the class byte CLA.
         */
        get() = apdu.get(0).toInt() and 0xff

    val iNS: Int
        /**
         * Returns the value of the instruction byte INS.
         *
         * @return the value of the instruction byte INS.
         */
        get() = apdu.get(1).toInt() and 0xff

    val p1: Int
        /**
         * Returns the value of the parameter byte P1.
         *
         * @return the value of the parameter byte P1.
         */
        get() = apdu.get(2).toInt() and 0xff

    val p2: Int
        /**
         * Returns the value of the parameter byte P2.
         *
         * @return the value of the parameter byte P2.
         */
        get() = apdu.get(3).toInt() and 0xff

    val data: ByteArray
        get() {
            val data = ByteArray(nc)
            System.arraycopy(apdu, dataOffset, data, 0, nc)
            return data
        }

    val bytes: ByteArray
        get() = apdu.clone()

    /**
     * Returns a string representation of this command APDU.
     *
     * @return a String representation of this command APDU.
     */
    override fun toString(): String {
        return "CommmandAPDU: " + apdu.size + " bytes, nc=" + nc + ", ne=" + ne
    }

    /**
     * Compares the specified object with this command APDU for equality.
     * Returns true if the given object is also a CommandAPDU and its bytes are
     * identical to the bytes in this CommandAPDU.
     *
     * @param obj the object to be compared for equality with this command APDU
     * @return true if the specified object is equal to this command APDU
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj is CommandAPDU == false) {
            return false
        }
        return apdu.contentEquals(obj.apdu)
    }

    /**
     * Returns the hash code value for this command APDU.
     *
     * @return the hash code value for this command APDU.
     */
    override fun hashCode(): Int {
        return apdu.contentHashCode()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        apdu = `in`.readUnshared() as ByteArray
        // initialize transient fields
        parse()
    }

    companion object {
        private val serialVersionUID = 398698301286670877L

        private val MAX_APDU_SIZE = 65544

        private fun arrayLength(b: ByteArray?): Int {
            return if ((b != null)) b.size else 0
        }
    }
}
