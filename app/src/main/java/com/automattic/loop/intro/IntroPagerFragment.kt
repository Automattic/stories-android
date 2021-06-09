package com.automattic.loop.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.automattic.loop.R
import com.automattic.loop.databinding.IntroTemplateViewBinding
import com.automattic.loop.util.INVALID_RESOURCE_ID
import com.bumptech.glide.Glide

class IntroPagerFragment : Fragment() {
    private var promoTextRes: Int = INVALID_RESOURCE_ID
    private var backgroundImageRes: Int = INVALID_RESOURCE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            promoTextRes = it.getInt(KEY_PROMO_TEXT)
            backgroundImageRes = it.getInt(KEY_BACKGROUND_IMAGE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.intro_template_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(IntroTemplateViewBinding.bind(view)) {
            Glide.with(view.context)
                        .load(backgroundImageRes)
                        .into(backgroundImage)
            promoText.setText(promoTextRes)
        }
    }

    companion object {
        private const val KEY_PROMO_TEXT = "KEY_PROMO_TEXT"
        private const val KEY_BACKGROUND_IMAGE = "KEY_BACKGROUND_IMAGE"

        internal fun newInstance(promoText: Int, backgroundImage: Int): IntroPagerFragment {
            val bundle = Bundle().apply {
                putInt(KEY_PROMO_TEXT, promoText)
                putInt(KEY_BACKGROUND_IMAGE, backgroundImage)
            }

            return IntroPagerFragment().apply {
                arguments = bundle
            }
        }
    }
}
