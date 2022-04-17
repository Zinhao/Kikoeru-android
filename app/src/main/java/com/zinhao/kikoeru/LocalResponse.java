package com.zinhao.kikoeru;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.Headers;

public class LocalResponse implements AsyncHttpResponse {
    private int code;

    public LocalResponse(int code) {
        this.code = code;
    }

    @Override
    public String protocol() {
        return null;
    }

    @Override
    public String message() {
        return null;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public Headers headers() {
        return null;
    }

    @Override
    public AsyncSocket detachSocket() {
        return null;
    }

    @Override
    public AsyncHttpRequest getRequest() {
        return null;
    }

    @Override
    public void setDataCallback(DataCallback dataCallback) {

    }

    @Override
    public DataCallback getDataCallback() {
        return null;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public void setEndCallback(CompletedCallback completedCallback) {

    }

    @Override
    public CompletedCallback getEndCallback() {
        return null;
    }

    @Override
    public AsyncServer getServer() {
        return null;
    }

    @Override
    public String charset() {
        return null;
    }
}
