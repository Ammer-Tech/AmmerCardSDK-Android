package tech.ammer.sdk.card.apdu

internal object Instructions {
    const val INS_GET_STATE: Byte = 0x01
    const val INS_INIT: Byte = 0x02
    const val INS_GET_CARD_GUID: Byte = 0x03
    const val INS_GET_CARD_ISSUER: Byte = 0x04
    const val INS_GET_CARD_SERIES: Byte = 0x05
    const val INS_GET_PROCESSING_PUBLIC_KEY: Byte = 0x06
    const val INS_SET_PROCESSING_PUBLIC_KEY: Byte = 0x07
    const val INS_ACTIVATE: Byte = 0x08
    const val INS_ACTIVATE_WITH_KEYS: Byte = 0x09
    const val INS_LOCK: Byte = 0x0A
    const val INS_UNLOCK: Byte = 0x0B
    const val INS_CHANGE_PIN: Byte = 0x0C
    const val INS_GET_PIN_RETRIES: Byte = 0x0D
    const val INS_GET_PUBLIC_KEY: Byte = 0x0E
    const val INS_EXPORT_PRIVATE_KEY: Byte = 0x0F
    const val INS_DISABLE_PRIVATE_KEY_EXPORT: Byte = 0x10
    const val INS_SIGN_DATA: Byte = 0x11
    const val INS_SIGN_PROCESSING_DATA: Byte = 0x12

    /* Since 2.2 - EdDSA */
    const val INS_ED_GET_PUBLIC_KEY: Byte = 0x13
    const val INS_ED_SIGN_DATA: Byte = 0x14
    const val INS_ED_SIGN_PROCESSING_DATA: Byte = 0x15

    const val INS_STATUS_TRANSACTION: Byte = 0x21
    const val INS_ALGORITHM_SIGN: Byte = 0x22
    const val INS_TRANSACTION_INFO: Byte = 0x23

    const val ERROR_DEBIT_RESPONSE = 0x11.toByte()
    const val ERROR_AUTH_RESPONSE = 0x13.toByte()
    const val ERROR_AUTH_RESPONSE2 = 0x15.toByte()
    const val ERROR_SUCCESS_RESPONSE = 0x16.toByte()

    const val INS_ECDH_HANDSHAKE: Byte = 0x16
}

internal object Tags {
    const val STATE: Byte = 0x01
    const val CARD_GUID: Byte = 0x02
    const val CARD_ISSUER: Byte = 0x03
    const val CARD_SERIES: Byte = 0x04
    const val PROCESSING_PUBLIC_KEY: Byte = 0x05
    const val CARD_PIN: Byte = 0x06
    const val CARD_PIN_RETRIES: Byte = 0x07
    const val CARD_PUBLIC_KEY: Byte = 0x08
    const val CARD_PRIVATE_KEY: Byte = 0x09
    const val DATA_FOR_SIGN: Byte = 0x0A
    const val DATA_SIGNATURE: Byte = 0x0B

    /* Since 3.0 */
    const val ECDH_NONCE: Byte = 0x11
    const val ED_CARD_PRIVATE_KEY: Byte = 0x12
    const val STATE_LENGTH: Byte = 1
    const val CARD_GUID_LENGTH: Byte = 16
    const val CARD_ISSUER_LENGTH: Byte = 2
    const val CARD_SERIES_LENGTH: Byte = 2
    const val PROCESSING_PUBLIC_KEY_LENGTH: Byte = 65
    const val CARD_PIN_LENGTH: Byte = 6
    const val CARD_PIN_RETRIES_LENGTH: Byte = 1
    const val CARD_PUBLIC_KEY_LENGTH: Byte = 65
    const val CARD_PRIVATE_KEY_LENGTH: Byte = 32
    const val DATA_FOR_SIGN_LENGTH: Byte = 32
    const val DATA_SIGNATURE_MIN_LENGTH: Byte = 70
    const val DATA_SIGNATURE_MAX_LENGTH: Byte = 72


