package com.automattic.loop.intro

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.automattic.loop.R.layout
import com.automattic.loop.databinding.FragmentIntroBinding

class IntroFragment : Fragment() {
    interface OnFragmentInteractionListener {
        fun onGetStartedPressed()
    }

    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout.fragment_intro, container, false)
    }

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
