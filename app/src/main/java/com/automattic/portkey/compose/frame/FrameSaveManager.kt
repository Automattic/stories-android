package com.automattic.portkey.compose.frame

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import androidx.core.view.children
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.util.FileUtils
import com.automattic.photoeditor.views.PhotoEditorView
import com.automattic.portkey.compose.story.StoryFrameItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

class FrameSaveManager : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    suspend fun saveImageFrame(context: Context, frame: StoryFrameItem, ghostPhotoEditorView: PhotoEditorView): File {
        // creating a new file shouldn't be an expensive operation so, not switching Coroutine context here but staying
        // on the Main dispatcher seems reasonable
        // TODO fix the "video: false" parameter here and make a distinction on frame types here (VIDEO, IMAGE, etc)
        val file = FileUtils.getLoopFrameFile(context, false)
        file.createNewFile()

        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(false)
            .build()

        preparePhotoEditorViewForSnapshot(frame, ghostPhotoEditorView)

        // switching coroutine to Dispatchers.IO scope to write image to file
        withContext(Dispatchers.IO) {
            FileUtils.saveViewToFile(file.absolutePath, saveSettings, ghostPhotoEditorView)
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

    private fun removeViewFromParent(view: View) {
        view.parent?.let {
            it as ViewGroup
            if (it.children.contains(view)) {
                it.removeView(view)
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

    fun createGhostPhotoEditor(context: Context, originalPhotoEditorView: PhotoEditorView): PhotoEditorView {
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
