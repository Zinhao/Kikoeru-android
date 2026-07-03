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
import com.zinhao.kikoeru.model.Pagination
import com.zinhao.kikoeru.model.Work
import com.zinhao.kikoeru.model.WorksGetPage
import org.json.JSONException
import org.json.JSONObject

class WorksViewModel: ViewModel() {
    private val TAG = "WorksViewModel"

    private val _allWorksList = MutableLiveData<List<Work>>()
    val allWorksList: LiveData<List<Work>> = _allWorksList
    private var _allPagination: Pagination? = null

    private val _listeningWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val listeningWorksList: LiveData<List<Work>> = _listeningWorksList
    private var _listeningPagination: Pagination? = null

    private val _listenedWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val listenedWorksList: LiveData<List<Work>> = _listenedWorksList
    private var _listenedPagination: Pagination? = null


    private val _markedWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val markedWorksList: LiveData<List<Work>> = _markedWorksList
    private var _markedPagination: Pagination? = null

    private val _replayWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val replayWorksList: LiveData<List<Work>> = _replayWorksList
    private var _replayPagination: Pagination? = null

    private val _postponedWorksList = MutableLiveData<List<Work>>(arrayListOf())
    val postponedWorksList: LiveData<List<Work>> = _postponedWorksList
    private var _postponedPagination: Pagination? = null

    private val gson = Gson()

    fun loadAllWorks(): Boolean {
        var page = 1
        _allPagination?.let {
            if(it.currentPage >= it.pageSize){
                return false
            }else{
                page = it.currentPage+1
            }
        }
        Api.doGetWorks(page,object : AsyncHttpClient.JSONObjectCallback(){
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
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()?: arrayListOf()
                    mutableListOfWorks.addAll(worksGetPage.works)
                    mutableListOfWorks.let {
                        _allWorksList.postValue(it)
                    }
                    _allPagination = worksGetPage.pagination
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
        return true
    }

    fun loadListeningWorks() : Boolean{
        var page = 1
        _listeningPagination?.let {
            if(it.currentPage >= it.pageSize){
                return false
            }else{
                page = it.currentPage+1
            }
        }
        doGetReview(Api.FILTER_LISTENING,page,object : AsyncHttpClient.JSONObjectCallback(){
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
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()?: arrayListOf()
                    mutableListOfWorks.addAll(worksGetPage.works)
                   _listeningPagination = worksGetPage.pagination
                    mutableListOfWorks.let {
                        _listeningWorksList.postValue(it)
                    }
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
        return true
    }

    fun loadListenedWorks(): Boolean {
        var page = 1
        _listenedPagination?.let {
            if(it.currentPage >= it.pageSize){
                return false
            }else{
                page = it.currentPage+1
            }
        }
        doGetReview(Api.FILTER_LISTENED,page,object : AsyncHttpClient.JSONObjectCallback(){
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
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()?: arrayListOf()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    _listenedPagination = worksGetPage.pagination
                    mutableListOfWorks?.let {
                        _listenedWorksList.postValue(it)
                    }
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
        return true
    }

    fun loadMarkedWorks(): Boolean {
        var page = 1
        _markedPagination?.let {
            if(it.currentPage >= it.pageSize){
                return false
            }else{
                page = it.currentPage+1
            }
        }
        doGetReview(Api.FILTER_MARKED,page,object : AsyncHttpClient.JSONObjectCallback(){
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
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()?: arrayListOf()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    _markedPagination = worksGetPage.pagination
                    mutableListOfWorks?.let {
                        _markedWorksList.postValue(it)
                    }
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }

        })
        return true
    }

    fun loadReplayWorks() : Boolean{
        var page = 1
        _replayPagination?.let {
            if(it.currentPage >= it.pageSize){
                return false
            }else{
                page = it.currentPage+1
            }
        }
        doGetReview(Api.FILTER_REPLAY,page,object : AsyncHttpClient.JSONObjectCallback(){
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
                    val mutableListOfWorks = _allWorksList.value?.toMutableList()?: arrayListOf()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    mutableListOfWorks?.let {
                        _replayWorksList.postValue(it)
                    }
                    _replayPagination = worksGetPage.pagination
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }
        })
        return true
    }

    fun loadPostponedWorks(): Boolean {
        var page = 1
        _postponedPagination?.let {
            if(it.currentPage >= it.pageSize){
                return false
            }else{
                page = it.currentPage+1
            }
        }
        doGetReview(Api.FILTER_POSTPONED,page,object : AsyncHttpClient.JSONObjectCallback(){
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
                    val mutableListOfWorks = _postponedWorksList.value?.toMutableList()?: arrayListOf()
                    mutableListOfWorks?.addAll(worksGetPage.works)
                    mutableListOfWorks?.let {
                        _postponedWorksList.postValue(it)
                    }
                    _postponedPagination = worksGetPage.pagination
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace(System.err)
                }
            }
        })
        return true
    }
}