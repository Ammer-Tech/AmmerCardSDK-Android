# Ammer Smart Card SDK

### Download android-card-sdk/sample/libs/card-sdk-#.#.aar

The Ammer Card SDK enables your Android application to use a Ammer smart-card as a cryptographic interface which is used to:

1. Generate a Secp256k1 Keypair
2. Set/Modify the PIN code used to trigger cryptographic operations (set at key generation time, used to invoke signature function)
3. Extract the public key to generate an address for a blockchain which leverage Secp256k1 keys (e.g. Bitcoin, Ethereum)
4. Perform a NONE_WITH_ECDSA signature scheme on a 64-byte payload

### Examples

Example

```kotlin
package com.example.sample

...
import tech . ammer . sdk . card . CardControllerFactory
        import tech . ammer . sdk . card . CardControllerListener
        import tech . ammer . sdk . card . ICardController
        import tech . ammer . sdk . card . ReaderMode

class MainActivity : Activity(), CardControllerListener {
    private var cardController: ICardController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ...
//       Start
        cardController = CardControllerFactory().getController(ReaderMode.ANDROID_DEFAULT, this)
        cardController?.open(this)
    }

    override fun onAppletSelected() {
        cardController?.select() //Required!!

        cardController?.isNotActivated()
        cardController?.activate(pin)
        
        cardController?.getAvailablePinCount().toString()
        cardController?.getCardUUID(pin)
        cardController?.getIssuer().toString()
        
        cardController?.getPublicKeyString(pin)
//      Can be called only once
        cardController?.getPrivateKeyString(pin)
        
        cardController?.signData("bce6d58f7da6c3cd7239cbf5fcc0e323302ff072b20ecf59c501752c0e98906a", pin)
        cardController?.signDataByNonce(toSign, gatewaySignature)

        cardController?.changePin(oldPin, newPin)
    }

    override fun onCardError(code: Short) {
        ...
    }
}
```