package com.pallycon.pallyconsample.dialog

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pallycon.pallyconsample.databinding.ItemCheckListBinding
import com.pallycon.widevine.track.AudioTrackInfo
import com.pallycon.widevine.track.TextTrackInfo
import com.pallycon.widevine.track.TrackInfo
import kotlin.math.roundToInt

class CheckRecyclerViewAdapter(
    private val callback: ((ArrayList<Int>) -> Unit)?
) : RecyclerView.Adapter<CheckRecyclerViewAdapter.ViewHolder>() {
    var audioTrackInfoArray = mutableListOf<AudioTrackInfo>()
    var textTrackInfoArray = mutableListOf<TextTrackInfo>()
    val selectedItems = ArrayList<Int>()

    inner class ViewHolder(private val binding: ItemCheckListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(track: TrackInfo, position: Int) {
            binding.root.setOnClickListener {
                selectItem(bindingAdapterPosition)
            }

            binding.checkbox.setOnClickListener {
                selectItem(bindingAdapterPosition)
            }

            if (selectedItems.contains(position)) {
                binding.checkbox.isChecked = true
                binding.txtTitle.setTextColor(Color.parseColor("#be2ed6"))
            } else {
                binding.checkbox.isChecked = false
                binding.txtTitle.setTextColor(Color.parseColor("#8c8c8c"))
            }

            if (audioTrackInfoArray.size > 0) {
                val bitrate = (((track.bitrate/1024.0)/1024.0) * 100.0).roundToInt() / 100.0
                binding.txtTitle.text = "${track.language}, ${track.codecs}, ${bitrate} Mbps"
            } else {
                binding.txtTitle.text = "${track.language}"
            }
        }
    }

    fun selectItem(position: Int) {
        if (!selectedItems.contains(position)) {
            selectedItems.add(position)
        } else {
            selectedItems.remove(position)
        }

        notifyItemChanged(position)
        callback?.invoke(selectedItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCheckListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return if (audioTrackInfoArray.size > 0) {
            audioTrackInfoArray.size
        } else {
            textTrackInfoArray.size
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (audioTrackInfoArray.size > 0) {
            holder.bind(audioTrackInfoArray[position], position)
        } else {
            holder.bind(textTrackInfoArray[position], position)
        }
    }
}