package com.wordpress.stories.compose.text

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId
import com.automattic.photoeditor.text.RoundedBackgroundColorSpan
import com.automattic.photoeditor.text.TextStyler
import com.wordpress.stories.R
import com.wordpress.stories.compose.StoriesAnalyticsListener
import com.wordpress.stories.compose.text.TextColorPickerAdapter.Companion.Mode
import com.wordpress.stories.databinding.AddTextDialogBinding

/**
 * Created by Burhanuddin Rashid on 1/16/2018.
 */

class TextEditorDialogFragment : DialogFragment() {
    private lateinit var binding: AddTextDialogBinding

    @ColorInt private var colorCode: Int = 0
    @ColorInt private var backgroundColorCode: Int = Color.TRANSPARENT

    private lateinit var textAlignment: TextAlignment
    @TypefaceId private var typefaceId: Int = 0
    private var textEditor: TextEditor? = null

    private lateinit var textStyleGroupManager: TextStyleGroupManager
    private var bottomSheetHandler: ColorPickerBottomSheetHandler? = null
    private lateinit var textSizeSlider: TextSizeSlider

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
            // It seems some default padding is applied to DialogFragment DecorViews in Android 11 - get rid of it.
            decorView.setPadding(0, 0, 0, 0)

            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

            attributes = attributes.apply { dimAmount = 0.5f } // The default dimAmount is 0.6
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = AddTextDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onStop() {
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        bottomSheetHandler?.hideBottomSheet()
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            binding.addTextEditText.setText(it.getString(EXTRA_INPUT_TEXT))
            colorCode = it.getInt(EXTRA_TEXT_COLOR_CODE)
            backgroundColorCode = it.getInt(EXTRA_TEXT_BACKGROUND_COLOR_CODE)
            textAlignment = TextAlignment.valueOf(it.getInt(EXTRA_TEXT_ALIGNMENT))
            typefaceId = it.getInt(EXTRA_TYPEFACE)
        }

        bottomSheetHandler = activity?.let {
            ColorPickerBottomSheetHandler(it, view)
        }

        // Hide the bottom sheet if the user taps in the EditText
        binding.addTextEditText.setOnClickListener {
            bottomSheetHandler?.hideBottomSheet()
        }

        textSizeSlider = TextSizeSlider(binding.textSizeSlider, binding.addTextEditText, resources) {
            textStyleGroupManager.customFontSizeApplied = true
        }

        initTextColoring()

        binding.textAlignmentButton.setOnClickListener {
            textAlignment = TextAlignment.getNext(textAlignment)
            updateTextAlignment(textAlignment)
        }

        binding.textStyleToggleButton.setOnClickListener {
            typefaceId = textStyleGroupManager.getNextTypeface(typefaceId)
            textStyleGroupManager.styleTextView(typefaceId, binding.addTextEditText)
            textStyleGroupManager.styleAndLabelTextView(typefaceId, binding.textStyleToggleButton)
            trackTextStyleToggled()
            textSizeSlider.update()
        }

        binding.colorPickerButton.setOnClickListener {
            bottomSheetHandler?.toggleBottomSheet()
        }

        // Apply any existing styling to text
        binding.addTextEditText.setTextColor(colorCode)
        applyBackgroundColor(backgroundColorCode)

        updateTextAlignment(textAlignment)

        // This first time pass the font size so we know if we should fix the size
        val initialTextSize = arguments?.getFloat(EXTRA_TEXT_SIZE) ?: 0F

        textStyleGroupManager.styleTextView(typefaceId, binding.addTextEditText, initialTextSize)
        textStyleGroupManager.styleAndLabelTextView(typefaceId, binding.textStyleToggleButton)

        textSizeSlider.update()

        updateColorPickerButton()

        binding.addTextEditText.requestFocus()

