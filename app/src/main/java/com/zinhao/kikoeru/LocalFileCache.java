package com.zinhao.kikoeru;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalFileCache implements Runnable, Closeable {
    private static final String TAG = "LocalFileCache";

    private static LocalFileCache instance;
    private Thread workThread;
    private final List<Runnable> mission;
    private boolean running = true;

    public static synchronized LocalFileCache getInstance() {
        if(instance == null){
            instance = new LocalFileCache();
        }
        return instance;
    }

    public LocalFileCache() {
        mission = new ArrayList<>();
        this.workThread = new Thread(this);
        this.workThread.start();
    }

    public File getExternalCacheDir(Context context, int id){
        File cacheDir = context.getExternalCacheDir();
        File worksLibDir = new File(cacheDir,"libs_work");
        File workLibDir = new File(worksLibDir,String.valueOf(id));
        if(!workLibDir.exists()){
            if(!workLibDir.mkdirs()){
                Log.e(TAG, "getCacheDir: mkdir failed！" );
                return null;
            }
        }
        Log.d(TAG, "getCacheDir: "+ workLibDir.getPath());
        return workLibDir;
    }

    public File mapLocalItemFile(Context context, JSONObject item, int id,String relativePath) throws JSONException {
        if(!item.has("hash") && !item.has("title")){
            return null;
        }
        File workDir = instance.getExternalCacheDir(context,id);
        String title = item.getString("title");
//        File relativeDir = new File(workDir.getPath() + relativePath);
//        if(!relativeDir.exists()){
//            if(!relativeDir.mkdirs()){
//                Log.e(TAG, "mapLocalItemFile: mkdir failed！" );
//                return null;
//            }
//        }
        return new File( workDir.getPath() + relativePath + File.separator + title);
    }

    public boolean getLrcText(Context context,File audioFile,AsyncHttpClient.StringCallback callback){
        File dir = audioFile.getParentFile();
        String name = audioFile.getName();

        String beforeName = name.substring(0,name.lastIndexOf("."));
        File lrcFile =new File(dir ,beforeName + ".lrc");
        if(lrcFile.exists()){
            mission.add(new Runnable() {
                @Override
                public void run() {
                    String lrcText = readTextSync(lrcFile);
                    callback.onCompleted(null, new LocalResponse(200), lrcText);
                }
            });
            return true;
        }
        return false;
    }

    public void saveWork(Context context, JSONObject work, JSONArray rootTree) throws JSONException {
        File cacheDir = context.getExternalCacheDir();
        int id = work.getInt("id");
        File workJsonDir = new File(cacheDir,"json_work");
        File workTreeDir = new File(cacheDir,"json_work_tree");
        if(!workJsonDir.exists()){
            if(!workJsonDir.mkdirs()){
                Log.e(TAG, "getCacheDir: mkdir failed！" );
                return;
            }
        }
        if(!workTreeDir.exists()){
            if(!workTreeDir.mkdirs()){
                Log.e(TAG, "getCacheDir: mkdir failed！" );
                return;
            }
        }
        File workJsonFile = new File(workJsonDir,String.format(Locale.US,"%d.json",id));
        File workTreeJsonFile =  new File(workTreeDir,String.format(Locale.US,"%d.json",id));
        if(!workJsonFile.exists()){
            writeText(workJsonFile,work.toString());
        }

        if(!workTreeJsonFile.exists()){
            writeText(workTreeJsonFile,rootTree.toString());
        }
    }

    public void readLocalWorks(Context context,AsyncHttpClient.JSONObjectCallback callback) throws JSONException {
        mission.add(new Runnable() {
            @Override
            public void run() {
                File cacheDir = context.getExternalCacheDir();
                File workJsonDir = new File(cacheDir,"json_work");
                if(workJsonDir.exists()){
                    File[] workFiles = workJsonDir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.isFile() && pathname.getName().endsWith(".json");
                        }
                    });
                    if(workFiles == null)
                        return;
                    JSONObject rootJson = new JSONObject();
                    JSONArray works = new JSONArray();
                    JSONObject pagination = new JSONObject();
                    try {
                        pagination.put("currentPage",1);
                        pagination.put("pageSize",1);
                        pagination.put("totalCount",workFiles.length);
                        rootJson.put("pagination",pagination);
                        for (int i = 0; i < workFiles.length; i++) {
                            String workStr = readTextSync(workFiles[i]);
                            if(workStr!=null && !workStr.isEmpty()){
                                JSONObject work = new JSONObject(workStr);
                                work.put("localWorK",true);
                                works.put(work);
                            }
                        }
                        rootJson.put("works",works);
                        callback.onCompleted(null,new LocalResponse(200),rootJson);
                    } catch (JSONException e) {
                        callback.onCompleted(e,null,null);
                    }
                }
            }
        });
    }

    public void readLocalWorkTree(Context context,int id,AsyncHttpClient.JSONArrayCallback callback){
        mission.add(new Runnable() {
            @Override
            public void run() {
                File cacheDir = context.getExternalCacheDir();
                File workJsonDir = new File(cacheDir,"json_work_tree");
                File workTreeFile = new File(workJsonDir,String.format(Locale.US,"%d.json",id));
                if(workTreeFile.exists()){
                    String workStr = readTextSync(workTreeFile);
                    if(workStr != null && !workStr.isEmpty() && workStr.startsWith("[")){
                        try {
                            JSONArray workTree = new JSONArray(workStr);
                            callback.onCompleted(null,new LocalResponse(200),workTree);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callback.onCompleted(null,null,null);
                        }
                    }
                }
            }
        });
    }

    public void writeText(final File save,final String text){
        mission.add(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(save));
                    bufferedWriter.write(text);
                    bufferedWriter.flush();
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void readText(final File save,AsyncHttpClient.StringCallback callback){
        mission.add(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(save));
                    String text = null;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((text = bufferedReader.readLine())!= null){
                        stringBuilder.append(text);
                    }
                    bufferedReader.close();
                    callback.onCompleted(null,null,stringBuilder.toString());
                }catch (Exception e){
                    callback.onCompleted(e,null,null);
                }
            }
        });
    }

    public String readTextSync(File save){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(save));
            String text = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((text = bufferedReader.readLine())!= null){
                stringBuilder.append(text).append("\n");
            }
            bufferedReader.close();
            return stringBuilder.toString();
        }catch (Exception e){
        }
        return null;
    }

    public void downLoadFile(File save,String url,AsyncHttpClient.FileCallback callback){
        if(!url.startsWith("http")){
            url = Api.HOST+String.format("%s?token=%s",url,Api.token);
        }else {
            url = String.format("%s?token=%s",url,Api.token);
        }
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(url),"GET");
        request.setTimeout(5000);
        AsyncHttpClient asyncHttpClient = AsyncHttpClient.getDefaultInstance();
        asyncHttpClient.executeFile(request, save.getPath(), callback);
    }

    @Override
    public void run() {
        running = true;
        while (running){
            synchronized (mission){
                if(mission.size() != 0 ){
                    mission.get(0).run();
                    mission.remove(0);
                    Log.d(TAG, "run: mission success!");
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
    }
}
