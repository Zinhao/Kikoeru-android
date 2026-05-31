package com.zinhao.kikoeru.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zinhao.kikoeru.model.Work

class WorksViewModel {
    private val _allWorksList = MutableLiveData<List<Work>>()
    val worksList = _allWorksList

    private val _listeningWorksList = MutableLiveData<List<Work>>()
    val listeningWorksList: LiveData<List<Work>> = _listeningWorksList

    private val _listenedWorksList = MutableLiveData<List<Work>>()
    val listenedWorksList: LiveData<List<Work>> = _listenedWorksList

    private val _markedWorksList = MutableLiveData<List<Work>>()
    val markedWorksList: LiveData<List<Work>> = _markedWorksList

    private val _replayWorksList = MutableLiveData<List<Work>>()
    val replayWorksList: LiveData<List<Work>> = _replayWorksList

    private val _postponedWorksList = MutableLiveData<List<Work>>()
    val postponedWorksList: LiveData<List<Work>> = _postponedWorksList
}