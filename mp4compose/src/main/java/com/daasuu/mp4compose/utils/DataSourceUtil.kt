package com.daasuu.mp4compose.utils

import android.annotation.TargetApi
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.webkit.URLUtil
import java.util.HashMap

class DataSourceUtil {
    companion object {
        @TargetApi(VERSION_CODES.JELLY_BEAN)
        fun setDataSource(
            context: Context,
            uri: Uri,
            mediaExtractor: MediaExtractor? = null,
            mediaMetadataRetriever: MediaMetadataRetriever? = null,
            addedRequestHeaders: Map<String, String>? = null
        ) {
            val isNetworkUrl = URLUtil.isNetworkUrl(uri.toString())
            if (!isNetworkUrl) {
                mediaExtractor?.setDataSource(context, uri, null)
                mediaMetadataRetriever?.setDataSource(context, uri)
            } else if (addedRequestHeaders != null) {
                mediaExtractor?.setDataSource(uri.toString(), addedRequestHeaders)
                mediaMetadataRetriever?.setDataSource(uri.toString(), addedRequestHeaders)
            } else {
                mediaExtractor?.setDataSource(uri.toString(), HashMap<String, String>())
                mediaMetadataRetriever?.setDataSource(uri.toString(), HashMap<String, String>())
            }
        }
    }
}
