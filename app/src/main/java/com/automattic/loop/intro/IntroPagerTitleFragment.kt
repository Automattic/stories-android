package com.automattic.loop.intro

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.automattic.loop.R
import com.automattic.loop.databinding.IntroTitleTemplateViewBinding
import com.automattic.loop.util.INVALID_RESOURCE_ID
import com.bumptech.glide.Glide
import com.automattic.loop.bindinghelpers.viewBinding

class IntroPagerTitleFragment : Fragment(R.layout.intro_title_template_view) {
    private val binding by viewBinding(IntroTitleTemplateViewBinding::bind)

    private var titleText: Int = INVALID_RESOURCE_ID
    private var promoText: Int = INVALID_RESOURCE_ID
    private var backgroundImage: Int = INVALID_RESOURCE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            titleText = it.getInt(KEY_TITLE_TEXT)
            promoText = it.getInt(KEY_PROMO_TEXT)
            backgroundImage = it.getInt(KEY_BACKGROUND_IMAGE)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(view) {
            Glide.with(context)
                .load(backgroundImage)
                .into(binding.backgroundImage)
        }

        binding.titleText.setText(titleText)
        binding.promoText.setText(promoText)
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
