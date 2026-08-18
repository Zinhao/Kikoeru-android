package com.zinhao.kikoeru.network;

import android.util.Log;

import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public class LoggingInterceptor implements Interceptor {
    private static final String TAG = "LoggingInterceptor";
    private static final int MAX_BODY = 80;
    private static final long MAX_TEXT_SIZE = 30 * 1024 * 1024; // 30MB
    @Override
    public @NonNull Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        Log.i(TAG, "➡️Request: "+request.method() +" ➡️ "+ request.url());

        Response response = chain.proceed(request);

        // ✅ 先从响应头获取信息，不读取响应体
        String contentTypeHeader = response.header("Content-Type");
        String contentLengthHeader = response.header("Content-Length");
        Log.i(TAG, "contentTypeHeader: "+contentTypeHeader+", contentLength: "+contentLengthHeader);
        long contentLength = -1;
        if (contentLengthHeader != null) {
            try {
                contentLength = Long.parseLong(contentLengthHeader);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        // ✅ 先判断是否需要拦截（不读取响应体）
        if (shouldBlockResponse(contentTypeHeader, contentLength)) {
            Log.e(TAG, "🚫 Blocking large file: type=" + contentTypeHeader +
                    ", size=" + formatSize(contentLength));

            // 关闭原始响应
            response.close();

            // 返回错误响应
            return createErrorResponse(request, "File too large: " + formatSize(contentLength));
        }

        ResponseBody peekBody = response.peekBody(MAX_BODY*2);
        contentLength = peekBody.contentLength();
        MediaType mediaType = peekBody.contentType();
        // ✅ 只处理文本类型
        if (isTextType(mediaType)) {
            BufferedSource source = peekBody.source();
            source.request(MAX_BODY);
            Buffer buffer = source.getBuffer();
            String responseStr = buffer.clone().readUtf8(Math.min(MAX_BODY, contentLength));
            if(contentLength >= MAX_BODY){
                responseStr = responseStr+"...";
            }
            Log.i(TAG, "✅ " + response.code() + " " + responseStr);
        } else {
            // 非文本类型且大于30MB，直接跳过
            if (contentLength > MAX_TEXT_SIZE) {
                Log.i(TAG, "← " + response.code() + " (binary file, size: " + formatSize(contentLength) + ", skip logging)");
                // 关闭原始响应
                response.close();
                // 返回自定义错误响应
                return createErrorResponse(request, "File too large: " + formatSize(contentLength));
            }
            Log.i(TAG, "✅ " + response.code() + " (binary body, skip logging)");
        }
        return response;
    }

    /**
     * 判断是否应该拦截响应（不读取响应体）
     */
    private boolean shouldBlockResponse(String contentType, long contentLength) {
        // 情况1：没有Content-Type，但有Content-Length且大于30MB
        if (contentType == null && contentLength > MAX_TEXT_SIZE) {
            return true;
        }

        // 情况2：有Content-Type且是非文本类型，大小大于30MB
        if (contentType != null && !isTextType(contentType) && contentLength > MAX_TEXT_SIZE) {
            return true;
        }

        // 情况3：文本类型但异常大（防止恶意大JSON）
        if (isTextType(contentType) && contentLength > MAX_TEXT_SIZE * 2) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否为文本类型（基于字符串，不解析MediaType）
     */
    private boolean isTextType(String contentType) {
        if (contentType == null) return false;
        String lower = contentType.toLowerCase();
        return lower.contains("text") ||
                lower.contains("json") ||
                lower.contains("xml") ||
                lower.contains("html");
    }

    /**
     * 判断是否为文本类型
     */
    private boolean isTextType(MediaType mediaType) {
        if (mediaType == null) return false;

        String type = mediaType.type();
        String subtype = mediaType.subtype().toLowerCase();

        // 文本类型
        if ("text".equals(type)) return true;

        // JSON
        if (subtype.contains("json")) return true;

        // XML
        if (subtype.contains("xml")) return true;

        // HTML
        if (subtype.contains("html")) return true;

        // JavaScript
        if (subtype.contains("javascript")) return true;

        // 表单
        if (subtype.contains("form")) return true;

        return false;
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private Response createErrorResponse(Request request, String message) {
        String json = String.format(
                "{\"error\":\"REQUEST_BLOCKED\",\"message\":\"%s\",\"timestamp\":%d}",
                message, System.currentTimeMillis()
        );

        ResponseBody errorBody = ResponseBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );

        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(413) // Payload Too Large
                .message("Request Blocked: " + message)
                .body(errorBody)
                .build();
    }
}