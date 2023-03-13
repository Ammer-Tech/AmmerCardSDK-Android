package tech.ammer.sdk.card

interface CardControllerListener {
    fun onCardAttached()
    fun onCardError(code: Short)
}