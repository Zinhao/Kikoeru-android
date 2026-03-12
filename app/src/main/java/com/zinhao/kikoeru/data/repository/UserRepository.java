package com.zinhao.kikoeru.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.kikoeru.Api;
import com.zinhao.kikoeru.App;
import com.zinhao.kikoeru.User;
import com.zinhao.kikoeru.data.model.LoginResponse;
import com.zinhao.kikoeru.data.model.Result;

import org.json.JSONObject;

import java.util.List;

/**
 * 用户数据仓库
 */
public class UserRepository {
    
    private static UserRepository instance;
    
    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }
    
    private UserRepository() {
    }
    
    /**
     * 用户登录
     */
    public LiveData<Result<LoginResponse>> login(String username, String password, String host) {
        MutableLiveData<Result<LoginResponse>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Log.d("UserRepository", "Login request: user=" + username + ", host=" + host);
        Api.doGetToken(username, password, host, new AsyncHttpClient.JSONObjectCallback() {
            private boolean isCompleted = false;
            
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                // 防止重复回调
                if (isCompleted) {
                    Log.w("UserRepository", "Callback already completed, ignore");
                    return;
                }
                isCompleted = true;
                
                Log.d("UserRepository", "Login response received");
                if (e != null) {
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Network error: " + e.getClass().getSimpleName();
                    }
                    Log.e("UserRepository", "Login exception: " + errorMsg, e);
                    result.postValue(Result.error(errorMsg, e));
                    return;
                }
                if (jsonObject == null) {
                    Log.w("UserRepository", "Login response is null");
                    result.postValue(Result.error("Network error"));
                    return;
                }
                
                Log.d("UserRepository", "Login response: " + jsonObject.toString());
                int statusCode = response != null ? response.code() : 0;
                Log.d("UserRepository", "HTTP status code: " + statusCode);
                
                LoginResponse loginResponse = parseLoginResponse(jsonObject);
                
                if (response != null && response.code() == 200 && loginResponse.getToken() != null) {
                    // 保存用户信息
                    User user = new User();
                    user.setName(username);
                    user.setPassword(password);
                    user.setHost(host);
                    user.setToken(loginResponse.getToken());
                    
                    long userId = App.getInstance().insertUser(user);
                    App.getInstance().setCurrentUserId(userId);
                    Api.init(loginResponse.getToken(), host);
                    
                    Log.i("UserRepository", "Login success, userId=" + userId);
                    result.postValue(Result.success(loginResponse));
                } else {
                    String errorMsg = loginResponse.getErrorMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        int code = response != null ? response.code() : 0;
                        errorMsg = "Login failed (HTTP " + code + ")";
                    }
                    Log.w("UserRepository", "Login failed: " + errorMsg);
                    result.postValue(Result.error(errorMsg));
                }
            }
        });
        
        return result;
    }
    
    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return App.getInstance().getAllUsers();
    }
    
    /**
     * 获取当前用户
     */
    public User getCurrentUser() {
        return App.getInstance().currentUser();
    }
    
    /**
     * 切换用户
     */
    public void switchUser(User user) {
        App.getInstance().setCurrentUserId(user.getId());
        Api.init(user.getToken(), user.getHost());
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(User user) {
        App.getInstance().deleteUser(user);
    }
    
    /**
     * 检查是否有用户
     */
    public boolean hasUsers() {
        return !App.getInstance().getAllUsers().isEmpty();
    }
    
    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        return App.getInstance().currentUser() != null;
    }
    
    private LoginResponse parseLoginResponse(JSONObject jsonObject) {
        LoginResponse response = new LoginResponse();
        response.setToken(jsonObject.optString("token", null));
        response.setError(jsonObject.optString("error", null));
        
        // 解析 errors 数组
        org.json.JSONArray errorsArray = jsonObject.optJSONArray("errors");
        if (errorsArray != null && errorsArray.length() > 0) {
            java.util.List<LoginResponse.ErrorItem> errorItems = new java.util.ArrayList<>();
            for (int i = 0; i < errorsArray.length(); i++) {
                JSONObject errorObj = errorsArray.optJSONObject(i);
                if (errorObj != null) {
                    LoginResponse.ErrorItem item = new LoginResponse.ErrorItem();
                    item.setMsg(errorObj.optString("msg", null));
                    errorItems.add(item);
                }
            }
            response.setErrors(errorItems);
        }
        
        return response;
    }
}
