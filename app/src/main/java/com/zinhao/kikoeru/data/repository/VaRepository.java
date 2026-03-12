package com.zinhao.kikoeru.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.kikoeru.Api;
import com.zinhao.kikoeru.data.model.Result;
import com.zinhao.kikoeru.data.model.Va;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 声优数据仓库
 */
public class VaRepository {
    
    private static VaRepository instance;
    
    public static VaRepository getInstance() {
        if (instance == null) {
            instance = new VaRepository();
        }
        return instance;
    }
    
    private VaRepository() {
    }
    
    /**
     * 获取所有声优
     */
    public LiveData<Result<List<Va>>> getAllVas() {
        MutableLiveData<Result<List<Va>>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doGetAllVas(new AsyncHttpClient.JSONArrayCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONArray jsonArray) {
                if (e != null) {
                    result.postValue(Result.error(e.getMessage(), e));
                    return;
                }
                if (response == null || response.code() != 200) {
                    result.postValue(Result.error("Request failed"));
                    return;
                }
                try {
                    List<Va> vas = parseVas(jsonArray);
                    result.postValue(Result.success(vas));
                } catch (JSONException ex) {
                    result.postValue(Result.error(ex.getMessage(), ex));
                }
            }
        });
        
        return result;
    }
    
    private List<Va> parseVas(JSONArray jsonArray) throws JSONException {
        List<Va> vas = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject vaObj = jsonArray.getJSONObject(i);
            Va va = new Va();
            va.setId(vaObj.optString("id"));
            va.setName(vaObj.optString("name"));
            vas.add(va);
        }
        return vas;
    }
}
