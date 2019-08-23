package com.automattic.portkey

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.emoji.text.EmojiCompat
import androidx.navigation.Navigation
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.portkey.intro.IntroActivity
import kotlinx.android.synthetic.main.activity_main.*
import androidx.emoji.text.FontRequestEmojiCompatConfig
import android.util.Log
import androidx.core.provider.FontRequest

class MainActivity : AppCompatActivity(), MainFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        // TODO: change OnFragmentInteractionListener for something relevant to our needs
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initEmojiCompat()
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (AppPrefs.isIntroRequired() || !PermissionUtils.allRequiredPermissionsGranted(this)) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }

        fab.setOnClickListener { view ->
            Navigation.findNavController(this, R.id.nav_host_fragment)
                .navigate(R.id.action_mainFragment_to_composeLoopFrameActivity)
        }
    }

    // TODO move this method to the Application instance once we have it
    private fun initEmojiCompat() {
        val config: EmojiCompat.Config

        // Use a downloadable font for EmojiCompat
        val fontRequest = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            "Noto Color Emoji Compat",
            R.array.com_google_android_gms_fonts_certs
        )
        config = FontRequestEmojiCompatConfig(applicationContext, fontRequest)

        config.registerInitCallback(object : EmojiCompat.InitCallback() {
            override fun onInitialized() {
                Log.d(TAG, "EmojiCompat initialized")
            }

            override fun onFailed(throwable: Throwable?) {
                Log.d(TAG, "EmojiCompat initialization failed", throwable)
            }
        })

        EmojiCompat.init(config)
    }

    override fun onSupportNavigateUp() =
        Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()

    companion object {
        private val TAG = "MainActivity"
    }
}
