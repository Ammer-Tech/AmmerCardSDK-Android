package com.example.sample

import android.app.Application
import tech.ammer.sdk.card.CardSDK

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        CardSDK.start()
    }
}