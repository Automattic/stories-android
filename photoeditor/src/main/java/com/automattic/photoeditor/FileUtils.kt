package com.automattic.photoeditor

import android.os.Environment
import java.io.File

class FileUtils {
    companion object {
        fun getLoopFrameFile(video: Boolean, prefix: String = "") : File {
            return File(
                Environment.getExternalStorageDirectory().toString()
                        + File.separator + prefix + "loop_"
                        + System.currentTimeMillis() + if (video) ".mp4" else ".png"
            )
        }
    }
}

