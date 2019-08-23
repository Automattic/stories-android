package com.automattic.portkey.intro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.automattic.portkey.AppPrefs
import com.automattic.portkey.MainActivity
import com.automattic.portkey.R.layout
import kotlinx.android.synthetic.main.fragment_intro.*

class IntroFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout.fragment_intro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        get_started_button.setOnClickListener {
            AppPrefs.setIntroRequired(false)
            startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        }

        intro_pager.adapter = IntroPagerAdapter(childFragmentManager)

        // Using a TabLayout for simulating a page indicator strip
        tab_layout_indicator.setupWithViewPager(intro_pager, true)
    }

    companion object {
        val TAG: String = IntroFragment::class.java.simpleName
    }
}
