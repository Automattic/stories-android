package com.wordpress.stories.compose.text

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText

class PortkeyEditText : AppCompatEditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            this.clearFocus()
            super.onKeyPreIme(keyCode, event)
        } else super.onKeyPreIme(keyCode, event)
    }
}
