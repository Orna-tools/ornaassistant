package com.lloir.ornaassistant.ui.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentAdapter(fragmentManager: FragmentManager,
                      lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    var fragments = mutableListOf<Fragment?>()
    override fun getItemCount(): Int = 1
    override fun createFragment(position: Int): Fragment {

        var frag: Fragment = when (position) {
            0 -> MainFragment()
            1 -> KingdomFragment()
            else -> MainFragment()
        }
        fragments.add(frag)

        return frag
    }
}