package com.automattic.portkey.compose.frame

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.util.FileUtils
import com.automattic.photoeditor.views.PhotoEditorView
import com.automattic.photoeditor.views.ViewType.STICKER_ANIMATED
import com.automattic.portkey.compose.story.StoryFrameItem
import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE
import com.automattic.portkey.compose.story.StoryFrameItemType.VIDEO
import com.automattic.portkey.util.cloneViewSpecs
import com.automattic.portkey.util.removeViewFromParent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import kotlin.coroutines.CoroutineContext

class FrameSaveManager : CoroutineScope {
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
        originalPhotoEditorView: PhotoEditorView,
        frames: List<StoryFrameItem>
    ): List<File> {
        // first, launch all frame save processes async
        return frames.mapIndexed { index, frame ->
            async {
                withContext(coroutineContext) {
                    yield()
                    // create ghost PhotoEditorView to be used for saving off-screen
                    val ghostPhotoEditorView = createGhostPhotoEditor(context, originalPhotoEditorView)
                    saveLoopFrame(context, frame, ghostPhotoEditorView, index)
                }
            }
        }.awaitAll()
    }

    private suspend fun saveLoopFrame(
        context: Context,
        frame: StoryFrameItem,
        ghostPhotoEditorView: PhotoEditorView,
        sequenceId: Int
    ): File {
        lateinit var frameFile: File
        when (frame.frameItemType) {
            VIDEO -> {
                if (frame.source.isFile()) {
                    frame.source.file?.let {
                        // TODO make saveVideo return File
                        // saveVideo(Uri.parse(it.toString()))
                    }
                } else {
                    frame.source.contentUri?.let {
                        // TODO make saveVideo return File
                        // saveVideo(it)
                    }
                }
            }
            IMAGE -> {
                // check whether there are any GIF stickers - if there are, we need to produce a video instead
                if (frame.addedViews.containsAnyAddedViewsOfType(STICKER_ANIMATED)) {
                    // TODO make saveVideoWithStaticBackground return File
                    // saveVideoWithStaticBackground()
                } else {
                    frameFile = saveImageFrame(context, frame, ghostPhotoEditorView, sequenceId)
                }
            }
        }
        return frameFile
    }

    suspend fun saveImageFrame(
        context: Context,
        frame: StoryFrameItem,
        ghostPhotoEditorView: PhotoEditorView,
        sequenceId: Int
    ): File {
        // prepare the ghostview with its background image and the AddedViews on top of it
        preparePhotoEditorViewForSnapshot(frame, ghostPhotoEditorView)
        val file = withContext(Dispatchers.IO) {
            // TODO fix the "video: false" parameter here and make a distinction on frame types here (VIDEO, IMAGE, etc)
            val localFile = FileUtils.getLoopFrameFile(context, false, sequenceId.toString())
            localFile.createNewFile()
            val saveSettings = SaveSettings.Builder()
                .setClearViewsEnabled(true)
                .setTransparencyEnabled(false)
                .build()
            FileUtils.saveViewToFile(localFile.absolutePath, saveSettings, ghostPhotoEditorView)
            return@withContext localFile
        }

        withContext(Dispatchers.Main) {
            // don't forget to remove these views from ghost offscreen view before exiting
            for (oneView in frame.addedViews) {
                removeViewFromParent(oneView.view)
            }
        }
        return file
    }

    private suspend fun preparePhotoEditorViewForSnapshot(
        frame: StoryFrameItem,
        ghostPhotoEditorView: PhotoEditorView
    ) {
        // prepare background
        frame.source.file?.let {
            ghostPhotoEditorView.source.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
        }
        frame.source.contentUri?.let {
            ghostPhotoEditorView.source.setImageURI(it)
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
