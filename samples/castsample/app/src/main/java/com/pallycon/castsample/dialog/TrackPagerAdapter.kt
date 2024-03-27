package com.pallycon.castsample.dialog

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TrackPagerAdapter(
    fragment: DialogFragment
): FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        var count = 0
        if (TrackSelectUtil.tracks.video.size > 0) {
            count++
        }

        if (TrackSelectUtil.tracks.audio.size > 0) {
            count++
        }

        if (TrackSelectUtil.tracks.text.size > 0) {
            count++
        }

        return count
    }

    override fun createFragment(position: Int): Fragment {
        when (position) {
            1 -> return CheckFragment(true) // audio
            2 -> return CheckFragment(false) // text
            else -> return OptionFragment() // video
        }
    }

}