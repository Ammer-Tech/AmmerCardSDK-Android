# Ammer Smart Card SDK

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
import tech.ammer.sdk.card.CardControllerFactory
import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.ICardController
import tech.ammer.sdk.card.ReaderMode

class MainActivity : Activity(), CardControllerListener {
    private var cardController: ICardController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//       Start
        cardController = CardControllerFactory().getController(ReaderMode.ANDROID_DEFAULT, this)
        cardController?.open(this)
    }

    override fun onAppletSelected() {
//         Required!!
        cardController?.select() 
        val isNotActivate = cardController?.isNotActivate()

        val pin = "123456"

        if (isNotActivate == true) {
//            Activate a new card and set PIN code
            cardController?.activate(pin)
        }

//        Modify card PIN code
        val newPin = "123456"
        cardController?.changePin(pin, newPin)

//        Extract public key and metadata
        val uuid = cardController?.getCardUUID(pin)
        val pubKey = cardController?.getPublicKeyString(pin)

//        Extract private key
//        ⚠️ You can only extract the private key once for offline backup such as a paper wallet, a USB stick,
//        or optical media which is never read on a device which is or will be connected to the internet ⚠️
        val pvkKey = cardController?.getPrivateKeyString(pin)

//        Sign a 64 - byte string using ECDSA
        val sign = cardController?.signData("bce6d58f7da6c3cd7239cbf5fcc0e323302ff072b20ecf59c501752c0e98906a", pin)
    }

    override fun onAppletNotSelected(message: String) {
        Log.d(TAG, "Error: $message")
    }

    override fun tagDiscoverTimeout() {
    }
}
```
