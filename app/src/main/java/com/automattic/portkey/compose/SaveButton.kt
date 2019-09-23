package com.automattic.portkey.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getDrawable

import com.automattic.portkey.R
import kotlinx.android.synthetic.main.content_save_button.view.*

class SaveButton : FrameLayout {
    private var savingState = false
    private var saveButtonClickListener: OnClickListener? = null

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
        val view = View.inflate(context, R.layout.content_save_button, null)
        addView(view)
        setSaving(false)
    }

    fun setSaving(isSaving: Boolean) {
        savingState = isSaving
        if (savingState) {
            save_button_text.setText(R.string.label_control_saving)
            save_button_icon.visibility = View.INVISIBLE
            save_button_spinner.visibility = View.VISIBLE
            background = getDrawable(context, R.drawable.save_button_background_disabled)
        } else {
            save_button_text.setText(R.string.label_control_save)
            save_button_icon.visibility = View.VISIBLE
            save_button_spinner.visibility = View.INVISIBLE
            background = getDrawable(context, R.drawable.save_button_background_enabled)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        saveButtonClickListener = l
        super.setOnClickListener {
            // no op
            onClick(it)
        }
    }

    fun onClick(view: View) {
        if (savingState) {
            // no op
        } else {
            saveButtonClickListener?.onClick(view)
        }
    }
}
