package com.wordpress.stories.compose.text

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.wordpress.stories.R

class ColorPickerBottomSheetHandler(val activity: Activity, val view: View) {
    private val bottomSheetBehavior by lazy { initBottomSheet() }
    private var keyboardHeight: Int = 0
    private var originalViewHeight: Int = 0

    private val editText: EditText = view.findViewById(R.id.add_text_edit_text)
    private val mainLayout: View = view.findViewById(R.id.main_layout)
    private val bottomSheetLayout: View = view.findViewById(R.id.bottom_sheet_layout)
    private val bottomSheetContainer: ViewGroup = view.findViewById(R.id.bottom_sheet_container)

    private val defaultBottomSheetHeight by lazy {
        activity.resources.getDimensionPixelSize(R.dimen.color_picker_bottom_sheet_default_height)
    }

    private val maxBottomSheetMargin by lazy {
        activity.resources.getDimensionPixelSize(R.dimen.color_picker_bottom_sheet_height_max_margin)
    }

    init {
        captureKeyboardHeight()
    }

    private fun captureKeyboardHeight() {
        val rootView = activity.window?.decorView?.rootView

        rootView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight: Int = rootView.height
                var heightDifference = screenHeight - (r.bottom - r.top)

                val resourceIdNav = activity.resources.getIdentifier("navigation_bar_height", "dimen", "android")
                if (resourceIdNav > 0) {
                    heightDifference -= activity.resources.getDimensionPixelSize(resourceIdNav)
                }

                val resourceIdStatus = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceIdStatus > 0 && r.top != 0) {
                    heightDifference -= activity.resources.getDimensionPixelSize(resourceIdStatus)
                }

                if (heightDifference > KEYBOARD_MINIMUM_HEIGHT) {
                    keyboardHeight = heightDifference
                    rootView.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    private fun initBottomSheet(): BottomSheetBehavior<out ViewGroup> {
        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // Show the keyboard
                    editText.requestFocus()
                    with(activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager) {
                        showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                    }

                    view.postDelayed({
                        // Restore the view's original size, after the keyboard comes back up to avoid jerkiness
                        with(mainLayout.layoutParams) {
                            height = originalViewHeight
                            mainLayout.layoutParams = this
                        }
                    }, BOTTOM_SHEET_DISPLAY_DELAY_MS)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContainer).apply {
            addBottomSheetCallback(bottomSheetCallback)
        }

        originalViewHeight = mainLayout.measuredHeight + keyboardHeight

        return bottomSheetBehavior
    }

    fun toggleBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            hideBottomSheet()
        } else {
            showBottomSheet(view)
        }
    }

    private fun showBottomSheet(rootView: View) {
        // Hide the software keyboard
        with(activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager) {
            hideSoftInputFromWindow(rootView.windowToken, 0)
        }

        // Set bottom sheet to the keyboard height
        with(bottomSheetLayout.layoutParams) {
            // Resize the bottom sheet to match the keyboard height, so the text is kept at around the same
            // height on the screen.
            // Fall back to default height if there's no keyboard, or the keyboard is too short or too tall.
            if (keyboardHeight > defaultBottomSheetHeight &&
                    keyboardHeight < defaultBottomSheetHeight + maxBottomSheetMargin) {
                height = keyboardHeight
                bottomSheetLayout.layoutParams = this
            }
        }

        // Shift layout up so text is still centered on screen
        with(mainLayout.layoutParams) {
            val bottomSheetHeight = if (bottomSheetLayout.layoutParams.height > 0) {
                bottomSheetLayout.layoutParams.height
            } else {
                defaultBottomSheetHeight
            }
            height = originalViewHeight - bottomSheetHeight
            mainLayout.layoutParams = this
        }

        rootView.postDelayed({
            // Show the bottom sheet
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }, BOTTOM_SHEET_DISPLAY_DELAY_MS)
    }

    fun hideBottomSheet() {
        // This will trigger additional view adjustment logic via BottomSheetCallback
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    companion object {
        const val BOTTOM_SHEET_DISPLAY_DELAY_MS = 300L

        // A minimum valid keyboard height.
        // If the screen height changes by at least this amount, we assume it's because a software keyboard
        // has been displayed.
        const val KEYBOARD_MINIMUM_HEIGHT = 150
    }
}
