package tech.ammer.sdk.card

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Provider

object CardSdk {

    fun getController(listener: CardControllerListener): ICardController {
        return NFCCardController(listener)
    }
}