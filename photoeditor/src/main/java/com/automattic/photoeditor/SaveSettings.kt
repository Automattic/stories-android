package com.automattic.photoeditor

import android.graphics.Bitmap

/**
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @since 8/8/2018
 * Builder Class to apply multiple save options
 */
class SaveSettings private constructor(builder: Builder) {
    internal val isTransparencyEnabled: Boolean
    internal val isClearViewsEnabled: Boolean

    init {
        this.isClearViewsEnabled = builder.isClearViewsEnabled
        this.isTransparencyEnabled = builder.isTransparencyEnabled
    }

    data class Builder(var isTransparencyEnabled: Boolean = true, var isClearViewsEnabled: Boolean = true) {
        /**
         * Define a flag to enable transparency while saving image
         *
         * @param transparencyEnabled true if enabled
         * @return Builder
         * @see BitmapUtil.removeTransparency
         */
        fun setTransparencyEnabled(transparencyEnabled: Boolean): Builder {
            isTransparencyEnabled = transparencyEnabled
            return this
        }

        /**
         * Define a flag to clear the view after saving the image
         *
         * @param clearViewsEnabled true if you want to clear all the views on [PhotoEditorView]
         * @return Builder
         */
        fun setClearViewsEnabled(clearViewsEnabled: Boolean): Builder {
            isClearViewsEnabled = clearViewsEnabled
            return this
        }

        fun build(): SaveSettings {
            return SaveSettings(this)
        }
    }
}
