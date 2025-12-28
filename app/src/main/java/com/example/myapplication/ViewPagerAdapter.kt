package com.example.myapplication

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    companion object {
        const val TAB_COUNT = 2
        const val HISTORY_TAB = 0
        const val CAMERA_TAB = 1
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            HISTORY_TAB -> HistoryFragment()
            CAMERA_TAB -> CameraFragment()
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }
}