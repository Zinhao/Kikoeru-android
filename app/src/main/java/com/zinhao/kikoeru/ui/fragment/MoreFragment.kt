package com.zinhao.kikoeru.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.zinhao.kikoeru.App
import com.zinhao.kikoeru.LauncherActivity
import com.zinhao.kikoeru.databinding.ActivityMoreBinding
import com.zinhao.kikoeru.ui.adapter.WorksAdapter
import com.zinhao.kikoeru.viewmodel.WorksViewModel

class MoreFragment : Fragment() {
    lateinit var binding: ActivityMoreBinding
    //    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ActivityMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBarLayout.visibility = View.GONE
        binding.cbHomeTab.isChecked = App.getInstance().isUseNewLayout
        binding.rlHomeTab.setOnClickListener {
            binding.cbHomeTab.toggle()
        }
        binding.cbHomeTab.setOnCheckedChangeListener { buttonView, isChecked ->
            App.getInstance().isUseNewLayout = isChecked
            startActivity(Intent(requireContext(), LauncherActivity::class.java))
            requireActivity().finish()
        }
    }
}
