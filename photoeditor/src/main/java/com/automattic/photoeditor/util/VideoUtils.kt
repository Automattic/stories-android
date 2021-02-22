package com.automattic.photoeditor.util

import android.util.Size

class VideoUtils {
    companion object {
        fun normalizeTargetVideoSize(
            requestedWidth: Int,
            requestedHeight: Int
        ): Size {
            var adjustedSize = Size(requestedWidth, requestedHeight)
            // As per CDD, all android devices running API level 21 (our minSdk) with H.264 codec must support 720 x 480 px.
            // see https://source.android.com/compatibility/5.0/android-5.0-cdd#5_2_video_encoding
            // MUST
            // - 320 x 240 px
            // - 720 x 480 px
            // SHOULD (when hardware available)
            // - 1280 x 720 px
            // - 1920 x 1080 px

            // the other formats (2160p, 1440p, 1080p etc) are "popular" ones found on many devices
            // note we reverse the width/height given we support portrait mode only.
            when {
                // 2160p = 3840x2160
                (requestedWidth % 2160 == 0) -> {
                    adjustedSize = Size(2160, 3840)
                }

                // 1440p = 2560x1440
                (requestedWidth % 1440 == 0) -> {
                    adjustedSize = Size(1440, 2560)
                }

                // 1080p = 1920x1080
                (requestedWidth % 1080 == 0) -> {
                    adjustedSize = Size(1080, 1920)
                }

                // 720p = 1280x720
                (requestedWidth % 720 == 0) -> {
                    adjustedSize = Size(720, 1280)
                }

                (requestedWidth % 480 == 0) -> {
                    adjustedSize = Size(480, 720)
                }

                (requestedWidth % 240 == 0) -> {
                    adjustedSize = Size(240, 320)
                }
            }
            return adjustedSize
        }
    }
}