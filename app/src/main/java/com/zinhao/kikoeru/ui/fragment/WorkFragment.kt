package com.zinhao.kikoeru.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zinhao.kikoeru.R
import com.zinhao.kikoeru.WorkTreeActivity
import com.zinhao.kikoeru.databinding.LayoutWorkFragmentBinding
import com.zinhao.kikoeru.model.Work
import com.zinhao.kikoeru.model.gson
import com.zinhao.kikoeru.ui.adapter.WorksAdapter
import com.zinhao.kikoeru.viewmodel.WorksViewModel
import org.json.JSONObject
import kotlin.math.max

open class WorkFragment : Fragment(){
    lateinit var binding: LayoutWorkFragmentBinding
    var worksAdapter: WorksAdapter? = null
    lateinit var layoutManager: RecyclerView.LayoutManager
    val viewModel : WorksViewModel by lazy {
        ViewModelProvider(requireActivity())[WorksViewModel::class.java]
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LayoutWorkFragmentBinding.inflate(inflater, container, false)
        binding.swipe.isRefreshing = false
        return binding.root
    }

    var scrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            onScrollBottom(newState)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val col = max(resources.displayMetrics.widthPixels / 395, 2)
        layoutManager = StaggeredGridLayoutManager(col, StaggeredGridLayoutManager.VERTICAL)
        setupObserve()
        binding.recyclerView.addOnScrollListener(scrollListener)
    }

    open fun onScrollBottom(newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (!binding.recyclerView.canScrollVertically(1)) {
                if(!binding.swipe.isRefreshing){
                    loadMore()
                }
            }
        }
    }

    fun bindItemAction(){
        worksAdapter?.setItemClickListener {
            val tag = it.tag as Work
            val intent = Intent(it.context, WorkTreeActivity::class.java).apply {
                putExtra("work_json_str", gson.toJson(tag))
            }
            val heroView = it.findViewById<View>(R.id.ivCover)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(), heroView, "hero_image"
            )
            startActivity(intent, options.toBundle())
        }
        worksAdapter?.setTagClickListener {

        }
    }

    open fun setupObserve(){
       liveData().observe(viewLifecycleOwner, { works ->
            works?.let {
                if(worksAdapter == null){
                    worksAdapter = WorksAdapter()
                    bindItemAction()
                    worksAdapter?.submitList(works)
                    binding.recyclerView.adapter = worksAdapter
                    binding.recyclerView.layoutManager = layoutManager
                }else{
                    worksAdapter?.submitList(works)
                }
            }
            binding.swipe.isRefreshing = false
        })
        if(liveData().value.isNullOrEmpty()){
            loadMore()
        }
    }

    open fun loadMore(){
        val loading = viewModel.loadAllWorks()
        binding.swipe.isRefreshing = loading
    }

    open fun liveData(): LiveData<List<Work>> {
        return viewModel.allWorksList
    }

    open fun getPageTitle(): CharSequence? {
        return requireContext().getString(R.string.all_works)
    }

}

class ListeningFragment : WorkFragment() {
    override fun liveData(): LiveData<List<Work>> {
        return viewModel.listeningWorksList
    }

    override fun loadMore() {
        val loading = viewModel.loadListeningWorks()
        binding.swipe.isRefreshing = loading
    }

    override fun getPageTitle(): CharSequence? {
        return requireContext().getString(R.string.listening)
    }
}

class ListenedFragment : WorkFragment() {
    override fun liveData(): LiveData<List<Work>> {
        return viewModel.listenedWorksList
    }

    override fun loadMore() {
        val loading = viewModel.loadListenedWorks()
        binding.swipe.isRefreshing = loading
    }

    override fun getPageTitle(): CharSequence? {
        return requireContext().getString(R.string.listened)
    }
}

class MarkedFragment : WorkFragment() {
    override fun liveData(): LiveData<List<Work>> {
        return viewModel.markedWorksList
    }

    override fun loadMore() {
        val loading = viewModel.loadMarkedWorks()
        binding.swipe.isRefreshing = loading
    }

    override fun getPageTitle(): CharSequence? {
        return requireContext().getString(R.string.marked)
    }
}

class ReplayFragment : WorkFragment() {
    override fun liveData(): LiveData<List<Work>> {
        return viewModel.replayWorksList
    }

    override fun loadMore() {
        val loading = viewModel.loadReplayWorks()
        binding.swipe.isRefreshing = loading
    }

    override fun getPageTitle(): CharSequence? {
        return requireContext().getString(R.string.replay)
    }
}

class PostponedFragment : WorkFragment() {

    override fun liveData(): LiveData<List<Work>> {
        return viewModel.postponedWorksList
    }

    override fun loadMore() {
        val loading = viewModel.loadPostponedWorks()
        binding.swipe.isRefreshing = loading
    }

    override fun getPageTitle(): CharSequence? {
        return requireContext().getString(R.string.postponed)
    }
}