        // Make a callback on activity when user is done with text editing
        binding.addTextDoneButton.setOnClickListener {
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        val inputText = binding.addTextEditText?.text.toString()
        textEditor?.onDone(inputText, TextStyler.from(binding.addTextEditText, typefaceId, backgroundColorCode))
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

    private fun initTextColoring() {
        activity?.let {
            // Set up the color picker for text color
            val textColorPickerAdapter = TextColorPickerAdapter(it, Mode.FOREGROUND, colorCode).apply {
                setOnColorPickerClickListener { colorCode ->
                    this@TextEditorDialogFragment.colorCode = colorCode
                    binding.addTextEditText.setTextColor(colorCode)
                    updateColorPickerButton()
                }
            }
            with(binding.colorPickerBottomSheet.textColorPickerRecyclerView) {
                this.layoutManager = LinearLayoutManager(it, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
                this.adapter = textColorPickerAdapter
            }

            // Set up the color picker for text background color
            val textBgColorPickerAdapter = TextColorPickerAdapter(it, Mode.BACKGROUND, backgroundColorCode).apply {
                setOnColorPickerClickListener { colorCode ->
                    this@TextEditorDialogFragment.backgroundColorCode = colorCode
                    applyBackgroundColor(colorCode)
                    updateColorPickerButton()
                    // Reapply the styles, since text shadow depends on the background color + style combination
                    textStyleGroupManager.styleTextView(typefaceId, binding.addTextEditText)
                }
            }
            with(binding.colorPickerBottomSheet.textBackgroundColorPickerRecyclerView) {
                this.layoutManager = LinearLayoutManager(it, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
                this.adapter = textBgColorPickerAdapter
            }
        }
    }

    private fun updateTextAlignment(textAlignment: TextAlignment) {
        // Externally, we track text alignment as one of the allowed values for View#setTextAlignment.
        // However text alignment doesn't seem to work well for modifying EditTexts after they're already
        // drawn, so we're relying on view gravity to change alignment in the EditText on the fly.
        // (Conversely, using gravity for the resulting TextView added to the canvas does not work as
        // intended, so this gravity/text alignment dichotomy seems necessary.)
        // We should still set the textAlignment value though to make it easier to extract style values from the
        // EditText when the fragment is dismissed.
        binding.addTextEditText.textAlignment = textAlignment.value
        binding.addTextEditText.gravity = when (textAlignment) {
            TextAlignment.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            TextAlignment.CENTER -> Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            TextAlignment.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
        }

        binding.textAlignmentButton.setImageResource(
                when (textAlignment) {
                    TextAlignment.LEFT -> R.drawable.ic_gridicons_align_left_32
                    TextAlignment.CENTER -> R.drawable.ic_gridicons_align_center_32
                    TextAlignment.RIGHT -> R.drawable.ic_gridicons_align_right_32
                }
        )

        // The background span needs to be re-applied to adjust for the alignment change
        applyBackgroundColor(backgroundColorCode)
    }

    /**
     * Applies the given background color as a span to the EditText.
     * Will clear any background color spans if [colorCode] is [Color.TRANSPARENT].
     */
    private fun applyBackgroundColor(@ColorInt colorCode: Int) {
        binding.addTextEditText.text?.apply {
            // Clear any existing background color spans
            getSpans(0, length, RoundedBackgroundColorSpan::class.java)?.forEach { removeSpan(it) }
            if (colorCode != Color.TRANSPARENT) {
                val span = RoundedBackgroundColorSpan(colorCode, textAlignment.value, resources)
                setSpan(span, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
        }
    }

    /**
     * Colorizes the color picker button image with the current text and text background colors.
     */
    private fun updateColorPickerButton() {
        context?.let { context ->
            PathAwareVectorDrawable(context, R.drawable.ic_textcolor, binding.colorPickerButton).apply {
                // The path names used need to be defined as 'name' properties in the drawable.
                setPathFillColor("inner-fill", colorCode)
                setPathFillColor("outer-fill", backgroundColorCode)
            }
        }
    }

    private fun trackTextStyleToggled() {
        textEditorAnalyticsHandler?.trackTextStyleToggled(textStyleGroupManager.getAnalyticsLabelFor(typefaceId))
    }

    companion object {
        private val TAG = TextEditorDialogFragment::class.java.simpleName
        const val EXTRA_INPUT_TEXT = "extra_input_text"
        const val EXTRA_TEXT_COLOR_CODE = "extra_color_code"
        const val EXTRA_TEXT_BACKGROUND_COLOR_CODE = "extra_background_color_code"
        const val EXTRA_TEXT_ALIGNMENT = "extra_text_alignment"
        const val EXTRA_TYPEFACE = "extra_typeface"
        const val EXTRA_TEXT_SIZE = "extra_text_size"

        @JvmOverloads
        fun show(
            appCompatActivity: AppCompatActivity,
            inputText: String = "",
            textStyler: TextStyler?
        ): TextEditorDialogFragment {
            return TextEditorDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_INPUT_TEXT, inputText)

                    putInt(
                            EXTRA_TEXT_COLOR_CODE, textStyler?.textColor
                            ?: ContextCompat.getColor(appCompatActivity, android.R.color.white)
                    )
                    putInt(EXTRA_TEXT_BACKGROUND_COLOR_CODE, textStyler?.textBackgroundColor ?: Color.TRANSPARENT)
                    putInt(EXTRA_TEXT_ALIGNMENT, textStyler?.textAlignment ?: TextAlignment.default())
                    putInt(EXTRA_TYPEFACE, textStyler?.typefaceId ?: TextStyleGroupManager.TYPEFACE_ID_NUNITO)
                    putFloat(EXTRA_TEXT_SIZE, textStyler?.fontSize ?: 0F)
                }
                show(appCompatActivity.supportFragmentManager, TAG)
            }
        }
    }
}
