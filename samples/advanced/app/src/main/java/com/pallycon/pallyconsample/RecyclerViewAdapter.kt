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

    var datalist = mutableListOf<ContentData>()
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
                if (content.status == DownloadState.PAUSED) {
                    callback?.invoke(content, SelectType.Resume)
                } else {
                    callback?.invoke(content, SelectType.Download)
                }
            }

            binding.removeButton.setOnClickListener {
                callback?.invoke(content, SelectType.Remove)
            }

            binding.pauseButton.setOnClickListener {
                callback?.invoke(content, SelectType.Pause)
            }

            if (content.downloadTracks == null && content.status == DownloadState.NOT) {
                binding.txtStatus.text = "preparing.."
                binding.downloadButton.visibility = View.INVISIBLE
                binding.removeButton.visibility = View.INVISIBLE
                binding.pauseButton.visibility = View.INVISIBLE
            } else {
                binding.txtStatus.text = content.subTitle
                if (content.status == DownloadState.COMPLETED) {
                    binding.downloadButton.visibility = View.INVISIBLE
                    binding.removeButton.visibility = View.VISIBLE
                    binding.pauseButton.visibility = View.INVISIBLE
                } else if (content.status == DownloadState.DOWNLOADING) {
                    binding.downloadButton.visibility = View.INVISIBLE
                    binding.removeButton.visibility = View.INVISIBLE
                    binding.pauseButton.visibility = View.VISIBLE
                } else {
                    binding.downloadButton.visibility = View.VISIBLE
                    binding.removeButton.visibility = View.INVISIBLE
                    binding.pauseButton.visibility = View.INVISIBLE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding=ItemListBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = datalist.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(datalist[position])
    }
}