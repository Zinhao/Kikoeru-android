package com.zinhao.kikoeru;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.JSONObjectBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Api {
//    public static final String HOST = "http://192.168.1.47:8888";
    private static final String TAG = "Api";
    public static final String HOST = "https://api.asmr.one";
    public static String authorization = "";
    public static String token = "";

    public static void init(String str){
        token = str;
        authorization = String.format("Bearer %s",str);
    }

    public static void doGetWorks(int page,AsyncHttpClient.StringCallback callback){
        //https://api.asmr.one/api/works?order=create_date&sort=desc&page=1&seed=40&subtitle=1
        //subtitle=1 带字幕
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST+String.format("/api/works?order=id&sort=desc&page=%d&seed=35&subtitle=1",page)),"GET");
        request.setTimeout(5000);
        request.addHeader("authorization",authorization);
        AsyncHttpClient.getDefaultInstance().executeString(request, callback);
    }

    public static void doGetDocTree(int id, AsyncHttpClient.StringCallback callback){
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST+String.format("/api/tracks/%d",id)),"GET");
        request.setTimeout(5000);
        request.addHeader("authorization",authorization);
        AsyncHttpClient.getDefaultInstance().executeString(request, callback);
    }

    public static void checkLrc(String hash,AsyncHttpClient.StringCallback callback){
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST+String.format("/api/media/check-lrc/%s?token=%s",hash,token)),"GET");
        request.setTimeout(5000);
        request.addHeader("authorization",authorization);
        AsyncHttpClient.getDefaultInstance().executeString(request, callback);
    }

    public static void doGetMediaString(String hash, AsyncHttpClient.StringCallback callback){
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST+String.format("/api/media/stream/%s?token=%s",hash,token)),"GET");
        request.setTimeout(5000);
        request.addHeader("authorization",authorization);
        AsyncHttpClient.getDefaultInstance().executeString(request, callback);
    }

    public static void doGetToken(){
        AsyncHttpRequest request =new AsyncHttpRequest(Uri.parse(HOST+"/api/auth/me"),"POST");
        JSONObject pwd = new JSONObject();
        try {
            pwd.put("name","zinhao");
            pwd.put("password", "zinhao1513");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        request.setBody(new JSONObjectBody(pwd));
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
                if(asyncHttpResponse==null || asyncHttpResponse.code() != 200 || jsonObject == null){
                    Log.d(TAG, "onCompleted: err ");
                    doGetToken();
                    return;
                }
                try {
                    token = jsonObject.getString("token");
                    init(token);
                    App.getInstance().setValue("token",token);
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
        });
    }
}
