package com.automattic.portkey.compose.frame

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.webkit.MimeTypeMap
import com.automattic.portkey.compose.story.StoryFrameItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import org.greenrobot.eventbus.EventBus

class FrameSaveService : Service() {
    private val binder = FrameSaveServiceBinder()
    private var storyIndex: Int = 0
    private lateinit var frameSaveManager: FrameSaveManager

    override fun onCreate() {
        super.onCreate()
        // TODO add logging
        Log.d("FrameSaveService", "onCreate()")
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d("FrameSaveService", "onBind()")
        return binder
    }

    // we won't really use intents to start the Service but we need it to be a started Service so we can make it
    // a forergorund Service as well. Hence, here we override onStartCommand() as well.
    // So basically we're using a bound Service to be able to pass the FrameSaveManager instance to it, which in turn
    // has an instance of PhotoEditor, which is needed to save each frame.
    // And, we're making it a started Service so we can also make it a foreground Service (bound services alone
    // can't be foreground services, and serializing a FrameSaveManager or a PhotoEditor instance seems way too
    // for something that will need to live on the screen anyway, at least for the time being).
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FrameSaveService", "onStartCommand()")
        // Skip this request if no items to upload were given
        if (intent == null) { // || !intent.hasExtra(KEY_MEDIA_LIST) && !intent.hasExtra(KEY_LOCAL_POST_ID)) {
            // AppLog.e(T.MAIN, "UploadService > Killed and restarted with an empty intent")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    fun saveStoryFrames(storyIndex: Int, frameSaveManager: FrameSaveManager, frames: List<StoryFrameItem>) {
        Log.d("FrameSaveService", "saveStoryFrames()")
        this.storyIndex = storyIndex
        this.frameSaveManager = frameSaveManager
        CoroutineScope(Dispatchers.Default).launch {
            saveStoryFramesAndDispatchNewFileBroadcast(frameSaveManager, frames)
            stopSelf()
        }
    }

    private suspend fun saveStoryFramesAndDispatchNewFileBroadcast(
        frameSaveManager: FrameSaveManager,
        frames: List<StoryFrameItem>
    ) {
        val frameFileList =
            frameSaveManager.saveStory(
                this,
                frames
            )

        // once all frames have been saved, issue a broadcast so the system knows these frames are ready
        sendNewMediaReadyBroadcast(frameFileList)

        // TODO collect all the errors somehow before posting the SaveResult for the whole Story
        EventBus.getDefault().post(StorySaveResult(true, storyIndex))
    }

    private fun sendNewMediaReadyBroadcast(rawMediaFileList: List<File?>) {
        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
        val mediaFileList = rawMediaFileList.filterNotNull()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            for (mediaFile in mediaFileList) {
                if (mediaFile.extension.startsWith("jpg")) {
                    sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(mediaFile)))
                } else {
                    sendBroadcast(Intent(Camera.ACTION_NEW_VIDEO, Uri.fromFile(mediaFile)))
                }
            }
        }

        val arrayOfmimeTypes = arrayOfNulls<String>(mediaFileList.size)
        val arrayOfPaths = arrayOfNulls<String>(mediaFileList.size)
        for ((index, mediaFile) in mediaFileList.withIndex()) {
            arrayOfmimeTypes[index] = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(mediaFile.extension)
            arrayOfPaths[index] = mediaFile.absolutePath
        }

        // If the folder selected is an external media directory, this is unnecessary
        // but otherwise other apps will not be able to access our images unless we
        // scan them using [MediaScannerConnection]
        MediaScannerConnection.scanFile(
            applicationContext, arrayOfPaths, arrayOfmimeTypes, null)
    }

    override fun onDestroy() {
        Log.d("FrameSaveService", "onDestroy()")
        frameSaveManager.onCancel()
        super.onDestroy()
    }

    inner class FrameSaveServiceBinder : Binder() {
        fun getService(): FrameSaveService = this@FrameSaveService
    }

    data class StorySaveResult(
        var success: Boolean,
        var storyIndex: Int,
        var frameSaveResult: List<FrameSaveResult>? = null
    )
    data class FrameSaveResult(val success: Boolean, val frameIndex: Int)

    companion object {
        fun startServiceAndGetSaveStoryIntent(context: Context): Intent {
            Log.d("FrameSaveService", "startServiceAndGetSaveStoryIntent()")
            val intent = Intent(context, FrameSaveService::class.java)
            context.startService(intent)
            return intent
        }
    }
}
