package com.automattic.loop.intro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.automattic.loop.AppPrefs
import com.automattic.loop.MainActivity
import com.automattic.loop.R
import com.automattic.loop.databinding.IntroActivityBinding
import com.automattic.photoeditor.util.PermissionUtils

class IntroActivity : AppCompatActivity(), IntroFragment.OnFragmentInteractionListener,
    PermissionRequestFragment.OnFragmentInteractionListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = IntroActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        showFragment(IntroFragment(), IntroFragment.TAG)
    }

    override fun onSupportNavigateUp() =
        Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()

    override fun onGetStartedPressed() {
        AppPrefs.setIntroRequired(false)
        if (PermissionUtils.allRequiredPermissionsGranted(this)) {
            launchMainActivity()
        } else {
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)
            showFragment(PermissionRequestFragment(), PermissionRequestFragment.TAG)
        }
    }

    override fun onTurnOnPermissionsPressed() {
        PermissionUtils.requestAllRequiredPermissions(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (PermissionUtils.allRequiredPermissionsGranted(this)) {
            launchMainActivity()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
        fragmentTransaction.commit()
    }

    private fun launchMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
