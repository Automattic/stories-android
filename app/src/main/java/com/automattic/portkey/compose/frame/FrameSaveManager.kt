package com.automattic.portkey.compose.frame

import android.content.Context
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.util.FileUtils
import com.automattic.portkey.compose.story.StoryFrameItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

class FrameSaveManager : CoroutineScope { // } by MainScope() {
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

        preparePhotoEditorViewForSnapshot(context, frame, photoEditor)

        // switching coroutine to Dispatchers.IO scope to write image to file
        withContext(Dispatchers.IO) {
            FileUtils.saveViewToFile(file.absolutePath, saveSettings, photoEditor.composedCanvas)
        }

        return file
    }

    private fun preparePhotoEditorViewForSnapshot(context: Context, frame: StoryFrameItem, photoEditor: PhotoEditor) {
        // disable layout change animations, we need this to make added views immediately visible, otherwise
        // we may end up capturing a Bitmap of a backing drawable that still has not been updated
        // (i.e. no visible added Views)
        val transition = photoEditor.composedCanvas.getLayoutTransition()
        photoEditor.composedCanvas.layoutTransition = null

        // now clear addedViews so we don't leak View.Context
        photoEditor.clearAllViews()

        Glide.with(context)
            .load(frame.source.file ?: frame.source.contentUri)
            .transform(CenterCrop())
            .into(photoEditor.source)

        // now call addViewToParent the addedViews remembered by this frame
        frame.addedViews.let {
            for (oneView in it) {
                photoEditor.addViewToParent(oneView.view, oneView.viewType)
            }
        }

        // re-enable layout change animations
        photoEditor.composedCanvas.layoutTransition = transition
    }
}
