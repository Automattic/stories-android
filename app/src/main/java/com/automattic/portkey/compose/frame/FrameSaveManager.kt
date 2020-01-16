package com.automattic.portkey.compose.frame

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.children
import com.automattic.photoeditor.PhotoEditor
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

    suspend fun saveImageFrame(context: Context, frame: StoryFrameItem, photoEditor: PhotoEditor): File {
        // creating a new file shouldn't be an expensive operation so, not switching Coroutine context here but staying
        // on the Main dispatcher seems reasonable
        // TODO fix the "video: false" parameter here and make a distinction on frame types here (VIDEO, IMAGE, etc)
        val file = FileUtils.getLoopFrameFile(context, false)
        file.createNewFile()

        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(false)
            .build()

        // photoEditor.clearAllViews()
        val ghostPhotoEditorView = createGhostPhotoEditor(context, photoEditor.composedCanvas)
        preparePhotoEditorViewForSnapshot(frame, ghostPhotoEditorView)
//        val ghostPhotoEditorView = createGhostPhotoEditor(context, photoEditor.composedCanvas)
//        preparePhotoEditorViewForSnapshot(frame, photoEditor.composedCanvas)

        // switching coroutine to Dispatchers.IO scope to write image to file
        withContext(Dispatchers.IO) {
//            FileUtils.saveViewToFile(file.absolutePath, saveSettings, photoEditor.composedCanvas)
            FileUtils.saveViewToFile(file.absolutePath, saveSettings, ghostPhotoEditorView)
        }

        return file
    }

    private fun preparePhotoEditorViewForSnapshot(frame: StoryFrameItem, photoEditorView: PhotoEditorView) {
        // disable layout change animations, we need this to make added views immediately visible, otherwise
        // we may end up capturing a Bitmap of a backing drawable that still has not been updated
        // (i.e. no visible added Views)
        val transition = photoEditorView.getLayoutTransition()
        photoEditorView.layoutTransition = null

        frame.source.file?.let {
            photoEditorView.source.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
        }
        frame.source.contentUri?.let {
            photoEditorView.source.setImageURI(it)
        }

        // now call addViewToParent the addedViews remembered by this frame
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        for (oneView in frame.addedViews) {
            removeViewFromParent(oneView.view)
            // oneView.view.parent
            photoEditorView.addView(oneView.view, params)
        }

        // re-enable layout change animations
        photoEditorView.layoutTransition = transition
    }

    private fun removeViewFromParent(view: View) {
        val parent = view.parent as ViewGroup
        if (parent.children.contains(view)) {
            parent.removeView(view)
        }
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
