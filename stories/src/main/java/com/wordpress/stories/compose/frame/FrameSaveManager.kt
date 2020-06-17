package com.wordpress.stories.compose.frame

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.PhotoEditor.OnSaveWithCancelAndProgressListener
import com.automattic.photoeditor.views.PhotoEditorView
import com.automattic.photoeditor.views.ViewType.STICKER_ANIMATED
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItemType
import com.wordpress.stories.compose.story.StoryFrameItemType.IMAGE
import com.wordpress.stories.compose.story.StoryFrameItemType.VIDEO
import com.wordpress.stories.util.cloneViewSpecs
import com.wordpress.stories.util.removeViewFromParent
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import kotlin.coroutines.CoroutineContext

typealias FrameIndex = Int

class FrameSaveManager(private val photoEditor: PhotoEditor) : CoroutineScope {
    // we're using SupervisorJob as the topmost job, so some children async{}
    // calls can fail without affecting the parent (and thus, all of its children) while we wait for each frame to get
    // saved
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    var saveProgressListener: FrameSaveProgressListener? = null

    // TODO: not sure whether we really want to cancel a Story frame saving operation, but for now I'll  let this
    // one in to be a good citizen with Activity / CoroutineScope
    fun onCancel() {
        job.cancel()
    }

    suspend fun saveStory(
        context: Context,
        frames: List<StoryFrameItem>
    ): List<File> {
        // calling the listener here so the progress notification initializes itself properly and
        // shows really how many Story frame pages we're going to save
        preDispatchStartProgressListenerCalls(frames.size)

        // first, save all images async and wait
        val savedImages = saveLoopFramesAsyncAwait(
            context, frames, IMAGE, IMAGE_CONCURRENCY_LIMIT
        )

        yield()

        // now, save all videos async and wait - this process is intense so only allow for 3 videos to be processed
        // concurrently
        val savedVideos = saveLoopFramesAsyncAwait(
            context, frames, VIDEO(), VIDEO_CONCURRENCY_LIMIT
        )

        return savedImages + savedVideos
    }

    private fun preDispatchStartProgressListenerCalls(framesAmount: Int) {
        for (frameIndex in 0..framesAmount) {
            saveProgressListener?.onFrameSaveStart(frameIndex)
        }
    }

    private suspend fun saveLoopFramesAsyncAwait(
        context: Context,
        frames: List<StoryFrameItem>,
        frameItemType: StoryFrameItemType,
        concurrencyLimit: Int
    ): List<File> {
        // don't process more than 5 Story Pages concurrently
        val concurrencyLimitSemaphore = Semaphore(concurrencyLimit)
        // we need to keep the frameIndex in sync with the whole Story frames list so, not filtering but mapping
        // them all, and only letting the saveLoopFrame fun be called when the frame's frameItemType matches
        val listFiles = frames.mapIndexed { index, frame ->
            async {
                concurrencyLimitSemaphore.withPermit {
                    // see above - we only want to save frames of frameItemType
                    if (frame.frameItemType.isSameType(frameItemType)) {
                        yield()
                        return@withPermit saveStoryFrame(context, frame, index)
                    } else {
                        return@withPermit null
                    }
                }
            }
        }.awaitAll().filterNotNull()
        return listFiles
    }

