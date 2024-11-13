package tech.ammer.sdk.card

import android.nfc.NfcAdapter

interface CardControllerListener {
    fun processCommand(byteArray: ByteArray): ByteArray?
    fun onAppletNotSelected(message: String?) {}
}