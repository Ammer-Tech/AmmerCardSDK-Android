package tech.ammer.sdk.card

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

object CardSDK {
    private var insertedBC = false

    fun getController(listener: CardControllerListener): ICardController {
        if (!insertedBC) {
            throw Exception("BouncyCastleProvider has not been added")
        }
        return NFCCardController(listener)
    }

    fun start() {
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        insertedBC = true
    }
}
