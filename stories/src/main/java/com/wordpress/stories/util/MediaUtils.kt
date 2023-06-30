package com.wordpress.stories.util

import java.util.Locale
// comes from WPAndroid WordPressUtils
fun isVideo(rawUrl: String?): Boolean {
    rawUrl?.lowercase(Locale.ROOT)?.let { url ->
        return (url.endsWith(".ogv") || url.endsWith(".mp4") || url.endsWith(".m4v") ||
                url.endsWith(".mov") || url.endsWith(".wmv") || url.endsWith(".avi") ||
                url.endsWith(".mpg") || url.endsWith(".3gp") || url.endsWith(".3g2") ||
                url.contains("video"))
    }
    return false
}
