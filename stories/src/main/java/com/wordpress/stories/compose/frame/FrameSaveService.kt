package com.wordpress.stories.compose.frame

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.webkit.MimeTypeMap
import com.wordpress.stories.compose.frame.FrameSaveManager.FrameSaveProgressListener
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.util.FileUtils.Companion.TEMP_FILE_NAME_PREFIX
import com.wordpress.stories.R
import com.wordpress.stories.compose.NotificationTrackerProvider
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveResult
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveError
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveProcessStart
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.compose.story.StoryRepository
import com.wordpress.stories.util.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import org.greenrobot.eventbus.EventBus

class FrameSaveService : Service() {
    private val binder = FrameSaveServiceBinder()
    private lateinit var frameSaveNotifier: FrameSaveNotifier
    private val storySaveProcessors = ArrayList<StorySaveProcessor>()
    private lateinit var notificationIntent: Intent
    private var deleteNotificationPendingIntent: PendingIntent? = null
    private var optionalMetadata: Bundle? = null // keeps optional metadata about the Story
    private var notificationErrorBaseId: Int = 700 // default
    private var notificationTrackerProvider: NotificationTrackerProvider? = null
    var useTempCaptureFile: Boolean = true

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
        }

        return START_NOT_STICKY
    }

    fun setNotificationIntent(intent: Intent) {
        notificationIntent = intent
    }

    fun getNotificationIntent(): Intent {
        return notificationIntent
    }

    fun setDeleteNotificationPendingIntent(pendingIntent: PendingIntent?) {
        deleteNotificationPendingIntent = pendingIntent
    }

    fun getDeleteNotificationPendingIntent(): PendingIntent? {
        return deleteNotificationPendingIntent
    }

    fun setMetadata(bundle: Bundle?) {
        optionalMetadata = bundle
    }

    fun getMetadata(): Bundle? {
        return optionalMetadata
    }

    fun setNotificationErrorBaseId(baseId: Int) {
        notificationErrorBaseId = baseId
    }

    fun getNotificationErrorBaseId(): Int {
        return notificationErrorBaseId
    }

    fun setNotificationTrackerProvider(provider: NotificationTrackerProvider) {
        notificationTrackerProvider = provider
    }

    fun getNotificationTrackerProvider(): NotificationTrackerProvider? {
        return notificationTrackerProvider
    }

    fun saveStoryFrames(
        storyIndex: Int,
        photoEditor: PhotoEditor,
        frameIndex: FrameIndex = StoryRepository.DEFAULT_NONE_SELECTED
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            EventBus.getDefault().postSticky(StorySaveProcessStart(storyIndex))

            lateinit var storyFrames: List<StoryFrameItem>
            if (frameIndex > StoryRepository.DEFAULT_NONE_SELECTED) {
                // in case frameIndex is passed, then save only one frame as indicated by frameIndex
                // easier to make a list and keep iterators
                storyFrames = listOf(StoryRepository.getStoryAtIndex(storyIndex).frames[frameIndex])
            } else {
                storyFrames = StoryRepository.getImmutableCurrentStoryFrames()
                // when saving a full story, first freeze the Story in the Repository
                StoryRepository.finishCurrentStory()
            }

            // now create a processor and run it.
            // also hold a reference to it in the storySaveProcessors list in case the Service is destroyed, so
            // we can cancel each coroutine.
            val isRetry = frameIndex > StoryRepository.DEFAULT_NONE_SELECTED
            val processor = createProcessor(storyIndex, frameIndex, photoEditor, isRetry)
            storySaveProcessors.add(processor)
            runProcessor(
                processor,
                storyIndex,
                storyFrames
            )
            // remove the processor from the list once it's done processing this Story's frames
            storySaveProcessors.remove(processor)

            if (useTempCaptureFile) {
                cleanUpTempStoryFrameFiles(storyFrames.filter { it.saveResultReason == SaveSuccess })
            }

            // also if more than one processor is running, let's not stop the Service just now.
            if (storySaveProcessors.isEmpty()) {
                stopSelf()
            }
        }
    }

    private suspend fun runProcessor(
        processor: StorySaveProcessor,
        storyIndex: Int,
        storyFrames: List<StoryFrameItem>
    ) {
        processor.attachProgressListener()
        saveStoryFramesAndDispatchNewFileBroadcast(processor, storyIndex, storyFrames)
        processor.detachProgressListener()
    }

    private fun createProcessor(
        storyIndex: StoryIndex,
        frameIndex: FrameIndex,
        photoEditor: PhotoEditor,
        isRetry: Boolean
    ): StorySaveProcessor {
        return StorySaveProcessor(
            this,
            storyIndex,
            frameIndex,
            frameSaveNotifier,
            FrameSaveManager(photoEditor),
            isRetry,
            FrameSaveTimeTracker(),
            metadata = optionalMetadata
        )
    }

    private suspend fun saveStoryFramesAndDispatchNewFileBroadcast(
        storySaveProcessor: StorySaveProcessor,
        storyIndex: Int,
        frames: List<StoryFrameItem>
    ) {
        storySaveProcessor.timeTracker.start()
        val frameFileList =
            storySaveProcessor.saveStory(
                this,
                frames
            )
        storySaveProcessor.timeTracker.end()
        storySaveProcessor.updateStorySaveResultTimeElapsed()

        // once all frames have been saved, issue a broadcast so the system knows these frames are ready
        sendNewMediaReadyBroadcast(frameFileList)

        // if we got the same amount of output files it means all went good, otherwise there were errors
        prepareStorySaveResult(storySaveProcessor, storyIndex, frames.size == frameFileList.size)
    }

    private fun prepareStorySaveResult(
        storySaveProcessor: StorySaveProcessor,
        storyIndex: Int,
        noErrors: Boolean
    ) {
        storySaveProcessor.storySaveResult.storyIndex = storyIndex
        if (!noErrors) {
            // let's handle these errors
            handleErrors(storySaveProcessor.storySaveResult)
        }

        // update the Repository with latest save statuses
        StoryRepository.setCurrentStorySaveResultsOnFrames(storyIndex, storySaveProcessor.storySaveResult)

        // errors have been collected, post the SaveResult for the whole Story
        EventBus.getDefault().postSticky(storySaveProcessor.storySaveResult)
    }

    private fun handleErrors(storyResult: StorySaveResult) {
        val storyTitle = StoryRepository.getStoryAtIndex(storyResult.storyIndex).title
        frameSaveNotifier.updateNotificationErrorForStoryFramesSave(storyTitle, storyResult)
    }

    private fun sendNewMediaReadyBroadcast(mediaFileList: List<File>) {
        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
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
        for (processor in storySaveProcessors) {
            processor.onCancel()
        }
        notificationTrackerProvider = null
        super.onDestroy()
    }

    inner class FrameSaveServiceBinder : Binder() {
        fun getService(): FrameSaveService = this@FrameSaveService
    }

    internal class StorySaveProcessor(
        private val context: Context,
        private val storyIndex: StoryIndex,
        private val frameIndexOverride: FrameIndex = StoryRepository.DEFAULT_NONE_SELECTED,
        private val frameSaveNotifier: FrameSaveNotifier,
        private val frameSaveManager: FrameSaveManager,
        private val isRetry: Boolean,
        val timeTracker: FrameSaveTimeTracker,
        private val metadata: Bundle? = null
    ) : FrameSaveProgressListener {
        val storySaveResult = StorySaveResult(isRetry = isRetry, metadata = metadata)
        val title =
            StoryRepository.getStoryAtIndex(storyIndex).title ?: context.getString(R.string.story_saving_untitled)

        // FrameSaveProgressListener overrides
        override fun onFrameSaveStart(frameIndex: FrameIndex) {
            Log.d(LOG_TAG, "START save frame idx: " + applyFrameIndexOverride(frameIndex))
            frameSaveNotifier.addStoryPageInfoToForegroundNotification(
                storyIndex,
                mediaIdFromStoryAndFrameIndex(storyIndex, applyFrameIndexOverride(frameIndex)),
                title
            )
        }

        override fun onFrameSaveProgress(frameIndex: FrameIndex, progress: Double) {
            Log.d(LOG_TAG, "PROGRESS save frame idx: " + applyFrameIndexOverride(frameIndex) + " %: " + progress)
            frameSaveNotifier.updateNotificationProgressForMedia(
                mediaIdFromStoryAndFrameIndex(storyIndex, applyFrameIndexOverride(frameIndex)), progress.toFloat())
        }

        override fun onFrameSaveCompleted(frameIndex: FrameIndex) {
            Log.d(LOG_TAG, "END save frame idx: " + applyFrameIndexOverride(frameIndex))
            frameSaveNotifier.incrementUploadedMediaCountFromProgressNotification(
                storyIndex,
                title,
                mediaIdFromStoryAndFrameIndex(storyIndex, applyFrameIndexOverride(frameIndex)),
                true
            )
            // add success data to StorySaveResult
            storySaveResult.frameSaveResult.add(FrameSaveResult(applyFrameIndexOverride(frameIndex), SaveSuccess))
        }

        override fun onFrameSaveCanceled(frameIndex: FrameIndex) {
            Log.d(LOG_TAG, "CANCELED save frame idx: " + applyFrameIndexOverride(frameIndex))
            // remove one from the count
            frameSaveNotifier.incrementUploadedMediaCountFromProgressNotification(
                storyIndex,
                title,
                mediaIdFromStoryAndFrameIndex(storyIndex, applyFrameIndexOverride(frameIndex))
            )
            // add error data to StorySaveResult
            storySaveResult.frameSaveResult.add(
                FrameSaveResult(applyFrameIndexOverride(frameIndex), SaveError(REASON_CANCELLED)))
        }

        override fun onFrameSaveFailed(frameIndex: FrameIndex, reason: String?) {
            Log.d(LOG_TAG, "FAILED save frame idx: " + applyFrameIndexOverride(frameIndex) +
                    " - error: " + reason.orEmpty())
            // remove one from the count
            frameSaveNotifier.incrementUploadedMediaCountFromProgressNotification(
                storyIndex,
                title,
                mediaIdFromStoryAndFrameIndex(storyIndex, applyFrameIndexOverride(frameIndex))
            )
            // add error data to StorySaveResult
            storySaveResult.frameSaveResult.add(FrameSaveResult(applyFrameIndexOverride(frameIndex), SaveError(reason)))
        }

        fun attachProgressListener() {
            frameSaveManager.saveProgressListener = this
        }

        fun detachProgressListener() {
            frameSaveManager.saveProgressListener = null
        }

        fun updateStorySaveResultTimeElapsed() {
            storySaveResult.elapsedTime = timeTracker.elapsedTime()
        }

        // frameIndex on listeners can be overriden if we're saving just one frame. This comes in handy
        // for save retries, where a list of one item is passed to the saveProcessor
        private fun applyFrameIndexOverride(frameIndex: Int): Int {
            if (frameIndexOverride > StoryRepository.DEFAULT_NONE_SELECTED) {
                return frameIndexOverride
            }
            return frameIndex
        }

        suspend fun saveStory(
            context: Context,
            frames: List<StoryFrameItem>
        ): List<File> {
            return frameSaveManager.saveStory(context, frames)
        }

        fun onCancel() {
            frameSaveManager.onCancel()
        }

        private fun mediaIdFromStoryAndFrameIndex(storyIndex: Int, frameIndex: Int): String {
            return storyIndex.toString() + "-" + frameIndex.toString()
        }
    }

    companion object {
        private const val REASON_CANCELLED = "cancelled"
        fun startServiceAndGetSaveStoryIntent(context: Context): Intent {
            Log.d("FrameSaveService", "startServiceAndGetSaveStoryIntent()")
            val intent = Intent(context, FrameSaveService::class.java)
            context.startService(intent)
            return intent
        }

        fun cleanUpTempStoryFrameFiles(frames: List<StoryFrameItem>) {
            for (frame in frames) {
                (frame.source as? FileBackgroundSource)?.file?.let {
                    if (it.name.startsWith(TEMP_FILE_NAME_PREFIX)) {
                        it.delete()
                    }
                }
            }
        }
    }
}
