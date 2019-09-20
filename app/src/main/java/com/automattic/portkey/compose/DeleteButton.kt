package com.automattic.portkey.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getDrawable

import com.automattic.portkey.R

class DeleteButton : FrameLayout {
    private var readyForDeleteState = false
    private var deleteButtonClickListener: OnClickListener? = null

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
        addView(view)
        setReadyForDelete(false)
    }

    fun setReadyForDelete(isReadyForDelete: Boolean) {
        readyForDeleteState = isReadyForDelete
        if (readyForDeleteState) {
            background = getDrawable(context, R.drawable.delete_button_background_ready)
        } else {
            background = getDrawable(context, R.drawable.delete_button_background_normal)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        deleteButtonClickListener = l
        super.setOnClickListener {
            // no op
            onClick(it)
        }
    }

    fun onClick(view: View) {
        if (readyForDeleteState) {
            // no op
        } else {
            deleteButtonClickListener?.onClick(view)
        }
    }
}
