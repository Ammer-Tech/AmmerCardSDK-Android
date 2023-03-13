# Ammer Smart Card SDK

The Ammer Card SDK enables your Android application to use a Ammer smart-card as an external keystore with signing capabilities.

The SDK covers the following functions:

1. Generate a Secp256k1 Keypair
2. Set/Modify the PIN code used to trigger cryptographic operations (e.g. digital signature, private key extraction)
3. Extract the public key which is then used to generate a DLT address (e.g. Bitcoin, Ethereum)
4. Perform a NONE_WITH_ECDSA signature scheme on a 64-byte payload (assuming the data has already been hashed)

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
        
    }
}
```