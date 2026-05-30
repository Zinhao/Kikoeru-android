package com.zinhao.kikoeru

import android.net.Proxy
import android.net.Uri
import androidx.annotation.StringDef
import com.koushikdutta.async.http.AsyncHttpClient
import com.koushikdutta.async.http.AsyncHttpClient.JSONArrayCallback
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback
import com.koushikdutta.async.http.AsyncHttpRequest
import com.koushikdutta.async.http.body.JSONObjectBody
import com.zinhao.kikoeru.network.HttpClientManager
import com.zinhao.kikoeru.network.LoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

object Api {
    private var HOST = "http://localhost:8888"
    private const val  TAG = "Api"
    const val REMOTE_HOST: String = "https://api.asmr.one"
    const val LOCAL_HOST: String = "http://localhost:8888"
    @JvmField
    var authorization: String = ""
    @JvmField
    var token: String = ""
    private var subtitle = 1
    private var sort = 1
    private var order = "id"

    private val okHttpClient: OkHttpClient = HttpClientManager.pacEnabledClient

    @JvmStatic
    fun init(tokenStr: String, host: String) {
        token = tokenStr
        authorization = String.format("Bearer %s", tokenStr)
        if (host.startsWith("http")) {
            HOST = host
        } else {
            HOST = String.format(Locale.US, "http://%s", host)
        }
        subtitle = App.getInstance().getValue(App.CONFIG_ONLY_DISPLAY_LRC, 1).toInt()
        order = App.getInstance().getValue(App.CONFIG_ORDER, "id")
        sort = App.getInstance().getValue(App.CONFIG_SORT, 0).toInt()
    }

    private fun makeSort(): String {
        if (sort != 1) {
            return "desc"
        } else {
            return "asc"
        }
    }

    @JvmStatic
    fun setOrder(order: String) {
        if (Api.order == order) {
            if (sort == 1) {
                sort = 0
            } else {
                sort = 1
            }
            App.getInstance().setValue(App.CONFIG_SORT, sort.toLong())
            return
        }
        Api.order = order
        App.getInstance().setValue(App.CONFIG_ORDER, order)
    }

    @JvmStatic
    fun setSubtitle(subtitle: Int) {
        Api.subtitle = subtitle
    }

