package com.automattic.portkey.util

import java.util.Locale
// comes from WPAndroid WordPressUtils
fun isVideo(url: String?): Boolean {
    var url: String? = url ?: return false
    url = url!!.toLowerCase(Locale.ROOT)
    return (url.endsWith(".ogv") || url.endsWith(".mp4") || url.endsWith(".m4v") ||
            url.endsWith(".mov") || url.endsWith(".wmv") || url.endsWith(".avi") ||
            url.endsWith(".mpg") || url.endsWith(".3gp") || url.endsWith(".3g2") ||
            url.contains("video"))
}
