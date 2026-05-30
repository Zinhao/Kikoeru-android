package com.zinhao.kikoeru.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.zinhao.kikoeru.model.Work

class WorksViewModel {
    private val _worksList = MutableLiveData<List<Work>>()
    private val worksList = _worksList
}