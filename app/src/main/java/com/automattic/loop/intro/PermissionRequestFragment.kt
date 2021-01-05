package com.automattic.loop.intro

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.automattic.loop.R
import com.automattic.loop.databinding.FragmentPermissionBinding
import com.wordpress.stories.viewBinding

class PermissionRequestFragment : Fragment(R.layout.fragment_permission) {
    interface OnFragmentInteractionListener {
        fun onTurnOnPermissionsPressed()
    }

    private val binding by viewBinding(FragmentPermissionBinding::bind)

    private var listener: OnFragmentInteractionListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.turnOnPermissionsButton.setOnClickListener {
            listener?.onTurnOnPermissionsPressed()
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
        val TAG: String = PermissionRequestFragment::class.java.simpleName
    }
}
