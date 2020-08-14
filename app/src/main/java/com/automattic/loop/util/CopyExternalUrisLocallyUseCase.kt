package com.automattic.loop.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CopyExternalUrisLocallyUseCase {
    /*
   * Some media providers (eg. Google Photos) give us a limited access to media files just so we can copy them and then
   * they revoke the access. Copying these files must be performed on the UI thread, otherwise the access might be
   * revoked before the action completes. See https://github.com/wordpress-mobile/WordPress-Android/issues/5818
   */
    suspend fun copyFilesToAppStorageIfNecessary(context: Context, uriList: List<Uri>): CopyMediaResult {
        return withContext(Dispatchers.Main) {
            uriList
                    .map { mediaUri ->
                        if (!MediaUtils.isInMediaStore(mediaUri)) {
                            copyToAppStorage(context, mediaUri)
                        } else {
                            mediaUri
                        }
                    }
                    .toList()
                    .let {
                        CopyMediaResult(
                                permanentlyAccessibleUris = it.filterNotNull(),
                                copyingSomeMediaFailed = it.contains(null)
                        )
                    }
        }
    }

    private fun copyToAppStorage(context: Context, mediaUri: Uri): Uri? {
        return try {
            MediaUtils.downloadExternalMedia(context, mediaUri)
        } catch (e: IllegalStateException) {
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
            val errorMessage = "Can't download the image at: $mediaUri See issue #5823"
            Log.e("UTILS", errorMessage, e)
            null
        }
    }

    data class CopyMediaResult(
        val permanentlyAccessibleUris: List<Uri>,
        val copyingSomeMediaFailed: Boolean
    )
}
