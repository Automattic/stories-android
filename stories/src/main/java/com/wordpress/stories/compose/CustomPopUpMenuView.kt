package com.wordpress.stories.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.wordpress.stories.R

import kotlinx.android.synthetic.main.view_compose_popup_menu.view.*

class CustomPopUpMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var deleteButtonClickListener: OnClickListener? = null

    init {
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
