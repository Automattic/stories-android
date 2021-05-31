package com.automattic.loop.intro

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.automattic.loop.R
import com.automattic.loop.databinding.FragmentIntroBinding

class IntroFragment : Fragment(R.layout.fragment_intro) {
    interface OnFragmentInteractionListener {
        fun onGetStartedPressed()
    }

    private var listener: OnFragmentInteractionListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentIntroBinding.bind(view)) {
            getStartedButton.setOnClickListener {
                listener?.onGetStartedPressed()
            }

            introPager.adapter = IntroPagerAdapter(childFragmentManager)

            // Using a TabLayout for simulating a page indicator strip
            tabLayoutIndicator.setupWithViewPager(introPager, true)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        val TAG: String = IntroFragment::class.java.simpleName
    }
}
