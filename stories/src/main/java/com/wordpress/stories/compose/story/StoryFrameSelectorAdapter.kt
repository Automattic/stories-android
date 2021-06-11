package com.wordpress.stories.compose.story

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.wordpress.stories.compose.story.StoryFrameSelectorAdapter.StoryFrameHolder.StoryFrameHolderItem
import com.wordpress.stories.compose.story.StoryViewModel.StoryFrameListItemUiState
import com.wordpress.stories.compose.story.StoryViewModel.StoryFrameListItemUiState.StoryFrameListItemUiStateFrame
import com.wordpress.stories.databinding.FragmentStoryFrameItemBinding

class StoryFrameSelectorAdapter : RecyclerView.Adapter<StoryFrameSelectorAdapter.StoryFrameHolder>() {
    private val items = mutableListOf<StoryFrameListItemUiState>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoryFrameHolder {
        return when (viewType) {
            VIEW_TYPE_IMAGE ->
                StoryFrameHolderItem(
                    FragmentStoryFrameItemBinding.inflate(
                        LayoutInflater
                            .from(parent.context)
                    )
                )
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: StoryFrameHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_IMAGE
    }

    // useful for loading an existing story to edit
    fun addAllItems(newItems: List<StoryFrameListItemUiState>) {
        items.clear()
        items.addAll(newItems) // now add all items from the passed Story frame UiState list
        notifyDataSetChanged()
    }

    fun updateContentUiStateSelection(oldSelection: Int, newSelection: Int) {
        if (oldSelection == newSelection) {
            // just call it once
            notifyItemChanged(oldSelection)
        } else {
            notifyItemChanged(oldSelection)
            notifyItemChanged(newSelection)
        }
    }

    fun updateContentUiStateItem(position: Int) {
        notifyItemChanged(position)
    }

    fun updateContentUiStateMovedIndex(oldPosition: Int, newPosition: Int) {
        notifyItemMoved(oldPosition, newPosition)
    }

    sealed class StoryFrameHolder(binding: FragmentStoryFrameItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val clickableView = binding.root // entire view should be clickable
        abstract fun onBind(uiState: StoryFrameListItemUiState)

        class StoryFrameHolderItem(val binding: FragmentStoryFrameItemBinding) : StoryFrameHolder(binding) {
            private var onFrameSelected: (() -> Unit)? = null
            private var onFrameLongPressed: (() -> Unit)? = null

            init {
                clickableView.setOnClickListener {
                    onFrameSelected?.invoke()
                }
                clickableView.setOnLongClickListener {
                    onFrameLongPressed?.invoke()
                    true
                }
            }

            override fun onBind(uiState: StoryFrameListItemUiState) {
                onFrameSelected = requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
                onFrameLongPressed = requireNotNull(uiState.onItemLongPressed) { "OnItemLongPressed is required." }
                uiState as StoryFrameListItemUiStateFrame

                val loadThumbnailImage = {
                    // get the first frame in the video, that is the frame located at frameTime 0
                    val options = RequestOptions().frame(0)
                    Glide.with(binding.frameImage.context)
                        .load(uiState.filePath)
                        .apply(options)
                        .transform(CenterCrop(), RoundedCorners(8))
                        .into(binding.frameImage)
                }

                if (URLUtil.isNetworkUrl(uiState.filePath)) {
                    binding.frameImage.postDelayed({
                        loadThumbnailImage()
                    }, REMOTE_DELAY_MILLIS)
                } else {
                    loadThumbnailImage()
                }

                if (uiState.selected) {
                    binding.frameImageSelected.visibility = View.VISIBLE
                } else {
                    binding.frameImageSelected.visibility = View.GONE
                }

                if (uiState.errored) {
                    binding.frameImageErrored.visibility = View.VISIBLE
                } else {
                    binding.frameImageErrored.visibility = View.GONE
                }
            }
        }
    }

    companion object {
        const val VIEW_TYPE_IMAGE = 0
        const val REMOTE_DELAY_MILLIS = 1000.toLong()
    }
}
