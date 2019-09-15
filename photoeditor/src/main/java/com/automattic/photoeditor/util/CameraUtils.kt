package com.automattic.photoeditor.util

import android.util.Log
import android.util.Size
import java.lang.Long
import java.util.Collections
import java.util.Comparator

class CameraUtils {
    companion object {
        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        val MAX_PREVIEW_HEIGHT = 1080

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as
         * the respective max size, and whose aspect ratio matches with the specified value. If such
         * size doesn't exist, choose the largest one that is at most as large as the respective max
         * size, and whose aspect ratio matches with the specified value.
         *
         * @param choices The list of sizes that the camera supports for the intended
         *                          output class
         * @param textureViewWidth The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth The maximum width that can be chosen
         * @param maxHeight The maximum height that can be chosen
         * @param aspectRatio The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        @JvmStatic fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {
            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Log.e("CameraUtils", "Couldn't find any suitable preview size")
                return choices[0]
            }
        }
    }

    internal class CompareSizesByArea : Comparator<Size> {
        // We cast here to ensure the multiplications won't overflow
        override fun compare(lhs: Size, rhs: Size) =
            Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
    }
}
