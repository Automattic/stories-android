package com.automattic.photoeditor.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtils {
    interface OnRequestPermissionGrantedCheck {
        fun isPermissionGranted(isGranted: Boolean, permission: String)
    }
    companion object {
        val READ_WRITE_STORAGE = 52

        fun checkPermission(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        fun requestPermission(activity: Activity, permission: String) {
            ActivityCompat.requestPermissions(activity, arrayOf(permission),
                READ_WRITE_STORAGE
            )
        }

        fun checkAndRequestPermission(activity: Activity, permission: String): Boolean {
            val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                ActivityCompat.requestPermissions(activity, arrayOf(permission),
                    READ_WRITE_STORAGE
                )
            }
            return isGranted
        }

        fun onRequestPermissionsResult(
            onRequestPermissionChecker: OnRequestPermissionGrantedCheck,
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
            when (requestCode) {
                READ_WRITE_STORAGE -> onRequestPermissionChecker.isPermissionGranted(
                    grantResults[0] == PackageManager.PERMISSION_GRANTED,
                    permissions[0]
                )
            }
        }
    }
}
