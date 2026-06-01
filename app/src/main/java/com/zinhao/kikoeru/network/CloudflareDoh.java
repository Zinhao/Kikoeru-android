package com.zinhao.kikoeru.network;

import android.util.Log;
import com.google.gson.Gson;
import com.zinhao.kikoeru.App;
import okhttp3.*;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class CloudflareDoh implements Dns {
    private static final String TAG = "CloudflareDoh";
    private final OkHttpClient bootstrapClient;
    private final Gson gson = new Gson();
    private final List<InetAddress> dnsAddressResult = new ArrayList<>();

    public CloudflareDoh() {
        // 用于请求 Cloudflare DoH 服务的独立 Client
        this.bootstrapClient = new OkHttpClient.Builder().build();
    }

    @Override
    public @NonNull List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        // 【核心修改】如果是 Cloudflare 自身的 IP（1.0.0.1 或 1.1.1.1），直接返回，防止死循环
        if ("1.0.0.1".equals(hostname) || "1.1.1.1".equals(hostname)) {
            return Dns.SYSTEM.lookup(hostname);
        }

        if(!dnsAddressResult.isEmpty()){
            return dnsAddressResult;
        }
        Log.d(TAG, "lookup: dns============================================");
        try {
            // 【核心修改】将 Host 直接指定为 1.0.0.1，规避传统 DNS 嗅探
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("https")
                    .host("1.0.0.1")
                    .encodedPath("/dns-query")
                    .addQueryParameter("name", hostname)
                    .addQueryParameter("type", "A") // 请求 A 记录 (IPv4)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("accept", "application/dns-json") // Cloudflare 的标准的 JSON 响应格式
                    .build();

            try (Response response = bootstrapClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new UnknownHostException("Cloudflare DoH (1.0.0.1) request failed");
                }

                String json = response.body().string();
                CloudflareDnsResponse dnsResponse = gson.fromJson(json, CloudflareDnsResponse.class);

                if (dnsResponse != null && dnsResponse.Answer != null) {
                    List<InetAddress> addresses = new ArrayList<>();
                    for (CloudflareDnsResponse.Answer answer : dnsResponse.Answer) {
                        // 过滤出标准的 IPv4 地址记录 (Type 1)
                        if (answer.type == 1) {
                            addresses.add(InetAddress.getByName(answer.data));
                        }
                    }
                    if (!addresses.isEmpty()) {
                        dnsAddressResult.addAll(addresses);
                        return addresses;
                    }
                }
            }
        } catch (IOException e) {
            // 可以在这里打 Log 记录 DoH 失败原因
            e.printStackTrace(System.err);
            App.getInstance().alertException(e);
        }

        // 如果 Cloudflare DoH 解析失败，降级使用系统原生 DNS，保证业务不中断
        dnsAddressResult.add(InetAddress.getByName("104.21.50.254"));
//        dnsAddressResult.add(InetAddress.getByName("172.67.215.121"));
//        dnsAddressResult.add(InetAddress.getByName("2606:4700:3037:0:0:0:ac43:d779"));
        // 如果 Cloudflare DoH 解析失败，降级使用系统原生 DNS
        dnsAddressResult.addAll(Dns.SYSTEM.lookup(hostname));
        return dnsAddressResult;
    }

    // --- Cloudflare JSON 响应对应的实体类 ---
    private static class CloudflareDnsResponse {
        int Status;
        List<Answer> Answer;

        static class Answer {
            int type;
            String data;
        }
    }
}
