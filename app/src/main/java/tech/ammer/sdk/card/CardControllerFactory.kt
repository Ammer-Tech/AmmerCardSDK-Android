package tech.ammer.sdk.card

class CardControllerFactory {
    fun getController(readerMode: ReaderMode, listener: CardControllerListener): ICardController {
        return when(readerMode){
            ReaderMode.ANDROID_DEFAULT -> NFCCardController(listener)
            ReaderMode.SUNME -> NFCCardController(listener)
        }
    }
}