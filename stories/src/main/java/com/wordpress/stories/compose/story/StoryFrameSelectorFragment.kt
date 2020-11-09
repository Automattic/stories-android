package com.wordpress.stories.compose.story

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import com.wordpress.stories.R
import com.wordpress.stories.compose.story.StoryViewModel.StoryFrameListUiState
import com.wordpress.stories.util.getStoryIndexFromIntentOrBundle
import kotlinx.android.synthetic.main.fragment_story_frame_selector.*
import kotlinx.android.synthetic.main.fragment_story_frame_selector.view.*

interface OnStoryFrameSelectorTappedListener {
    fun onStoryFrameSelected(oldIndex: Int, newIndex: Int)
    fun onStoryFrameAddTapped()
    fun onCurrentFrameTapped()
}

class StoryFrameSelectorFragment : Fragment() {
    lateinit var storyViewModel: StoryViewModel
    private var storyFrameTappedListener: OnStoryFrameSelectorTappedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val storyIndex: StoryIndex = getStoryIndexFromIntentOrBundle(savedInstanceState, activity?.intent)
        storyViewModel =
                ViewModelProvider(requireActivity(), // important to use Activity's context, so we don't
                        // end up looking into the wrong ViewModelProviders bucket key
                        StoryViewModelFactory(StoryRepository, storyIndex))[StoryViewModel::class.java]

        storyViewModel.onSelectedFrameIndex.observe(this, Observer<Pair<Int, Int>> { selectedFrameIndexChange ->
            updateContentUiStateSelection(selectedFrameIndexChange.first, selectedFrameIndexChange.second)
        })

        storyViewModel.onUserSelectedFrame.observe(this, Observer { selectedFrameIndexChange ->
            storyFrameTappedListener?.onStoryFrameSelected(
                selectedFrameIndexChange.first, selectedFrameIndexChange.second
            )
        })

        storyViewModel.onUserTappedCurrentFrame.observe(this, Observer {
            storyFrameTappedListener?.onCurrentFrameTapped()
        })

        storyViewModel.onFrameIndexMoved.observe(this, Observer<Pair<Int, Int>> { positionFrameIndexChange ->
            updateContentUiStateMovedIndex(positionFrameIndexChange.first, positionFrameIndexChange.second)
        })

        storyViewModel.addButtonClicked.observe(this, Observer {
            storyFrameTappedListener?.onStoryFrameAddTapped()
        })

        storyViewModel.itemAtIndexChangedUiState.observe(this, Observer { uiStateFrameIndex ->
            updateContentUiStateItem(uiStateFrameIndex)
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
        val view = inflater.inflate(R.layout.fragment_story_frame_selector, container, false)
        view.story_frames_view.adapter = StoryFrameSelectorAdapter()
        // disable animations on selected border visibility changes so users can see the selection
        // change on the selector immediately
        (view.story_frames_view.getItemAnimator() as SimpleItemAnimator).setSupportsChangeAnimations(false)
        view.plus_icon.setOnClickListener {
            storyViewModel.addButtonClicked.call()
        }
        setupItemTouchListener(view)
        view.visibility = View.INVISIBLE
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

    private fun updateContentUiStateMovedIndex(oldPosition: Int, newPosition: Int) {
        (story_frames_view.adapter as StoryFrameSelectorAdapter)
            .updateContentUiStateMovedIndex(oldPosition, newPosition)
    }

    private fun updateContentUiStateItem(position: Int) {
        (story_frames_view.adapter as StoryFrameSelectorAdapter)
            .updateContentUiStateItem(position)
    }

    private fun setupItemTouchListener(view: View) {
        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                storyViewModel.swapItemsInPositions(fromPos, toPos)
                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.setAlpha(1.0f)
                storyViewModel.onSwapActionEnded()
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
                var dragFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                val swipeFlags = 0
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.alpha = 0.5f
                } else {
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(view.story_frames_view)
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.GONE
    }

    fun hideAddFrameControl() {
        view?.plus_icon?.visibility = View.INVISIBLE
    }

    fun showAddFrameControl() {
        view?.plus_icon?.visibility = View.VISIBLE
    }

    fun setBottomOffset(offset: Int) {
        val params = view?.layoutParams as ConstraintLayout.LayoutParams
        val hasChanged = params.bottomMargin != offset
        if (hasChanged) {
            params.bottomMargin = offset
            view?.requestLayout()
        }
    }
}
