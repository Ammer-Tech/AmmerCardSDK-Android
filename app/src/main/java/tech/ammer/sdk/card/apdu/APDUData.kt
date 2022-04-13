package tech.ammer.sdk.card.apdu


class APDUData {
    private var payload: ByteArray = byteArrayOf()
    private var pin: ByteArray = byteArrayOf()

    fun setPayload(payload: ByteArray?): APDUData {
        payload ?: return this

        this.payload = payload
        return this
    }

    fun setPIN(pin: ByteArray?): APDUData {
        pin ?: return this

        this.pin = pin
        return this
    }

    fun getBytes(): ByteArray {
        return byteArrayOf(
            Tags.CARD_PIN,
            Tags.CARD_PIN_MAX_LENGTH, *pin,
            Tags.DATA_FOR_SIGN, payload.size.toByte(), *payload)
    }

    companion object {
        @kotlin.jvm.JvmStatic
        fun init(): APDUData {
            return APDUData()
        }
    }
}