package com.automattic.portkey.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.automattic.portkey.R
import com.wordpress.stories.util.INVALID_RESOURCE_ID
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.intro_title_template_view.*

class IntroPagerFragment : Fragment() {
    private var promoText: Int = INVALID_RESOURCE_ID
    private var backgroundImage: Int = INVALID_RESOURCE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            promoText = it.getInt(KEY_PROMO_TEXT)
            backgroundImage = it.getInt(KEY_BACKGROUND_IMAGE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.intro_template_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(view) {
            Glide.with(context)
                .load(backgroundImage)
                .into(background_image)
        }
        promo_text.setText(promoText)
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
