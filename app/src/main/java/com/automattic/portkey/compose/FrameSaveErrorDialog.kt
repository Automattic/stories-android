package com.automattic.portkey.compose

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.automattic.portkey.R

class FrameSaveErrorDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(activity, R.style.AlertDialogTheme)
            .setMessage(arguments?.getString(ARG_MESSAGE))
            .setPositiveButton(android.R.string.ok) { _, _ -> dismiss() }
            .create()

    companion object {
        @JvmStatic private val ARG_MESSAGE = "message"

        @JvmStatic fun newInstance(message: String): FrameSaveErrorDialog = FrameSaveErrorDialog().apply {
            arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
        }
    }
}
