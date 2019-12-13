package com.automattic.portkey.compose.story

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.automattic.portkey.R.layout
import kotlinx.android.synthetic.main.fragment_story_frame_selector.view.*

open class StoryFrameSelectorFragment : Fragment() {
    init {
        // TODO remove this init, used for development only
        FAKE_CONTENT.frames.add(StoryFrameItem("test1"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test2"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test3"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test4"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test5"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test6"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test7"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test8"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test9"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test10"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test11"))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(layout.fragment_story_frame_selector, container, false)
        view.story_frames_view.adapter = StoryFrameSelectorAdapter(FAKE_CONTENT, activity!!)
        return view
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.GONE
    }

    companion object {
        // TODO remove this val, used for development only
        val FAKE_CONTENT = Story(ArrayList())
    }
}
