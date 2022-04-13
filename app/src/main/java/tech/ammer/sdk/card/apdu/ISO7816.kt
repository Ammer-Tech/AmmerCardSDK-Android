package tech.ammer.sdk.card.apdu

internal interface ISO7816 {
    companion object {
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
        const val OFFSET_CLA: Byte = 0
        const val OFFSET_INS: Byte = 1
        const val OFFSET_P1: Byte = 2
        const val OFFSET_P2: Byte = 3
        const val OFFSET_LC: Byte = 4
        const val OFFSET_CDATA: Byte = 5
        const val OFFSET_EXT_CDATA: Byte = 7
        const val CLA_ISO7816: Byte = 0
        const val INS_SELECT: Byte = -92
        const val INS_EXTERNAL_AUTHENTICATE: Byte = -126
    }
}

internal object State {
    const val NOT_INITED: Byte = 2
    const val INITED: Byte = 4
    const val ACTIVATED_LOCKED: Byte = 8
    const val ACTIVATED_UNLOCKED: Byte = 16
}