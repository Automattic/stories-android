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
import com.automattic.portkey.R
import com.automattic.portkey.compose.frame.FrameSaveManager.FrameSaveProgressListener
import com.automattic.photoeditor.PhotoEditor
import com.automattic.portkey.compose.frame.FrameSaveService.SaveResultReason.SaveError
import com.automattic.portkey.compose.frame.FrameSaveService.SaveResultReason.SaveSuccess
import com.automattic.portkey.compose.story.StoryFrameItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import org.greenrobot.eventbus.EventBus

class FrameSaveService : Service(), FrameSaveProgressListener {
    private val binder = FrameSaveServiceBinder()
    private var storyIndex: Int = 0
    private lateinit var frameSaveNotifier: FrameSaveNotifier
    private lateinit var frameSaveManager: FrameSaveManager
    private val storySaveResult = StorySaveResult(false, 0)
    private var storyTitle: String? = null

    override fun onCreate() {
        super.onCreate()
        frameSaveNotifier = FrameSaveNotifier(applicationContext, this)
        Log.d("FrameSaveService", "onCreate()")
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d("FrameSaveService", "onBind()")
        return binder
    }

    // we won't really use intents to start the Service but we need it to be a started Service so we can make it
    // a foreground Service as well. Hence, here we override onStartCommand() as well.
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
        } else if (intent.hasExtra(KEY_STORY_TITLE)) {
            storyTitle = intent.getStringExtra(KEY_STORY_TITLE)
        }

        return START_NOT_STICKY
    }

    fun saveStoryFrames(storyIndex: Int, photoEditor: PhotoEditor, frames: List<StoryFrameItem>) {
        this.storyIndex = storyIndex
        this.frameSaveManager = FrameSaveManager(photoEditor)
        CoroutineScope(Dispatchers.Default).launch {
            attachProgressListener(frameSaveManager)
            saveStoryFramesAndDispatchNewFileBroadcast(frameSaveManager, frames)
            detachProgressListener(frameSaveManager)
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
            ).filterNotNull()

        // once all frames have been saved, issue a broadcast so the system knows these frames are ready
        sendNewMediaReadyBroadcast(frameFileList)

        prepareSaveResult(frames.size, frameFileList.size)
    }

    private fun prepareSaveResult(expectedSuccessCases: Int, actualSuccessCases: Int) {
        storySaveResult.storyIndex = this.storyIndex
        // if we got the same amount of output files it means all went good
        if (actualSuccessCases == expectedSuccessCases) {
            storySaveResult.success = true
        } else {
            // otherwise, let's handle these errors
            handleErrors(storySaveResult)
        }

        // errors have beem collected, post the SaveResult for the whole Story
        EventBus.getDefault().post(storySaveResult)
    }

    private fun handleErrors(storyResult: StorySaveResult) {
        val fails = storyResult.frameSaveResult.filterNot { it.resultReason == SaveSuccess }
        // val count = fails.count()
        frameSaveNotifier.updateNotificationErrorForStoryFramesSave(storyTitle, fails)
//        fails.forEach {
//            // TODO HERE do something
//        }
    }

    private fun sendNewMediaReadyBroadcast(rawMediaFileList: List<File?>) {
        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
        val mediaFileList = rawMediaFileList.filterNotNull()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            for (mediaFile in mediaFileList) {
                if (mediaFile.extension == "jpg") {
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

    private fun attachProgressListener(frameSaveManager: FrameSaveManager) {
        frameSaveManager.saveProgressListener = this
    }

    private fun detachProgressListener(frameSaveManager: FrameSaveManager) {
        frameSaveManager.saveProgressListener = null
    }

    // FrameSaveProgressListener overrides
    override fun onFrameSaveStart(frameIndex: FrameIndex) {
        Log.d("PORTKEY", "START save frame idx: " + frameIndex)
        frameSaveNotifier.addStoryPageInfoToForegroundNotification(
            frameIndex.toString(),
            getString(R.string.story_saving_untitled)
        )
    }

    override fun onFrameSaveProgress(frameIndex: FrameIndex, progress: Double) {
        Log.d("PORTKEY", "PROGRESS save frame idx: " + frameIndex + " %: " + progress)
        frameSaveNotifier.updateNotificationProgressForMedia(frameIndex.toString(), progress.toFloat())
    }

    override fun onFrameSaveCompleted(frameIndex: FrameIndex) {
        Log.d("PORTKEY", "END save frame idx: " + frameIndex)
        frameSaveNotifier.incrementUploadedMediaCountFromProgressNotification(frameIndex.toString(), true)
        // add success data to StorySaveResult
        storySaveResult.frameSaveResult.add(FrameSaveResult(frameIndex, SaveSuccess))
    }

    override fun onFrameSaveCanceled(frameIndex: FrameIndex) {
        // remove one from the count
        frameSaveNotifier.incrementUploadedMediaCountFromProgressNotification(frameIndex.toString())
        // add error data to StorySaveResult
        storySaveResult.frameSaveResult.add(FrameSaveResult(frameIndex, SaveError(REASON_CANCELLED)))
    }

    override fun onFrameSaveFailed(frameIndex: FrameIndex, reason: String?) {
        // remove one from the count
        frameSaveNotifier.incrementUploadedMediaCountFromProgressNotification(frameIndex.toString())
        // add error data to StorySaveResult
        storySaveResult.frameSaveResult.add(FrameSaveResult(frameIndex, SaveError(reason)))
    }

    inner class FrameSaveServiceBinder : Binder() {
        fun getService(): FrameSaveService = this@FrameSaveService
    }

    data class StorySaveResult(
        var success: Boolean,
        var storyIndex: Int,
        val frameSaveResult: MutableList<FrameSaveResult> = mutableListOf<FrameSaveResult>()
    )
    data class FrameSaveResult(val frameIndex: FrameIndex, val resultReason: SaveResultReason)

    sealed class SaveResultReason {
        object SaveSuccess : SaveResultReason()

        data class SaveError(
            var reason: String? = null
        ) : SaveResultReason()
    }

    companion object {
        private const val REASON_CANCELLED = "cancelled"
        private const val KEY_STORY_TITLE = "key_story_title"
        fun startServiceAndGetSaveStoryIntent(context: Context, storyTitle: String? = null): Intent {
            Log.d("FrameSaveService", "startServiceAndGetSaveStoryIntent()")
            val intent = Intent(context, FrameSaveService::class.java)
            intent.putExtra(KEY_STORY_TITLE, storyTitle)
            context.startService(intent)
            return intent
        }
    }
}
