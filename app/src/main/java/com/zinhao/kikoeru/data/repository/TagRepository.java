package com.zinhao.kikoeru.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.kikoeru.Api;
import com.zinhao.kikoeru.data.model.Result;
import com.zinhao.kikoeru.data.model.Tag;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签数据仓库
 */
public class TagRepository {
    
    private static TagRepository instance;
    
    public static TagRepository getInstance() {
        if (instance == null) {
            instance = new TagRepository();
        }
        return instance;
    }
    
    private TagRepository() {
    }
    
    /**
     * 获取所有标签
     */
    public LiveData<Result<List<Tag>>> getAllTags() {
        MutableLiveData<Result<List<Tag>>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doGetAllTags(new AsyncHttpClient.JSONArrayCallback() {
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
                    List<Tag> tags = parseTags(jsonArray);
                    result.postValue(Result.success(tags));
                } catch (JSONException ex) {
                    result.postValue(Result.error(ex.getMessage(), ex));
                }
            }
        });
        
        return result;
    }
    
    private List<Tag> parseTags(JSONArray jsonArray) throws JSONException {
        List<Tag> tags = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject tagObj = jsonArray.getJSONObject(i);
            Tag tag = new Tag();
            tag.setId(tagObj.optInt("id"));
            tag.setName(tagObj.optString("name"));
            
            // 解析国际化信息
            if (tagObj.has("i18n")) {
                JSONObject i18nObj = tagObj.getJSONObject("i18n");
                Tag.TagI18n i18n = new Tag.TagI18n();
                i18n.setZhCn(i18nObj.optString("zh-cn", null));
                i18n.setJaJp(i18nObj.optString("ja-jp", null));
                i18n.setEnUs(i18nObj.optString("en-us", null));
                tag.setI18n(i18n);
            }
            
            tags.add(tag);
        }
        return tags;
    }
}
