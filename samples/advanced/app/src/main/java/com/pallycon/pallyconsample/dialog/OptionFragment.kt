package com.pallycon.pallyconsample.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.pallycon.pallyconsample.RecyclerViewAdapter
import com.pallycon.pallyconsample.SelectType
import com.pallycon.pallyconsample.databinding.FragmentOptionBinding

class OptionFragment() : Fragment() {
    private lateinit var binding: FragmentOptionBinding
    private lateinit var adapter: OptionRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOptionBinding.inflate(inflater, container, false)
        adapter = OptionRecyclerViewAdapter() { index ->
            for (n in 0 until TrackSelectUtil.tracks.video.size) {
                TrackSelectUtil.tracks.video[n].isDownload = index == n
            }
        }
        adapter?.datalist = TrackSelectUtil.tracks.video
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val animator: RecyclerView.ItemAnimator? = binding.recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            (animator as SimpleItemAnimator).supportsChangeAnimations = false
        }
        return binding.root
    }

}