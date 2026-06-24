package com.zinhao.kikoeru.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.zinhao.kikoeru.BaseActivity
import com.zinhao.kikoeru.R
import com.zinhao.kikoeru.databinding.ActivityWorkPageBinding
import com.zinhao.kikoeru.ui.fragment.ListenedFragment
import com.zinhao.kikoeru.ui.fragment.ListeningFragment
import com.zinhao.kikoeru.ui.fragment.MarkedFragment
import com.zinhao.kikoeru.ui.fragment.MoreFragment
import com.zinhao.kikoeru.ui.fragment.PostponedFragment
import com.zinhao.kikoeru.ui.fragment.ReplayFragment
import com.zinhao.kikoeru.ui.fragment.WorkFragment
import com.zinhao.kikoeru.viewmodel.WorksViewModel

class WorkPageActivity : BaseActivity() {
    private lateinit var binding: ActivityWorkPageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSafeArea(binding.appBarLayout)

        val tabTitles = arrayListOf<String>(getString(R.string.all_works),getString(R.string.listening),
            getString(R.string.listened),getString(R.string.marked),getString(R.string.replay),getString(R.string.postponed),getString(R.string.more))
        binding.vp.adapter = WorkPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.vp) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.add(0,0,0,"More")
        menuItem?.setIcon(R.drawable.ic_baseline_more_horiz_24)
        menuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    class WorkPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = TOTAL_PAGE_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WorkFragment()
                1 -> ListeningFragment()
                2 -> ListenedFragment()
                3 -> MarkedFragment()
                4 -> ReplayFragment()
                5 -> PostponedFragment()
                6 -> MoreFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }

        companion object{
            const val TOTAL_PAGE_COUNT = 7
        }
    }
}