    private suspend fun saveStoryFrame(
        context: Context,
        frame: StoryFrameItem,
        frameIndex: FrameIndex
    ): File? {
        var frameFile: File? = null
        when (frame.frameItemType) {
            is VIDEO -> {
                // - if we have addedViews then we need to process the vido with mp4composer
                // - if the source video is a Uri, let's process it through mp4composer anyway to obtain
                // a local file we can upload
                if (frame.addedViews.isNotEmpty() || frame.source is UriBackgroundSource) {
                    frameFile = saveVideoFrame(frame, frameIndex)
                    releaseAddedViewsAfterSnapshot(frame)
                } else {
                    // don't process the video but return the original file if no added views in this Story frame
                    frameFile = (frame.source as FileBackgroundSource).file
                }
            }
            is IMAGE -> {
                // check whether there are any GIF stickers - if there are, we need to produce a video instead
                if (frame.addedViews.containsAnyAddedViewsOfType(STICKER_ANIMATED)) {
                    // TODO make saveVideoWithStaticBackground return File
                    // saveVideoWithStaticBackground()
                } else {
                    try {
                        // create ghost PhotoEditorView to be used for saving off-screen
                        val ghostPhotoEditorView = createGhostPhotoEditor(context, photoEditor.composedCanvas)
                        frameFile = saveImageFrame(context, frame, ghostPhotoEditorView, frameIndex)
                        saveProgressListener?.onFrameSaveCompleted(frameIndex)
                    } catch (ex: Exception) {
                        saveProgressListener?.onFrameSaveFailed(frameIndex, ex.message)
                    } finally {
                        // if anything happened, just make sure the added views were removed from the offscreen
                        // photoEditor layout, otherwise it won't be possible to re-add them when we show the
                        // error screen and the user taps on the errored frames (crash will happen)
                        // Also, it's okay if this gets called more than once (for instance, when no exceptions
                        // are thrown) given it internally does check whether the parent contains the view before
                        // attempting to remove it
                        releaseAddedViewsAfterSnapshot(frame)
                    }
                }
            }
        }
        return frameFile
    }

    private suspend fun saveImageFrame(
        context: Context,
        frame: StoryFrameItem,
        ghostPhotoEditorView: PhotoEditorView,
        frameIndex: FrameIndex
    ): File {
        // prepare the ghostview with its background image and the AddedViews on top of it
        preparePhotoEditorViewForSnapshot(context, frame, ghostPhotoEditorView)

        val file = withContext(Dispatchers.IO) {
            return@withContext photoEditor.saveImageFromPhotoEditorViewAsLoopFrameFile(frameIndex, ghostPhotoEditorView)
        }

        releaseAddedViewsAfterSnapshot(frame)

        return file
    }

    private suspend fun releaseAddedViewsAfterSnapshot(frame: StoryFrameItem) {
        withContext(Dispatchers.Main) {
            // don't forget to remove these views from ghost offscreen view before exiting
            releaseAddedViews(frame)
        }
    }

    private suspend fun saveVideoFrame(
        frame: StoryFrameItem,
        frameIndex: FrameIndex
    ): File? {
        var file: File? = null

        withContext(Dispatchers.IO) {
            var listenerDone = false
            val saveListener = object : OnSaveWithCancelAndProgressListener {
                override fun onCancel(noAddedViews: Boolean) {
                    saveProgressListener?.onFrameSaveCanceled(frameIndex)
                    listenerDone = true
                }

                override fun onSuccess(filePath: String) {
                    // all good here, continue success path
                    file = File(filePath)
                    saveProgressListener?.onFrameSaveCompleted(frameIndex)
                    listenerDone = true
                }

                override fun onFailure(exception: Exception) {
                    saveProgressListener?.onFrameSaveFailed(frameIndex, exception.message)
                    listenerDone = true
                }
                override fun onProgress(progress: Double) {
                    saveProgressListener?.onFrameSaveProgress(frameIndex, progress)
                }
            }

            try {
                if (saveVideoAsLoopFrameFile(frame, frameIndex, saveListener)) {
                    // don't return until we get a signal in the listener
                    while (!listenerDone) {
                        delay(VIDEO_PROCESSING_READY_WAIT_TIME_MILLIS)
                    }
                } else {
                    throw Exception("Save not called")
                }
            } catch (ex: Exception) {
                saveProgressListener?.onFrameSaveFailed(frameIndex, ex.message)
            }
        }

        return file
    }

