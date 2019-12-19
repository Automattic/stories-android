package com.automattic.portkey.compose.story

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.automattic.portkey.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.android.synthetic.main.fragment_story_frame_item.view.*

interface OnStoryFrameSelectorTappedListener {
    fun onStoryFrameSelected(index: Int)
    fun onStoryFrameAddTapped()
}

class StoryFrameSelectorAdapter(
    val context: Context,
    val clickListener: OnStoryFrameSelectorTappedListener?
) : RecyclerView.Adapter<StoryFrameSelectorAdapter.StoryFrameHolder>() {
    private val storyFrameItemsWithPlusControl = Story(ArrayList())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoryFrameHolder {
        return if (viewType == VIEW_TYPE_PLUS_ICON) {
            StoryFrameHolder(
                LayoutInflater
                    .from(context)
                    .inflate(R.layout.fragment_story_frame_item_plus, parent, false)
            )
        } else {
            StoryFrameHolder(
                LayoutInflater
                    .from(context)
                    .inflate(R.layout.fragment_story_frame_item, parent, false)
            )
        }
    }

    override fun getItemCount(): Int {
        return storyFrameItemsWithPlusControl.frames.size
    }

    override fun onBindViewHolder(holder: StoryFrameHolder, position: Int) {
        // first position has the plus icon, so skip that one
        if (position != 0) {
            holder.clickableView.setOnClickListener { view ->
                val previousSelectedPosition = StoryRepository.getSelectedFrameIndex() + 1
                if (position != previousSelectedPosition) {
                    notifyItemChanged(position)
                    notifyItemChanged(previousSelectedPosition)
                }
                clickListener?.onStoryFrameSelected(position - 1)
            }

            Glide.with(context)
                .load(storyFrameItemsWithPlusControl.frames[position].filePath)
                .transform(CenterCrop(), RoundedCorners(8))
                .into(holder.imageView)

            if (StoryRepository.getSelectedFrameIndex() == (position - 1)) {
                // paint it selected
                holder.frameSelected.visibility = View.VISIBLE
            } else {
                holder.frameSelected.visibility = View.GONE
            }
        } else {
            holder.clickableView.setOnClickListener { view ->
                clickListener?.onStoryFrameAddTapped()
            }
            // always draw border for the PLUS icon button
            holder.frameSelected.visibility = View.VISIBLE
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            // plus icon
            VIEW_TYPE_PLUS_ICON
        } else {
            VIEW_TYPE_IMAGE
        }
    }

    fun insertItem(item: StoryFrameItem) {
        // items are inserted to the left. Index 0 is always occupied by the Plus control, next available index is 1.
        storyFrameItemsWithPlusControl.frames.add(1, item)
        notifyDataSetChanged()
    }

    // useful for loading an existing story to edit
    fun addAllItems(items: List<StoryFrameItem>) {
        storyFrameItemsWithPlusControl.frames.clear()
        storyFrameItemsWithPlusControl.frames.add(StoryFrameItem("")) // adds a placeholder for the plus button
        storyFrameItemsWithPlusControl.frames.addAll(items) // now add all items from the passed Story frame list
        notifyDataSetChanged()
    }

    class StoryFrameHolder(v: View) : RecyclerView.ViewHolder(v) {
        val clickableView = v // entire view should be clickable
        val imageView: ImageView = v.frame_image
        val frameSelected: ImageView = v.frame_image_selected
    }

    companion object {
        const val VIEW_TYPE_PLUS_ICON = 0
        const val VIEW_TYPE_IMAGE = 1
    }
}
