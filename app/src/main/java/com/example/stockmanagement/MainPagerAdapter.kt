package com.example.stockmanagement

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardProduct()
            1 -> PrimaryRecordsFragment()
            2 -> ListRecordsFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}