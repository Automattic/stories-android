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

    override fun onCreate(savedInstanceState: Bundle?) {
        val model =
            ViewModelProviders.of(this,
                StoryViewModelFactory(StoryRepository.getInstance(), 0))[StoryViewModel::class.java]
        model.storyFrameItems.observe(this, Observer<List<StoryFrameItem>> { frames ->
            // update adapter
            adapter.addAllItems(frames)
        })

        // now setup an observer on the StoryRepository's current story frames so we can update our StoryViewModel
        // this way. By writing to the StoryRepository with its exposed methods, it will propagate changes to
        // their observers, which will update the model's StoryFrameItems and these in turn will update the
        // RecyclerView's adapter.
        //  StoryRepository --> model.storyFrameItems --> RecyclerView's adapter.
        StoryRepository
            .getInstance()
            .getCurrentStoryFramesLiveData()
            .observe(this, Observer<List<StoryFrameItem>> { frames -> model.storyFrameItems.value = frames
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
        adapter = StoryFrameSelectorAdapter(requireContext())
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