    private fun saveVideoAsLoopFrameFile(
        frame: StoryFrameItem,
        frameIndex: FrameIndex,
        onSaveListener: OnSaveWithCancelAndProgressListener
    ): Boolean {
        var callMade = false
        val uri: Uri? = (frame.source as? UriBackgroundSource)?.contentUri
            ?: Uri.parse((frame.source as FileBackgroundSource).file?.absolutePath)
        // we only need the width and height of a model canvas, not creating a canvas clone in the case of videos
        // as these are all processed in the background
        uri?.let {
            photoEditor.saveVideoAsLoopFrameFile(
                sequenceId = frameIndex,
                videoInputPath = it,
                muteAudio = (frame.frameItemType as? VIDEO)?.muteAudio ?: false,
                canvasWidth = photoEditor.composedCanvas.width,
                canvasHeight = photoEditor.composedCanvas.height,
                customAddedViews = frame.addedViews,
                onSaveListener = onSaveListener
            )
            callMade = true
        }
        return callMade
    }

    private suspend fun preparePhotoEditorViewForSnapshot(
        context: Context,
        frame: StoryFrameItem,
        ghostPhotoEditorView: PhotoEditorView
    ) {
        // prepare background
        val uri = (frame.source as? UriBackgroundSource)?.contentUri
            ?: (frame.source as FileBackgroundSource).file

        // making use of Glide to decode bitmap and get the right orientation automatically
        // http://bumptech.github.io/glide/doc/getting-started.html#background-threads
        val futureTarget = Glide.with(context)
            .asBitmap()
            .load(uri)
            .transform(CenterCrop()) // also use CenterCrop as it's the same the user was seeing as per WYSIWYG
            .submit(ghostPhotoEditorView.source.measuredWidth, ghostPhotoEditorView.source.measuredHeight)
        val bitmap = futureTarget.get()
        ghostPhotoEditorView.source.setImageBitmap(bitmap)
        Glide.with(context).clear(futureTarget)

        // removeViewFromParent for views that were added in the UI thread need to also run on the main thread
        // otherwise we'd get a android.view.ViewRootImpl$CalledFromWrongThreadException:
        // Only the original thread that created a view hierarchy can touch its views.
        withContext(Dispatchers.Main) {
            // now call addViewToParent the addedViews remembered by this frame
            for (oneView in frame.addedViews) {
                removeViewFromParent(oneView.view)
                ghostPhotoEditorView.addView(oneView.view, getViewLayoutParams())
            }
        }
    }

    private fun getViewLayoutParams(): LayoutParams {
        val params = RelativeLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        return params
    }

    private fun createGhostPhotoEditor(context: Context, originalPhotoEditorView: PhotoEditorView): PhotoEditorView {
        val ghostPhotoView = PhotoEditorView(context)
        cloneViewSpecs(originalPhotoEditorView, ghostPhotoView)
        ghostPhotoView.setBackgroundColor(Color.BLACK)
        return ghostPhotoView
    }

    interface FrameSaveProgressListener {
        // only one Story gets saved at a time, frameIndex is the frame's position within the Story array
        fun onFrameSaveStart(frameIndex: FrameIndex)
        fun onFrameSaveProgress(frameIndex: FrameIndex, progress: Double)
        fun onFrameSaveCompleted(frameIndex: FrameIndex)
        fun onFrameSaveCanceled(frameIndex: FrameIndex)
        fun onFrameSaveFailed(frameIndex: FrameIndex, reason: String?)
    }

    companion object {
        private const val VIDEO_CONCURRENCY_LIMIT = 3
        private const val IMAGE_CONCURRENCY_LIMIT = 10
        private const val VIDEO_PROCESSING_READY_WAIT_TIME_MILLIS: Long = 500

        fun releaseAddedViews(frame: StoryFrameItem) {
            // don't forget to remove these views from ghost offscreen view before exiting
            for (oneView in frame.addedViews) {
                removeViewFromParent(oneView.view)
            }
        }
    }
}
