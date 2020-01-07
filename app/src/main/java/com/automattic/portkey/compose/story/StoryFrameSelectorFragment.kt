package com.automattic.portkey.compose.story

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.automattic.portkey.R.layout
import com.automattic.portkey.compose.story.StoryViewModel.StoryFrameListUiState
import kotlinx.android.synthetic.main.fragment_story_frame_selector.*
import kotlinx.android.synthetic.main.fragment_story_frame_selector.view.*

open class StoryFrameSelectorFragment : Fragment() {
    lateinit var storyViewModel: StoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO storyIndex here is hardcoded to 0, will need to change once we have multiple stories stored.
        storyViewModel =
                ViewModelProviders.of(requireActivity(), // important to use Activity's context, so we don't
                        // end up looking into the wrong ViewModelProviders bucket key
                        StoryViewModelFactory(StoryRepository, 0))[StoryViewModel::class.java]

        storyViewModel.onSelectedFrameIndex.observe(this, Observer<Pair<Int, Int>> { selectedFrameIndexChange ->
            updateContentUiStateSelection(selectedFrameIndexChange.first, selectedFrameIndexChange.second)
        })

        storyViewModel.addButtonClicked.observe(this, Observer {
            // TODO here introduce the actual handler
            Toast.makeText(requireContext(), "CLICKED ADD", Toast.LENGTH_SHORT).show()
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

    private fun updateContentUiState(contentState: StoryFrameListUiState) {
        (story_frames_view.adapter as StoryFrameSelectorAdapter).addAllItems(contentState.items)
    }

    private fun updateContentUiStateSelection(oldSelection: Int, newSelection: Int) {
        val adapter = story_frames_view.adapter as StoryFrameSelectorAdapter
        if (oldSelection == newSelection) {
            // just call it once
            adapter.notifyItemChanged(oldSelection)
        } else {
            adapter.notifyItemChanged(oldSelection)
            adapter.notifyItemChanged(newSelection)
        }
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.GONE
    }
}
