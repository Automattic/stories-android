package com.automattic.portkey.compose

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

interface DiscardOk {
    fun discardOkClicked()
}

class DiscardDialog : DialogFragment() {
    private lateinit var discardOkListener: DiscardOk
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(activity)
            .setMessage(arguments?.getString(ARG_MESSAGE))
            .setPositiveButton(android.R.string.ok) { _, _ -> discardOkListener.discardOkClicked() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .create()

    companion object {
        @JvmStatic private val ARG_MESSAGE = "message"

        @JvmStatic fun newInstance(message: String, listener: DiscardOk): DiscardDialog = DiscardDialog().apply {
            arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            discardOkListener = listener
        }
    }
}
