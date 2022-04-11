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
    public static String HOST = "http://192.168.1.47:8888";
    private static final String TAG = "Api";
    public static final String REMOTE_HOST = "https://api.asmr.one";
    public static final String LOCAL_HOST = "http://192.168.1.47:8888";
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
            String userName = App.getInstance().getValue(App.CONFIG_USER_ACCOUNT,"guest");
            String password = App.getInstance().getValue(App.CONFIG_USER_PASSWORD,"guest");
            pwd.put("name", userName);
            pwd.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        request.setBody(new JSONObjectBody(pwd));
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
                if(asyncHttpResponse==null || asyncHttpResponse.code() != 200 || jsonObject == null){
                    Log.d(TAG, "onCompleted: get token err ");
//                    doGetToken();
                    return;
                }
                try {
                    token = jsonObject.getString("token");
                    Log.d(TAG, "onCompleted: "+token);
                    init(token);
                    App.getInstance().setValue(App.CONFIG_TOKEN,token);
                    App.getInstance().setValue(App.CONFIG_UPDATE_TIME,System.currentTimeMillis());
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
        });
    }
}
