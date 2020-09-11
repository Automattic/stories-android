package com.wordpress.stories.compose.text

import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId
import com.automattic.photoeditor.text.TextStyler
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

    private var bottomSheetBehavior: BottomSheetBehavior<RelativeLayout>? = null
    private var keyboardHeight: Int = 0
    private var originalViewHeight: Int = 0

    interface TextEditor {
        fun onDone(inputText: String, textStyler: TextStyler)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        textStyleGroupManager = TextStyleGroupManager(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = activity?.window?.decorView?.rootView

        rootView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight: Int = rootView.height
                var heightDifference = screenHeight - (r.bottom - r.top)

                val resourceIdNav = resources.getIdentifier("navigation_bar_height", "dimen", "android")
                if (resourceIdNav > 0) {
                    heightDifference -= resources.getDimensionPixelSize(resourceIdNav)
                }

                if (heightDifference > 150) {
                    keyboardHeight = heightDifference
                    rootView.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // It seems some default padding is applied to DialogFragment DecorViews in Android 11 - get rid of it.
            decorView.setPadding(0, 0, 0, 0)

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
            activity?.let {
                if (bottomSheetBehavior == null) { initBottomSheet() }
                if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                    hideBottomSheet()
                } else {
                    showBottomSheet(view)
                }
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

    private fun initBottomSheet() {
        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // Restore the view's original size
                    with(main_layout.layoutParams) { height = originalViewHeight }

                    // Show the keyboard
                    add_text_edit_text.requestFocus()
                    with(activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager) {
                        showSoftInput(add_text_edit_text, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_container).apply {
            addBottomSheetCallback(bottomSheetCallback)
        }

        originalViewHeight = main_layout.measuredHeight + keyboardHeight
    }

    private fun showBottomSheet(view: View) {
        // Hide the software keyboard
        with(activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager) {
            hideSoftInputFromWindow(view.windowToken, 0)
        }

        view.postDelayed({
            // Set bottom sheet to the keyboard height
            val defaultBottomSheetHeight =
                    resources.getDimensionPixelSize(R.dimen.color_picker_bottom_sheet_default_height)
            val maxBottomSheetMargin =
                    resources.getDimensionPixelSize(R.dimen.color_picker_bottom_sheet_height_max_margin)
            with(bottom_sheet_layout.layoutParams) {
                // Resize the bottom sheet to match the keyboard height, so the text is kept at around the same
                // height on the screen.
                // Fall back to default height if there's no keyboard, or the keyboard is too short or too tall.
                if (keyboardHeight > defaultBottomSheetHeight &&
                        keyboardHeight < defaultBottomSheetHeight + maxBottomSheetMargin) {
                    height = keyboardHeight
                    bottom_sheet_layout.layoutParams = this
                }
            }

            // Shift layout up so text is still centered on screen
            with(main_layout.layoutParams) {
                val bottomSheetHeight = if (bottom_sheet_layout.layoutParams.height > 0) {
                    bottom_sheet_layout.layoutParams.height
                } else {
                    defaultBottomSheetHeight
                }
                height = originalViewHeight - bottomSheetHeight
                main_layout.layoutParams = this
            }

            // Show the bottom sheet
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }, BOTTOM_SHEET_DISPLAY_DELAY_MS)
    }

    private fun hideBottomSheet() {
        // This will trigger additional view adjustment logic via BottomSheetCallback
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
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

        const val BOTTOM_SHEET_DISPLAY_DELAY_MS = 300L

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
