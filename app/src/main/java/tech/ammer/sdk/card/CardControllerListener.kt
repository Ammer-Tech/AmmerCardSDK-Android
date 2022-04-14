package tech.ammer.sdk.card

interface CardControllerListener {
    fun onAppletSelected()
    fun onAppletNotSelected(message: String)
    fun tagDiscoverTimeout()
}