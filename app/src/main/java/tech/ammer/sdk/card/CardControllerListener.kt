package tech.ammer.sdk.card

interface CardControllerListener {
    fun onCardAttached()
    fun onAppletSelected()
    fun onAppletNotSelected(message: String?)
    fun tagDiscoverTimeout()
}