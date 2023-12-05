package com.rockethat.ornaassistant.ui.fragment

import OrnaHubFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    val frags = mutableListOf<Fragment>()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> MainFragment()
            1 -> OrnaHubFragment()
            2 -> KingdomFragment()
            else -> MainFragment()
        }
        // Update or add the fragment in the list
        if (position < frags.size) {
            frags[position] = fragment
        } else {
            frags.add(fragment)
        }
        return fragment
    }
}
