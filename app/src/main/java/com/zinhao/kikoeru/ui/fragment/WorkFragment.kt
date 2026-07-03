package com.zinhao.kikoeru.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zinhao.kikoeru.databinding.LayoutWorkFragmentBinding
import com.zinhao.kikoeru.ui.adapter.WorksAdapter
import com.zinhao.kikoeru.viewmodel.WorksViewModel
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

    open fun setupObserve(){
        viewModel.allWorksList.observe(viewLifecycleOwner, { works ->
            works?.let {
                if(worksAdapter == null){
                    worksAdapter = WorksAdapter()
                    worksAdapter?.submitList(works)
                    binding.recyclerView.adapter = worksAdapter
                    binding.recyclerView.layoutManager = layoutManager
                }else{
                    worksAdapter?.submitList(works)
                }
            }
            binding.swipe.isRefreshing = false
        })
        if(viewModel.allWorksList.value.isNullOrEmpty()){
            loadMore()
        }
    }

    open fun loadMore(){
        val loading = viewModel.loadAllWorks()
        binding.swipe.isRefreshing = loading
    }

}

class ListeningFragment : WorkFragment() {
    override fun setupObserve() {
        viewModel.listeningWorksList.observe(viewLifecycleOwner, { works ->
            works?.let {
                if(worksAdapter == null){
                    worksAdapter = WorksAdapter()
                    worksAdapter?.submitList(works)
                    binding.recyclerView.adapter = worksAdapter
                    binding.recyclerView.layoutManager = layoutManager
                }else{
                    worksAdapter?.submitList(works)
                }
            }
            binding.swipe.isRefreshing = false
        })
        if(viewModel.listeningWorksList.value.isNullOrEmpty()){
            loadMore()
        }
    }

    override fun loadMore() {
        val loading = viewModel.loadListeningWorks()
        binding.swipe.isRefreshing = loading
    }
}

class ListenedFragment : WorkFragment() {
    override fun setupObserve() {
        viewModel.listenedWorksList.observe(viewLifecycleOwner, { works ->
            works?.let {
                if(worksAdapter == null){
                    worksAdapter = WorksAdapter()
                    binding.recyclerView.adapter = worksAdapter
                    binding.recyclerView.layoutManager = layoutManager
                }else{
                    worksAdapter?.submitList(works)
                }
            }
        })
        if(viewModel.listenedWorksList.value.isNullOrEmpty()){
            loadMore()
        }
    }

    override fun loadMore() {
        val loading = viewModel.loadListenedWorks()
        binding.swipe.isRefreshing = loading
    }
}

class MarkedFragment : WorkFragment() {
    override fun setupObserve() {
        viewModel.markedWorksList.observe(viewLifecycleOwner, { works ->
            works?.let {
                if(worksAdapter == null){
                    worksAdapter = WorksAdapter()
                    binding.recyclerView.adapter = worksAdapter
                    binding.recyclerView.layoutManager = layoutManager
                }else{
                    worksAdapter?.submitList(works)
                }
            }
        })
        if(viewModel.markedWorksList.value.isNullOrEmpty()){
            loadMore()
        }
    }

    override fun loadMore() {
        val loading = viewModel.loadMarkedWorks()
        binding.swipe.isRefreshing = loading
    }
}

class ReplayFragment : WorkFragment() {
    override fun setupObserve() {
        viewModel.replayWorksList.observe(viewLifecycleOwner, { works ->
            works?.let {
                if(worksAdapter == null){
                    worksAdapter = WorksAdapter()
                    worksAdapter?.submitList(works)
                    binding.recyclerView.adapter = worksAdapter
                    binding.recyclerView.layoutManager = layoutManager
                }else{
                    worksAdapter?.submitList(works)
                }
            }
        })
        if(viewModel.replayWorksList.value.isNullOrEmpty()){
            loadMore()
        }

    }

    override fun loadMore() {
        val loading = viewModel.loadReplayWorks()
        binding.swipe.isRefreshing = loading
    }
}

class PostponedFragment : WorkFragment() {

    override fun setupObserve() {
        viewModel.postponedWorksList.observe(viewLifecycleOwner, { works ->
            works?.let {
                if(worksAdapter == null){
                    worksAdapter = WorksAdapter()
                    worksAdapter?.submitList(works)
                    binding.recyclerView.adapter = worksAdapter
                    binding.recyclerView.layoutManager = layoutManager
                }else{
                    worksAdapter?.submitList(works)
                }
            }
        })
        if(viewModel.postponedWorksList.value.isNullOrEmpty()){
            loadMore()
        }

    }

    override fun loadMore() {
        val loading = viewModel.loadPostponedWorks()
        binding.swipe.isRefreshing = loading
    }
}

