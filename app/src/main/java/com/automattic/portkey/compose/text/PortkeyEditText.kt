package com.automattic.portkey.compose.text

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.EditText
import android.view.KeyEvent

class PortkeyEditText : EditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?,    defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.getKeyCode() === KeyEvent.KEYCODE_BACK) {
            this.clearFocus()
            super.onKeyPreIme(keyCode, event)
        } else super.onKeyPreIme(keyCode, event)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (focused) {
            setSelection(selectionStart, selectionEnd)
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }
}
