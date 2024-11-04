package tech.ammer.sdk.card

interface CardControllerListener {
    fun processCommand(byteArray: ByteArray): ByteArray?
    fun onCardError(code: Short)
}