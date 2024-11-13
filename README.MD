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
        // ... enable NFS read mode
    }
    
    override fun processCommand(byteArray: ByteArray): ByteArray? {
       // If there is no standard NFC adapter (such as Pax or similar), develop your own implementation
        return isoDep?.transceive(byteArray)
    }
}

```

# Actions with the card

```kotlin

ovveride fun onTagDiscovered(tag: Tag) {
    ...

    cardController?.select() //Required!!
    
    val countPinAttempts = cardController?.countPinAttempts().toString()
    val uuid = cardController?.getCardUUID()
    val cardIssuer = cardController?.getIssuer().toString()
    val series = cardController?.getSeries().toString()

    val isRealDevice: Boolean = cardController.getAID().realDevice
    
    val privateKeyEC = cardController?.getPrivateKey(pin)
    val publicKeyEC = cardController?.getPublicKeyECDSA(pin)
    val publicKeyED = cardController?.getPublicKeyEDDSA(pin)

    // Action
    val sign = cardController?.signData(dataEC, pin)
    val signWithGateway = cardController?.signDataByNonce(dataEC, gatewaySignature)

    cardController?.changePin(pin, newPin)
    cardController?.blockGetPrivateKey(pin) // The action can only be used once for a card
}
```

## Actions for the Ammer Wallet app (android)
```kotlin
val pubKeyECF = cardController?.getPublicKeyECDSA(pin)
cardController?.setTransactionInfoForNFCPay(amount = BigDecimal("20"), assetId = "USDT", orderID = UUID.randomUUID(), isEDKey = false)

val sign_NFC = cardController?.signData(toSignEC, null)
cardController?.statusTransaction(1)
```

## [Sample](https://github.com/Ammer-Tech/AmmerCardSDK-Android/blob/master/sample/src/main/java/com/example/sample/MainActivity.kt)