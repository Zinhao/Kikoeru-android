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
    public void getWorks(int page, String order, String sort, int subtitle, ResultCallback<WorksResponse> callback) {
        android.util.Log.d("WorkRepository", "getWorks: page=" + page);
        Api.doGetWorks(page, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, callback);
            }
        });
    }
    
    /**
     * 根据标签获取作品
     */
    public void getWorksByTag(int page, int tagId, ResultCallback<WorksResponse> callback) {
        Api.doGetWorksByTag(page, tagId, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, callback);
            }
        });
    }
    
    /**
     * 根据声优获取作品
     */
    public void getWorksByVa(int page, String vaId, ResultCallback<WorksResponse> callback) {
        Api.doGetWorkByVa(page, vaId, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, callback);
            }
        });
    }
    
    /**
     * 根据社团获取作品
     */
    public void getWorksByCircles(int page, long circlesId, ResultCallback<WorksResponse> callback) {
        Api.doGetWorkByCircles(page, circlesId, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, callback);
            }
        });
    }
    
    /**
     * 搜索作品
     */
    public void searchWorks(String keyword, int page, ResultCallback<WorksResponse> callback) {
        Api.doGetWork(keyword, page, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, callback);
            }
        });
    }
    
    /**
     * 根据进度筛选获取作品
     */
    public void getWorksByProgress(@Api.Filter String filter, int page, ResultCallback<WorksResponse> callback) {
        Api.doGetReview(filter, page, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                handleWorksResponse(e, response, jsonObject, callback);
            }
        });
    }
    
    /**
     * 获取本地作品
     */
    public void getLocalWorks(ResultCallback<List<Work>> callback) {
        try {
            LocalFileCache.getInstance().readLocalWorks(null, new AsyncHttpClient.JSONObjectCallback() {
                @Override
                public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                    if (e != null) {
                        callback.onError(e.getMessage(), e);
                        return;
                    }
                    if (jsonObject == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    try {
                        List<Work> works = parseWorks(jsonObject);
                        for (Work work : works) {
                            work.setLocalWork(true);
                        }
                        callback.onSuccess(works);
                    } catch (JSONException ex) {
                        callback.onError(ex.getMessage(), ex);
                    }
                }
            });
        } catch (JSONException e) {
            callback.onError(e.getMessage(), e);
        }
    }
    
    /**
     * 获取作品音轨列表
     */
    public void getWorkTracks(int workId, ResultCallback<List<Track>> callback) {
        Api.doGetDocTree(workId, new AsyncHttpClient.JSONArrayCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONArray jsonArray) {
                if (e != null) {
                    callback.onError(e.getMessage(), e);
                    return;
                }
                if (response == null || response.code() != 200) {
                    callback.onError("Request failed", null);
                    return;
                }
                try {
                    List<Track> tracks = parseTracks(jsonArray);
                    callback.onSuccess(tracks);
                } catch (JSONException ex) {
                    callback.onError(ex.getMessage(), ex);
                }
            }
        });
    }
    
    /**
     * 标记作品进度
     */
    public void markWorkProgress(int workId, @Api.Filter String progress, ResultCallback<Boolean> callback) {
        Api.doPutReview(workId, progress, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject jsonObject) {
                if (e != null) {
                    callback.onError(e.getMessage(), e);
                    return;
                }
                if (response != null && response.code() == 200) {
                    callback.onSuccess(true);
                } else {
                    String msg = jsonObject != null ? jsonObject.optString("message", "Failed") : "Failed";
                    callback.onError(msg, null);
                }
            }
        });
    }
    
    // 处理作品列表响应
    private void handleWorksResponse(Exception e, AsyncHttpResponse response, 
                                     JSONObject jsonObject, 
                                     ResultCallback<WorksResponse> callback) {
        if (e != null) {
            callback.onError(e.getMessage(), e);
            return;
        }
        if (response == null || response.code() != 200) {
            if (jsonObject != null && jsonObject.has("works")) {
                // 本地缓存数据
                try {
                    WorksResponse data = parseWorksResponse(jsonObject);
                    callback.onSuccess(data);
                } catch (JSONException ex) {
                    callback.onError(ex.getMessage(), ex);
                }
            } else {
                String errorMsg = "Request failed";
                if (jsonObject != null) {
                    errorMsg = jsonObject.optString("message", jsonObject.optString("error", "Request failed"));
                }
                callback.onError(errorMsg, null);
            }
            return;
        }
        try {
            WorksResponse data = parseWorksResponse(jsonObject);
            callback.onSuccess(data);
        } catch (JSONException ex) {
            callback.onError(ex.getMessage(), ex);
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
            android.util.Log.d("WorkRepository", "Parsed pagination: " + 
                "currentPage=" + pagination.getCurrentPage() + 
                ", totalPage=" + pagination.getTotalPage() +
                ", totalCount=" + pagination.getTotalCount());
        } else {
            android.util.Log.w("WorkRepository", "No pagination in response");
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
    
    // 结果回调接口
    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(String message, Throwable error);
    }
}
