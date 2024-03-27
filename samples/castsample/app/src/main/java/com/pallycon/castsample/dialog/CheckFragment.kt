package com.pallycon.castsample.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.pallycon.castsample.databinding.FragmentCheckBinding

class CheckFragment(
    val isAudio: Boolean
) : Fragment() {

    private lateinit var binding: FragmentCheckBinding
    private lateinit var adapter: CheckRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCheckBinding.inflate(inflater, container, false)
        adapter = CheckRecyclerViewAdapter { selected ->
            if (isAudio) {
                for (n in 0 until TrackSelectUtil.tracks.audio.size) {
                    TrackSelectUtil.tracks.audio[n].isDownload = selected.contains(n)
                }
            } else {
                for (n in 0 until TrackSelectUtil.tracks.text.size) {
                    TrackSelectUtil.tracks.text[n].isDownload = selected.contains(n)
                }
            }

        }

        if (isAudio) {
            adapter.audioTrackInfoArray = TrackSelectUtil.tracks.audio
            for (i in 0 until TrackSelectUtil.tracks.audio.size) {
                if (TrackSelectUtil.tracks.audio[i].isDownload) {
                    adapter.selectedItems.add(i)
                }
            }
        } else {
            adapter.textTrackInfoArray = TrackSelectUtil.tracks.text
            for (i in 0 until TrackSelectUtil.tracks.text.size) {
                if (TrackSelectUtil.tracks.text[i].isDownload) {
                    adapter.selectedItems.add(i)
                }
            }
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val animator: RecyclerView.ItemAnimator? = binding.recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            (animator as SimpleItemAnimator).supportsChangeAnimations = false
        }
        return binding.root
    }
}