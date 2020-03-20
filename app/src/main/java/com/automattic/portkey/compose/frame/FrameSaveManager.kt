package com.automattic.portkey.compose.frame

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.PhotoEditor.OnSaveWithCancelAndProgressListener
import com.automattic.photoeditor.views.PhotoEditorView
import com.automattic.photoeditor.views.ViewType.STICKER_ANIMATED
import com.automattic.portkey.compose.story.StoryFrameItem
import com.automattic.portkey.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.automattic.portkey.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE
import com.automattic.portkey.compose.story.StoryFrameItemType.VIDEO
import com.automattic.portkey.util.cloneViewSpecs
import com.automattic.portkey.util.removeViewFromParent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import kotlin.coroutines.CoroutineContext

typealias FrameIndex = Int

class FrameSaveManager(private val photoEditor: PhotoEditor) : CoroutineScope {
    private val job = Job()
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
    ): List<File?> {
        // first, launch all frame save processes async
        return frames.mapIndexed { index, frame ->
            async {
                yield()
                saveLoopFrame(context, frame, index)
            }
        }.awaitAll()
    }

    private suspend fun saveLoopFrame(
        context: Context,
        frame: StoryFrameItem,
        frameIndex: FrameIndex
    ): File? {
        var frameFile: File? = null
        when (frame.frameItemType) {
            VIDEO -> {
                frameFile = saveVideoFrame(frame, frameIndex)
            }
            IMAGE -> {
                // check whether there are any GIF stickers - if there are, we need to produce a video instead
                if (frame.addedViews.containsAnyAddedViewsOfType(STICKER_ANIMATED)) {
                    // TODO make saveVideoWithStaticBackground return File
                    // saveVideoWithStaticBackground()
                } else {
                    saveProgressListener?.onFrameSaveStart(frameIndex)
                    // create ghost PhotoEditorView to be used for saving off-screen
                    val ghostPhotoEditorView = createGhostPhotoEditor(context, photoEditor.composedCanvas)
                    frameFile = saveImageFrame(frame, ghostPhotoEditorView, frameIndex)
                    saveProgressListener?.onFrameSaveCompleted(frameIndex)
                }
            }
        }
        return frameFile
    }

    private suspend fun saveImageFrame(
        frame: StoryFrameItem,
        ghostPhotoEditorView: PhotoEditorView,
        frameIndex: FrameIndex
    ): File {
        // prepare the ghostview with its background image and the AddedViews on top of it
        preparePhotoEditorViewForSnapshot(frame, ghostPhotoEditorView)
        val file = withContext(Dispatchers.IO) {
            return@withContext photoEditor.saveImageFromPhotoEditorViewAsLoopFrameFile(frameIndex, ghostPhotoEditorView)
        }

        withContext(Dispatchers.Main) {
            // don't forget to remove these views from ghost offscreen view before exiting
            for (oneView in frame.addedViews) {
                removeViewFromParent(oneView.view)
            }
        }
        return file
    }

    private suspend fun saveVideoFrame(
        frame: StoryFrameItem,
        frameIndex: FrameIndex
    ): File? {
        var file: File? = null

        saveProgressListener?.onFrameSaveStart(frameIndex)

        withContext(Dispatchers.IO) {
            var listenerDone = false
            val saveListener = object : OnSaveWithCancelAndProgressListener {
                override fun onCancel(noAddedViews: Boolean) {
                    // TODO: error handling
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
                    // TODO: error handling
                    saveProgressListener?.onFrameSaveFailed(frameIndex, exception.message)
                    listenerDone = true
                }
                override fun onProgress(progress: Double) {
                    // TODO inform progress
                    saveProgressListener?.onFrameSaveProgress(frameIndex, progress)
                }
            }

            if (saveVideoAsLoopFrameFile(frame, frameIndex, saveListener)) {
                // don't return until we get a signal in the listener
                while (!listenerDone) {
                    delay(100)
                }
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
                frameIndex,
                it,
                photoEditor.composedCanvas.width,
                photoEditor.composedCanvas.height,
                frame.addedViews,
                onSaveListener
            )
            callMade = true
        }
        return callMade
    }

    private suspend fun preparePhotoEditorViewForSnapshot(
        frame: StoryFrameItem,
        ghostPhotoEditorView: PhotoEditorView
    ) {
        // prepare background
        if (frame.source is FileBackgroundSource) {
            frame.source.file?.let {
                ghostPhotoEditorView.source.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
            }
        }
        if (frame.source is UriBackgroundSource) {
            frame.source.contentUri?.let {
                ghostPhotoEditorView.source.setImageURI(it)
            }
        }

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
        cloneViewSpecs(context, originalPhotoEditorView, ghostPhotoView)
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
}
