package com.automattic.portkey.compose.frame

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.util.FileUtils
import com.automattic.portkey.compose.story.StoryFrameItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

class FrameSaveManager : CoroutineScope { // } by MainScope() {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
//    get() = Dispatchers.IO + job

    suspend fun saveImageFrame(context: Context, frame: StoryFrameItem, photoEditor: PhotoEditor): File {
        Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 1")
        // TODO fix the "video: false" parameter here and make a distinction on frame types here (VIDEO, IMAGE, etc)
        val file = FileUtils.getLoopFrameFile(context, false)
        file.createNewFile()

        Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 2 ")
        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(false)
            .build()

        Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 3")
        prepareCanvasForSnapshot(context, frame, photoEditor)

        withContext(Dispatchers.IO) {
            Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 4")
            FileUtils.saveViewToFile(file.absolutePath, saveSettings, photoEditor.composedCanvas)
        }

        Log.d("PORTKEY", "FrameSaveManager.saveImageFrame 5 ")
        return file
    }

    suspend private fun prepareCanvasForSnapshot(context: Context, frame: StoryFrameItem, photoEditor: PhotoEditor) {
        Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 1")
//        val job = withContext(Dispatchers.Main) {
            Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 2")
            // now clear addedViews so we don't leak View.Context
            photoEditor.clearAllViews()
            photoEditor.composedCanvas.removeAllViews()

            Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 3")
            // re-create the view
            frame.source.file?.let {
                photoEditor.source.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
            }
            frame.source.contentUri?.let {
                photoEditor.source.setImageURI(it)
            }
//            Glide.with(context)
//                .load(frame.source.file ?: frame.source.contentUri)
//                // .load("https://i.giphy.com/Ok4HaWlYrewuY.gif")
//                // .load("https://upload.wikimedia.org/wikipedia/en/3/33/Cat_breastfeeding_a_random_adult_cat.jpg")
//                .transform(CenterCrop())
//                .into(photoEditor.source)

            Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 4")
            // now call addViewToParent the addedViews remembered by this frame
            frame.addedViews.let {
                for (oneView in it) {
                    photoEditor.addViewToParent(oneView.view, oneView.viewType)
                }
            }
//            photoEditor.composedCanvas.requestLayout()
            // delay(2000)
            Log.d("PORTKEY", "FrameSaveManager.prepareCanvasForSnapshot 5")
//        }
    }
}
