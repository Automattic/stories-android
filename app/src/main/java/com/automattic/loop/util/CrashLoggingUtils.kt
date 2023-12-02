package com.automattic.loop.util

import android.content.Context
import com.automattic.loop.AppPrefs
import com.automattic.loop.BuildConfig
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

class CrashLoggingUtils {
    companion object {
        fun startCrashLogging(context: Context) {
            if (!AppPrefs.isCrashLoggingEnabled()) return

            SentryAndroid.init(context) { options ->
                options.apply {
                    dsn = BuildConfig.SENTRY_DSN
                    beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                        if (AppPrefs.isCrashLoggingEnabled()) event else null
                    }

                    // TODO: BuildConfig.VERSION_NAME value is removed in AGP 4.1 because the version_name does not reflect the final value
                    // https://developer.android.com/studio/releases/gradle-plugin#4-1-0
                    // We'll start publishing the artifacts to S3 and will not be using this information either, so unfortunately we can't use `version_name` here
                    // If this is an important piece of information, we should look into alternative ways to obtain it. One possibility is to get the client version instead
                    // setTag("version", BuildConfig.VERSION_NAME)
                }
            }
        }
    }
}
