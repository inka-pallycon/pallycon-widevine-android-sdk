package com.pallycon.pallyconsample.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.pallycon.pallyconsample.databinding.DialogTrackSelectBinding
import com.pallycon.widevine.track.PallyConDownloaderTracks

class TrackSelectDialog(
    tracks: PallyConDownloaderTracks,
    private val onOk: ((PallyConDownloaderTracks) -> Unit)?
) : DialogFragment() {
    private lateinit var binding: DialogTrackSelectBinding
    private val tabTitleArray = arrayOf(
        "video",
        "audio",
        "text"
    )
//    var tabLayout: TabLayout? = null
//    var viewPager: ViewPager2? = null
//    var pagerAdapter: TrackPagerAdapter? = null

    init {
        TrackSelectUtil.tracks = tracks
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogTrackSelectBinding.inflate(inflater, container, false)
        val pagerAdapter = TrackPagerAdapter(this)
        val viewPager = binding.trackSelectionDialogViewPager
        viewPager.adapter = pagerAdapter
        val tabLayout = binding.trackSelectionDialogTabLayout

        binding.trackSelectionDialogOkButton.setOnClickListener {
            onOk?.invoke(TrackSelectUtil.tracks)
            this.dismiss()
        }

        binding.trackSelectionDialogCancelButton.setOnClickListener {
            this.dismiss()
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitleArray[position]
        }.attach()

        return binding.root
    }

    companion object {
        const val TAG = "TrackSelectDialog"
    }
}