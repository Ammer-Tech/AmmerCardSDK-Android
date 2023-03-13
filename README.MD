# Ammer Smart Card SDK

The Ammer Card SDK enables your Android application to use a Ammer smart-card as a cryptographic interface which is used to:

1. Generate a Secp256k1 Keypair
2. Set/Modify the PIN code used to trigger cryptographic operations (set at key generation time, used to invoke signature function)
3. Extract the public key to generate an address for a blockchain which leverage Secp256k1 keys (e.g. Bitcoin, Ethereum)
4. Perform a NONE_WITH_ECDSA signature scheme on a 64-byte payload

### Examples

# Dependency

```Groovy
implementation files('sample/libs/card-sdk-1.4.aar')
implementation 'org.bouncycastle:bcprov-jdk15to18:1.68'
```

# Example

```kotlin
package com.example.sample

import tech.ammer.sdk.card.CardControllerListener
import tech.ammer.sdk.card.ICardController

class MainActivity : Activity(), CardControllerListener {
    private var cardController: ICardController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cardController = CardSdk.getController(this)
        cardController?.open(this)
    }

    override fun onCardAttached() {
        cardController?.select() //Required!!

        cardController?.isNotActivated()
        cardController?.activate(newPin)

        cardController?.getAvailablePinCount()
        cardController?.getCardUUID(pin)
        cardController?.getIssuer().toString()

        cardController?.getPublicKeyString(pin)
//      Can be called only once
        cardController?.getPrivateKeyString(pin)

        cardController?.signData(payload, pin)
        cardController?.signDataByNonce(toSign, gatewaySignature)

        cardController?.changePin(oldPin, newPin)
    }

    override fun onCardError(code: Short) {
        when (code) {
            CardErrors.SW_WRONG_P1P2 -> "Wrong PIN"
                ....
        }
    }
}
```