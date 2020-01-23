package com.automattic.portkey.compose.frame

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.PhotoEditor.OnSaveWithCancelListener
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import kotlin.coroutines.CoroutineContext

class FrameSaveManager(val photoEditor: PhotoEditor) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    // TODO: not sure whether we really want to cancel a Story frame saving operation, but for now I'll  let this
    // one in to be a good citizen with Activity / CoroutineScope
    fun onCancel() {
        job.cancel()
    }

    suspend fun saveStory(
        context: Context,
        frames: List<StoryFrameItem>
    ): List<File> {
        // first, launch all frame save processes async
        val frameDeferreds = ArrayList<Deferred<File?>>()
        for ((index, frame) in frames.withIndex()) {
            frameDeferreds.add(
                async {
                    yield()
                    saveLoopFrame(context, frame, index)
                }
            )
        }
        frameDeferreds.awaitAll()

        // now that all of them have ended, let's return the files saved as frames for the Story collection
        val frameFileList = ArrayList<File>()
        for (deferred in frameDeferreds) {
            deferred.getCompleted()?.let {
                frameFileList.add(it)
            }
        }

        return frameFileList
    }

    private suspend fun saveLoopFrame(
        context: Context,
        frame: StoryFrameItem,
        sequenceId: Int
    ): File? {
        var frameFile: File? = null
        when (frame.frameItemType) {
            VIDEO -> {
                frameFile = saveVideoFrame(frame, sequenceId)
            }
            IMAGE -> {
                // check whether there are any GIF stickers - if there are, we need to produce a video instead
                if (frame.addedViews.containsAnyAddedViewsOfType(STICKER_ANIMATED)) {
                    // TODO make saveVideoWithStaticBackground return File
                    // saveVideoWithStaticBackground()
                } else {
                    // create ghost PhotoEditorView to be used for saving off-screen
                    val ghostPhotoEditorView = createGhostPhotoEditor(context, photoEditor.composedCanvas)
                    frameFile = saveImageFrame(frame, ghostPhotoEditorView, sequenceId)
                }
            }
        }
        return frameFile
    }

    private suspend fun saveImageFrame(
        frame: StoryFrameItem,
        ghostPhotoEditorView: PhotoEditorView,
        sequenceId: Int
    ): File? {
        var file: File? = null
        // prepare the ghostview with its background image and the AddedViews on top of it
        preparePhotoEditorViewForSnapshot(frame, ghostPhotoEditorView)
        withContext(Dispatchers.IO) {
            file = photoEditor.saveImageFromPhotoEditorViewAsLoopFrameFile(sequenceId, ghostPhotoEditorView)
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
        sequenceId: Int
    ): File? {
        var file: File? = null

        withContext(Dispatchers.IO) {
            var listenerDone = false
            val saveListener = object : PhotoEditor.OnSaveWithCancelListener {
                override fun onCancel(noAddedViews: Boolean) {
//                    runOnUiThread {
//                        hideLoading()
//                        showSnackbar("No views added - original video saved")
//                    }
                    listenerDone = true
                }

                override fun onSuccess(filePath: String) {
//                    runOnUiThread {
//                        hideLoading()
//                        deleteCapturedMedia()
//                        photoEditor.clearAllViews()
//                        sendNewStoryFrameReadyBroadcast(file)
//                        showSnackbar(
//                            getString(R.string.label_snackbar_loop_frame_saved),
//                            getString(R.string.label_snackbar_share),
//                            OnClickListener { shareAction(file) }
//                        )
//                        hideEditModeUIControls()
//                        switchCameraPreviewOn()
//                    }
                    file = File(filePath)
                    listenerDone = true
                }

                override fun onFailure(exception: Exception) {
//                    runOnUiThread {
//                        hideLoading()
//                        showSnackbar("Failed to save Video")
//                    }
                    listenerDone = true
                }
            }

            if (saveVideoAsLoopFrameFile(frame, sequenceId, saveListener)) {
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
        sequenceId: Int,
        onSaveListener: OnSaveWithCancelListener
    ): Boolean {
        var callMade = false
        // we only need the width and height of a model canvas, not creating a canvas clone in the case of videos
        // as these are all processed in the background
        if (frame.source is UriBackgroundSource) {
            frame.source.contentUri?.let {
                photoEditor.saveVideoAsLoopFrameFile(
                    sequenceId,
                    it,
                    photoEditor.composedCanvas.width,
                    photoEditor.composedCanvas.height,
                    frame.addedViews,
                    onSaveListener
                )
                callMade = true
            }
        } else {
            (frame.source as FileBackgroundSource).file?.let {
                photoEditor.saveVideoAsLoopFrameFile(
                    sequenceId,
                    Uri.parse(it.absolutePath),
                    photoEditor.composedCanvas.width,
                    photoEditor.composedCanvas.height,
                    frame.addedViews,
                    onSaveListener
                )
                callMade = true
            }
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
}
