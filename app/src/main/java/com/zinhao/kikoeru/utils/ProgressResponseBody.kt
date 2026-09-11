package com.zinhao.kikoeru.utils

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.buffer

class ProgressResponseBody(
    private val url: String,
    private val delegate: ResponseBody
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = delegate.source().peekAndCount()
        }
        return bufferedSource!!
    }

    private fun Source.peekAndCount(): BufferedSource {
        // 用 ForwardingSource 统计读取字节
        val forwarding = object : okio.ForwardingSource(this) {
            private var totalRead = 0L
            override fun read(sink: Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                if (read != -1L) totalRead += read
                val length = contentLength()
                ProgressManager.update(url, totalRead, length)
                return read
            }
        }
        return forwarding.buffer()
    }
}