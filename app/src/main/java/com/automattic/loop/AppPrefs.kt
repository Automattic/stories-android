package com.automattic.loop

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import com.automattic.loop.util.PreferenceUtils

// Guaranteed to hold a reference to the application context, which is safe
@SuppressLint("StaticFieldLeak")
object AppPrefs {
    private interface PrefKey

    private lateinit var context: Context

    /**
     * Application related preferences. When the user logs out, these preferences are erased.
     */
    private enum class DeletablePrefKey : PrefKey {
        CRASH_LOGGING_ENABLED,
    }

    /**
     * These preferences won't be deleted when the user disconnects.
     * They should be used for device specific or user-independent preferences.
     */
    private enum class UndeletablePrefKey : PrefKey {
        // Whether we need to show the intro flow
        INTRO_REQUIRED,
    }

    fun init(context: Context) {
        AppPrefs.context = context.applicationContext
    }

    fun isIntroRequired(): Boolean {
        return getBoolean(UndeletablePrefKey.INTRO_REQUIRED, true)
    }

    fun setIntroRequired(required: Boolean) {
        setBoolean(UndeletablePrefKey.INTRO_REQUIRED, required)
    }

    fun isCrashLoggingEnabled(): Boolean {
        return getBoolean(DeletablePrefKey.CRASH_LOGGING_ENABLED, true)
    }

    fun setCrashLoggingEnabled(enabled: Boolean) {
        setBoolean(DeletablePrefKey.CRASH_LOGGING_ENABLED, enabled)
    }

    /**
     * Remove all user-related preferences.
     */
    fun reset() {
        val editor = getPreferences().edit()
        DeletablePrefKey.values().forEach { editor.remove(it.name) }
        editor.apply()
    }

    private fun getInt(key: PrefKey, default: Int = 0) =
            PreferenceUtils.getInt(getPreferences(), key.toString(), default)

    private fun setInt(key: PrefKey, value: Int) =
            PreferenceUtils.setInt(getPreferences(), key.toString(), value)

    private fun getString(key: PrefKey, defaultValue: String = ""): String {
        return PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue)?.let {
            it
        } ?: defaultValue
    }

    private fun setString(key: PrefKey, value: String) =
            PreferenceUtils.setString(getPreferences(), key.toString(), value)

    private fun getBoolean(key: PrefKey, default: Boolean) =
            PreferenceUtils.getBoolean(getPreferences(), key.toString(), default)

    private fun setBoolean(key: PrefKey, value: Boolean = false) =
            PreferenceUtils.setBoolean(getPreferences(), key.toString(), value)

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun remove(key: PrefKey) {
        getPreferences().edit().remove(key.toString()).apply()
    }
}
