package com.automattic.portkey.compose.frame

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.util.FileUtils
import com.automattic.photoeditor.views.PhotoEditorView
import com.automattic.photoeditor.views.ViewType.STICKER_ANIMATED
import com.automattic.portkey.compose.story.StoryFrameItem
import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE
import com.automattic.portkey.compose.story.StoryFrameItemType.VIDEO
import com.automattic.portkey.util.removeViewFromParent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

class FrameSaveManager : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    suspend fun saveStory(
        context: Context,
        originalPhotoEditorView: PhotoEditorView,
        frames: List<StoryFrameItem>
    ): List<File> {
        // create ghost PhotoEditorView only once for the Story saving process (we'll reuse it)
        val ghostPhotoEditorView = createGhostPhotoEditor(context, originalPhotoEditorView)

        // first, launch all frame save processes async
        val frameDeferreds = ArrayList<Deferred<File>>()
        for ((index, frame) in frames.withIndex()) {
            frameDeferreds.add(
                async {
                    saveLoopFrame(context, frame, ghostPhotoEditorView, index)
                }
            )
        }
        frameDeferreds.awaitAll()

        // now that all of them have ended, let's return the files saved as frames for the Story collection
        val frameFileList = ArrayList<File>()
        for (deferred in frameDeferreds) {
            frameFileList.add(deferred.getCompleted())
        }

        return frameFileList
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
        lateinit var file: File
        // creating a new file shouldn't be an expensive operation so, not switching Coroutine context here but staying
        // on the Main dispatcher seems reasonable
        // TODO fix the "video: false" parameter here and make a distinction on frame types here (VIDEO, IMAGE, etc)
        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(false)
            .build()

        preparePhotoEditorViewForSnapshot(frame, ghostPhotoEditorView)

        withContext(Dispatchers.IO) {
            val localFile = FileUtils.getLoopFrameFile(context, false, sequenceId.toString())
            localFile.createNewFile()
            FileUtils.saveViewToFile(localFile.absolutePath, saveSettings, ghostPhotoEditorView)
            file = localFile
        }

        // don't forget to remove these views from ghost offscreen view before exiting
        for (oneView in frame.addedViews) {
            removeViewFromParent(oneView.view)
        }
        return file
    }

    private fun preparePhotoEditorViewForSnapshot(frame: StoryFrameItem, ghostPhotoEditorView: PhotoEditorView) {
        // prepare background
        frame.source.file?.let {
            ghostPhotoEditorView.source.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
        }
        frame.source.contentUri?.let {
            ghostPhotoEditorView.source.setImageURI(it)
        }

        // now call addViewToParent the addedViews remembered by this frame
        for (oneView in frame.addedViews) {
            removeViewFromParent(oneView.view)
            ghostPhotoEditorView.addView(oneView.view, getViewLayoutParams())
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
        ghostPhotoView.setBackgroundColor(Color.BLACK)
        // get target measures from original PhotoEditorView
        val originalWidth = originalPhotoEditorView.getWidth()
        val originalHeight = originalPhotoEditorView.getHeight()

        val measuredWidth = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(originalHeight, View.MeasureSpec.EXACTLY)

        ghostPhotoView.measure(measuredWidth, measuredHeight)
        ghostPhotoView.layout(0, 0, ghostPhotoView.getMeasuredWidth(), ghostPhotoView.getMeasuredHeight())

        return ghostPhotoView
    }
}
