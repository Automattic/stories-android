package com.automattic.portkey

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun testBrush() {
        // TODO something here
        Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show()
    }

    private fun testText() {
        // TODO something here
        Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show()
    }

    private fun testEmoji() {
        // TODO something here
        Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show()
    }

    private fun testSticker() {
        // TODO something here
        Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show()
    }
}
