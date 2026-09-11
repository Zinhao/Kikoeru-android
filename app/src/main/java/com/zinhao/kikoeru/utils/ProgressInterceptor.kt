package com.zinhao.kikoeru.utils

import android.util.Log
import com.zinhao.kikoeru.utils.ProgressGlideModule
import okhttp3.Interceptor
import okhttp3.Response

class ProgressInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!response.isSuccessful) {
            // 消费 body 内容后关闭，防止泄漏
            response.body?.close()
            return response
        }
        val body = response.body ?: return response
        val url = request.url.toString()
        Log.d(ProgressGlideModule.TAG, "intercept: $url")
        return response.newBuilder()
            .body(ProgressResponseBody(url, body))
            .build()
    }
}