package com.wordpress.stories.compose.frame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView.ScaleType.CENTER_CROP
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_START
import android.widget.RelativeLayout
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.PhotoEditor.OnSaveWithCancelAndProgressListener
import com.automattic.photoeditor.views.PhotoEditorView
import com.automattic.photoeditor.views.ViewType.STICKER_ANIMATED
import com.automattic.photoeditor.views.background.fixed.BackgroundImageView
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItemType
import com.wordpress.stories.compose.story.StoryFrameItemType.IMAGE
import com.wordpress.stories.compose.story.StoryFrameItemType.VIDEO
import com.wordpress.stories.util.cloneViewSpecs
import com.wordpress.stories.util.removeViewFromParent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.wordpress.stories.util.isSizeRatio916
import com.wordpress.stories.util.normalizeSizeExportTo916
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

class FrameSaveManager(
    private val photoEditor: PhotoEditor,
    private val normalizeTo916: Boolean = true
) : CoroutineScope {
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
        preDispatchStartProgressListenerCalls(frames)

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

    private fun preDispatchStartProgressListenerCalls(frames: List<StoryFrameItem>) {
        for ((frameIndex, frame) in frames.withIndex()) {
            saveProgressListener?.onFrameSaveStart(frameIndex, frame)
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
                frame.composedFrameFile = frameFile
            }
            is IMAGE -> {
                // check whether there are any GIF stickers - if there are, we need to produce a video instead
                if (frame.addedViews.containsAnyAddedViewsOfType(STICKER_ANIMATED)) {
                    // TODO make saveVideoWithStaticBackground return File
                    // saveVideoWithStaticBackground()
                } else {
                    try {
                        // create ghost PhotoEditorView to be used for saving off-screen
                        val originalMatrix = Matrix()
                        frame.source.backgroundViewInfo?.let {
                            originalMatrix.setValues(it.imageMatrixValues)
                        }

                        val ghostPhotoEditorView = createGhostPhotoEditor(context, photoEditor.composedCanvas)
                        frameFile = saveImageFrame(context, frame, ghostPhotoEditorView, originalMatrix, frameIndex)
                        frame.composedFrameFile = frameFile
                        saveProgressListener?.onFrameSaveCompleted(frameIndex, frame)
                    } catch (ex: Exception) {
                        saveProgressListener?.onFrameSaveFailed(frameIndex, frame, ex.message)
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
        originalMatrix: Matrix,
        frameIndex: FrameIndex
    ): File {
        // prepare the ghostview with its background image and the AddedViews on top of it
        val futureTarget = preparePhotoEditorViewForSnapshot(context, frame, originalMatrix, ghostPhotoEditorView)

        val file = withContext(Dispatchers.IO) {
            if (normalizeTo916 && !isSizeRatio916(ghostPhotoEditorView.width, ghostPhotoEditorView.height)) {
                return@withContext photoEditor.saveImageFromPhotoEditorViewAsLoopFrameFile(
                        frameIndex,
                        ghostPhotoEditorView,
                        normalizeSizeExportTo916(ghostPhotoEditorView.width, ghostPhotoEditorView.height).toSize()
                )
            } else {
                return@withContext photoEditor.saveImageFromPhotoEditorViewAsLoopFrameFile(
                        frameIndex,
                        ghostPhotoEditorView
                )
            }
        }

        releaseAddedViewsAfterSnapshot(frame)
        Glide.with(context).clear(futureTarget)

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
                    saveProgressListener?.onFrameSaveCanceled(frameIndex, frame)
                    listenerDone = true
                }

                override fun onSuccess(filePath: String) {
                    // all good here, continue success path
                    file = File(filePath)
                    frame.composedFrameFile = file
                    saveProgressListener?.onFrameSaveCompleted(frameIndex, frame)
                    listenerDone = true
                }

                override fun onFailure(exception: Exception) {
                    saveProgressListener?.onFrameSaveFailed(frameIndex, frame, exception.message)
                    listenerDone = true
                }
                override fun onProgress(progress: Double) {
                    saveProgressListener?.onFrameSaveProgress(frameIndex, frame, progress)
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
                saveProgressListener?.onFrameSaveFailed(frameIndex, frame, ex.message)
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
                ?: Uri.fromFile(requireNotNull((frame.source as FileBackgroundSource).file))
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
        originalMatrix: Matrix,
        ghostPhotoEditorView: PhotoEditorView
    ): FutureTarget<Bitmap> {
        // prepare background
        val uri = (frame.source as? UriBackgroundSource)?.contentUri
            ?: (frame.source as FileBackgroundSource).file

        val targetView = ghostPhotoEditorView.source
        val scaleType = frame.source.backgroundViewInfo?.scaleType

        // making use of Glide to decode bitmap and get the right orientation automatically
        // http://bumptech.github.io/glide/doc/getting-started.html#background-threads
        val futureTarget = when (scaleType) {
            FIT_START ->
                Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        // no transform used when FIT_START, see correlation in ComposeLoopFrameActivity's
                        // loadImageWithGlideToPrepare()
                        .submit(targetView.measuredWidth, targetView.measuredHeight)
            FIT_CENTER ->
                Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        .fitCenter()    // we use fitCenter at first (instead of cropping) so we don't lose any information
                        .submit(targetView.measuredWidth, targetView.measuredHeight)
            CENTER_CROP ->
                Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        .centerCrop()    // we use fitCenter at first (instead of cropping) so we don't lose any information
                        .submit(targetView.measuredWidth, targetView.measuredHeight)
            else -> // default case with no transform needed so futureTarget is initialized,
                    // but we don't really expect to get this case
                Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        .submit(targetView.measuredWidth, targetView.measuredHeight)
        }
        val bitmap = futureTarget.get()
        targetView.setImageBitmap(bitmap)

        // IMPORTANT: scaleType and setSuppMatrix should only be called _after_ the bitmap is set on the targetView
        // by means of targetView.setImageBitmap(). Calling this before will have no effect due to PhotoView's checks.
        (targetView as BackgroundImageView).apply {
            frame.source.backgroundViewInfo?.let {
                this.scaleType = it.scaleType
            }
            setSuppMatrix(originalMatrix)
        }

        // removeViewFromParent for views that were added in the UI thread need to also run on the main thread
        // otherwise we'd get a android.view.ViewRootImpl$CalledFromWrongThreadException:
        // Only the original thread that created a view hierarchy can touch its views.
        withContext(Dispatchers.Main) {
            // now call addViewToParent the addedViews remembered by this frame
            for (oneView in frame.addedViews) {
                oneView.view?.let {
                    removeViewFromParent(it)
                    // this is needed, otherwise some vector graphics such as emoji in text will not render
                    // correctly when a hardware display is not in place (such is the case of FrameSaveManager,
                    // as we're laying out the views on an off-screen view).
                    it.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    ghostPhotoEditorView.addView(it, getViewLayoutParams())
                }
            }
        }
        return futureTarget
    }

    private fun getViewLayoutParams(): LayoutParams {
        val params = RelativeLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        return params
    }

    private suspend fun createGhostPhotoEditor(
        context: Context,
        originalPhotoEditorView: PhotoEditorView
    ) = withContext(Dispatchers.Main) {
            val ghostPhotoView = PhotoEditorView(context)
            cloneViewSpecs(originalPhotoEditorView, ghostPhotoView)
            ghostPhotoView.setBackgroundColor(Color.BLACK)
            return@withContext ghostPhotoView
        }

    interface FrameSaveProgressListener {
        // only one Story gets saved at a time, frameIndex is the frame's position within the Story array
        fun onFrameSaveStart(frameIndex: FrameIndex, frame: StoryFrameItem)
        fun onFrameSaveProgress(frameIndex: FrameIndex, frame: StoryFrameItem, progress: Double)
        fun onFrameSaveCompleted(frameIndex: FrameIndex, frame: StoryFrameItem)
        fun onFrameSaveCanceled(frameIndex: FrameIndex, frame: StoryFrameItem)
        fun onFrameSaveFailed(frameIndex: FrameIndex, frame: StoryFrameItem, reason: String?)
    }

    companion object {
        private const val VIDEO_CONCURRENCY_LIMIT = 3
        private const val IMAGE_CONCURRENCY_LIMIT = 10
        private const val VIDEO_PROCESSING_READY_WAIT_TIME_MILLIS: Long = 500

        fun releaseAddedViews(frame: StoryFrameItem) {
            // don't forget to remove these views from ghost offscreen view before exiting
            for (oneView in frame.addedViews) {
                oneView.view?.let {
                    removeViewFromParent(it)
                }
            }
        }
    }
}
