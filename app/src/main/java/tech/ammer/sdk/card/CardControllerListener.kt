package tech.ammer.sdk.card

interface CardControllerListener {
    fun onCardAttached()
    fun onAppletSelected()
    fun onAppletNotSelected(message: String)
    fun tagDiscoverTimeout()
}

interface CardControllerListenerV2 {
    fun onErrorAttach(errorCode: Int)
    fun onStartListening() {}
    fun onStopListening() {}
    fun onCardAttached(isActivated: Boolean, pubKey: String, cardController: CardControllerV2)
}