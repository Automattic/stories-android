package com.automattic.portkey.compose

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.content.ContextCompat.getDrawable

import com.automattic.portkey.R

class DeleteButton : FrameLayout {
    private var readyForDeleteState = false
    private var deleteButtonClickListener: OnClickListener? = null
    private lateinit var deleteButton: ImageButton

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init()
    }

    private fun init() {
        val view = View.inflate(context, R.layout.content_delete_button, null)
        deleteButton = view.findViewById(R.id.delete_button)
        addView(view)
        setReadyForDelete(false)
    }

    fun setReadyForDelete(isReadyForDelete: Boolean) {
        readyForDeleteState = isReadyForDelete
        if (readyForDeleteState) {
            deleteButton.background = getDrawable(context, R.drawable.bg_oval_white_delete_control)
            deleteButton.setColorFilter(Color.argb(255, 0, 0, 0))
        } else {
            deleteButton.background = getDrawable(context, R.drawable.edit_mode_controls_circle_selector)
            deleteButton.setColorFilter(Color.argb(255, 255, 255, 255))
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        deleteButtonClickListener = l
        super.setOnClickListener {
            onClick(it)
        }
    }

    private fun onClick(view: View) {
        if (!readyForDeleteState) {
            deleteButtonClickListener?.onClick(view)
        }
    }
}
