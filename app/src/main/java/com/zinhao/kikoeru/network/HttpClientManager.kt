package com.zinhao.kikoeru.network

import android.util.Log
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.*
import java.util.concurrent.TimeUnit


object HttpClientManager {
    val TAG = "HttpClientManager"
    val pacEnabledClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .proxySelector(object : ProxySelector() {
                override fun select(uri: URI): MutableList<Proxy?> {
                    // 1. 如果系统能够正确解析 PAC（部分高版本系统），直接使用系统的
                    val systemProxies = getDefault().select(uri)
                    if (systemProxies != null && !systemProxies.isEmpty() && systemProxies.get(0)!!
                            .type() != Proxy.Type.DIRECT
                    ) {
                        Log.i(TAG, "select:正确解析 PAC")
                        return systemProxies
                    }

                    // 2. 如果系统罢工了（返回了 DIRECT/NO_PROXY），且不是访问你的局域网/本地地址
                    val host = uri.getHost()
                    if (host != null && (host != "127.0.0.1") && !host.startsWith("192.168.")) {
                        // 强制让 OkHttp 的流量去连接你的电脑代理（这里动态或硬编码你的电脑代理 IP）
                        // 相当于在 App 内部实现了一套“手动代理”的兜底逻辑
                        Log.i(TAG, "select:手动代理")
                        return mutableListOf<Proxy?>(
                            Proxy(Proxy.Type.HTTP, InetSocketAddress("192.168.31.253", 7890))
                        )
                    }

                    // 3. 国内流量或局域网，直连
                    Log.i(TAG, "select:直连")
                    return mutableListOf<Proxy?>(Proxy.NO_PROXY)
                }

                override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                    // 代理连接失败时的回调
                    getDefault().connectFailed(uri, sa, ioe)
                }
            }).callTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(100, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor()).build()

}