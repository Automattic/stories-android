package com.automattic.portkey

import android.app.Application
import android.util.Log
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import com.automattic.portkey.R.array
import com.automattic.portkey.util.CrashLoggingUtils

class Portkey : Application() {
    private var statusBarHeight: Int = 0

    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)

        CrashLoggingUtils.startCrashLogging(this)

        initEmojiCompat()
    }

    private fun initEmojiCompat() {
        val config: EmojiCompat.Config

        // Use a downloadable font for EmojiCompat
        val fontRequest = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            "Noto Color Emoji Compat",
            array.com_google_android_gms_fonts_certs
        )
        config = FontRequestEmojiCompatConfig(applicationContext, fontRequest)
        config.setReplaceAll(true)
        config.setUseEmojiAsDefaultStyle(true)

        config.registerInitCallback(object : EmojiCompat.InitCallback() {
            override fun onInitialized() {
                Log.d(TAG, "EmojiCompat initialized")
            }

            override fun onFailed(throwable: Throwable?) {
                Log.d(TAG, "EmojiCompat initialization failed", throwable)
            }
        })

        EmojiCompat.init(config)
    }

    companion object {
        const val TAG = "Portkey"
    }

    fun setStatusBarHeight(newHeight: Int) {
        statusBarHeight = newHeight
    }

    fun getStatusBarHeight(): Int {
        return statusBarHeight
    }
}
