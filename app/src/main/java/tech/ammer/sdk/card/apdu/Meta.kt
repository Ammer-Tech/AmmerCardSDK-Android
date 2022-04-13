package tech.ammer.sdk.card.apdu

class Meta(buffer: ByteArray) {
    val state: Byte
    val type: Byte
    var id: String? = null
    val isNotActivated: Boolean
        get() = state == STATE_NOT_ACTIVATED
    val isLocked: Boolean
        get() = state == STATE_ACTIVATED_LOCKED
    val isUnlocked: Boolean
        get() = state == STATE_ACTIVATED_UNLOCKED

    fun hasId(): Boolean {
        return id != null
    }

    companion object {
        const val META_MAX_LENGTH: Byte = 13
        const val META_MAX_ID_LENGTH: Byte = 10
        const val STATE_NOT_ACTIVATED: Byte = 0x02
        const val STATE_ACTIVATED_LOCKED: Byte = 0x04
        const val STATE_ACTIVATED_UNLOCKED: Byte = 0x08
        const val TYPE_BTC: Byte = 0x00
        const val OFFSET_STATE: Byte = 0x00
        const val OFFSET_TYPE: Byte = 0x01
        const val OFFSET_ID_LENGTH: Byte = 0x02
        const val OFFSET_ID: Byte = 0x03
    }

    init {
        state = buffer[OFFSET_STATE.toInt()]
        type = buffer[OFFSET_TYPE.toInt()]
        if (buffer[OFFSET_ID_LENGTH.toInt()] > 0) {
            val idBytes = buffer.copyOfRange(OFFSET_ID.toInt(), buffer.size)
            id = String(idBytes)
        }
    }
}