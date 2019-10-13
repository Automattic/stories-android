package com.automattic.portkey

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.portkey.intro.IntroActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MainFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        // TODO: change OnFragmentInteractionListener for something relevant to our needs
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        activity_main.setOnApplyWindowInsetsListener { view, insets ->
            // remember the insetTop as margin to all controls appearing at the top of the screen for full screen
            // screens (i.e. ComposeLoopFrameActivity)
            (application as Portkey).setStatusBarHeight(insets.systemWindowInsetTop)
            view.onApplyWindowInsets(insets)
        }

        if (AppPrefs.isIntroRequired() || !PermissionUtils.allRequiredPermissionsGranted(this)) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }

        fab.setOnClickListener { view ->
            fab.isEnabled = false
            Navigation.findNavController(this, R.id.nav_host_fragment)
                .navigate(R.id.action_mainFragment_to_composeLoopFrameActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        fab.isEnabled = true
    }

    override fun onSupportNavigateUp() =
        Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()
}
