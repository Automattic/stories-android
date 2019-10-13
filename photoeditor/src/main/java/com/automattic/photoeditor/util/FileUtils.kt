package com.automattic.photoeditor.util

import android.content.Context
import com.automattic.photoeditor.R
import java.io.File

class FileUtils {
    companion object {
        fun getLoopFrameFile(context: Context, video: Boolean, prefix: String = ""): File {
            return File(getOutputDirectory(context),
                "loop_" +
                        System.currentTimeMillis() + if (video) ".mp4" else ".jpg"
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

        /* internal / disposable files used in capturing */
        fun getInternalDirectory(context: Context): File {
            return context.getDir("tmp", 0)
        }

        fun getCaptureFile(context: Context, video: Boolean, prefix: String = ""): File {
            return File(getInternalDirectory(context),
                "loop_tmp" +
                        System.currentTimeMillis() + if (video) ".mp4" else ".jpg"
            )
        }
    }
}
