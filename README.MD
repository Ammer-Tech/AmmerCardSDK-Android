# Ammer Smart Card SDK

The Ammer Card SDK enables your Android application to use a Ammer smart-card as a cryptographic interface which is used to:

1. Generate a Secp256k1 Keypair
2. Set/Modify the PIN code used to trigger cryptographic operations (set at key generation time, used to invoke signature function)
3. Extract the public key to generate an address for a blockchain which leverage Secp256k1 keys (e.g. Bitcoin, Ethereum)
4. Perform a NONE_WITH_ECDSA signature scheme on a 64-byte payload

### Examples

# Gradle

```Groovy
implementation files('libs/card-sdk-#.#.#.aar')
implementation 'org.bouncycastle:bcprov-jdk18on:1.79'
```

# Using the Library

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CardSDK.start()
    }
}
```

```kotlin
class MainActivity : Activity(), CardControllerListener, NfcAdapter.ReaderCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val cardController = CardSdk.getController(this)
        ... enable NFS read mode
    }
    
    override fun processCommand(byteArray: ByteArray): ByteArray? {
       // If not a standard NFC adapter (such as Pax or similar), implement your solution
        return isoDep?.transceive(byteArray)
    }
}

```

# Actions with the card

```kotlin

fun onTagDiscovered(tag: Tag) {
    ...

    cardController?.select() //Required!!

    //About Card
    val availablePinCount = cardController?.countPinAttempts().toString()
    val uuid = cardController?.getCardUUID()
    val cardIssuer = cardController?.getIssuer().toString()
    val series = cardController?.getSeries().toString()

    val isRealDevice = cardController.isRealDevice()

    // Private information about the card
    val privateKeyEC = cardController?.getPrivateKey(pin)
    val publicKeyEC = cardController?.getPublicKeyECDSA(pin)
    val publicKeyED = cardController?.getPublicKeyEDDSA(pin)

    // Action
    val signEC = cardController?.signDataEC(dataEC, pin)
    val signED = cardController?.signDataED(dataED, pin)
    val signByNonceEC = cardController?.signDataByNonceEC(dataEC, gatewaySignature)
    val signByNonceED = cardController?.signDataByNonceED(dataED, gatewaySignatureED, Hex.decode(pubKeyED))

    cardController?.changePin(pin, newPin)
    cardController?.blockGetPrivateKey(pin) // The action can only be used once for a card
}
```

## Actions for the Ammer Wallet app (android)
```kotlin
val pubKeyECF = cardController?.getPublicKeyECDSA(pin)
cardController?.setTransactionInfoForNFCPay(amount = BigDecimal("0.0005"), assetId = "USDT", orderID = UUID.randomUUID(), isEDKey = false)

val sign_EC_NFC = cardController?.signDataEC(toSignEC, null)
val sign_ED_NFC = cardController?.signDataED(toSignED, null)

cardController?.statusTransaction(1)
```

## [Sample](https://github.com/Ammer-Tech/AmmerCardSDK-Android/blob/master/sample/src/main/java/com/example/sample/MainActivity.kt)