package com.example.sample

import android.app.Application
import tech.ammer.sdk.card.CardSDK
import tech.ammer.sdk.card.pax.PaxInterface
import tech.ammer.sdk.card.pax.PaxWrapper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        CardSDK.start()
        PaxWrapper.start(this, PaxInterface.PAX_INTERFACE_ALL)
    }
}