package com.automattic.loop.intro

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.automattic.loop.R
import com.automattic.loop.databinding.IntroTitleTemplateViewBinding
import com.automattic.loop.util.INVALID_RESOURCE_ID
import com.bumptech.glide.Glide

class IntroPagerTitleFragment : Fragment(R.layout.intro_title_template_view) {
    private var titleTextRes: Int = INVALID_RESOURCE_ID
    private var promoTextRes: Int = INVALID_RESOURCE_ID
    private var backgroundImageRes: Int = INVALID_RESOURCE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            titleTextRes = it.getInt(KEY_TITLE_TEXT)
            promoTextRes = it.getInt(KEY_PROMO_TEXT)
            backgroundImageRes = it.getInt(KEY_BACKGROUND_IMAGE)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(IntroTitleTemplateViewBinding.bind(view)) {
            Glide.with(view.context)
                    .load(backgroundImageRes)
                    .into(backgroundImage)

            titleText.setText(titleTextRes)
            promoText.setText(promoTextRes)
        }
    }

    companion object {
        private const val KEY_TITLE_TEXT = "KEY_TITLE_TEXT"
        private const val KEY_PROMO_TEXT = "KEY_PROMO_TEXT"
        private const val KEY_BACKGROUND_IMAGE = "KEY_BACKGROUND_IMAGE"

        internal fun newInstance(titleText: Int, promoText: Int, backgroundImage: Int): IntroPagerTitleFragment {
            val bundle = Bundle().apply {
                putInt(KEY_TITLE_TEXT, titleText)
                putInt(KEY_PROMO_TEXT, promoText)
                putInt(KEY_BACKGROUND_IMAGE, backgroundImage)
            }

            return IntroPagerTitleFragment().apply {
                arguments = bundle
            }
        }
    }
}
