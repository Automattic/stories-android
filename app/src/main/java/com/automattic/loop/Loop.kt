package com.automattic.loop

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.CameraFactory
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import com.automattic.loop.R.array
import com.automattic.loop.R.string
import com.automattic.loop.util.CrashLoggingUtils
import com.wordpress.stories.compose.NotificationTrackerProvider
import com.wordpress.stories.compose.frame.StoryNotificationType

open class Loop : Application(), NotificationTrackerProvider, CameraXConfig.Provider {
    private var statusBarHeight: Int = 0
    private var test: CameraFactory? = null

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig(this)
    }

    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)

        CrashLoggingUtils.startCrashLogging(this)

        createNotificationChannelsOnSdk26()

        initEmojiCompat()
    }

    // copied from WPAndroid to match the same level notification channels used there
    private fun createNotificationChannelsOnSdk26() {
        // create Notification channels introduced in Android Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NORMAL channel (used for likes, comments, replies, etc.)
            val normalChannel = NotificationChannel(
                getString(string.notification_channel_normal_id),
                getString(string.notification_channel_general_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(normalChannel)

            // Create the IMPORTANT channel (used for 2fa auth, for example)
            val importantChannel = NotificationChannel(
                getString(string.notification_channel_important_id),
                getString(string.notification_channel_important_title),
                NotificationManager.IMPORTANCE_HIGH
            )
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(importantChannel)

            // Create the REMINDER channel (used for various reminders, like Quick Start, etc.)
            val reminderChannel = NotificationChannel(
                getString(string.notification_channel_reminder_id),
                getString(string.notification_channel_reminder_title),
                NotificationManager.IMPORTANCE_LOW
            )
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(reminderChannel)

            // Create the TRANSIENT channel (used for short-lived notifications such as processing a Like/Approve,
            // or media upload)
            val transientChannel = NotificationChannel(
                getString(string.notification_channel_transient_id),
                getString(string.notification_channel_transient_title),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(transientChannel)
        }
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
        const val TAG = "Loop"
    }

    fun setStatusBarHeight(newHeight: Int) {
        statusBarHeight = newHeight
    }

    fun getStatusBarHeight(): Int {
        return statusBarHeight
    }

    override fun trackShownNotification(storyNotificationType: StoryNotificationType) {
        // example
        Toast.makeText(this, "Notification shown! : ", Toast.LENGTH_LONG).show()
    }

    override fun trackTappedNotification(storyNotificationType: StoryNotificationType) {
        // example
        Toast.makeText(this, "Notification tapped! : " + storyNotificationType, Toast.LENGTH_LONG).show()
    }

    override fun trackDismissedNotification(storyNotificationType: StoryNotificationType) {
        // example
        Toast.makeText(this, "Notification dismissed! : " + storyNotificationType, Toast.LENGTH_LONG).show()
    }
}
