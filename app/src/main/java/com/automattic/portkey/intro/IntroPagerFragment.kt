package com.automattic.portkey.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.automattic.portkey.R
import com.automattic.portkey.util.INVALID_RESOURCE_ID
import kotlinx.android.synthetic.main.intro_title_template_view.*

class IntroPagerFragment : Fragment() {
    private var promoText: Int = INVALID_RESOURCE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            promoText = it.getInt(KEY_PROMO_TEXT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.intro_template_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(view) {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            promo_text.setText(promoText)
        }
    }

    companion object {
        private const val KEY_PROMO_TEXT = "KEY_PROMO_TEXT"

        internal fun newInstance(promoText: Int): IntroPagerFragment {
            val bundle = Bundle().apply {
                putInt(KEY_PROMO_TEXT, promoText)
            }

            return IntroPagerFragment().apply {
                arguments = bundle
            }
        }
    }
}
