package com.automattic.portkey.compose.frame

import android.content.Context
import android.util.Log
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
        // TODO fix the "video: false" parameter here and make a distinction on frame types here (VIDEO, IMAGE, etc)

        // creating a new file shouldn't be an expensive operation so, not switching Coroutine context here but staying
        // on the Main dispatcher seems reasonable
        val file = FileUtils.getLoopFrameFile(context, false)
        file.createNewFile()

        Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 2 ")
        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(false)
            .build()

        Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 3")
        prepareCanvasForSnapshot(context, frame, photoEditor)

        // switching coroutine to Dispatchers.IO to write image to file
        withContext(Dispatchers.IO) {
            Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 4")
            FileUtils.saveViewToFile(file.absolutePath, saveSettings, photoEditor.composedCanvas)
        }

        Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 5 ")
        return file
    }

    private fun prepareCanvasForSnapshot(context: Context, frame: StoryFrameItem, photoEditor: PhotoEditor) {
        Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 1")
        Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 2")
        // now clear addedViews so we don't leak View.Context
//            photoEditor.clearAllViews()
//            photoEditor.composedCanvas.removeAllViews()

        Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 3")
        Glide.with(context)
            .load(frame.source.file ?: frame.source.contentUri)
            .transform(CenterCrop())
            .into(photoEditor.source)

        Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 4")
        // now call addViewToParent the addedViews remembered by this frame
//            frame.addedViews.let {
//                for (oneView in it) {
//                    photoEditor.addViewToParent(oneView.view, oneView.viewType)
//                }
//            }
//            photoEditor.composedCanvas.requestLayout()
        Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 5")
    }
}
