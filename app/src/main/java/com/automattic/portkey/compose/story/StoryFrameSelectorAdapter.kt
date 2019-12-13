package com.automattic.portkey.compose.story

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.automattic.portkey.R
import kotlinx.android.synthetic.main.fragment_story_frame_item.view.*

class StoryFrameSelectorAdapter(val storyFrameItems: Story, val context: Context) : RecyclerView.Adapter<StoryFrameSelectorAdapter.StoryFrameHolder>()  {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoryFrameHolder {
        return StoryFrameHolder(LayoutInflater.from(context).inflate(R.layout.fragment_story_frame_item, parent, false))
    }

    override fun getItemCount(): Int {
        return storyFrameItems.frames.size
    }

    override fun onBindViewHolder(holder: StoryFrameHolder, position: Int) {
        holder.textView.text = storyFrameItems.frames.get(position).name
    }

    class StoryFrameHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textView = v.frame_name
    }
}
