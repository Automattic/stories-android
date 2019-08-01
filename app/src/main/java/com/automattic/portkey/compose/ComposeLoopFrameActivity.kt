package com.automattic.portkey.compose

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.state.BackgroundSurfaceManager
import com.automattic.photoeditor.views.ViewType
import com.automattic.portkey.R
import com.automattic.portkey.R.color
import com.automattic.portkey.R.id
import com.automattic.portkey.R.layout
import com.automattic.portkey.R.string
import kotlinx.android.synthetic.main.activity_composer.*

import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_composer.*

class ComposeLoopFrameActivity : AppCompatActivity() {
    private lateinit var photoEditor: PhotoEditor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_composer)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        photoEditor = PhotoEditor.Builder(this, photoEditorView)
            .setPinchTextScalable(true) // set flag to make text scalable when pinch
            .build() // build photo editor sdk

        photoEditor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int) {
                val textEditorDialogFragment = TextEditorDialogFragment.show(
                    this@ComposeLoopFrameActivity,
                    text,
                    colorCode)
                textEditorDialogFragment.setOnTextEditorListener(object : TextEditorDialogFragment.TextEditor {
                    override fun onDone(inputText: String, colorCode: Int) {
                        photoEditor.editText(rootView, inputText, colorCode)
                        txtCurrentTool.setText(string.label_tool_text)
                    }
                })
            }

            override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                // no op
            }

            override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                // no op
            }

            override fun onStartViewChangeListener(viewType: ViewType) {
                // no op
            }

            override fun onStopViewChangeListener(viewType: ViewType) {
                // no op
            }

            override fun onRemoveViewListener(numberOfAddedViews: Int) {
                // no op
            }
        })

        lifecycle.addObserver(BackgroundSurfaceManager(this, lifecycle, photoEditorView, supportFragmentManager))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // test actions just to test photoEditor module integration
        return when (item.itemId) {
            id.action_brush -> {
                testBrush()
                true
            }
            id.action_eraser -> {
                testEraser()
                true
            }
            id.action_text -> {
                testText()
                true
            }
            id.action_emoji -> {
                testEmoji()
                true
            }
            id.action_sticker -> {
                testSticker()
                true
            }
            id.action_save -> {
                Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun testBrush() {
        txtCurrentTool.setText(string.label_tool_brush)
        photoEditor.setBrushDrawingMode(true)
        photoEditor.brushColor = ContextCompat.getColor(baseContext, color.red)
    }

    private fun testEraser() {
        txtCurrentTool.setText(string.label_tool_eraser)
        photoEditor.setBrushDrawingMode(false)
        photoEditor.brushEraser()
    }

    private fun testText() {
        txtCurrentTool.setText("")
        photoEditor.addText(
            text = getString(string.text_placeholder),
            colorCodeTextView = ContextCompat.getColor(baseContext, color.white))
    }

    private fun testEmoji() {
        txtCurrentTool.setText("")
        val emojisList = PhotoEditor.getEmojis(this)
        // get some random emoji
        val randomEmojiPos = (0..emojisList.size).shuffled().first()
        photoEditor.addEmoji(emojisList.get(randomEmojiPos))
    }

    private fun testSticker() {
        txtCurrentTool.setText("")
        photoEditor.addNewImageView(true, Uri.parse("https://i.giphy.com/Ok4HaWlYrewuY.gif"))
    }
}
