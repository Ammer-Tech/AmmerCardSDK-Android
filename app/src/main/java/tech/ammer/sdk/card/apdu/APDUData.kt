package tech.ammer.sdk.card.apdu


class APDUData(val payload: ByteArray, val pin: ByteArray) {

    val bytes: ByteArray
        get() = byteArrayOf(
            Tags.CARD_PIN,
            Tags.CARD_PIN_MAX_LENGTH,
            *pin,
            Tags.DATA_FOR_SIGN,
            payload.size.toByte(),
            *payload
        )
}