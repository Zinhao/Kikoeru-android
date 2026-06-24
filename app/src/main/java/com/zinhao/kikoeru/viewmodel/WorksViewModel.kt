package com.zinhao.kikoeru.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.koushikdutta.async.http.AsyncHttpClient
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api
import com.zinhao.kikoeru.Api.doGetReview
import com.zinhao.kikoeru.model.Work
import com.zinhao.kikoeru.model.WorksGetPage
import org.json.JSONException
import org.json.JSONObject

class WorksViewModel: ViewModel() {
    private val TAG = "WorksViewModel"
    private val _allWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val allWorksList = _allWorksList
    private val _allTotalCount = MutableLiveData<Int>(0)
    val allTotalCount: LiveData<Int> = _allTotalCount

    private val _listeningWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val listeningWorksList: LiveData<List<Work>> = _listeningWorksList
    private val _listeningTotalCount = MutableLiveData<Int>(0)
    val listeningTotalCount: LiveData<Int> = _listeningTotalCount

    private val _listenedWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val listenedWorksList: LiveData<List<Work>> = _listenedWorksList
    private val _listenedTotalCount = MutableLiveData<Int>(0)
    val listenedTotalCount: LiveData<Int> = _listenedTotalCount


    private val _markedWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val markedWorksList: LiveData<List<Work>> = _markedWorksList
    private val _markedTotalCount = MutableLiveData<Int>(0)
    val markedTotalCount: LiveData<Int> = _markedTotalCount

    private val _replayWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val replayWorksList: LiveData<List<Work>> = _replayWorksList
    private val _replayTotalCount = MutableLiveData<Int>(0)
    val replayTotalCount: LiveData<Int> = _replayTotalCount

    private val _postponedWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val postponedWorksList: LiveData<List<Work>> = _postponedWorksList
    private val _postponedTotalCount = MutableLiveData<Int>(0)
    val postponedTotalCount: LiveData<Int> = _postponedTotalCount

    private val gson = Gson()

    fun loadAllWorks() {
        Api.doGetWorks(1,object : AsyncHttpClient.JSONObjectCallback(){
            override fun onCompleted(
                e: Exception?,
                asyncHttpResponse: AsyncHttpResponse?,
                jsonObject: JSONObject?
            ) {
                if (e != null) {
                    e.printStackTrace(System.err)
                    return
                }
                if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                    if (jsonObject != null && jsonObject.has("works")) {
                        Log.d(TAG, "onCompleted: load local cache!")
                    } else {
                        return
                    }
                }
                try {
                    val worksGetPage: WorksGetPage = gson.fromJson(jsonObject.toString(), WorksGetPage::class.java)
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    _allTotalCount.value = worksGetPage.pagination.totalCount
                    _allWorksList.postValue(mutableListOfWorks)
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
    }

    fun loadListeningWorks() {
        doGetReview(Api.FILTER_LISTENING,1,object : AsyncHttpClient.JSONObjectCallback(){
            override fun onCompleted(
                e: Exception?,
                asyncHttpResponse: AsyncHttpResponse?,
                jsonObject: JSONObject?
            ) {
                if (e != null) {
                    e.printStackTrace(System.err)
                    return
                }
                if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                    if (jsonObject != null && jsonObject.has("works")) {
                        Log.d(TAG, "onCompleted: load local cache!")
                    } else {
                        return
                    }
                }
                try {
                    val worksGetPage: WorksGetPage = gson.fromJson(jsonObject.toString(), WorksGetPage::class.java)
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    _listeningTotalCount.value = worksGetPage.pagination.totalCount
                    _listeningWorksList.postValue(mutableListOfWorks)
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
    }

    fun loadListenedWorks() {
        doGetReview(Api.FILTER_LISTENED,1,object : AsyncHttpClient.JSONObjectCallback(){
            override fun onCompleted(
                e: Exception?,
                asyncHttpResponse: AsyncHttpResponse?,
                jsonObject: JSONObject?
            ) {
                if (e != null) {
                    e.printStackTrace(System.err)
                    return
                }
                if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                    if (jsonObject != null && jsonObject.has("works")) {
                        Log.d(TAG, "onCompleted: load local cache!")
                    } else {
                        return
                    }
                }
                try {
                    val worksGetPage: WorksGetPage = gson.fromJson(jsonObject.toString(), WorksGetPage::class.java)
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    _listenedTotalCount.value = worksGetPage.pagination.totalCount
                    _listenedWorksList.postValue(mutableListOfWorks)
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
    }

    fun loadMarkedWorks() {
        doGetReview(Api.FILTER_MARKED,1,object : AsyncHttpClient.JSONObjectCallback(){
            override fun onCompleted(
                e: Exception?,
                asyncHttpResponse: AsyncHttpResponse?,
                jsonObject: JSONObject?
            ) {
                if (e != null) {
                    e.printStackTrace(System.err)
                    return
                }
                if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                    if (jsonObject != null && jsonObject.has("works")) {
                        Log.d(TAG, "onCompleted: load local cache!")
                    } else {
                        return
                    }
                }
                try {
                    val worksGetPage: WorksGetPage = gson.fromJson(jsonObject.toString(), WorksGetPage::class.java)
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    _markedTotalCount.value = worksGetPage.pagination.totalCount
                    _markedWorksList.postValue(mutableListOfWorks)
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
    }

    fun loadReplayWorks() {
        doGetReview(Api.FILTER_REPLAY,1,object : AsyncHttpClient.JSONObjectCallback(){
            override fun onCompleted(
                e: Exception?,
                asyncHttpResponse: AsyncHttpResponse?,
                jsonObject: JSONObject?
            ) {
                if (e != null) {
                    e.printStackTrace(System.err)
                    return
                }
                if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                    if (jsonObject != null && jsonObject.has("works")) {
                        Log.d(TAG, "onCompleted: load local cache!")
                    } else {
                        return
                    }
                }
                try {
                    val worksGetPage: WorksGetPage = gson.fromJson(jsonObject.toString(), WorksGetPage::class.java)
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    _replayWorksList.postValue(mutableListOfWorks)
                    _replayTotalCount.value = worksGetPage.pagination.totalCount
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
    }

    fun loadPostponedWorks() {
        doGetReview(Api.FILTER_POSTPONED,1,object : AsyncHttpClient.JSONObjectCallback(){
            override fun onCompleted(
                e: Exception?,
                asyncHttpResponse: AsyncHttpResponse?,
                jsonObject: JSONObject?
            ) {
                if (e != null) {
                    e.printStackTrace(System.err)
                    return
                }
                if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                    if (jsonObject != null && jsonObject.has("works")) {
                        Log.d(TAG, "onCompleted: load local cache!")
                    } else {
                        return
                    }
                }
                try {
                    val worksGetPage: WorksGetPage = gson.fromJson(jsonObject.toString(), WorksGetPage::class.java)
                    _postponedWorksList.postValue(worksGetPage.works)
                    _postponedTotalCount.value = worksGetPage.pagination.totalCount
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }
        })

    }
}