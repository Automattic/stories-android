package com.automattic.portkey.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

import com.automattic.portkey.R
import kotlinx.android.synthetic.main.view_compose_popup_menu.view.*

class CustomPopUpMenuView : FrameLayout {
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
        val view = View.inflate(context, R.layout.view_compose_popup_menu, null)
        addView(view)
        setOnClickListener {
            dismissCustomPopupMenu()
        }
    }

    fun setOnDeletePageButtonClickListener(l: OnClickListener?) {
        deleteButtonClickListener = l
        delete_button.setOnClickListener {
            dismissCustomPopupMenu()
            deleteButtonClickListener?.onClick(it)
        }
    }

    fun setTopOffset(offset: Int) {
        val params = view_popup_menu_actual_menu.layoutParams as LayoutParams
        params.topMargin = offset
    }

    private fun dismissCustomPopupMenu() {
        visibility = View.GONE
    }
}
