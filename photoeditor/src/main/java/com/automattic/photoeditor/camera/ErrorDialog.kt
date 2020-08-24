/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.automattic.photoeditor.camera

import android.app.Dialog
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.DialogFragment

interface ErrorDialogOk {
    fun OnOkClicked(dialog: DialogFragment)
}

/**
 * Shows an error message dialog.
 */
class ErrorDialog : DialogFragment() {
    private var okListener: ErrorDialogOk? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var builder = MaterialAlertDialogBuilder(activity)
                .setMessage(arguments?.getString(ARG_MESSAGE))

        okListener?.let {
            builder.setPositiveButton(activity?.getString(android.R.string.ok)) {
                    _, _ -> okListener?.OnOkClicked(this)
            }
        } ?: builder.setPositiveButton(activity?.getString(android.R.string.ok)) { _, _ -> activity?.finish() }

        return builder.create()
    }

    companion object {
        @JvmStatic private val ARG_MESSAGE = "message"

        @JvmStatic
        fun newInstance(message: String, listener: ErrorDialogOk? = null): ErrorDialog = ErrorDialog().apply {
            arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            okListener = listener
        }
    }
}
