package com.automattic.portkey.util

import com.automattic.portkey.AppPrefs
import com.automattic.portkey.BuildConfig
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.event.BreadcrumbBuilder

class CrashLoggingUtils {
    companion object {
        @JvmStatic fun shouldEnableCrashLogging(context: android.content.Context): Boolean {
            return AppPrefs.isCrashLoggingEnabled()
        }

        @JvmStatic fun enableCrashLogging(context: android.content.Context) {
            Sentry.init(BuildConfig.SENTRY_DSN, AndroidSentryClientFactory(context))
            Sentry.getContext().addTag("version", BuildConfig.VERSION_NAME)
        }

        @JvmStatic fun startCrashLogging(context: android.content.Context) {
            if (shouldEnableCrashLogging(context)) {
                enableCrashLogging(context)
            }
        }

        @JvmStatic fun stopCrashLogging() {
            Sentry.clearContext()
            Sentry.close()
        }

        @JvmStatic fun log(message: String?) {
            if (message == null) {
                return
            }

            Sentry.getContext().recordBreadcrumb(
                BreadcrumbBuilder().setMessage(message).build()
            )
        }

        @JvmStatic fun log(exception: Throwable) {
            Sentry.capture(exception)
        }

        @JvmStatic fun logException(tr: Throwable) {
            log(tr)
        }

        @JvmStatic fun logException(tr: Throwable, message: String?) {
            log(message)
            log(tr)
        }
    }
}
