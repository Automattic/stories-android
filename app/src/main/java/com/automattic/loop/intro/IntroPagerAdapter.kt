package com.automattic.loop.intro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.automattic.loop.R

class IntroPagerAdapter(supportFragmentManager: FragmentManager) : FragmentPagerAdapter(supportFragmentManager) {
    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            IntroPagerTitleFragment.newInstance(R.string.intro_title_text, PROMO_TEXTS[position],
                BACKGROUND_IMAGES[position])
        } else {
            IntroPagerFragment.newInstance(PROMO_TEXTS[position], BACKGROUND_IMAGES[position])
        }
    }

    override fun getCount(): Int {
        return PROMO_TEXTS.size
    }

    companion object {
        private val PROMO_TEXTS = intArrayOf(
            R.string.intro_promo_text_share,
            R.string.intro_promo_text_create,
            R.string.intro_promo_text_organize,
            R.string.intro_promo_text_control,
            R.string.intro_promo_text_invite
        )

        private val BACKGROUND_IMAGES = intArrayOf(
            R.drawable.intro01,
            R.drawable.intro02,
            R.drawable.intro03,
            R.drawable.intro04,
            R.drawable.intro05
        )
    }
}
