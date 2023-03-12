package tech.ammer.sdk.card

interface CardControllerListener {
    fun onCardAttach()
    fun onCardError(code: Short)
}