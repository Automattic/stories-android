package com.wordpress.stories.compose

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

interface FrameSaveErrorDialogOk {
    fun OnOkClicked(dialog: DialogFragment)
}

class FrameSaveErrorDialog : DialogFragment() {
    private var okListener: FrameSaveErrorDialogOk? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(arguments?.getString(ARG_TITLE))
            .setMessage(arguments?.getString(ARG_MESSAGE))

        okListener?.let {
            val hideCancel = arguments?.getBoolean(ARG_HIDE_CANCEL) ?: false
            if (!hideCancel) {
                builder.setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            }
            builder.setPositiveButton(
                    arguments?.getString(ARG_OK_LABEL) ?: activity?.getString(android.R.string.ok)) {
                    _, _ -> okListener?.OnOkClicked(this)
            }
        } ?: builder.setPositiveButton(
            arguments?.getString(ARG_OK_LABEL) ?: activity?.getString(android.R.string.ok)) {
                _, _ -> dismiss()
        }
        return builder.create()
    }

    companion object {
        @JvmStatic private val ARG_MESSAGE = "message"
        @JvmStatic private val ARG_TITLE = "title"
        @JvmStatic private val ARG_OK_LABEL = "ok_label"
        @JvmStatic private val ARG_HIDE_CANCEL = "hide_cancel"

        @JvmStatic fun newInstance(
            title: String,
            message: String,
            okButtonLabel: String? = null,
            listener: FrameSaveErrorDialogOk? = null,
            hideCancelButton: Boolean = false
        ): FrameSaveErrorDialog =
            FrameSaveErrorDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_OK_LABEL, okButtonLabel)
                    putBoolean(ARG_HIDE_CANCEL, hideCancelButton)
                }
                okListener = listener
            }
    }
}
