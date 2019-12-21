package com.automattic.portkey.compose.story

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.automattic.portkey.R.layout
import kotlinx.android.synthetic.main.fragment_story_frame_selector.view.*

open class StoryFrameSelectorFragment : Fragment() {
    lateinit var adapter: StoryFrameSelectorAdapter
    lateinit var storyViewModel: StoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO storyIndex here is hardcoded to 0, will need to change once we have multiple stories stored.
        storyViewModel =
                ViewModelProviders.of(requireActivity(), // important to use Activity's context, so we don't
                        // end up looking into the wrong ViewModelProviders bucket key
                        StoryViewModelFactory(StoryRepository, 0))[StoryViewModel::class.java]
        storyViewModel.onStoryFrameItems.observe(this, Observer<List<StoryFrameItem>> { frames ->
            // update adapter
            adapter.addAllItems(frames)
        })

        storyViewModel.onSelectedFrameIndex.observe(this, Observer<Int> { newSelectedFrameIndex ->
            // update adapter
            adapter.notifyDataSetChanged()
        })


        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(layout.fragment_story_frame_selector, container, false)
        // instantiate adapter with an empty Story until it gets loaded
        adapter = StoryFrameSelectorAdapter(requireContext(), storyViewModel)
        view.story_frames_view.adapter = adapter
        return view
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.GONE
    }
}
