package com.automattic.portkey.compose.story

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.automattic.portkey.R
import com.automattic.portkey.R.layout
import com.automattic.portkey.compose.story.StoryFrameSelectorAdapter.StoryFrameHolder.StoryFrameHolderItem
import com.automattic.portkey.compose.story.StoryFrameSelectorAdapter.StoryFrameHolder.StoryFrameHolderPlusIcon
import com.automattic.portkey.compose.story.StoryViewModel.StoryFrameListItemUiState
import com.automattic.portkey.compose.story.StoryViewModel.StoryFrameListItemUiState.StoryFrameListItemUiStateFrame
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.android.synthetic.main.fragment_story_frame_item.view.*

class StoryFrameSelectorAdapter : RecyclerView.Adapter<StoryFrameSelectorAdapter.StoryFrameHolder>() {
    private val items = mutableListOf<StoryFrameListItemUiState>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoryFrameHolder {
        return when (viewType) {
            VIEW_TYPE_PLUS_ICON ->
                StoryFrameHolderPlusIcon(
                    LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.fragment_story_frame_item_plus, parent, false)
                )
            VIEW_TYPE_IMAGE ->
                StoryFrameHolderItem(
                    LayoutInflater
                        .from(parent.context)
                        .inflate(layout.fragment_story_frame_item, parent, false)
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
        return if (position == 0) {
            // plus icon
            VIEW_TYPE_PLUS_ICON
        } else {
            VIEW_TYPE_IMAGE
        }
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

    fun updateContentUiStateMovedIndex(oldPosition: Int, newPosition: Int) {
        notifyItemMoved(oldPosition, newPosition)
    }

    sealed class StoryFrameHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clickableView = view // entire view should be clickable
        val imageView: ImageView = view.frame_image
        val frameBorder: ImageView = view.frame_image_selected
        val frameErrored: TextView? = view.frame_image_errored
        abstract fun onBind(uiState: StoryFrameListItemUiState)

        class StoryFrameHolderPlusIcon(view: View) : StoryFrameHolder(view) {
            private var onPlusIconClicked: (() -> Unit)? = null

            init {
                clickableView.setOnClickListener {
                    onPlusIconClicked?.invoke()
                }
            }

            override fun onBind(uiState: StoryFrameListItemUiState) {
                onPlusIconClicked = requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
                // always draw border for the PLUS icon button
                frameBorder.visibility = View.VISIBLE
                frameErrored?.visibility = View.GONE
            }
        }

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

                Glide.with(imageView.context)
                    .load(uiState.filePath)
                    .transform(CenterCrop(), RoundedCorners(8))
                    .into(imageView)

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
        const val VIEW_TYPE_PLUS_ICON = 0
        const val VIEW_TYPE_IMAGE = 1
    }
}