    /* Since 2.2 - EdDSA */
    const val ASSET_ID: Byte = 0x20
    const val AMOUNT_TX: Byte = 0x21
    const val ORDER_ID: Byte = 0x22
    const val IS_ED_KEY: Byte = 0x23

    const val ED_CARD_PUBLIC_KEY: Byte = 0x0C
    const val ED_DATA_SIGNATURE: Byte = 0x10
    const val ED_CARD_PUBLIC_KEY_ENCODED: Byte = 0x0D
    const val ED_PRIVATE_NONCE: Byte = 0x0E
    const val ED_PUBLIC_NONCE: Byte = 0x0F

    const val ED_CARD_PUBLIC_KEY_LENGTH: Byte = 65
    const val ED_CARD_PUBLIC_KEY_ENCODED_LENGTH: Byte = 32
    const val ED_PRIVATE_NONCE_LENGTH: Byte = 32
    const val ED_PUBLIC_NONCE_LENGTH: Byte = 32
    const val ED_DATA_SIGNATURE_LENGTH: Byte = 64

    /* Since 3.0 */
    const val ECDH_NONCE_LENGTH: Byte = 32
    const val ED_CARD_PRIVATE_KEY_LENGTH: Byte = 32
}

internal object ISO7816{
    const val OFFSET_CLA: Byte = 0
    const val OFFSET_INS: Byte = 1
    const val OFFSET_P1: Byte = 2
    const val OFFSET_P2: Byte = 3
    const val OFFSET_LC: Byte = 4

    var CLA_ISO7816: Byte = 0

    var INS_SELECT: Byte = -92
    var INS_EXTERNAL_AUTHENTICATE: Byte = -126
}

object CardErrors {
    const val SW_NO_ERROR: Short = -28672
    const val SW_BYTES_REMAINING_00: Short = 24832
    const val SW_WRONG_LENGTH: Short = 26368
    const val SW_SECURITY_STATUS_NOT_SATISFIED: Short = 27010
    const val SW_FILE_INVALID: Short = 27011
    const val SW_AUTHENTICATION_METHOD_BLOCKED: Short = 27011
    const val SW_DATA_INVALID: Short = 27012
    const val SW_CONDITIONS_NOT_SATISFIED: Short = 27013
    const val SW_COMMAND_NOT_ALLOWED: Short = 27014
    const val SW_APPLET_SELECT_FAILED: Short = 27033
    const val SW_WRONG_DATA: Short = 27264
    const val SW_FUNC_NOT_SUPPORTED: Short = 27265
    const val SW_FILE_NOT_FOUND: Short = 27266
    const val SW_RECORD_NOT_FOUND: Short = 27267
    const val SW_INCORRECT_P1P2: Short = 27270
    const val SW_WRONG_P1P2: Short = 27392
    const val SW_CORRECT_LENGTH_00: Short = 27648
    const val SW_INS_NOT_SUPPORTED: Short = 27904
    const val SW_CLA_NOT_SUPPORTED: Short = 28160
    const val SW_UNKNOWN: Short = 28416
    const val SW_FILE_FULL: Short = 27268
    const val SW_LOGICAL_CHANNEL_NOT_SUPPORTED: Short = 26753
    const val SW_SECURE_MESSAGING_NOT_SUPPORTED: Short = 26754
    const val SW_WARNING_STATE_UNCHANGED: Short = 25088
    const val SW_LAST_COMMAND_EXPECTED: Short = 26755
    const val SW_COMMAND_CHAINING_NOT_SUPPORTED: Short = 26756
    const val SIGN_NO_VERIFY: Short = 26759
    const val TAG_WAL_LOST: Short = 26761
    const val OTHER_ALGORITHM: Short = 26762
    const val SW_CARD_BLOCK: Short = -27901
}

internal object State {
    const val NOT_INITED: Byte = 2
    const val INITED: Byte = 4
    const val ACTIVATED_LOCKED: Byte = 8
    const val ACTIVATED_UNLOCKED: Byte = 16
}