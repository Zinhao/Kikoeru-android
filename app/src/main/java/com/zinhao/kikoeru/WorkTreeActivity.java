package com.zinhao.kikoeru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkTreeActivity extends BaseActivity implements View.OnClickListener,MusicChangeListener,
        ServiceConnection,LrcRowChangeListener,View.OnLongClickListener, TagsView.TagClickListener<JSONObject>,
        WorkTreeAdapter.RelativePathChangeListener{
    /**
     * http://localhost:8888/api/tracks/357844
     * @param savedInstanceState
     */
    private static final String TAG = "WorkActivity";
    private RecyclerView recyclerView;
    private WorkTreeAdapter workTreeAdapter;
    private AudioService.CtrlBinder ctrlBinder;

    private JSONObject work;
    private JSONArray jsonWorkTrees;

    private View bottomLayout;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvWorkTitle;
    private ImageButton ibStatus;

    private final AsyncHttpClient.JSONArrayCallback docTreeCallback = new AsyncHttpClient.JSONArrayCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONArray jsonArray) {
            if(e != null){
                alertException(e);
                return;
            }
            if(asyncHttpResponse==null || asyncHttpResponse.code() != 200){
                return;
            }
            jsonWorkTrees = jsonArray;
            // TODO 来自不同服务器的同一个作品（RJ号码相同），当用户执行下载操作时，目录树不一致。
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    workTreeAdapter = new WorkTreeAdapter(jsonWorkTrees,work);
                    workTreeAdapter.setItemClickListener(WorkTreeActivity.this);
                    workTreeAdapter.setTagClickListener(WorkTreeActivity.this);
                    workTreeAdapter.setVaClickListener(vaClickListener);
                    workTreeAdapter.setItemLongClickListener(WorkTreeActivity.this);
                    workTreeAdapter.setPathChangeListener(WorkTreeActivity.this);
                    RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(WorkTreeActivity.this,DividerItemDecoration.VERTICAL);
                    recyclerView.addItemDecoration(itemDecoration);
                    recyclerView.setLayoutManager(new LinearLayoutManager(WorkTreeActivity.this));
                    recyclerView.setAdapter(workTreeAdapter);
                }
            });
        }
    };

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);
        String workStr = getIntent().getStringExtra("work_json_str");
        if(workStr!=null && !workStr.isEmpty()){
            try {
                work = new JSONObject(workStr);
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
                finish();
                return;
            }
        }
        recyclerView = findViewById(R.id.recyclerView);
        bottomLayout = findViewById(R.id.bottomLayout);
        ivCover = bottomLayout.findViewById(R.id.imageView);
        tvTitle = bottomLayout.findViewById(R.id.textView);
        tvWorkTitle = bottomLayout.findViewById(R.id.textView2);
        ibStatus = bottomLayout.findViewById(R.id.button);

        bindService(new Intent(this, AudioService.class),this,BIND_AUTO_CREATE);
        init();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String workStr = intent.getStringExtra("work_json_str");
        Log.d(TAG, "onNewIntent: " + workStr);
        if(workStr!=null && !workStr.isEmpty()){
            try {
                work = new JSONObject(workStr);
                init();
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
                return;
            }
        }
    }

    private void init(){
        try {
            if(work.has(JSONConst.Work.IS_LOCAL_WORK)){
                boolean isLocalWork = work.getBoolean(JSONConst.Work.IS_LOCAL_WORK);
                if(isLocalWork){
                    LocalFileCache.getInstance().readLocalWorkTree(this,work.getInt("id"),docTreeCallback);
                    return;
                }
            }
            Api.doGetDocTree(work.getInt("id"), docTreeCallback);
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }

    @Override
    public void onBackPressed() {
        if(workTreeAdapter == null || workTreeAdapter.parentDir()){
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu subMenu = menu.addSubMenu(0,0,0, R.string.mark_action);
        subMenu.add(1,1,1, R.string.marked);
        subMenu.add(1,2,2, R.string.listening);
        subMenu.add(1,3,3, R.string.listened);
        subMenu.add(1,4,4, R.string.replay);
        subMenu.add(1,5,5, R.string.postponed);
        subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            if(item.getItemId() == 1){
                Api.doPutReview(work.getInt("id"),Api.FILTER_MARKED,actionCallBack);
            }else if(item.getItemId() == 2){
                Api.doPutReview(work.getInt("id"),Api.FILTER_LISTENING,actionCallBack);
            }else if(item.getItemId() == 3){
                Api.doPutReview(work.getInt("id"),Api.FILTER_LISTENED,actionCallBack);
            }else if(item.getItemId() == 4){
                Api.doPutReview(work.getInt("id"),Api.FILTER_REPLAY,actionCallBack);
            }else if(item.getItemId() == 5){
                Api.doPutReview(work.getInt("id"),Api.FILTER_POSTPONED,actionCallBack);
            }
        }catch (Exception e){

        }
        return super.onOptionsItemSelected(item);
    }

    private AsyncHttpClient.JSONObjectCallback actionCallBack = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            if(e!=null){
                alertException(e);
                return;
            }
            if(asyncHttpResponse == null)
                return;
            if(asyncHttpResponse.code() == 200){
                try {
                    String message = jsonObject.getString("message");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WorkTreeActivity.this,message,Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                    alertException(jsonException);
                }
            }
        }
    };

    @Override
    public void onClick(View v) {
        JSONObject item = (JSONObject) v.getTag();
        try {
            String itemType = item.getString("type");
            if("image".equals(itemType)){
                openImage(item);
            }else if("audio".equals(itemType)){
                openAudioOrVideo(item);
            } else if("text".equals(itemType)){
                openText(item);
            }else {
                openOther(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }

    private void openImage(JSONObject item) throws JSONException {
        List<String> imageList = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < workTreeAdapter.getData().length(); i++) {
            JSONObject _item = workTreeAdapter.getData().getJSONObject(i);
            if(_item.getString("type").equals("image")){
                String url;
                try {
                    url = _item.getString(JSONConst.WorkTree.MAP_FILE_PATH);
                    if(!new File(url).exists()){
                        url = _item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
                        url = Api.formatGetUrl(url,true);
                    }
                    imageList.add(url);
                    if(_item.getString(JSONConst.WorkTree.HASH).equals(item.getString(JSONConst.WorkTree.HASH))){
                        index = imageList.size()-1;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    alertException(e);
                }
            }
        }
        ImageBrowserActivity.start(WorkTreeActivity.this,imageList,index);
    }

    private void openText(JSONObject item){
        TextRowActivity.start(this,item.toString());
    }

    private void openOther(JSONObject item) throws JSONException {
        String url  = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if(!url.startsWith("http")){
            url = String.format("%s%s",Api.HOST,url);
        }
        intent.setData(Uri.parse(url));
        try {
            startActivity(intent);
        }catch (ActivityNotFoundException e){
            alertException(e);
        }
    }

    private void openAudioOrVideo(JSONObject item) throws JSONException {
        String itemHash = item.getString(JSONConst.WorkTree.HASH);
        String itemTitle = item.getString("title");
        String itemMediaStreamUrl = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
        List<JSONObject> musicArray = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < workTreeAdapter.getData().length(); i++) {
            JSONObject _item = workTreeAdapter.getData().getJSONObject(i);
            if("audio".equals(_item.getString("type"))){
                musicArray.add(_item);
                if(_item.getString(JSONConst.WorkTree.HASH).equals(itemHash)){
                    index = musicArray.size()-1;
                }
            }
        }
        if(ctrlBinder.getCurrent()!=null && ctrlBinder.getCurrent().getString(JSONConst.WorkTree.MEDIA_STREAM_URL).equals(itemMediaStreamUrl)){

        }else {
            ctrlBinder.setReapAll();
            ctrlBinder.play(musicArray,index);
        }
        if(itemTitle.toLowerCase(Locale.ROOT).endsWith(".mp4")){
            startActivity(new Intent(WorkTreeActivity.this,VideoPlayerActivity.class));
        }else {
            startActivity(new Intent(WorkTreeActivity.this, AudioPlayerActivity.class));
        }
    }

    @Override
    public void onAlbumChange(int rjNumber) {
        Glide.with(this).load(Api.HOST+String.format("/api/cover/%d?type=sam",rjNumber)).into(ivCover);
    }

    @Override
    public void onAudioChange(JSONObject audio) {
        try {
            tvTitle.setText(audio.getString("title"));
            tvWorkTitle.setText(audio.getString("workTitle"));
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }

    @Override
    public void onStatusChange(int status) {
        if(status == 0){
            ibStatus.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }else {
            ibStatus.setImageResource(R.drawable.ic_baseline_pause_24);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ctrlBinder.removeMusicChangeListener(this);
        ctrlBinder.removeLrcRowChangeListener(this);
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder)service;
        ibStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ctrlBinder.getController().getPlaybackState() == null)
                    return;
                if(ctrlBinder.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                    ctrlBinder.getController().getTransportControls().pause();
                }else {
                    ctrlBinder.getController().getTransportControls().play();
                }
            }
        });
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(ctrlBinder.getCurrentTitle().endsWith("mp4")){
                        startActivity(new Intent(WorkTreeActivity.this,VideoPlayerActivity.class));
                    }else if(ctrlBinder.getCurrentTitle().endsWith("mp3")){

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    alertException(e);
                }
            }
        });
        ctrlBinder.addMusicChangeListener(this);
        ctrlBinder.addLrcRowChangeListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public void onChange(Lrc.LrcRow currentRow) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Override
    public boolean onLongClick(View v) {
        JSONObject item = (JSONObject) v.getTag();
        if(item == null)
            return false;
        if(work ==null)
            return false;
        try {
            String itemType = item.getString("type");
            if(!item.has(JSONConst.WorkTree.MAP_FILE_PATH)){
                return false;
            }
            File itemFile = new File(item.getString(JSONConst.WorkTree.MAP_FILE_PATH));
            AlertDialog.Builder builder= new AlertDialog.Builder(this);
            builder.setMessage(itemFile.getAbsolutePath());
            if(itemFile.exists()){
                DownloadUtils.Mission mapMission = DownloadUtils.mapMission(item);
                if(mapMission != null){
                    builder.setTitle(R.string.downloading);
                    builder.setNegativeButton(R.string.cancel_download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mapMission.stop();
                        }
                    });
                    builder.setPositiveButton(R.string.check_mission, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(WorkTreeActivity.this,DownLoadMissionActivity.class));
                            dialogInterface.dismiss();
                        }
                    });
                }else {
                    builder.setNegativeButton(R.string.open, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                if(itemType.equals("audio")){
                                    openAudioOrVideo(item);
                                } else if(itemType.equals("text")){
                                    openText(item);
                                }else if(itemType.equals("image")){
                                   openImage(item);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    builder.setPositiveButton("open with", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = FileProvider.getUriForFile(WorkTreeActivity.this,getPackageName()+".fileProvider",itemFile);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            intent.setDataAndType(uri,String.format("%s/*",itemType));
                            try {
                                startActivity(intent);
                            }catch (ActivityNotFoundException e){
                                alertException(e);
                            }

                        }
                    });
                }

            }else {
                builder.setTitle(getString(R.string.not_download));
                builder.setMessage(itemFile.getAbsolutePath());
                builder.setNegativeButton(R.string.download, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(work.has(JSONConst.Work.IS_LOCAL_WORK)){
                            // 从本地目录树开始下载
                            if(!work.has(JSONConst.Work.HOST)){
                                return;
                            }
                            try {
                                String workHost = work.getString(JSONConst.Work.HOST);
                                if(!Api.HOST.equals(workHost)){
                                    Toast.makeText(WorkTreeActivity.this,"switch host user then start download!",Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        boolean havePermission;
                        final DownloadUtils.Mission downLoadMission = new DownloadUtils.Mission(item);
                        downLoadMission.setSuccessCallback(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!isDestroyed()){
                                            workTreeAdapter.notifyWorkDataSetChanged();
                                            workTreeAdapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                            }
                        });
                        if(App.getInstance().isSaveExternal()){
                            havePermission = requestReadWriteExternalPermission(new Runnable() {
                                @Override
                                public void run() {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        if(!Environment.isExternalStorageManager()){
                                            return;
                                        }
                                    }else {
                                        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                                            return;
                                        }
                                    }
                                    saveWorkWithTree();
                                    downLoadMission.start();
                                }
                            });
                        }else {
                            havePermission = true;
                        }
                        if(havePermission){
                            saveWorkWithTree();
                            downLoadMission.start();
                        }
                        dialog.dismiss();

                    }
                });
                final String itemStreamUrl = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
                builder.setPositiveButton("open in browser", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        String readableUrl;
                        if(!itemStreamUrl.startsWith("http")){
                            readableUrl = String.format("%s%s?token=%s",Api.HOST,itemStreamUrl,Api.token);
                        }else {
                            readableUrl = String.format("%s?token=%s",itemStreamUrl,Api.token);
                        }
                        intent.setData(Uri.parse(readableUrl));
                        try {
                            startActivity(intent);
                        }catch (ActivityNotFoundException e){
                            alertException(e);
                        }
                    }
                });
            }
            builder.create().show();
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
        return true;
    }

    private void saveWorkWithTree(){
        if(jsonWorkTrees != null){
            try {
                work.put(JSONConst.Work.HOST,Api.HOST);
                LocalFileCache.getInstance().saveWork(work,jsonWorkTrees);
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
        }
    }

    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            int tagId = jsonObject.getInt("id");
            String tagName = jsonObject.getString("name");
            setTitle(tagName);
            Intent intent = new Intent(WorkTreeActivity.this,WorksActivity.class);
            intent.putExtra("resultType","tag");
            intent.putExtra("id",tagId);
            intent.putExtra("name",tagName);
            startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }


    private final TagsView.TagClickListener<JSONObject> vaClickListener = new TagsView.TagClickListener<JSONObject>() {
        @Override
        public void onTagClick(JSONObject jsonObject) {
            try {
                String vaId = jsonObject.getString("id");
                Log.d(TAG, "onTagClick: "+ vaId);
                String vaName = jsonObject.getString("name");
                setTitle(vaName);
                Intent intent = new Intent(WorkTreeActivity.this,WorksActivity.class);
                intent.putExtra("resultType","va");
                intent.putExtra("id",vaId);
                intent.putExtra("name",vaName);
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
        }
    };

    @Override
    public void onPathChange(String path) {
        setTitle(path);
    }
}