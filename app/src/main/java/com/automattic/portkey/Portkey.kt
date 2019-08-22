package com.automattic.portkey

import androidx.multidex.MultiDexApplication

class Portkey : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        AppPrefs.init(this)
    }
}
