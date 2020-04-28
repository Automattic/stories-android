package com.automattic.portkey.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

class NextButton : FrameLayout {
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet? = null) {
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
}
