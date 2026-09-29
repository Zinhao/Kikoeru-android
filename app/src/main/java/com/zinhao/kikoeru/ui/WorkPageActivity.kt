package com.zinhao.kikoeru.ui

import android.content.Intent
import android.graphics.Outline
import android.os.Bundle
import android.util.Log.v
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.zinhao.kikoeru.BaseActivity
import com.zinhao.kikoeru.R
import com.zinhao.kikoeru.SearchActivity
import com.zinhao.kikoeru.UserSwitchActivity
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
    private val firstPagerAdapter: FirstPagerAdapter by lazy { FirstPagerAdapter(this) }
    private val workPagerAdapter: WorkPagerAdapter by lazy { WorkPagerAdapter(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSafeArea(binding.appBarLayout,object :InsetReady{
            override fun onInsetReady(insets: Insets) {
                binding.linearLayout.setPadding(0,0,0,insets.bottom)
            }
        })

        setupListener()

        val tabTitles = arrayListOf<String>(getString(R.string.all_works),getString(R.string.more))
        binding.vp.adapter = firstPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.vp) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

    }

    fun setupListener(){
        binding.btAccount.clipToOutline = true
        binding.btAccount.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                outline?.setRoundRect(0,0,view?.width?:0,view?.height?:0,view?.width?.toFloat()?:0f)
            }
        }
        binding.btAccount.setOnClickListener {
            startActivity(Intent(it.context, UserSwitchActivity::class.java))
        }
        binding.searchBar.setOnClickListener {
            startActivity(Intent(it.context, SearchActivity::class.java))
        }
        binding.bt1.setOnClickListener {
            val tabTitles = arrayListOf<String>(getString(R.string.all_works),getString(R.string.more))
            binding.vp.adapter = firstPagerAdapter
            TabLayoutMediator(binding.tabLayout, binding.vp) { tab, position ->
                tab.text = tabTitles[position]
            }.attach()
        }

        binding.bt2.setOnClickListener {
            val tabTitles = arrayListOf<String>(getString(R.string.listening),
                getString(R.string.listened),getString(R.string.marked),getString(R.string.replay),getString(R.string.postponed))
            binding.vp.adapter = workPagerAdapter
            TabLayoutMediator(binding.tabLayout, binding.vp) { tab, position ->
                tab.text = tabTitles[position]
            }.attach()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.add(0,0,0,"More")
        menuItem?.setIcon(R.drawable.ic_baseline_more_horiz_24)
        menuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    class FirstPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = TOTAL_PAGE_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WorkFragment()
                1 -> MoreFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }

        companion object{
            const val TOTAL_PAGE_COUNT = 2
        }
    }


    class WorkPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = TOTAL_PAGE_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ListeningFragment()
                1 -> ListenedFragment()
                2 -> MarkedFragment()
                3 -> ReplayFragment()
                4 -> PostponedFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }

        companion object{
            const val TOTAL_PAGE_COUNT = 5
        }
    }
}