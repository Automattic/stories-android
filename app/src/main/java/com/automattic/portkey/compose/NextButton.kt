package com.automattic.portkey.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

class NextButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        val view = View.inflate(context, com.automattic.portkey.R.layout.content_next_button, null)
        val styledAttrs = context.theme.obtainStyledAttributes(
            attrs,
            com.automattic.portkey.R.styleable.NextButton,
            0, 0
        )

        val customText: String?
        try {
            customText = styledAttrs.getString(com.automattic.portkey.R.styleable.NextButton_text)
        } finally {
            styledAttrs.recycle()
        }
        customText?.let {
            view.findViewById<TextView>(com.automattic.portkey.R.id.next_button_text).text = customText
        }
        addView(view)
    }

    override fun setEnabled(enabled: Boolean) {
        setAlpha(if (enabled) 1f else 0.6f)
        super.setEnabled(enabled)
        invalidate()
    }
}
