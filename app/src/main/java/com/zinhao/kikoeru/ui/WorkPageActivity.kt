package com.zinhao.kikoeru.ui

import android.content.Intent
import android.graphics.Outline
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.zinhao.kikoeru.*
import com.zinhao.kikoeru.databinding.ActivityWorkPageBinding
import com.zinhao.kikoeru.ui.fragment.*

class WorkPageActivity : BaseActivity() {
    private val favTitleList:List<String> by lazy {
        arrayListOf<String>(getString(R.string.listening),
            getString(R.string.listened),getString(R.string.marked),getString(R.string.replay),getString(R.string.postponed))
    }
    private val mainTitleList:List<String> by lazy {
        arrayListOf<String>(getString(R.string.all_works),getString(R.string.more))
    }
    private lateinit var binding: ActivityWorkPageBinding
    private val mainPagerAdapter: MainPagerAdapter by lazy { MainPagerAdapter(this) }
    private val favPagerAdapter: FavPagerAdapter by lazy { FavPagerAdapter(this) }
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
        binding.vp.adapter = mainPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.vp) { tab, position ->
            tab.text = mainTitleList[position]
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

            binding.vp.adapter = mainPagerAdapter
            TabLayoutMediator(binding.tabLayout, binding.vp) { tab, position ->
                tab.text = mainTitleList[position]
            }.attach()
        }

        binding.bt2.setOnClickListener {

            binding.vp.adapter = favPagerAdapter
            TabLayoutMediator(binding.tabLayout, binding.vp) { tab, position ->
                tab.text = favTitleList[position]
            }.attach()
        }
        binding.bt3.setOnClickListener {
            startActivity(Intent(it.context, MoreActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.add(0,0,0,"More")
        menuItem?.setIcon(R.drawable.ic_baseline_more_horiz_24)
        menuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
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


    class FavPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
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