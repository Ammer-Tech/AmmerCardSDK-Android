package tech.ammer.sdk.card

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

object CardSDK {
    private var insertedBCProvider = false

    fun getController(listener: CardControllerListener): ICardController {
        if (!insertedBCProvider) {
            throw Exception("CardSDK hasn't started")
        }

        return NFCCardController(listener)
    }

    fun start() {
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        ECController.start()
        insertedBCProvider = true
    }
}