    private fun okhttpGetJsonObject(url: String, callback: JSONObjectCallback) {
        val request = Request.Builder().get().url(url).addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "Android Application <Kikoeru>")
            .addHeader("authorization", authorization)
            .build()
        okHttpClient.newCall(request).enqueue(object :Callback{
            override fun onFailure(call: Call, e: IOException) {
                callback.onCompleted(e, LocalResponse(404), null)
            }

            override fun onResponse(call: Call, response: Response) {
                if(response.isSuccessful){
                    val body = response.body?.string()
                    callback.onCompleted(null, LocalResponse(response.code), JSONObject(body?:""))
                }else{
                    callback.onCompleted(null, LocalResponse(response.code), JSONObject("{}"))
                }
            }
        })
    }

    private fun okhttpPutJsonObject(url: String, data: JSONObject, callback: JSONObjectCallback) {
        val request = Request.Builder().put(data.toString().toRequestBody("application/json;charset=utf-8".toMediaType()))
            .url(url).addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "Android Application <Kikoeru>")
            .addHeader("authorization", authorization)
            .build()
        okHttpClient.newCall(request).enqueue(object :Callback{
            override fun onFailure(call: Call, e: IOException) {
                callback.onCompleted(e, LocalResponse(404), null)
            }

            override fun onResponse(call: Call, response: Response) {
                if(response.isSuccessful){
                    val body = response.body?.string()
                    callback.onCompleted(null, LocalResponse(response.code), JSONObject(body?:""))
                } else{
                    callback.onCompleted(null, LocalResponse(response.code), JSONObject("{}"))
                }
            }
        })
    }

    private fun okhttpGetJsonArray(url: String, callback: JSONArrayCallback) {
        val request = Request.Builder().get().url(url).addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "Android Application <Kikoeru>")
            .addHeader("authorization", authorization)
            .build()
        okHttpClient.newCall(request).enqueue(object :Callback{
            override fun onFailure(call: Call, e: IOException) {
                callback.onCompleted(e, LocalResponse(404), null)
            }

            override fun onResponse(call: Call, response: Response) {
                if(response.isSuccessful){
                    val body = response.body?.string()
                    callback.onCompleted(null, LocalResponse(response.code), JSONArray(body))
                }else{
                    callback.onCompleted(null, LocalResponse(response.code), JSONArray("[]"))
                }
            }
        })
    }

    private fun okhttpGetString(url: String, callback: AsyncHttpClient.StringCallback) {
        val request = Request.Builder().get().url(url).addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "Android Application <Kikoeru>")
            .addHeader("authorization", authorization)
            .build()
        okHttpClient.newCall(request).enqueue(object :Callback{
            override fun onFailure(call: Call, e: IOException) {
                callback.onCompleted(e, LocalResponse(404), null)
            }

            override fun onResponse(call: Call, response: Response) {
                if(response.isSuccessful){
                    callback.onCompleted(null, LocalResponse(response.code), response.body?.string())
                }else{
                    callback.onCompleted(null, LocalResponse(response.code), "")
                }
            }
        })
    }
    @JvmStatic
    fun doGetWorks(page: Int, callback: JSONObjectCallback?) {
        val url = "${HOST}/api/works?order=${order}&sort${makeSort()}&page=${page}&seed=35&subtitle=${subtitle}"
        callback?.let {
            okhttpGetJsonObject(url,it)
        }
    }
    @JvmStatic
    fun doGetWorksByTag(page: Int, tagId: Int, callback: JSONObjectCallback?) {
        val url = "${HOST}/api/tags/${tagId}/works?order=${order}&sort=${makeSort()}&page=${page}&seed=21&subtitle=${subtitle}"
        callback?.let {
            okhttpGetJsonObject(url,it)
        }
    }

    @JvmStatic
    fun doGetWorkByVa(page: Int, vaId: String, callback: JSONObjectCallback?) {
//        http://localhost:8888/api/vas/2b5e7ab5-d994-5491-a53c-f1b6ae562d0e/works?order=price&sort=desc&page=1&seed=68
        val url = HOST +  "/api/vas/${vaId}/works?order=${order}&sort=${makeSort()}&page=${page}&seed=21&subtitle=${subtitle}"
        callback?.let {
            okhttpGetJsonObject(url,it)
        }
    }

    @JvmStatic
    fun doGetWorkByCircles(page: Int, circlesId: Long, callback: JSONObjectCallback?) {
        //    http://localhost:8980/api/circles/54978/works?order=release&sort=desc&page=1&seed=59
        val url = "${HOST}/api/circles/${circlesId}/works?order=${order}&sort=${makeSort()}&page=${page}&seed=21&subtitle=${subtitle}"
        callback?.let {
            okhttpGetJsonObject(url,it)
        }
    }

    @JvmStatic
    fun doGetAllTags(callback: JSONArrayCallback?) {
        val url = "$HOST/api/tags/"
        callback?.let {
            okhttpGetJsonArray(url,it)
        }
    }

    @JvmStatic
    fun doGetAllVas(callback: JSONArrayCallback?) {
        val url = "$HOST/api/vas/"
        callback?.let {
            okhttpGetJsonArray(url,it)
        }
    }

    @JvmStatic
    fun doGetDocTree(id: Int, callback: JSONArrayCallback?) {
        val url = "$HOST/api/tracks/${id}"
        callback?.let {
            okhttpGetJsonArray(url,it)
        }
    }

    @JvmStatic
    fun doGetWork(keyword: String, page: Int, callback: JSONObjectCallback?) {
//        http://localhost:8888/api/search/RJ381400?order=release&sort=desc&page=1&seed=18
        val url = "${HOST}/api/search/${keyword}?order=${order}&sort=${makeSort()}&page=${page}&seed=18&subtitle=0"
        callback?.let {
            okhttpGetJsonObject(url,it)
        }
    }
    @JvmStatic
    fun checkLrc(hash: String, callback: JSONObjectCallback?) {
        val url = "${HOST}/api/media/check-lrc/${hash}?token=${token}"
        callback?.let {
            okhttpGetJsonObject(url,it)
        }
    }

    @JvmStatic
    fun doGetMediaString(hash: String, callback: AsyncHttpClient.StringCallback?) {
        val url = "${HOST}/api/media/stream/${hash}?token=$token"
        callback?.let {
            okhttpGetString(url,it)
        }
    }

    @JvmStatic
    fun doGetToken(userName: String?, password: String?, host: String?, callback: JSONObjectCallback?) {
        val pwd = JSONObject()
        try {
            pwd.put("name", userName)
            pwd.put("password", password)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val requestBody: RequestBody = pwd.toString().toRequestBody("application/json;charset=utf-8".toMediaType())
        val request = Request.Builder().post(requestBody)
            .url("$host/api/auth/me")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "Android Application <Kikoeru>")
            .build()
        okHttpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback?.onCompleted(e, LocalResponse(404), null)
            }

            override fun onResponse(call: Call, response: Response) {
                if(response.isSuccessful){
                    response.body?.string()?.let {
                        callback?.onCompleted(null, LocalResponse(response.code), JSONObject(it))
                    }
                }else{
                    callback?.onCompleted(null, LocalResponse(response.code), null)
                }
            }

        })
    }

    const val FILTER_MARKED: String = "marked"
    const val FILTER_LISTENING: String = "listening"
    const val FILTER_LISTENED: String = "listened"
    const val FILTER_REPLAY: String = "replay"
    const val FILTER_POSTPONED: String = "postponed"

    /**
     * GET
     * [...](https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=marked)      我的进度 - 想听
     * [...](https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=listening)   我的进度 - 在听
     * [...](https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=listened)    我的进度 - 听过
     * [...](https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=replay)      我的进度 - 重听
     * [...](https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=postponed)   我的进度 - 搁置
     * [...](https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1)                    我的评价
     */
    @JvmStatic
    fun doGetReview(@Filter filter: String?, page: Int, callback: JSONObjectCallback?) {
        val url = "$HOST/api/review?order=updated_at&sort=desc&page=${page}&filter=${filter}"
        callback?.let {
            okhttpGetJsonObject(url,it)
        }
    }

    /***
     * http://localhost:8980/api/circles/
     * @param callback
     */
    @JvmStatic
    fun doGetCirclesList(callback: JSONArrayCallback?) {
        val url = "${HOST}/api/circles/"
        callback?.let {
            okhttpGetJsonArray(url,it)
        }
    }

    /**
     * PUT
     * 标记在听 [...](https://api.asmr.one/api/review?starOnly=false&progressOnly=true)
     * data:   {"user_name":"guest","work_id":380205,"progress":"listening"}
     * result: 200: {message: "更新进度成功"}
     */
    @JvmStatic
    fun doPutReview(id: Long, @Filter progress: String?, callback: JSONObjectCallback?) {
        val url = "${HOST}/api/review?starOnly=false&progressOnly=true"
        val jsonObject = JSONObject()
        val userName = App.getInstance().currentUser().getName()
        try {
            jsonObject.put("user_name", userName)
            jsonObject.put("work_id", id)
            jsonObject.put("progress", progress)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
       callback?.let {
           okhttpPutJsonObject(url,jsonObject,it)
       }
    }

    @JvmStatic
    fun formatGetUrl(path: String, useToken: Boolean): String {
        if (path.startsWith("http")) {
            if (useToken) {
                return String.format("%s?token=%s", path, token)
            } else {
                return path
            }
        } else {
            if (useToken) {
                return String.format("%s%s?token=%s", HOST, path, token)
            } else {
                return String.format("%s%s", HOST, path)
            }
        }
    }

    @JvmStatic
    fun minCoverImageUrl(rjNumber: Long): String {
        //App.getInstance().currentUser().getHost() + String.format(Locale.US, "/api/cover/%d?type=sam", rjNumber
        return (App.getInstance().currentUser().getHost()
                + String.format("/api/cover/%d?type=sam&token=%s", rjNumber, token))
    }

    @JvmStatic
    fun fullCoverImageUrl(rjNumber: Long): String {
        return (App.getInstance().currentUser().getHost()
                + String.format("/api/cover/%d?token=%s", rjNumber, token))
    }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(value = [FILTER_MARKED, FILTER_LISTENING, FILTER_LISTENED, FILTER_REPLAY, FILTER_POSTPONED])
    annotation class Filter
}
