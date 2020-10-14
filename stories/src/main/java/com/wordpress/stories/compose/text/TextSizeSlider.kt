package com.wordpress.stories.compose.text

import android.content.res.Resources
import android.util.TypedValue
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView

class TextSizeSlider(
    private val seekBar: SeekBar,
    private val textView: TextView,
    private val resources: Resources,
    onProgressCallback: () -> Unit = {}
) {
    init {
        seekBar.max = TEXT_SIZE_SLIDER_MAX // This corresponds to a font size of MAX + MIN_VALUE sp

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                        (progress * TEXT_SIZE_SLIDER_STEP + TEXT_SIZE_SLIDER_MIN_VALUE).toFloat())
                onProgressCallback()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun update() {
        val fontSizeSp = (textView.textSize / resources.displayMetrics.scaledDensity).toInt()
        seekBar.progress = (fontSizeSp - TEXT_SIZE_SLIDER_MIN_VALUE) / TEXT_SIZE_SLIDER_STEP
    }

    companion object {
        const val TEXT_SIZE_SLIDER_MAX = 20
        const val TEXT_SIZE_SLIDER_MIN_VALUE = 14
        const val TEXT_SIZE_SLIDER_STEP = 2
    }
}
