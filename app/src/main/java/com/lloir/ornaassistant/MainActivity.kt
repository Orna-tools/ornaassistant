package com.lloir.ornaassistant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.viewpager2.widget.ViewPager2
import com.lloir.ornaassistant.ui.fragment.FragmentAdapter
import com.lloir.ornaassistant.ui.fragment.MainFragment

class MainActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        setupComposeView()
    }

    private fun initializeViews() {
        pager = findViewById(R.id.pager)
        adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        pager.adapter = adapter
    }

    private fun setupComposeView() {
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent { AppDrawer(this@MainActivity) }
    }

    private fun updateMainFragment() {
        if (pager.currentItem == 0 && adapter.fragments.size >= 1) {
            val fragment = adapter.fragments[0] as MainFragment
            fragment.view?.let { view ->
                // Update the 7-day chart
                fragment.drawChart(view, R.id.cWeeklyDungeons, 7)
                // Update the 14-day chart
                fragment.drawChart(view, R.id.cCustomDungeons, 14)
                // Add more drawChart calls if you have more charts or custom days
            }
        }
    }
}
