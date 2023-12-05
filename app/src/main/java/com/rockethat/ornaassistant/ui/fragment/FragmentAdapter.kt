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

    val frags = mutableListOf<Fragment?>()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        // Check if the fragment already exists in the list
        if (position < frags.size && frags[position] != null) {
            return frags[position]!!
        }

        // Create a new fragment for the given position
        val fragment = when (position) {
            0 -> MainFragment() // Replace with your actual MainFragment
            1 -> OrnaHubFragment() // Replace with your actual OrnaHubFragment
            2 -> KingdomFragment() // Replace with your actual KingdomFragment
            else -> MainFragment() // Default case
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
