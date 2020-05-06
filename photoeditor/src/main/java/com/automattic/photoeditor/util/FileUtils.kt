package com.automattic.photoeditor.util

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import com.automattic.photoeditor.R
import com.automattic.photoeditor.SaveSettings
import java.io.File
import java.io.FileOutputStream

class FileUtils {
    companion object {
        const val TEMP_FILE_NAME_PREFIX = "tmp_wp_story"
        const val OUTPUT_FILE_NAME_PREFIX = "wp_story"

        fun getLoopFrameFile(context: Context, video: Boolean, seqId: String = ""): File {
            return File(getOutputDirectory(context),
                OUTPUT_FILE_NAME_PREFIX +
                        System.currentTimeMillis() + "_" + seqId + if (video) ".mp4" else ".jpg"
            )
        }

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }

        fun isAvailableSpaceLow(context: Context): Boolean {
            // if available space is at or below 10%, consider it low
            val appContext = context.applicationContext
            return (appContext.cacheDir.usableSpace * 100 / appContext.cacheDir.totalSpace <= 10)
        }

        /* internal / disposable files used in capturing */
        fun getInternalDirectory(context: Context): File {
            return context.getDir("tmp", 0)
        }

        fun getTempCaptureFile(context: Context, video: Boolean): File {
            return File(getInternalDirectory(context),
                TEMP_FILE_NAME_PREFIX +
                        System.currentTimeMillis() + if (video) ".mp4" else ".jpg"
            )
        }

        fun saveViewToFile(
            imagePath: String,
            saveSettings: SaveSettings,
            view: View
        ) {
            val file = File(imagePath)
            val out = FileOutputStream(file, false)
            val wholeBitmap = if (saveSettings.isTransparencyEnabled)
                BitmapUtil.removeTransparency(BitmapUtil.createBitmapFromView(view))
            else
                BitmapUtil.createBitmapFromView(view)
            wholeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        }
    }
}
