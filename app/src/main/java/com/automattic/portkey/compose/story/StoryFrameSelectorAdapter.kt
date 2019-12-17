package com.automattic.portkey.compose.story

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.automattic.portkey.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.android.synthetic.main.fragment_story_frame_item.view.*

class StoryFrameSelectorAdapter(
    val storyFrameItems: Story,
    val context: Context
) : RecyclerView.Adapter<StoryFrameSelectorAdapter.StoryFrameHolder>() {
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
        return storyFrameItems.frames.size
    }

    override fun onBindViewHolder(holder: StoryFrameHolder, position: Int) {
        // first position has the plus icon, so skip that one
        if (position != 0) {
            // holder.textView.text = storyFrameItems.frames.get(position).name
            holder.clickableView.setOnClickListener { view ->
                Toast.makeText(context, "IMAGE CLICKED: " + position, Toast.LENGTH_SHORT).show()
            }

            Glide.with(context)
                .load(R.drawable.intro02) // TODO change for data coming from datasource at [position]
                .transform(CenterCrop(), RoundedCorners(16))
                .into(holder.imageView)
        } else {
            holder.clickableView.setOnClickListener { view ->
                Toast.makeText(context, "PLUS CLICKED: " + position, Toast.LENGTH_SHORT).show()
            }
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

    class StoryFrameHolder(v: View) : RecyclerView.ViewHolder(v) {
        val clickableView = v // entire view should be clickable
        val imageView: ImageView = v.frame_image
    }

    companion object {
        const val VIEW_TYPE_PLUS_ICON = 0
        const val VIEW_TYPE_IMAGE = 1
    }
}
