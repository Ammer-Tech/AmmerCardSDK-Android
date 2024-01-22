package com.example.sample

import android.app.Application
import com.example.sample.ingenico.common.DeviceHelper

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        DeviceHelper.me().init(this)
        DeviceHelper.me().bindService()
    }

    override fun onTerminate() {
        super.onTerminate()
        DeviceHelper.me().unbindService()
    }
}