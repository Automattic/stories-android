package com.automattic.portkey.compose.story

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.automattic.portkey.R.layout
import com.automattic.portkey.compose.story.StoryViewModel.StoryFrameListUiState
import kotlinx.android.synthetic.main.fragment_story_frame_selector.*
import kotlinx.android.synthetic.main.fragment_story_frame_selector.view.*

interface OnStoryFrameSelectorTappedListener {
    fun onStoryFrameSelected(index: Int)
    fun onStoryFrameAddTapped()
}

open class StoryFrameSelectorFragment : Fragment() {
    lateinit var storyViewModel: StoryViewModel
    private var storyFrameTappedListener: OnStoryFrameSelectorTappedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO storyIndex here is hardcoded to 0, will need to change once we have multiple stories stored.
        storyViewModel =
                ViewModelProviders.of(requireActivity(), // important to use Activity's context, so we don't
                        // end up looking into the wrong ViewModelProviders bucket key
                        StoryViewModelFactory(StoryRepository, 0))[StoryViewModel::class.java]

        storyViewModel.onSelectedFrameIndex.observe(this, Observer<Pair<Int, Int>> { selectedFrameIndexChange ->
            updateContentUiStateSelection(selectedFrameIndexChange.first, selectedFrameIndexChange.second)
            storyFrameTappedListener?.onStoryFrameSelected(selectedFrameIndexChange.second)
        })

        storyViewModel.addButtonClicked.observe(this, Observer {
            storyFrameTappedListener?.onStoryFrameAddTapped()
        })

        storyViewModel.uiState.observe(this, Observer { uiState ->
            uiState?.let {
                updateContentUiState(uiState)
            }
        })

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(layout.fragment_story_frame_selector, container, false)
        view.story_frames_view.adapter = StoryFrameSelectorAdapter()
        // TODO storyIndex here is hardcoded to 0, will need to change once we have multiple stories stored.
        storyViewModel.loadStory(0)
        return view
    }

    override fun onAttach(context: Context) {
        if (activity is OnStoryFrameSelectorTappedListener) {
            storyFrameTappedListener = activity as OnStoryFrameSelectorTappedListener
        }
        super.onAttach(context)
    }

    private fun updateContentUiState(contentState: StoryFrameListUiState) {
        (story_frames_view.adapter as StoryFrameSelectorAdapter).addAllItems(contentState.items)
    }

    private fun updateContentUiStateSelection(oldSelection: Int, newSelection: Int) {
        (story_frames_view.adapter as StoryFrameSelectorAdapter)
            .updateContentUiStateSelection(oldSelection, newSelection)
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.GONE
    }
}
