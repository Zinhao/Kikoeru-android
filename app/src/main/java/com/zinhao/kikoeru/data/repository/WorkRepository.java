package com.zinhao.kikoeru.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.kikoeru.Api;
import com.zinhao.kikoeru.LocalFileCache;
import com.zinhao.kikoeru.data.model.Pagination;
import com.zinhao.kikoeru.data.model.Result;
import com.zinhao.kikoeru.data.model.Tag;
import com.zinhao.kikoeru.data.model.Track;
import com.zinhao.kikoeru.data.model.Va;
import com.zinhao.kikoeru.data.model.Work;
import com.zinhao.kikoeru.data.model.WorksResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 作品数据仓库
 * 协调远程 API 和本地缓存
 */
public class WorkRepository {
    
    private static WorkRepository instance;
    
    public static WorkRepository getInstance() {
        if (instance == null) {
            instance = new WorkRepository();
        }
        return instance;
    }
    
    private WorkRepository() {
    }
    
    /**
     * 获取作品列表
     */
    public LiveData<Result<WorksResponse>> getWorks(int page, String order, String sort, int subtitle) {
        MutableLiveData<Result<WorksResponse>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doGetWorks(page, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, result);
            }
        });
        
        return result;
    }
    
    /**
     * 根据标签获取作品
     */
    public LiveData<Result<WorksResponse>> getWorksByTag(int page, int tagId) {
        MutableLiveData<Result<WorksResponse>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doGetWorksByTag(page, tagId, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, result);
            }
        });
        
        return result;
    }
    
    /**
     * 根据声优获取作品
     */
    public LiveData<Result<WorksResponse>> getWorksByVa(int page, String vaId) {
        MutableLiveData<Result<WorksResponse>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doGetWorkByVa(page, vaId, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, result);
            }
        });
        
        return result;
    }
    
    /**
     * 搜索作品
     */
    public LiveData<Result<WorksResponse>> searchWorks(String keyword, int page) {
        MutableLiveData<Result<WorksResponse>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doGetWork(keyword, page, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, result);
            }
        });
        
        return result;
    }
    
    /**
     * 根据进度筛选获取作品
     */
    public LiveData<Result<WorksResponse>> getWorksByProgress(@Api.Filter String filter, int page) {
        MutableLiveData<Result<WorksResponse>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doGetReview(filter, page, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, result);
            }
        });
        
        return result;
    }
    
    /**
     * 获取本地作品
     */
    public LiveData<Result<List<Work>>> getLocalWorks() {
        MutableLiveData<Result<List<Work>>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        try {
            LocalFileCache.getInstance().readLocalWorks(null, new AsyncHttpClient.JSONObjectCallback() {
                @Override
                public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                    if (e != null) {
                        result.postValue(Result.error(e.getMessage(), e));
                        return;
                    }
                    if (jsonObject == null) {
                        result.postValue(Result.success(new ArrayList<>()));
                        return;
                    }
                    try {
                        List<Work> works = parseWorks(jsonObject);
                        for (Work work : works) {
                            work.setLocalWork(true);
                        }
                        result.postValue(Result.success(works));
                    } catch (JSONException ex) {
                        result.postValue(Result.error(ex.getMessage(), ex));
                    }
                }
            });
        } catch (JSONException e) {
            result.setValue(Result.error(e.getMessage(), e));
        }
        
        return result;
    }
    
    /**
     * 获取作品音轨列表
     */
    public LiveData<Result<List<Track>>> getWorkTracks(int workId) {
        MutableLiveData<Result<List<Track>>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doGetDocTree(workId, new AsyncHttpClient.JSONArrayCallback() {
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
                    List<Track> tracks = parseTracks(jsonArray);
                    result.postValue(Result.success(tracks));
                } catch (JSONException ex) {
                    result.postValue(Result.error(ex.getMessage(), ex));
                }
            }
        });
        
        return result;
    }
    
    /**
     * 标记作品进度
     */
    public LiveData<Result<Boolean>> markWorkProgress(int workId, @Api.Filter String progress) {
        MutableLiveData<Result<Boolean>> result = new MutableLiveData<>();
        result.setValue(Result.loading());
        
        Api.doPutReview(workId, progress, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                if (e != null) {
                    result.postValue(Result.error(e.getMessage(), e));
                    return;
                }
                if (response != null && response.code() == 200) {
                    result.postValue(Result.success(true));
                } else {
                    String msg = jsonObject != null ? jsonObject.optString("message", "Failed") : "Failed";
                    result.postValue(Result.error(msg, false));
                }
            }
        });
        
        return result;
    }
    
    // 处理作品列表响应
    private void handleWorksResponse(Exception e, AsyncHttpResponse response, 
                                     JSONObject jsonObject, 
                                     MutableLiveData<Result<WorksResponse>> result) {
        if (e != null) {
            result.postValue(Result.error(e.getMessage(), e));
            return;
        }
        if (response == null || response.code() != 200) {
            if (jsonObject != null && jsonObject.has("works")) {
                // 本地缓存数据
                try {
                    WorksResponse data = parseWorksResponse(jsonObject);
                    result.postValue(Result.success(data));
                } catch (JSONException ex) {
                    result.postValue(Result.error(ex.getMessage(), ex));
                }
            } else {
                result.postValue(Result.error("Request failed"));
            }
            return;
        }
        try {
            WorksResponse data = parseWorksResponse(jsonObject);
            result.postValue(Result.success(data));
        } catch (JSONException ex) {
            result.postValue(Result.error(ex.getMessage(), ex));
        }
    }
    
    // 解析作品列表响应
    @NonNull
    private WorksResponse parseWorksResponse(JSONObject jsonObject) throws JSONException {
        WorksResponse response = new WorksResponse();
        
        // 解析分页信息
        if (jsonObject.has("pagination")) {
            JSONObject paginationObj = jsonObject.getJSONObject("pagination");
            Pagination pagination = new Pagination();
            pagination.setCurrentPage(paginationObj.optInt("currentPage", 1));
            pagination.setPageSize(paginationObj.optInt("pageSize", 12));
            pagination.setTotalCount(paginationObj.optInt("totalCount", 0));
            pagination.setTotalPage(paginationObj.optInt("totalPage", 1));
            response.setPagination(pagination);
        }
        
        // 解析作品列表
        List<Work> works = parseWorks(jsonObject);
        response.setWorks(works);
        
        return response;
    }
    
    // 解析作品列表
    @NonNull
    private List<Work> parseWorks(JSONObject jsonObject) throws JSONException {
        List<Work> works = new ArrayList<>();
        if (jsonObject.has("works")) {
            JSONArray worksArray = jsonObject.getJSONArray("works");
            for (int i = 0; i < worksArray.length(); i++) {
                JSONObject workObj = worksArray.getJSONObject(i);
                Work work = parseWork(workObj);
                works.add(work);
            }
        }
        return works;
    }
    
    // 解析单个作品
    @NonNull
    private Work parseWork(JSONObject workObj) throws JSONException {
        Work work = new Work();
        work.setId(workObj.optInt("id"));
        work.setTitle(workObj.optString("title"));
        work.setNsfw(workObj.optBoolean("nsfw", false));
        work.setReleaseDate(workObj.optString("release"));
        work.setDlCount(workObj.optInt("dl_count"));
        work.setPrice(workObj.optInt("price"));
        work.setRateAverage(workObj.optDouble("rate_average_2dp", 0));
        work.setRateCount(workObj.optInt("rate_count", 0));
        
        // 解析社团
        if (workObj.has("circle")) {
            JSONObject circleObj = workObj.getJSONObject("circle");
            Work.Circle circle = new Work.Circle();
            circle.setId(circleObj.optInt("id"));
            circle.setName(circleObj.optString("name"));
            work.setCircle(circle);
        }
        
        // 解析标签
        if (workObj.has("tags")) {
            JSONArray tagsArray = workObj.getJSONArray("tags");
            List<Tag> tags = new ArrayList<>();
            for (int j = 0; j < tagsArray.length(); j++) {
                JSONObject tagObj = tagsArray.getJSONObject(j);
                Tag tag = new Tag();
                tag.setId(tagObj.optInt("id"));
                tag.setName(tagObj.optString("name"));
                tags.add(tag);
            }
            work.setTags(tags);
        }
        
        // 解析声优
        if (workObj.has("vas")) {
            JSONArray vasArray = workObj.getJSONArray("vas");
            List<Va> vas = new ArrayList<>();
            for (int j = 0; j < vasArray.length(); j++) {
                JSONObject vaObj = vasArray.getJSONObject(j);
                Va va = new Va();
                va.setId(vaObj.optString("id"));
                va.setName(vaObj.optString("name"));
                vas.add(va);
            }
            work.setVas(vas);
        }
        
        return work;
    }
    
    // 解析音轨列表
    @NonNull
    private List<Track> parseTracks(JSONArray jsonArray) throws JSONException {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject trackObj = jsonArray.getJSONObject(i);
            Track track = parseTrack(trackObj);
            tracks.add(track);
        }
        return tracks;
    }
    
    // 递归解析音轨
    @NonNull
    private Track parseTrack(JSONObject trackObj) throws JSONException {
        Track track = new Track();
        track.setId(trackObj.optInt("id"));
        track.setTitle(trackObj.optString("title"));
        track.setFileName(trackObj.optString("file_name"));
        track.setWorkTitle(trackObj.optString("work_title"));
        track.setHash(trackObj.optString("hash"));
        track.setSE(trackObj.optBoolean("is_se", false));
        track.setDuration(trackObj.optDouble("duration", 0));
        track.setSize(trackObj.optLong("size", 0));
        track.setType(trackObj.optString("type"));
        track.setWorkId(trackObj.optInt("work_id"));
        
        // 递归解析子文件
        if (trackObj.has("children")) {
            JSONArray childrenArray = trackObj.getJSONArray("children");
            List<Track> children = new ArrayList<>();
            for (int i = 0; i < childrenArray.length(); i++) {
                children.add(parseTrack(childrenArray.getJSONObject(i)));
            }
            track.setChildren(children);
        }
        
        return track;
    }
}
