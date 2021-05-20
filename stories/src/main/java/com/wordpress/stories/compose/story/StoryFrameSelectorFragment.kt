package com.wordpress.stories.compose.story

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
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
import com.wordpress.stories.databinding.FragmentStoryFrameItemPlusBinding
import com.wordpress.stories.databinding.FragmentStoryFrameSelectorBinding
import com.wordpress.stories.util.getStoryIndexFromIntentOrBundle
import com.wordpress.stories.viewBinding

interface OnStoryFrameSelectorTappedListener {
    fun onStoryFrameSelected(oldIndex: Int, newIndex: Int)
    fun onStoryFrameAddTapped()
    fun onCurrentFrameTapped()
    fun onStoryFrameLongPressed(oldIndex: Int, newIndex: Int)
    fun onStoryFrameMovedLongPressed()
}

class StoryFrameSelectorFragment : Fragment(R.layout.fragment_story_frame_selector) {
    private val binding by viewBinding(FragmentStoryFrameSelectorBinding::bind)
    private lateinit var plusIconBinding: FragmentStoryFrameItemPlusBinding

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

        storyViewModel.onUserLongPressedFrame.observe(this, Observer { selectedFrameIndexChange ->
            storyFrameTappedListener?.onStoryFrameLongPressed(
                    selectedFrameIndexChange.first, selectedFrameIndexChange.second)
        })

        storyViewModel.onUserMovedLongPressedFrame.observe(this, Observer {
            storyFrameTappedListener?.onStoryFrameMovedLongPressed()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        plusIconBinding = binding.plusIcon
        binding.storyFramesView.adapter = StoryFrameSelectorAdapter()
        // disable animations on selected border visibility changes so users can see the selection
        // change on the selector immediately
        (binding.storyFramesView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        plusIconBinding.root.setOnClickListener {
            storyViewModel.addButtonClicked.call()
        }
        setupItemTouchListener(binding.root)
        binding.root.visibility = View.INVISIBLE
    }

    override fun onAttach(context: Context) {
        if (activity is OnStoryFrameSelectorTappedListener) {
            storyFrameTappedListener = activity as OnStoryFrameSelectorTappedListener
        }
        super.onAttach(context)
    }

    private fun updateContentUiState(contentState: StoryFrameListUiState) {
        (binding.storyFramesView.adapter as StoryFrameSelectorAdapter).addAllItems(contentState.items)
    }

    private fun updateContentUiStateSelection(oldSelection: Int, newSelection: Int) {
        (binding.storyFramesView.adapter as StoryFrameSelectorAdapter)
            .updateContentUiStateSelection(oldSelection, newSelection)
    }

    private fun updateContentUiStateMovedIndex(oldPosition: Int, newPosition: Int) {
        (binding.storyFramesView.adapter as StoryFrameSelectorAdapter)
            .updateContentUiStateMovedIndex(oldPosition, newPosition)
    }

    private fun updateContentUiStateItem(position: Int) {
        (binding.storyFramesView.adapter as StoryFrameSelectorAdapter)
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
                viewHolder.itemView.alpha = 1.0f
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
        itemTouchHelper.attachToRecyclerView(binding.storyFramesView)
    }

    fun show() {
        binding.root.visibility = View.VISIBLE
    }

    fun hide() {
        binding.root.visibility = View.INVISIBLE
    }

    fun hideAddFrameControl() {
        plusIconBinding.root.visibility = View.INVISIBLE
    }

    fun showAddFrameControl() {
        plusIconBinding.root.visibility = View.VISIBLE
    }

    fun setBottomOffset(offset: Int) {
        val params = binding.root.layoutParams as ConstraintLayout.LayoutParams
        val hasChanged = params.bottomMargin != offset
        if (hasChanged) {
            params.bottomMargin = offset
            binding.root.requestLayout()
        }
    }
}
