package com.automattic.portkey

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.automattic.photoeditor.PhotoEditor
import kotlinx.android.synthetic.main.activity_composer.*

import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_composer.*

class ComposeLoopFrameActivity : AppCompatActivity() {
    private lateinit var photoEditor: PhotoEditor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_composer)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        photoEditor = PhotoEditor.Builder(this, photoEditorView)
            .setPinchTextScalable(true) // set flag to make text scalable when pinch
            .build() // build photo editor sdk
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
            R.id.action_brush -> {
                testBrush()
                true
            }
            R.id.action_eraser -> {
                testEraser()
                true
            }
            R.id.action_text -> {
                testText()
                true
            }
            R.id.action_emoji -> {
                testEmoji()
                true
            }
            R.id.action_sticker -> {
                testSticker()
                true
            }
            R.id.action_save -> {
                Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun testBrush() {
        txtCurrentTool.setText(R.string.label_tool_brush)
        photoEditor.setBrushDrawingMode(true)
        photoEditor.brushColor = ContextCompat.getColor(baseContext, R.color.red)
    }

    private fun testEraser() {
        txtCurrentTool.setText(R.string.label_tool_eraser)
        photoEditor.setBrushDrawingMode(false)
        photoEditor.brushEraser()
    }

    private fun testText() {
        txtCurrentTool.setText("")
        photoEditor.addText(
            text = getString(R.string.text_placeholder),
            colorCodeTextView = ContextCompat.getColor(baseContext, R.color.black))
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
