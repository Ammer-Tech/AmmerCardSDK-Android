package com.example.sample.ingenico.common

import android.os.RemoteException
import com.usdk.apiservice.aidl.data.IntValue
import com.usdk.apiservice.aidl.system.nfc.NfcState
import com.usdk.apiservice.aidl.system.nfc.UNfc

object NfcMan {
    private var uNfc: UNfc? = null

    fun start() {
        if (uNfc == null) {
            uNfc = DeviceHelper.me().nfc
        }
    }

    fun nfcOn() {
        var retry = 0
        while (retry < 5) {
            doNfcOn()
            if (checkIsOn()) {
                return
            }
            try {
                Thread.sleep(500)
            } catch (ignored: InterruptedException) {
            }
            retry++
        }
    }

    fun doNfcOff() {
        try {
            uNfc!!.setNfcState(NfcState.DISENABLED)
        } catch (ignored: RemoteException) {
        }
    }

    private fun doNfcOn() {
        try {
            uNfc!!.setNfcState(NfcState.ENABLED)
        } catch (ignored: RemoteException) {
        }
    }

    fun checkIsOn(): Boolean {
        val intValue = IntValue()
        try {
            uNfc!!.getNfcState(intValue)
        } catch (e: RemoteException) {
            return false
        }
        return intValue.data == 1
    }

    private const val TAG = "NfcMan"
}
