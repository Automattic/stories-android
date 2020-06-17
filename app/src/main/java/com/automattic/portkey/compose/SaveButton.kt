package com.automattic.portkey.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getDrawable

import com.automattic.portkey.R
import kotlinx.android.synthetic.main.content_save_button.view.*

class SaveButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var savingState = false
    private var saveButtonClickListener: OnClickListener? = null

    init {
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
            save_button_saved_icon.visibility = View.INVISIBLE
            background = getDrawable(context, R.drawable.save_button_background_disabled)
        } else {
            save_button_text.setText(R.string.label_control_retry)
            save_button_icon.visibility = View.VISIBLE
            save_button_spinner.visibility = View.INVISIBLE
            save_button_saved_icon.visibility = View.INVISIBLE
            background = getDrawable(context, R.drawable.save_button_background_enabled)
        }
    }

    fun showSavedAnimation(callback: Runnable?) {
        if (savingState) {
            savingState = false
            save_button_text.setText(R.string.label_control_saved)
            save_button_icon.visibility = View.INVISIBLE
            save_button_spinner.visibility = View.INVISIBLE
            save_button_saved_icon.visibility = View.VISIBLE
            background = getDrawable(context, R.drawable.save_button_background_disabled)
            postDelayed({
                callback?.run()
            }, 250)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        saveButtonClickListener = l
        super.setOnClickListener {
            onClick(it)
        }
    }

    private fun onClick(view: View) {
        if (!savingState) {
            saveButtonClickListener?.onClick(view)
        }
    }
}
