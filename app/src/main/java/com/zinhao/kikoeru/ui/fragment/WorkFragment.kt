package com.zinhao.kikoeru.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zinhao.kikoeru.WorksActivity.Companion.TAG
import com.zinhao.kikoeru.databinding.LayoutWorkFragmentBinding
import com.zinhao.kikoeru.ui.adapter.WorksAdapter
import com.zinhao.kikoeru.viewmodel.WorksViewModel
import kotlin.math.max

open class WorkFragment : Fragment(){
    lateinit var binding: LayoutWorkFragmentBinding
//    var oldDataList = ArrayList<Work>()
    var worksAdapter: WorksAdapter? = null
    val viewModel : WorksViewModel by lazy {
        ViewModelProvider(requireActivity())[WorksViewModel::class.java]
    }
//    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LayoutWorkFragmentBinding.inflate(inflater, container, false)
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
        val layoutManager = StaggeredGridLayoutManager(col, StaggeredGridLayoutManager.VERTICAL)
        load(viewModel,layoutManager)
        binding.recyclerView.addOnScrollListener(scrollListener)
    }

    open fun onScrollBottom(newState: Int) {
        viewModel.allWorksList.value?.size?.let {
            if (it >= viewModel.allTotalCount.value!!) {
                return
            }
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                worksAdapter?.let {
                    if(!it.isLoading()){
                        if (!binding.recyclerView.canScrollVertically(1)) {
//                            loadFromNetWork(type)
                        }
                    }
                }
            }
        }

    }

    open fun load(viewmodel: WorksViewModel, layoutManager: RecyclerView.LayoutManager){
        viewmodel.allWorksList.observe(viewLifecycleOwner, { works ->
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
        if(viewmodel.allWorksList.value.isNullOrEmpty()){
            viewmodel.loadAllWorks()
        }
    }
}

class ListeningFragment : WorkFragment() {
    override fun load(viewmodel: WorksViewModel, layoutManager: RecyclerView.LayoutManager) {
        viewmodel.listeningWorksList.observe(viewLifecycleOwner, { works ->
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
        if(viewmodel.listeningWorksList.value.isNullOrEmpty()){
            viewmodel.loadListeningWorks()
        }
    }
}

class ListenedFragment : WorkFragment() {
    override fun load(viewmodel: WorksViewModel, layoutManager: RecyclerView.LayoutManager) {
        viewmodel.listenedWorksList.observe(viewLifecycleOwner, { works ->
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
        if(viewmodel.listenedWorksList.value.isNullOrEmpty()){
            viewmodel.loadListenedWorks()
        }
    }
}

class MarkedFragment : WorkFragment() {
    override fun load(viewmodel: WorksViewModel, layoutManager: RecyclerView.LayoutManager) {
        viewmodel.markedWorksList.observe(viewLifecycleOwner, { works ->
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
        if(viewmodel.markedWorksList.value.isNullOrEmpty()){
            viewmodel.loadMarkedWorks()
        }
    }
}

class ReplayFragment : WorkFragment() {
    override fun load(viewmodel: WorksViewModel, layoutManager: RecyclerView.LayoutManager) {
        viewmodel.replayWorksList.observe(viewLifecycleOwner, { works ->
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
        if(viewmodel.replayWorksList.value.isNullOrEmpty()){
            viewmodel.loadReplayWorks()
        }

    }
}

class PostponedFragment : WorkFragment() {

    override fun load(viewmodel: WorksViewModel, layoutManager: RecyclerView.LayoutManager) {
        viewmodel.postponedWorksList.observe(viewLifecycleOwner, { works ->
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
        if(viewmodel.postponedWorksList.value.isNullOrEmpty()){
            viewmodel.loadPostponedWorks()
        }

    }
}

