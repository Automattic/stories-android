package com.automattic.portkey.intro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.automattic.portkey.R.id
import com.automattic.portkey.R.layout
import kotlinx.android.synthetic.main.activity_main.*

class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_intro)
        setSupportActionBar(toolbar)
    }

    override fun onSupportNavigateUp() =
        Navigation.findNavController(this, id.nav_host_fragment).navigateUp()
}
