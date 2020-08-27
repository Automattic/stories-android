package com.wordpress.stories.compose.text

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId
import com.automattic.photoeditor.text.TextStyler
import com.wordpress.stories.R
import com.wordpress.stories.compose.StoriesAnalyticsListener
import kotlinx.android.synthetic.main.add_text_dialog.*
import kotlinx.android.synthetic.main.add_text_dialog.view.*

/**
 * Created by Burhanuddin Rashid on 1/16/2018.
 */

class TextEditorDialogFragment : DialogFragment() {
    private var colorCode: Int = 0
    private lateinit var textAlignment: TextAlignment
    @TypefaceId private var typefaceId: Int = 0
    private var textEditor: TextEditor? = null

    private lateinit var textStyleGroupManager: TextStyleGroupManager

    private var analyticsListener: StoriesAnalyticsListener? = null
    private var textEditorAnalyticsHandler: TextEditorAnalyticsHandler? = null

    interface TextEditor {
        fun onDone(inputText: String, textStyler: TextStyler)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        textStyleGroupManager = TextStyleGroupManager(context)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

            attributes = attributes.apply { dimAmount = 0.5f } // The default dimAmount is 0.6
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.add_text_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        text_alignment_button.setOnClickListener {
            textAlignment = TextAlignment.getNext(textAlignment)
            updateTextAlignment(textAlignment)
        }

        activity?.let {
            text_style_toggle_button?.setOnClickListener { _ ->
                typefaceId = textStyleGroupManager.getNextTypeface(typefaceId)
                textStyleGroupManager.styleTextView(typefaceId, add_text_edit_text)
                textStyleGroupManager.styleAndLabelTextView(typefaceId, text_style_toggle_button)
                trackTextStyleToggled()
            }
        }

        color_picker_button.setOnClickListener {
            if (add_text_color_picker_recycler_view.visibility == View.VISIBLE) {
                add_text_color_picker_recycler_view.visibility = View.GONE
                text_alignment_button.visibility = View.VISIBLE
                text_style_toggle_button.visibility = View.VISIBLE
            } else {
                add_text_color_picker_recycler_view.visibility = View.VISIBLE
                text_alignment_button.visibility = View.GONE
                text_style_toggle_button.visibility = View.GONE
            }
        }

        arguments?.let {
            add_text_edit_text.setText(it.getString(EXTRA_INPUT_TEXT))
            colorCode = it.getInt(EXTRA_COLOR_CODE)
            add_text_edit_text.setTextColor(colorCode)

            textAlignment = TextAlignment.valueOf(it.getInt(EXTRA_TEXT_ALIGNMENT))
            updateTextAlignment(textAlignment)

            typefaceId = it.getInt(EXTRA_TYPEFACE)
            textStyleGroupManager.styleTextView(typefaceId, add_text_edit_text)
            textStyleGroupManager.styleAndLabelTextView(typefaceId, text_style_toggle_button)
        }
        add_text_edit_text.requestFocus()

        // Make a callback on activity when user is done with text editing
        add_text_done_tv?.setOnClickListener { _ ->
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            dismiss()
            val inputText = add_text_edit_text.text.toString()
            textEditor?.onDone(inputText, TextStyler.from(add_text_edit_text, typefaceId))
            textEditorAnalyticsHandler?.report()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        val inputText = add_text_edit_text?.text.toString()
        textEditor?.onDone(inputText, TextStyler.from(add_text_edit_text, typefaceId))
        textEditorAnalyticsHandler?.report()
        super.onDismiss(dialog)
    }

    // Callback to listener if user is done with text editing
    fun setOnTextEditorListener(textEditor: TextEditor) {
        this.textEditor = textEditor
    }

    fun setAnalyticsEventListener(listener: StoriesAnalyticsListener?) {
        analyticsListener = listener
        textEditorAnalyticsHandler = TextEditorAnalyticsHandler { analyticsListener?.trackStoryTextChanged(it) }
    }

    private fun updateTextAlignment(textAlignment: TextAlignment) {
        // Externally, we track text alignment as one of the allowed values for View#setTextAlignment.
        // However text alignment doesn't seem to work well for modifying EditTexts after they're already
        // drawn, so we're relying on view gravity to change alignment in the EditText on the fly.
        // (Conversely, using gravity for the resulting TextView added to the canvas does not work as
        // intended, so this gravity/text alignment dichotomy seems necessary.)
        // We should still set the textAlignment value though to make it easier to extract style values from the
        // EditText when the fragment is dismissed.
        add_text_edit_text.textAlignment = textAlignment.value
        add_text_edit_text.gravity = when (textAlignment) {
            TextAlignment.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            TextAlignment.CENTER -> Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            TextAlignment.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
        }

        text_alignment_button.setImageResource(when (textAlignment) {
            TextAlignment.LEFT -> R.drawable.ic_gridicons_align_left_32
            TextAlignment.CENTER -> R.drawable.ic_gridicons_align_center_32
            TextAlignment.RIGHT -> R.drawable.ic_gridicons_align_right_32
        })
    }

    private fun trackTextStyleToggled() {
        textEditorAnalyticsHandler?.trackTextStyleToggled(textStyleGroupManager.getAnalyticsLabelFor(typefaceId))
    }

    companion object {
        private val TAG = TextEditorDialogFragment::class.java.simpleName
        const val EXTRA_INPUT_TEXT = "extra_input_text"
        const val EXTRA_COLOR_CODE = "extra_color_code"
        const val EXTRA_TEXT_ALIGNMENT = "extra_text_alignment"
        const val EXTRA_TYPEFACE = "extra_typeface"

        // Show dialog with provide text and text color
        @JvmOverloads
        fun show(
            appCompatActivity: AppCompatActivity,
            inputText: String = "",
            textStyler: TextStyler?
        ): TextEditorDialogFragment {
            return TextEditorDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_INPUT_TEXT, inputText)

                    putInt(EXTRA_COLOR_CODE, textStyler?.textColor
                                ?: ContextCompat.getColor(appCompatActivity, android.R.color.white))
                    putInt(EXTRA_TEXT_ALIGNMENT, textStyler?.textAlignment ?: TextAlignment.default())
                    putInt(EXTRA_TYPEFACE, textStyler?.typefaceId ?: TextStyleGroupManager.TYPEFACE_ID_NUNITO)
                }
                show(appCompatActivity.supportFragmentManager, TAG)
            }
        }
    }
}
