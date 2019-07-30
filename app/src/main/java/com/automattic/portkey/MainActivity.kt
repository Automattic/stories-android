package com.automattic.portkey

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // test buttons just to test photoEditor module integration
        initTestListeners()

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
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.testBrush -> testBrush()
            R.id.testText -> testText()
            R.id.testEmoji -> testEmoji()
            R.id.testSticker -> testSticker()
        }
    }

    private fun initTestListeners() {
        testBrush.setOnClickListener(this)
        testText.setOnClickListener(this)
        testEmoji.setOnClickListener(this)
        testSticker.setOnClickListener(this)
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
