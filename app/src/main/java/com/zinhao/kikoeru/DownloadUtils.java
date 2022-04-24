package com.zinhao.kikoeru;

import android.content.Context;
import android.net.Uri;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class DownloadUtils implements Closeable {
    private static DownloadUtils instance = null;
    private static File missionConfigFile;
    public static List<Mission> missionList = new ArrayList<>();

    public static class Mission extends AsyncHttpClient.FileCallback{
        private final AsyncHttpClient downLoadClient;
        private JSONObject jsonObject;
        private File mapFile;
        private int workId;
        private long downloaded;
        private long total;
        private String eTag;
        private String title;
        private String type;
        private boolean update;

        private boolean isCompleted = false;

        public Mission(JSONObject jsonObject) {
            this.downLoadClient = new AsyncHttpClient(new AsyncServer());
            this.jsonObject = jsonObject;
            this.downloaded = 0;
            this.total = 0;
            this.eTag = "";
            this.title = "";
            this.update = false;
            try {
                if(jsonObject.has("downloaded")){
                    this.downloaded = jsonObject.getLong("downloaded");
                }
                if(jsonObject.has("total")){
                    this.total = jsonObject.getLong("total");
                }
                if(jsonObject.has("relativePath") && jsonObject.has("workId")){
                    String relativePath = jsonObject.getString("relativePath");
                    workId = jsonObject.getInt("workId");
                    mapFile = LocalFileCache.getInstance().mapLocalItemFile(jsonObject,workId,relativePath);
                }
                if(jsonObject.has("eTag")){
                    this.eTag = jsonObject.getString("eTag");
                }
                if(jsonObject.has("title")){
                    this.title = jsonObject.getString("title");
                }
                if(jsonObject.has("type")){
                    this.type = jsonObject.getString("type");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
            missionList.add(this);
        }


        public boolean equals(JSONObject jsonObject) {
            try {
                return this.jsonObject.getString("hash").equals(jsonObject.getString("hash"));
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }

        public String getTitle() {
            return title;
        }

        public File getMapFile() {
            return mapFile;
        }

        public int getWorkId() {
            return workId;
        }

        public int getProgress(){
            int progress = 0;
            if(total!=0 && total >= downloaded){
                progress =  Math.round(downloaded*100f/total);
            }
            return progress;
        }

        public int getTypeCover(){
            if(type.equals("image")){
                return R.drawable.ic_baseline_image_24;
            }else if(type.equals("audio")){
                return R.drawable.ic_baseline_audiotrack_24;
            }else if(type.equals("text")){
                return R.drawable.ic_baseline_text_snippet_24;
            }
            return R.drawable.ic_baseline_text_snippet_24;
        }


        public boolean isUpdate() {
            return update;
        }

        public void setUpdate(boolean update) {
            this.update = update;
        }

        private String getDownLoadUrl() throws JSONException {
            String url = jsonObject.getString("mediaDownloadUrl");
            if(!url.startsWith("http")){
                url = Api.HOST+String.format("%s?token=%s",url,Api.token);
            }else {
                url = String.format("%s?token=%s",url,Api.token);
            }
            return url;
        }

        public void start(){
            AsyncHttpRequest request = null;
            try {
                request = new AsyncHttpRequest(Uri.parse(getDownLoadUrl()),"GET");
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
                return;
            }
            request.setTimeout(5000);
            request.addHeader("range",String.format(Locale.US,"bytes=%d-",downloaded));
            request.addHeader("if-range",eTag);
            downLoadClient.executeFile(request,mapFile.getAbsolutePath(),this);
        }

        public void stop(){
            downLoadClient.getServer().stop();
        }

        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, File file) {
            isCompleted = true;
            update = true;
        }

        public boolean isCompleted() {
            if(total!=0 && downloaded!=0){
                return total == downloaded;
            }
            return isCompleted;
        }

        @Override
        public void onProgress(AsyncHttpResponse response, long downloaded, long total) {
            super.onProgress(response, downloaded, total);
            String eTag = response.headers().get("etag");
            this.downloaded = downloaded;
            this.total = total;
            this.eTag = eTag;
            this.update = true;
        }

        private JSONObject getJsonObject(){
            try {
                jsonObject.put("downloaded",downloaded);
                jsonObject.put("total",total);
                jsonObject.put("eTag",eTag);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
    }

    public DownloadUtils() { }

    public static DownloadUtils getInstance() {
        if(instance == null){
            instance = new DownloadUtils();
        }
        return instance;
    }

    public void init(Context context){
        missionConfigFile = new File(context.getCacheDir(),"downloadMission.json");
        missionList.clear();
        if(missionConfigFile.exists()){
            LocalFileCache.getInstance().readText(missionConfigFile, new AsyncHttpClient.StringCallback() {
                @Override
                public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
                    if(e!=null){
                        App.getInstance().alertException(e);
                        return;
                    }
                    try {
                        JSONArray missions = new JSONArray(s);
                        for (int i = 0; i < missions.length(); i++) {
                            JSONObject mission =missions.getJSONObject(i);
                            missionList.add(new Mission(mission));
                        }
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                        App.getInstance().alertException(jsonException);
                    }
                }
            });
        }
    }

    public static Mission mapMission(JSONObject item){
        for (int i = 0; i < missionList.size(); i++) {
            DownloadUtils.Mission mission = DownloadUtils.missionList.get(i);
            if(mission.equals(item) && !mission.isCompleted()){
                return mission;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        if(missionList == null){
            return;
        }
        JSONArray jsonArray = new JSONArray();
        missionList.forEach(new Consumer<Mission>() {
            @Override
            public void accept(Mission downLoadMission) {
                downLoadMission.stop();
                jsonArray.put(downLoadMission.getJsonObject());
            }
        });
        LocalFileCache.getInstance().writeText(missionConfigFile, jsonArray.toString());
    }
}
