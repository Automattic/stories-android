package com.automattic.portkey

import android.app.Application
import com.automattic.portkey.util.CrashLoggingUtils

class Portkey : Application() {
    private var statusBarHeight: Int = 0

    override fun onCreate() {
        super.onCreate()

        AppPrefs.init(this)

        CrashLoggingUtils.startCrashLogging(this)
    }

    fun setStatusBarHeight(newHeight: Int) {
        statusBarHeight = newHeight
    }

    fun getStatusBarHeight(): Int {
        return statusBarHeight
    }
}
