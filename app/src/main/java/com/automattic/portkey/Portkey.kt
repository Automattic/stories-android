package com.automattic.portkey

import android.app.Application

class Portkey : Application() {
    override fun onCreate() {
        super.onCreate()

        AppPrefs.init(this)
    }
}
