package com.automattic.portkey.compose.story

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.automattic.portkey.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.android.synthetic.main.fragment_story_frame_item.view.*

class StoryFrameSelectorAdapter(
    val context: Context
) : RecyclerView.Adapter<StoryFrameSelectorAdapter.StoryFrameHolder>() {
    private val storyFrameItemsWithPlusControl = Story(ArrayList())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoryFrameHolder {
        if (viewType == VIEW_TYPE_PLUS_ICON) {
            return StoryFrameHolder(
                LayoutInflater
                    .from(context)
                    .inflate(R.layout.fragment_story_frame_item_plus, parent, false)
            )
        } else {
            return StoryFrameHolder(
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
                Toast.makeText(context, "IMAGE CLICKED: " + position, Toast.LENGTH_SHORT).show()
            }

            Glide.with(context)
                .load(R.drawable.intro02) // TODO change for data coming from datasource at [position]
                .transform(CenterCrop(), RoundedCorners(16))
                .into(holder.imageView)

            if (StoryRepository.getInstance().getSelectedFrameIndex() == (position - 1)) {
                // paint it selected
                holder.frameSelected.visibility = View.VISIBLE
            } else {
                holder.frameSelected.visibility = View.GONE
            }
        } else {
            holder.clickableView.setOnClickListener { view ->
                Toast.makeText(context, "PLUS CLICKED: " + position, Toast.LENGTH_SHORT).show()
            }
            // always draw border for the PLUS icon button
            holder.frameSelected.visibility = View.VISIBLE
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            // plus icon
            return VIEW_TYPE_PLUS_ICON
        } else {
            return VIEW_TYPE_IMAGE
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
        val imageView = v.frame_image
        val frameSelected = v.frame_image_selected
    }

    companion object {
        const val VIEW_TYPE_PLUS_ICON = 0
        const val VIEW_TYPE_IMAGE = 1
    }
}
