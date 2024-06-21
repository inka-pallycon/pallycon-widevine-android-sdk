package com.pallycon.pallyconsample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pallycon.pallyconsample.databinding.ItemListBinding
import com.pallycon.widevine.model.DownloadState

class RecyclerViewAdapter(
    private val callback: ((ContentData, SelectType) -> Unit)?
): RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    var dataList = mutableListOf<ContentData>()
    inner class ViewHolder(private val binding: ItemListBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(content: ContentData){
            binding.root.setOnClickListener {
                callback?.invoke(content, SelectType.Play)
            }

            binding.root.setOnLongClickListener {
                callback?.invoke(content, SelectType.Menu)
                false
            }

            binding.txtTitle.text = content.title

            binding.downloadButton.setOnClickListener {
                callback?.invoke(content, SelectType.Download)
            }

            binding.removeButton.setOnClickListener {
                callback?.invoke(content, SelectType.Remove)
            }

            binding.stopButton.setOnClickListener {
                callback?.invoke(content, SelectType.Stop)
            }

            if (content.downloadTracks == null && content.status == DownloadState.NOT) {
                binding.txtStatus.text = "preparing.."
                binding.downloadButton.visibility = View.INVISIBLE
                binding.removeButton.visibility = View.INVISIBLE
                binding.stopButton.visibility = View.INVISIBLE
            } else {
                binding.txtStatus.text = content.subTitle
                if (content.status == DownloadState.COMPLETED) {
                    binding.downloadButton.visibility = View.INVISIBLE
                    binding.removeButton.visibility = View.VISIBLE
                    binding.stopButton.visibility = View.INVISIBLE
                } else if (content.status == DownloadState.DOWNLOADING) {
                    binding.downloadButton.visibility = View.INVISIBLE
                    binding.removeButton.visibility = View.INVISIBLE
                    binding.stopButton.visibility = View.VISIBLE
                } else {
                    binding.downloadButton.visibility = View.VISIBLE
                    binding.removeButton.visibility = View.INVISIBLE
                    binding.stopButton.visibility = View.INVISIBLE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding=ItemListBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    fun updateItem(index: Int, content: ContentData) {
        if (index in dataList.indices) {
            dataList[index] = content
            notifyItemChanged(index)
        }
    }
}