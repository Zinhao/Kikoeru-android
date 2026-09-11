package com.zinhao.kikoeru.utils

interface ProgressListener {
    fun onProgress(bytesRead: Long, contentLength: Long, percent: Int)
}