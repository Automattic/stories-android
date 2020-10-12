package com.wordpress.stories.compose

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import com.wordpress.stories.R

class FinishButton(context: Context, attrs: AttributeSet? = null) : AppCompatImageButton(context, attrs) {
    enum class FinishButtonMode { NEXT, DONE }

    var buttonMode: FinishButtonMode = FinishButtonMode.NEXT
        set(value) {
            field = value
            updateMode()
        }

    init {
        val styledAttrs = context.theme.obtainStyledAttributes(attrs, R.styleable.FinishButton, 0, 0)

        try {
            buttonMode = FinishButtonMode.values()[styledAttrs.getInt(R.styleable.FinishButton_buttonMode, 0)]
        } finally {
            styledAttrs.recycle()
        }

        setBackgroundResource(R.drawable.navigation_controls_circle_selector)
    }

    private fun updateMode() {
        when (buttonMode) {
            FinishButtonMode.NEXT -> {
                contentDescription = resources.getString(R.string.label_next_button)
                setImageResource(R.drawable.ic_arrow_forward)
            }
            FinishButtonMode.DONE -> {
                contentDescription = resources.getString(R.string.label_done_button)
                setImageResource(R.drawable.ic_checkmark)
            }
        }
    }
}
