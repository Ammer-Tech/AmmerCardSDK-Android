package tech.ammer.sdk.card.apdu


object APDUData2 {
    fun createSignData(pin: ByteArray, payload: ByteArray): ByteArray {
        return byteArrayOf(Tags.CARD_PIN, pin.size.toByte(), *pin, Tags.DATA_FOR_SIGN, payload.size.toByte(), *payload)
    }

    fun createSignData(data: ByteArray): ByteArray {
        return byteArrayOf(Tags.DATA_FOR_SIGN, data.size.toByte(), *data)
    }

    fun createSignByNonceData(toSign: ByteArray,gatewaySignature: ByteArray): ByteArray {
        return byteArrayOf(Tags.DATA_FOR_SIGN, toSign.size.toByte(), *toSign, Tags.DATA_SIGNATURE, gatewaySignature.size.toByte(), *gatewaySignature)
    }

    fun createChangePinData(oldPin: String, newPin: String): ByteArray {
        return byteArrayOf(Tags.CARD_PIN, oldPin.length.toByte(), *pinGetBytes(oldPin), Tags.CARD_PIN, newPin.length.toByte(), *pinGetBytes(newPin))
    }

    fun createSignForEDKeyData(publicKey: ByteArray, privateNonce: ByteArray, publicNonce: ByteArray, payload: ByteArray, pin: ByteArray): ByteArray {
        return byteArrayOf(
            Tags.CARD_PIN, pin.size.toByte(), *pin,
            Tags.CARD_PUBLIC_KEY_ED, publicKey.size.toByte(), *publicKey,
            Tags.PRIVATE_NONCE, privateNonce.size.toByte(), *privateNonce,
            Tags.PUBLIC_NONCE, publicNonce.size.toByte(), *publicNonce,
            Tags.DATA_FOR_SIGN, payload.size.toByte(), *payload
        )
    }

    fun Buider(): MyApduData {
        return MyApduData()
    }

    private fun pinGetBytes(pin: String): ByteArray {
        val pinBytes = ByteArray(pin.length)
        for (i in pin.indices) {
            pinBytes[i] = (pin[i].code - 48).toByte()
        }
        return pinBytes
    }
}

class MyApduData {
    private val _data = arrayListOf<ByteArray>()

    fun setData(tag: Byte, data: ByteArray): MyApduData {
        _data.add(byteArrayOf(tag, data.size.toByte(), *data))
        return this
    }

    fun build(): ByteArray {
        return _data.reduce { y, x -> y + x }
    }
}