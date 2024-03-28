package com.pallycon.castsample.dialog

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pallycon.castsample.databinding.ItemOptionListBinding
import com.pallycon.widevine.track.VideoTrackInfo
import kotlin.math.roundToInt

class OptionRecyclerViewAdapter(
    private val callback: ((Int) -> Unit)?
) : RecyclerView.Adapter<OptionRecyclerViewAdapter.ViewHolder>() {
    var datalist = mutableListOf<VideoTrackInfo>()
    var selectedItemPos = 0
    var lastItemSelectedPos = 0

    inner class ViewHolder(private val binding: ItemOptionListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(video: VideoTrackInfo, position: Int) {
            binding.root.setOnClickListener {
                selectItem(bindingAdapterPosition)
            }

            binding.radioButton.setOnClickListener {
                selectItem(bindingAdapterPosition)
            }

            if (position == selectedItemPos) {
                binding.radioButton.isChecked = true
                binding.txtTitle.setTextColor(Color.parseColor("#be2ed6"))
            } else {
                binding.radioButton.isChecked = false
                binding.txtTitle.setTextColor(Color.parseColor("#8c8c8c"))
            }

//            binding.radioButton.isChecked = position == selectedItemPos
            val bitrate = (((video.bitrate/1024.0)/1024.0) * 100.0).roundToInt() / 100.0
            binding.txtTitle.text = "${video.width} X ${video.height}, ${bitrate} Mbps"
        }
    }

    fun selectItem(position: Int) {
        selectedItemPos = position
        if (lastItemSelectedPos == -1)
            lastItemSelectedPos = selectedItemPos
        else {
            notifyItemChanged(lastItemSelectedPos)
            lastItemSelectedPos = selectedItemPos
        }
        notifyItemChanged(selectedItemPos)
        callback?.invoke(selectedItemPos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemOptionListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = datalist.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(datalist[position], position)
    }
}