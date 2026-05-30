package com.zinhao.kikoeru;

import android.content.Context;
import androidx.collection.SimpleArrayMap;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;


import okhttp3.*;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class DownloadUtils implements Closeable {
    private static final String TAG = "DownloadUtils";
    private static DownloadUtils instance = null;
    private static File missionConfigFile;
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public List<Mission> missionList = new ArrayList<>();
    public SimpleArrayMap<String, Mission> hashMissionMap = new SimpleArrayMap<>();

    public static class Mission {
        private Call currentCall;
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
        private String hash;
        private Runnable successCallback;
        private Runnable stepCallback;
        private Exception missionException;

        public Exception getMissionException() {
            return missionException;
        }

        public void setSuccessCallback(Runnable successCallback) {
            this.successCallback = successCallback;
        }

        public void setStepCallback(Runnable stepCallback) {
            this.stepCallback = stepCallback;
        }

        public Mission(JSONObject jsonObject) {
            this.jsonObject = jsonObject;
            this.downloaded = 0;
            this.total = 0;
            this.eTag = "";
            this.title = "";
            this.update = false;
            try {
                if (jsonObject.has("downloaded")) {
                    this.downloaded = jsonObject.getLong("downloaded");
                }
                if (jsonObject.has("total")) {
                    this.total = jsonObject.getLong("total");
                }
                if (jsonObject.has(JSONConst.WorkTree.MAP_FILE_PATH)) {
                    String mapPath = jsonObject.getString(JSONConst.WorkTree.MAP_FILE_PATH);
                    mapFile = new File(mapPath);
                }
                if (jsonObject.has(JSONConst.WorkTree.WORK_ID)) {
                    workId = jsonObject.getInt(JSONConst.WorkTree.WORK_ID);
                }
                if (jsonObject.has("eTag")) {
                    this.eTag = jsonObject.getString("eTag");
                }
                if (jsonObject.has("title")) {
                    this.title = jsonObject.getString("title");
                }
                if (jsonObject.has("type")) {
                    this.type = jsonObject.getString("type");
                }
                if (jsonObject.has(JSONConst.WorkTree.HASH)) {
                    this.hash = jsonObject.getString(JSONConst.WorkTree.HASH);
                }
                getInstance().hashMissionMap.put(jsonObject.getString(JSONConst.WorkTree.HASH), this);
                getInstance().missionList.add(this);
            } catch (JSONException e) {
                e.printStackTrace();
                missionException = e;
                App.getInstance().alertException(e);
            }
        }

        public boolean equals(JSONObject jsonObject) {
            try {
                return hash.equals(jsonObject.getString(JSONConst.WorkTree.HASH));
            } catch (JSONException e) {
                missionException = e;
                e.printStackTrace();
                return false;
            }
        }

        public String getHash() {
            return hash;
        }

        public String getFormatProgressText() {
            if (total == 0) {
                return "-- / --";
            }
            float m = 1024 * 1024;
            float downLoadedM = downloaded / m;
            float totalM = total / m;
            if (isCompleted()) {
                return String.format(Locale.US, "已完成(共%.2fMB)", downLoadedM);
            }
            return String.format(Locale.US, "%.2fMB / %.2fMB", downLoadedM, totalM);
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

        public int getProgress() {
            if (isCompleted()) {
                return 100;
            }
            int progress = 0;
            if (total != 0 && total >= downloaded) {
                progress = Math.round(downloaded * 100f / total);
            }
            return progress;
        }

        public int getTypeCover() {
            if (type.equals("image")) {
                return R.drawable.ic_baseline_image_24;
            } else if (type.equals("audio")) {
                if (title.endsWith(".mp4")) {
                    return R.drawable.ic_baseline_video_library_24;
                }
                return R.drawable.ic_baseline_audiotrack_24;
            } else if (type.equals("text")) {
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
            if (!url.startsWith("http")) {
                url = App.getInstance().currentUser().getHost() + String.format("%s?token=%s", url, Api.token);
            } else {
                url = String.format("%s?token=%s", url, Api.token);
            }
            return url;
        }

        public void start() {
            if (isCompleted()) {
                return;
            }
            if (isDownloading()) {
                return;
            }

            try {
                String url = getDownLoadUrl();
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .addHeader("authorization", Api.authorization);

                // 断点续传
                if (downloaded > 0 && mapFile.exists()) {
                    requestBuilder.addHeader("Range", String.format(Locale.US, "bytes=%d-", downloaded));
                    if (eTag != null && !eTag.isEmpty()) {
                        requestBuilder.addHeader("If-Range", eTag);
                    }
                }

                Request request = requestBuilder.build();
                currentCall = okHttpClient.newCall(request);
                currentCall.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        missionException = e;
                        setDownloading(false);
                        App.getInstance().alertException(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            missionException = new IOException("Unexpected code " + response);
                            setDownloading(false);
                            return;
                        }

                        // 获取ETag
                        String newETag = response.header("ETag");
                        if (newETag != null) {
                            eTag = newETag;
                        }

                        // 获取文件总大小
                        long contentLength = response.body().contentLength();
                        if (contentLength > 0) {
                            total = contentLength + downloaded;
                        }

                        // 写入文件
                        BufferedSource source = null;
                        BufferedSink sink = null;
                        try {
                            source = Okio.buffer(response.body().source());

                            // 使用追加模式
                            FileOutputStream fos = new FileOutputStream(mapFile, downloaded > 0);
                            sink = Okio.buffer(Okio.sink(fos));

                            byte[] buffer = new byte[8192];
                            long bytesRead;
                            while ((bytesRead = source.read(buffer)) != -1) {
                                sink.write(buffer, 0, (int) bytesRead);
                                downloaded += bytesRead;

                                // 触发进度回调
                                if (stepCallback != null) {
                                    stepCallback.run();
                                }

                                update = true;
                            }
                            sink.flush();

                            // 下载完成
                            completed = true;
                            if (successCallback != null) {
                                successCallback.run();
                            }
                        } catch (IOException e) {
                            missionException = e;
                            App.getInstance().alertException(e);
                        } finally {
                            if (source != null) source.close();
                            if (sink != null) sink.close();
                            setDownloading(false);
                        }
                    }
                });

                setDownloading(true);
            } catch (JSONException e) {
                missionException = e;
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }

        public void stop() {
            if (currentCall != null && !currentCall.isCanceled()) {
                currentCall.cancel();
            }
            setDownloading(false);
        }

        public boolean isDownloading() {
            return downloading;
        }

        public void setDownloading(boolean downloading) {
            this.downloading = downloading;
        }

        public boolean isCompleted() {
            if (total == -1) {
                if (mapFile.exists() && mapFile.length() == downloaded) {
                    return true;
                }
            } else {
                if (total != 0 && downloaded != 0) {
                    completed = total == downloaded;
                }
                return completed;
            }
            return false;
        }

        private JSONObject getJsonObject() {
            try {
                jsonObject.put("downloaded", downloaded);
                jsonObject.put("total", total);
                jsonObject.put("eTag", eTag);
            } catch (JSONException e) {
                missionException = e;
                e.printStackTrace();
            }
            return jsonObject;
        }
    }

    public DownloadUtils() {
    }

    public static DownloadUtils getInstance() {
        if (instance == null) {
            instance = new DownloadUtils();
        }
        return instance;
    }

    public int removeMission(Mission mission) {
        if (hashMissionMap.containsKey(mission.getHash())) {
            hashMissionMap.remove(mission.getHash());
        }
        for (int i = 0; i < missionList.size(); i++) {
            if (missionList.get(i).equals(mission.getJsonObject())) {
                missionList.remove(i);
                return i;
            }
        }
        return -1;
    }

    public int removeCompleteMission() {
        for (int i = missionList.size() - 1; i >= 0; i--) {
            if (missionList.get(i).completed) {
                missionList.remove(i);
            }
        }
        return -1;
    }

    public void init(Context context) {
        missionConfigFile = new File(context.getCacheDir(), "downloadMission.json");
        missionList.clear();
        hashMissionMap.clear();
        if (missionConfigFile.exists()) {
            LocalFileCache.getInstance().readText(missionConfigFile, new AsyncHttpClient.StringCallback() {
                @Override
                public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
                    if (e != null) {
                        App.getInstance().alertException(e);
                        return;
                    }
                    try {
                        JSONArray missions = new JSONArray(s);
                        for (int i = 0; i < missions.length(); i++) {
                            JSONObject mission = missions.getJSONObject(i);
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

    public static Mission mapMission(JSONObject item) {
        for (int i = 0; i < getInstance().missionList.size(); i++) {
            DownloadUtils.Mission mission = DownloadUtils.getInstance().missionList.get(i);
            if (mission.equals(item) && !mission.isCompleted()) {
                return mission;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        if (missionList == null) {
            return;
        }
        JSONArray jsonArray = new JSONArray();
        missionList.forEach(new Consumer<Mission>() {
            @Override
            public void accept(Mission downLoadMission) {
                downLoadMission.stop();
                if (!downLoadMission.isCompleted()) {
                    jsonArray.put(downLoadMission.getJsonObject());
                }
            }
        });
        LocalFileCache.getInstance().writeText(missionConfigFile, jsonArray.toString());
    }
}
