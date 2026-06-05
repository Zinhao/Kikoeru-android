package com.zinhao.kikoeru.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zinhao.kikoeru.databinding.LayoutWorkFragmentBinding
import com.zinhao.kikoeru.model.Work
import com.zinhao.kikoeru.ui.adapter.WorksAdapter
import com.zinhao.kikoeru.viewmodel.WorksViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.count
import kotlin.math.max

open class WorkFragment : Fragment() {
    lateinit var binding: LayoutWorkFragmentBinding
//    var oldDataList = ArrayList<Work>()
    var worksAdapter: WorksAdapter? = null
//    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LayoutWorkFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewmodel = ViewModelProvider(requireActivity())[WorksViewModel::class.java]
        val col = max(getResources().getDisplayMetrics().widthPixels / 395, 3)
        val layoutManager = StaggeredGridLayoutManager(col, StaggeredGridLayoutManager.VERTICAL)
        load(viewmodel,layoutManager)
    }

    open fun load(viewmodel: WorksViewModel, layoutManager: RecyclerView.LayoutManager){
        viewmodel.worksList.observe(viewLifecycleOwner, { works ->
            works?.let {
                binding.text.setText(works.count().toString())
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
        if(viewmodel.worksList.value.isNullOrEmpty()){
            viewmodel.loadAllWorks()
        }
    }
}

class ListeningFragment : WorkFragment() {
    override fun load(viewmodel: WorksViewModel, layoutManager: RecyclerView.LayoutManager) {
        viewmodel.listeningWorksList.observe(viewLifecycleOwner, { works ->
            works?.let {
                binding.text.setText(works.count().toString())
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
                binding.text.setText(works.count().toString())
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
                binding.text.setText(works.count().toString())
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
                binding.text.setText(works.count().toString())
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
                binding.text.setText(works.count().toString())
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

