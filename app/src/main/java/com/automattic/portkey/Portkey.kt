package com.automattic.portkey

import android.app.Application
import com.automattic.portkey.util.CrashLoggingUtils

class Portkey : Application() {
    override fun onCreate() {
        super.onCreate()

        AppPrefs.init(this)

        CrashLoggingUtils.startCrashLogging(this)
    }
}
