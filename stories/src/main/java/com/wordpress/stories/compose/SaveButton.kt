package com.wordpress.stories.compose

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getDrawable
import com.wordpress.stories.R
import com.wordpress.stories.databinding.ContentSaveButtonBinding

class SaveButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var savingState = false
    private var saveButtonClickListener: OnClickListener? = null

    private var binding: ContentSaveButtonBinding? = null

    init {
        binding = ContentSaveButtonBinding.inflate(LayoutInflater.from(context))
//        val view = View.inflate(context, R.layout.content_save_button, null)
        addView(binding?.root)
        setSaving(false)
    }

    fun setSaving(isSaving: Boolean) {
        savingState = isSaving
        binding?.run {
            if (savingState) {
                saveButtonText.setText(R.string.label_control_saving)
                saveButtonIcon.visibility = View.INVISIBLE
                saveButtonSpinner.visibility = View.VISIBLE
                saveButtonSavedIcon.visibility = View.INVISIBLE
                background = getDrawable(context, R.drawable.save_button_background_disabled)
            } else {
                saveButtonText.setText(R.string.label_control_retry)
                saveButtonIcon.visibility = View.VISIBLE
                saveButtonSpinner.visibility = View.INVISIBLE
                saveButtonSavedIcon.visibility = View.INVISIBLE
                background = getDrawable(context, R.drawable.save_button_background_enabled)
            }
        }
    }

    fun showSavedAnimation(callback: Runnable?) {
        if (savingState) {
            binding?.run {
                savingState = false
                saveButtonText.setText(R.string.label_control_saved)
                saveButtonIcon.visibility = View.INVISIBLE
                saveButtonSpinner.visibility = View.INVISIBLE
                saveButtonSavedIcon.visibility = View.VISIBLE
                background = getDrawable(context, R.drawable.save_button_background_disabled)
                postDelayed({
                    callback?.run()
                }, 250)
            }
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
