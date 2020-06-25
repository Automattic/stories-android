package com.wordpress.stories.compose.story

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.wordpress.stories.compose.story.StoryFrameSelectorAdapter.StoryFrameHolder.StoryFrameHolderItem
import com.wordpress.stories.compose.story.StoryViewModel.StoryFrameListItemUiState
import com.wordpress.stories.compose.story.StoryViewModel.StoryFrameListItemUiState.StoryFrameListItemUiStateFrame
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.wordpress.stories.R
import kotlinx.android.synthetic.main.fragment_story_frame_item.view.*

class StoryFrameSelectorAdapter : RecyclerView.Adapter<StoryFrameSelectorAdapter.StoryFrameHolder>() {
    private val items = mutableListOf<StoryFrameListItemUiState>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoryFrameHolder {
        return when (viewType) {
            VIEW_TYPE_IMAGE ->
                StoryFrameHolderItem(
                    LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.fragment_story_frame_item, parent, false)
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

    sealed class StoryFrameHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clickableView = view // entire view should be clickable
        val imageView: ImageView = view.frame_image
        val frameBorder: ImageView = view.frame_image_selected
        val frameErrored: ImageView? = view.frame_image_errored
        abstract fun onBind(uiState: StoryFrameListItemUiState)

        class StoryFrameHolderItem(v: View) : StoryFrameHolder(v) {
            private var onFrameSelected: (() -> Unit)? = null

            init {
                clickableView.setOnClickListener {
                    onFrameSelected?.invoke()
                }
            }

            override fun onBind(uiState: StoryFrameListItemUiState) {
                onFrameSelected = requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
                uiState as StoryFrameListItemUiStateFrame

                if (URLUtil.isNetworkUrl(uiState.filePath)) {
                    imageView.postDelayed({
                        // get the first frame in the video, that is the frame located at frameTime 0
                        val options = RequestOptions().frame(0)
                        Glide.with(imageView.context)
                            .load(uiState.filePath)
                            .apply(options)
                            .transform(CenterCrop(), RoundedCorners(8))
                            .into(imageView)
                    }, REMOTE_DELAY_MILLIS)
                } else {
                    // get the first frame in the video, that is the frame located at frameTime 0
                    val options = RequestOptions().frame(0)
                    Glide.with(imageView.context)
                        .load(uiState.filePath)
                        .apply(options)
                        .transform(CenterCrop(), RoundedCorners(8))
                        .into(imageView)
                }

                if (uiState.selected) {
                    frameBorder.visibility = View.VISIBLE
                } else {
                    frameBorder.visibility = View.GONE
                }

                frameErrored?.let {
                    if (uiState.errored) {
                        it.visibility = View.VISIBLE
                    } else {
                        it.visibility = View.GONE
                    }
                }
            }
        }
    }

    companion object {
        const val VIEW_TYPE_IMAGE = 0
        const val REMOTE_DELAY_MILLIS = 1000.toLong()
    }
}
