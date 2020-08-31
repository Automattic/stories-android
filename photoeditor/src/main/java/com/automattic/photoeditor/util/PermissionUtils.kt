package com.automattic.photoeditor.util

import android.Manifest
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
        val PERMISSION_REQUEST_CODE = 5200
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Video requires access to recording audio (microphone).
        val REQUIRED_PERMISSIONS_WITH_AUDIO = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        fun checkPermission(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        fun requestPermission(activity: Activity, permission: String) {
            ActivityCompat.requestPermissions(activity, arrayOf(permission),
                PERMISSION_REQUEST_CODE
            )
        }

        fun requestPermissions(activity: Activity, permissions: Array<String>) {
            ActivityCompat.requestPermissions(activity, permissions,
                PERMISSION_REQUEST_CODE
            )
        }

        fun requestAllRequiredPermissions(activity: Activity) {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
            )
        }

        fun requestAllRequiredPermissionsIncludingAudioForVideo(activity: Activity) {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS_WITH_AUDIO,
                    PERMISSION_REQUEST_CODE
            )
        }

        fun checkAndRequestPermission(activity: Activity, permission: String): Boolean {
            val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                ActivityCompat.requestPermissions(activity, arrayOf(permission),
                    PERMISSION_REQUEST_CODE
                )
            }
            return isGranted
        }

        fun onRequestPermissionsResult(
            onRequestPermissionChecker: OnRequestPermissionGrantedCheck,
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ): Boolean {
            return when (requestCode) {
                PERMISSION_REQUEST_CODE -> {
                    onRequestPermissionChecker.isPermissionGranted(
                        grantResults[0] == PackageManager.PERMISSION_GRANTED,
                        permissions[0]
                    )
                    true
                }
                else -> false
            }
        }

        fun allRequestedPermissionsGranted(grantResults: IntArray): Boolean {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

        fun allRequiredPermissionsGranted(context: Context): Boolean {
            return checkPermissionsForArray(context, REQUIRED_PERMISSIONS)
        }

        fun allVideoPermissionsGranted(context: Context): Boolean {
            return checkPermissionsForArray(context, REQUIRED_PERMISSIONS_WITH_AUDIO)
        }

        private fun checkPermissionsForArray(context: Context, permissions: Array<String>): Boolean {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                                context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

        fun anyVideoNeededPermissionPermanentlyDenied(activity: Activity): String? {
            return checkPermanentDenyForPermissions(activity, REQUIRED_PERMISSIONS_WITH_AUDIO)
        }

        private fun checkPermanentDenyForPermissions(activity: Activity, permissions: Array<String>): String? {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                                activity, permission) == PackageManager.PERMISSION_DENIED &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    return permission
                }
            }
            return null
        }
    }
}
