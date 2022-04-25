package com.zinhao.kikoeru;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.collection.SimpleArrayMap;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.callback.HttpConnectCallback;
import com.koushikdutta.async.stream.OutputStreamDataCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class DownloadUtils implements Closeable {
    private static final String TAG = "DownloadUtils";
    private static DownloadUtils instance = null;
    private static File missionConfigFile;

    public List<Mission> missionList = new ArrayList<>();
    public SimpleArrayMap<String,Mission> hashMissionMap = new SimpleArrayMap<>();

    public static class Mission extends AsyncHttpClient.FileCallback{
        private AsyncHttpClient downLoadClient;
        private JSONObject jsonObject;
        private File mapFile;
        private int workId;
        private long downloaded;
        private long total;
        private String eTag;
        private String title;
        private String type;
        private boolean update;
        private boolean downloading = false;
        private boolean completed = false;
        private AsyncHttpRequest request;
        private String hash;
        private static final int BLOCK_SIZE = 1024*1024;

        public Mission(JSONObject jsonObject) {
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
                if(jsonObject.has("hash")){
                    this.hash = jsonObject.getString("hash");
                }
                getInstance().hashMissionMap.put(jsonObject.getString("hash"),this);
                getInstance().missionList.add(this);
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }

        public boolean equals(JSONObject jsonObject) {
            try {
                return hash.equals(jsonObject.getString("hash"));
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }

        public String getHash() {
            return hash;
        }

        public String getFormatProgressText(){
            if(total == 0){
                return "-- / --";
            }
            float m = 1024*1024;
            float downLoadedM = downloaded/m;
            float totalM = total/m;
            if(isCompleted()){
                return String.format(Locale.US,"已完成(共%.2fMb)",downLoadedM);
            }
            return String.format(Locale.US,"%.2fMb / %.2fMb",downLoadedM,totalM);
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
                if(title.endsWith(".mp4")){
                    return R.drawable.ic_baseline_video_library_24;
                }
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
            if(isCompleted()){
                return;
            }
            if(isDownloading())
                return;
            request = null;
            try {
                request = new AsyncHttpRequest(Uri.parse(getDownLoadUrl()),"GET");
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
                return;
            }
            request.setTimeout(5000);
            this.downLoadClient = new AsyncHttpClient(new AsyncServer());
            if(downloaded != 0){
                continueDownLoad();
            }else {
                if(mapFile.exists()){
                    App.getInstance().alertException(new FileAlreadyExistsException("文件已存在"));
                    return;
                }
                downLoadClient.executeFile(request,mapFile.getAbsolutePath(),this);
            }
            setDownloading(true);
        }

        private void continueDownLoad(){
            request.addHeader("range",String.format(Locale.US,"bytes=%d-",downloaded));
            request.addHeader("if-range",eTag);
            final BufferedOutputStream fout;
            try {
                fout = new BufferedOutputStream(new FileOutputStream(mapFile,true), 8192);
            } catch (FileNotFoundException var8) {
                return;
            }
            downLoadClient.execute(request, new HttpConnectCallback() {
                @Override
                public void onConnectCompleted(Exception e, AsyncHttpResponse response) {
                    if (e != null) {
                        try { fout.close(); } catch (IOException ignored) { }
                    } else {
                        response.setDataCallback(new OutputStreamDataCallback(fout) {
                            public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                                downloaded += bb.remaining();
                                super.onDataAvailable(emitter, bb);
                                Mission.this.onProgress(response, downloaded, total);
                            }
                        });
                        response.setEndCallback(new CompletedCallback() {
                            public void onCompleted(Exception ex) {
                                try {
                                    fout.close();
                                } catch (IOException var3) {
                                    Log.d(TAG, "onCompleted: close failed!");
                                }
                                if (ex != null) {
                                    Mission.this.onCompleted(ex,response,mapFile);
                                } else {
                                    Mission.this.onCompleted(null,response,mapFile);
                                }
                            }
                        });
                    }
                }
            });
        }

        public void stop(){
            if(downLoadClient!=null){
                downLoadClient.getServer().stop();
            }
            setDownloading(false);
        }

        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, File file) {
            update = true;
            if(e != null){
                App.getInstance().alertException(e);
                return;
            }
            if(mapFile.length() != total){
                String fileMapErr = String.format("rec: %d ,total: %d !",mapFile.getPath(),mapFile.length(),total);
                AppMessage fileNoMap = new AppMessage("file size not map!", fileMapErr, new Runnable() {
                    @Override
                    public void run() {
                        if(mapFile.exists()){
                            if(mapFile.delete()){
                            }
                        }
                    }
                },"try delete");
                App.getInstance().alertMessage(fileNoMap);
            }
            Log.d(TAG, String.format("onCompleted: rec: %d ,total: %d",mapFile.length(),total));
            setDownloading(false);
            completed = true;
        }

        public boolean isDownloading() {
            return downloading;
        }

        public void setDownloading(boolean downloading) {
            this.downloading = downloading;
        }

        public boolean isCompleted() {
            if(total!=0 && downloaded!=0){
                completed = total == downloaded;
            }
            return completed;
        }

        @Override
        public void onProgress(AsyncHttpResponse response, long downloaded, long total) {
            super.onProgress(response, downloaded, total);
            String eTag = response.headers().get("etag");
            this.downloaded = downloaded;
            this.total = total;
            this.eTag = eTag;
            Log.d(TAG, String.format("onProgress: %d",getProgress()));
            this.update = true;
            if(downloaded == total){
                downloading = false;
                completed = true;
            }
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

    public int removeMission(Mission mission){
        if(hashMissionMap.containsKey(mission.getHash())){
            hashMissionMap.remove(mission.getHash());
        }
        for (int i = 0; i < missionList.size(); i++) {
            if(missionList.get(i).equals(mission.getJsonObject())){
                missionList.remove(i);
                return i;
            }
        }
        return -1;
    }

    public void init(Context context){
        missionConfigFile = new File(context.getCacheDir(),"downloadMission.json");
        missionList.clear();
        hashMissionMap.clear();
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
                            new Mission(mission);
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
        for (int i = 0; i < getInstance().missionList.size(); i++) {
            DownloadUtils.Mission mission = DownloadUtils.getInstance().missionList.get(i);
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
                if(!downLoadMission.isCompleted()){
                    jsonArray.put(downLoadMission.getJsonObject());
                }
            }
        });
        LocalFileCache.getInstance().writeText(missionConfigFile, jsonArray.toString());
    }
}
