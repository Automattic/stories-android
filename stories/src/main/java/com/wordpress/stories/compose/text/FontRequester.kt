package com.wordpress.stories.compose.text

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.Typeface
import android.util.Log
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.ResourcesCompat.FontCallback
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FontRequestFailReason

/**
 * Handles dispatching requests to ResourcesCompat.getFont() asynchronously. These requests can take a long time,
 * for example when the font is downloadable and there are connection issues.
 *
 * When registering or requesting a font from FontRequester, a fallback Typeface must be specified which will
 * be returned if:
 * - The request to ResourcesCompat hasn't completed yet
 * - The request to ResourcesCompat failed for any reason
 * - The font being requested wasn't previously registered with FontRequester
 */
object FontRequester {
    private val TAG = FontRequester::class.java.simpleName

    val fontMap = mutableMapOf<Int, Typeface>()

    fun registerFont(context: Context, @FontRes fontRes: Int, fallback: Typeface) {
        // Set the font to the fallback right away in case it's requested before getFont completes.
        fontMap[fontRes] = fallback
        try {
            ResourcesCompat.getFont(context, fontRes, fontCallbackFor(context, fontRes), null)
        } catch (e: NotFoundException) {
            Log.e(TAG, "Font ${context.resources.getResourceEntryName(fontRes)} not found")
        }
    }

    fun getFont(@FontRes fontRes: Int, fallback: Typeface): Typeface {
        return fontMap[fontRes] ?: fallback
    }

    private fun fontCallbackFor(context: Context, @FontRes fontRes: Int): FontCallback {
        return object : FontCallback() {
            /**
             * Called when an asynchronous font was finished loading.
             *
             * @param typeface The font that was loaded.
             */
            override fun onFontRetrieved(typeface: Typeface) {
                fontMap[fontRes] = typeface
            }

            /**
             * Called when an asynchronous font failed to load.
             *
             * @param reason The reason the font failed to load. One of
             * [FontRequestFailReason.FAIL_REASON_PROVIDER_NOT_FOUND],
             * [FontRequestFailReason.FAIL_REASON_WRONG_CERTIFICATES],
             * [FontRequestFailReason.FAIL_REASON_FONT_LOAD_ERROR],
             * [FontRequestFailReason.FAIL_REASON_SECURITY_VIOLATION],
             * [FontRequestFailReason.FAIL_REASON_FONT_NOT_FOUND],
             * [FontRequestFailReason.FAIL_REASON_FONT_UNAVAILABLE] or
             * [FontRequestFailReason.FAIL_REASON_MALFORMED_QUERY].
             */
            @Suppress("KDocUnresolvedReference")
            override fun onFontRetrievalFailed(@FontRequestFailReason reason: Int) {
                // Just log the failure - the font mapping is already pointed to the fallback font.
                Log.e(TAG, "Font retrieval for ${context.resources.getResourceEntryName(fontRes)} failed. " +
                        "FontRequestFailReason error code: $reason.")
            }
        }
    }
}
