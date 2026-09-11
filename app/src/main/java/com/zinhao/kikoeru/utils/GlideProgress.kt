package com.zinhao.kikoeru.utils

import android.os.Looper


object ProgressManager {
    private val listeners = mutableMapOf<String, ProgressListener>()
    private val mainHandler = android.os.Handler(Looper.getMainLooper())

    fun expect(url: String, listener: ProgressListener) {
        listeners[url] = listener
    }

    fun forget(url: String) {
        listeners.remove(url)
    }

    fun update(url: String, bytesRead: Long, contentLength: Long) {
        val listener = listeners[url] ?: return
        val percent = if (contentLength > 0) {
            (bytesRead * 100 / contentLength).toInt()
        } else -1 // 未知总大小

        mainHandler.post {
            listener.onProgress(bytesRead, contentLength, percent)
        }
    }
}
