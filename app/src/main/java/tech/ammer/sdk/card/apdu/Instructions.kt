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

    const val STATE_MIN_LENGTH: Short = 1
    const val STATE_MAX_LENGTH: Short = 1
    const val CARD_GUID_MIN_LENGTH: Short = 16
    const val CARD_GUID_MAX_LENGTH: Short = 16
    const val CARD_ISSUER_MIN_LENGTH: Short = 1
    const val CARD_ISSUER_MAX_LENGTH: Short = 1
    const val CARD_SERIES_MIN_LENGTH: Short = 1
    const val CARD_SERIES_MAX_LENGTH: Short = 1
    const val PROCESSING_PUBLIC_KEY_MIN_LENGTH: Short = 65
    const val PROCESSING_PUBLIC_KEY_MAX_LENGTH: Short = 65
    const val CARD_PIN_MIN_LENGTH: Short = 5
    const val CARD_PIN_MAX_LENGTH: Byte = 0x5
    const val CARD_PUBLIC_KEY_MIN_LENGTH: Short = 65
    const val CARD_PUBLIC_KEY_MAX_LENGTH: Short = 65
    const val CARD_PRIVATE_KEY_MIN_LENGTH: Short = 32
    const val CARD_PRIVATE_KEY_MAX_LENGTH: Short = 32
    const val DATA_FOR_SIGN_MIN_LENGTH: Short = 32
    const val DATA_FOR_SIGN_MAX_LENGTH: Short = 32
    const val DATA_SIGNATURE_MIN_LENGTH: Short = 70
    const val DATA_SIGNATURE_MAX_LENGTH: Short = 72
}