package com.automattic.portkey.compose.text

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.portkey.R
import kotlinx.android.synthetic.main.add_text_dialog.*
import kotlinx.android.synthetic.main.add_text_dialog.view.*

/**
 * Created by Burhanuddin Rashid on 1/16/2018.
 */

class TextEditorDialogFragment : DialogFragment() {
    private var inputMethodManager: InputMethodManager? = null
    private var colorCode: Int = 0
    private var textEditor: TextEditor? = null

    interface TextEditor {
        fun onDone(inputText: String, colorCode: Int)
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.add_text_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Setup the color picker for text color
        val addTextColorPickerRecyclerView = view.add_text_color_picker_recycler_view
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        addTextColorPickerRecyclerView.layoutManager = layoutManager
        addTextColorPickerRecyclerView.setHasFixedSize(true)
        activity?.let {
            val colorPickerAdapter = ColorPickerAdapter(it)
            // This listener will change the text color when clicked on any color from picker
            colorPickerAdapter.setOnColorPickerClickListener { colorCode ->
                this.colorCode = colorCode
                add_text_edit_text?.setTextColor(colorCode)
            }
            addTextColorPickerRecyclerView.adapter = colorPickerAdapter
        }

        arguments?.let {
            add_text_edit_text?.setText(it.getString(EXTRA_INPUT_TEXT))
            colorCode = it.getInt(EXTRA_COLOR_CODE)
            add_text_edit_text?.setTextColor(colorCode)
        }
        inputMethodManager?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        // Make a callback on activity when user is done with text editing
        add_text_done_tv?.setOnClickListener { textView ->
            inputMethodManager?.hideSoftInputFromWindow(textView.windowToken, 0)
            dismiss()
            val inputText = add_text_edit_text?.text.toString()
            textEditor?.onDone(inputText, colorCode)
        }
    }

    // Callback to listener if user is done with text editing
    fun setOnTextEditorListener(textEditor: TextEditor) {
        this.textEditor = textEditor
    }

    companion object {
        private val TAG = TextEditorDialogFragment::class.java.simpleName
        const val EXTRA_INPUT_TEXT = "extra_input_text"
        const val EXTRA_COLOR_CODE = "extra_color_code"

        // Show dialog with provide text and text color
        @JvmOverloads
        fun show(
            appCompatActivity: AppCompatActivity,
            inputText: String = "",
            @ColorInt colorCode: Int = ContextCompat.getColor(appCompatActivity, R.color.white)
        ): TextEditorDialogFragment {
            return TextEditorDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_INPUT_TEXT, inputText)
                    putInt(EXTRA_COLOR_CODE, colorCode)
                }
                show(appCompatActivity.supportFragmentManager,
                    TAG
                )
            }
        }
    }
} // Show dialog with default text input as empty and text color white
