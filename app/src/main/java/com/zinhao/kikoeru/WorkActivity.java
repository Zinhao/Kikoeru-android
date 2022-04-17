package com.zinhao.kikoeru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WorkActivity extends AppCompatActivity implements View.OnClickListener,MusicChangeListener,
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
    private List<JSONObject> workTrees;
    private JSONArray jsonWorkTrees;

    private RecyclerView.OnScrollListener scrollListener;

    private View bottomLayout;
    private Animation outAnim;
    private Animation inAnim;
    private boolean shouldShowAnim = true;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvWorkTitle;
    private ImageButton ibStatus;


    private AsyncHttpClient.JSONArrayCallback docTreeCallback = new AsyncHttpClient.JSONArrayCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONArray jsonArray) {
            if(asyncHttpResponse==null || asyncHttpResponse.code() != 200){
                if(jsonArray != null && jsonArray.length() != 0){
                    Log.d(TAG, "onCompleted: local work tree get success!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                setTitle(String.format("RJ%d (local)",work.getInt("id")));
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                        }
                    });
                }else {
                    Log.d(TAG, "onCompleted: docTreeCallback err ");
                    return;
                }
            }
            workTrees = new ArrayList<>();
            try {
                jsonWorkTrees = jsonArray;
                for (int i = 0; i < jsonArray.length(); i++) {
                    workTrees.add(jsonArray.getJSONObject(i));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        workTreeAdapter = new WorkTreeAdapter(workTrees);
                        workTreeAdapter.setItemClickListener(WorkActivity.this);
                        workTreeAdapter.setTagClickListener(WorkActivity.this);
                        workTreeAdapter.setItemLongClickListener(WorkActivity.this);
                        workTreeAdapter.setPathChangeListener(WorkActivity.this);
                        workTreeAdapter.setHeaderInfo(work);
                        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(WorkActivity.this,DividerItemDecoration.VERTICAL);
                        recyclerView.addItemDecoration(itemDecoration);
                        recyclerView.setLayoutManager(new LinearLayoutManager(WorkActivity.this));
                        recyclerView.setAdapter(workTreeAdapter);
                    }
                });
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
        }
    };

    private AsyncHttpClient.StringCallback lrcTextCallback = new AsyncHttpClient.StringCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
            if(asyncHttpResponse ==null || asyncHttpResponse.code() !=200){
                Log.d(TAG, "onCompleted: lrcTextCallback err!");
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(ctrlBinder.getLrc()!=null && ctrlBinder.getLrc().getText().equals(s)){
                        LrcShowActivity.start(WorkActivity.this,ctrlBinder.getLrc().getText(),true);
                    }else {
                        LrcShowActivity.start(WorkActivity.this,s,false);
                    }

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
        outAnim = AnimationUtils.loadAnimation(this,R.anim.move_bottom_out);
        inAnim = AnimationUtils.loadAnimation(this,R.anim.move_bottom_in);
        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
//                if (dy > 0 && shouldShowAnim && bottomLayout.getVisibility() == View.VISIBLE){
//                    toggleBottom();
//                }else if(dy<0 && shouldShowAnim && bottomLayout.getVisibility() == View.GONE){
//                    toggleBottom();
//                }
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
        try {
            if(work.has("localWorK")){
                boolean isLocalWork = work.getBoolean("localWorK");
                if(isLocalWork){
                    LocalFileCache.getInstance().readLocalWorkTree(this,work.getInt("id"),docTreeCallback);
                }
            }else {
                Api.doGetDocTree(work.getInt("id"), docTreeCallback);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void toggleBottom(){
        if (shouldShowAnim && bottomLayout.getVisibility() == View.VISIBLE){
            shouldShowAnim = false;
            bottomLayout.startAnimation(outAnim);
            bottomLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bottomLayout.setVisibility(View.GONE);
                    shouldShowAnim = true;
                }
            },outAnim.getDuration());
        }else if(shouldShowAnim && bottomLayout.getVisibility() == View.GONE){
            shouldShowAnim = false;
            bottomLayout.setVisibility(View.VISIBLE);
            bottomLayout.startAnimation(inAnim);
            bottomLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    shouldShowAnim = true;
                }
            },inAnim.getDuration());
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
        SubMenu subMenu = menu.addSubMenu(0,0,0,"action");
        subMenu.add(1,1,1,Api.FILTER_MARKED);
        subMenu.add(1,2,2,Api.FILTER_LISTENING);
        subMenu.add(1,3,3,Api.FILTER_LISTENED);
        subMenu.add(1,4,4,Api.FILTER_REPLAY);
        subMenu.add(1,5,5,Api.FILTER_POSTPONED);
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
            if(asyncHttpResponse == null)
                return;
            if(asyncHttpResponse.code() == 200){
                try {
                    String message = jsonObject.getString("message");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WorkActivity.this,message,Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.d(TAG, "onCompleted: "+message);
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }else {
                Log.d(TAG, "onCompleted: "+asyncHttpResponse.code());
            }
        }
    };

    @Override
    public void onClick(View v) {
        JSONObject item = (JSONObject) v.getTag();
        try {
            if("image".equals(item.getString("type"))){
                List<String> imageList = new ArrayList<>();
                int index = 0;
                workTreeAdapter.getData().stream().filter(new Predicate<JSONObject>() {
                    @Override
                    public boolean test(JSONObject jsonObject) {
                        try {
                            return "image".equals(jsonObject.getString("type"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                }).forEach(new Consumer<JSONObject>() {
                    @Override
                    public void accept(JSONObject jsonObject) {
                        try {
                            imageList.add(jsonObject.getString("mediaStreamUrl"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                for (int i = 0; i < imageList.size(); i++) {
                    try {
                        if(imageList.get(i).equals(item.getString("mediaStreamUrl"))){
                            index = i;
                            break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                ImageBrowserActivity.start(this,imageList,index);
            }else if("audio".equals(item.getString("type"))){
                List<JSONObject> musicArray = new ArrayList<>();
                int index = 0;
                workTreeAdapter.getData().forEach(new Consumer<JSONObject>() {
                    @Override
                    public void accept(JSONObject jsonObject) {
                        try {
                            if("audio".equals(jsonObject.getString("type"))){
                                int id = work.getInt("id");
                                File itemFile = LocalFileCache.getInstance().mapLocalItemFile(WorkActivity.this,jsonObject,id,workTreeAdapter.getRelativePath());
                                if(itemFile!=null){
                                    if(itemFile.exists()){
                                        jsonObject.put("local_file_path",itemFile.getAbsolutePath());
                                    }
                                }
                                musicArray.add(jsonObject);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                for (int i = 0; i < musicArray.size(); i++) {
                    try {
                        if(musicArray.get(i).getString("hash").equals(item.getString("hash"))){
                            index = i;
                            break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    if(ctrlBinder.getCurrent()!=null && ctrlBinder.getCurrent().getString("mediaStreamUrl").equals(item.getString("mediaStreamUrl"))){

                    }else {
                        ctrlBinder.setReap();
                        ctrlBinder.setCurrentAlbumId(work.getInt("id"));
                        ctrlBinder.play(musicArray,index);
                    }

                    if(item.getString("title").toLowerCase(Locale.ROOT).endsWith("mp4")){
                        startActivity(new Intent(this,VideoPlayerActivity.class));
                    }else {
                        startActivity(new Intent(this, MusicPlayerActivity.class));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if("text".equals(item.getString("type"))){
                if(item.getString("title").toLowerCase(Locale.ROOT).endsWith("lrc")){
                    Api.doGetMediaString(item.getString("hash"),lrcTextCallback);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
                if(ctrlBinder.getCtrl().getPlaybackState() == null)
                    return;
                if(ctrlBinder.getCtrl().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                    ctrlBinder.getCtrl().getTransportControls().pause();
                }else {
                    ctrlBinder.getCtrl().getTransportControls().play();
                }
            }
        });
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(ctrlBinder.getCurrentTitle().endsWith("mp4")){
                        startActivity(new Intent(WorkActivity.this,VideoPlayerActivity.class));
                    }else if(ctrlBinder.getCurrentTitle().endsWith("mp3")){

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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
//                setTitle(currentRow.content);
            }
        });
    }

    @Override
    public boolean onLongClick(View v) {
        if(downLoadDialog != null){
            downLoadDialog.show();
            return true;
        }
        JSONObject item = (JSONObject) v.getTag();
        try {
            if(jsonWorkTrees != null && work !=null){
                LocalFileCache.getInstance().saveWork(this,work,jsonWorkTrees);
            }
            String type = item.getString("type");
            String title = item.getString("title");
            if(!"audio".equals(type) && !"text".equals(type) && !"image".equals(type)){
                return true;
            }
            int id = work.getInt("id");
            String relativePath = workTreeAdapter.getRelativePath();
            File itemFile = LocalFileCache.getInstance().mapLocalItemFile(this,item,id,relativePath);
            if(itemFile == null)
                return true;
            AlertDialog.Builder builder= new AlertDialog.Builder(this);
            builder.setMessage(itemFile.getAbsolutePath());
            if(itemFile.exists()){
                final String streamUrl = item.getString("mediaStreamUrl");
                String currentPlayUrl = ctrlBinder.getCurrent().getString("mediaStreamUrl");
                builder.setTitle("已下载").setNegativeButton("打开", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(type.equals("audio")){
                            if(ctrlBinder.getCurrent()!=null && currentPlayUrl.equals(streamUrl)){

                            }else {
                                ctrlBinder.setReap();
                                ctrlBinder.setCurrentAlbumId(id);
                                try {
                                    item.put("local_file_path",itemFile.getAbsolutePath());
                                    ctrlBinder.play(Arrays.asList(item),0);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if(title.toLowerCase(Locale.ROOT).endsWith("mp4")){
                            startActivity(new Intent(WorkActivity.this,VideoPlayerActivity.class));
                        }else if(title.toLowerCase(Locale.ROOT).endsWith("lrc")){
//                                Api.doGetMediaString(hash,lrcTextCallback);
                        }else if(title.toLowerCase(Locale.ROOT).endsWith("mp3")){
                            startActivity(new Intent(WorkActivity.this, MusicPlayerActivity.class));
                        }
                    }
                });
            }else {
                final String downLoadUrl = item.getString("mediaDownloadUrl");
                builder.setTitle("未下载")
                        .setMessage(itemFile.getAbsolutePath())
                        .setNegativeButton("下载", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(downLoadDialog == null){
                                    AlertDialog.Builder builder = new AlertDialog.Builder(WorkActivity.this).setTitle("正在下载")
                                            .setNegativeButton("hide", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    downLoadDialog = builder.create();
                                    downLoadDialog.setMessage("开始下载");
                                    downLoadDialog.show();
                                }
                                LocalFileCache.getInstance().downLoadFile(itemFile,downLoadUrl,downloadCallback);
                            }
                        });
            }
            builder.create().show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    private AlertDialog downLoadDialog;
    private AsyncHttpClient.FileCallback downloadCallback = new AsyncHttpClient.FileCallback() {

        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, File file) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    downLoadDialog.setTitle("下载完成");
                    downLoadDialog.setMessage(file.getAbsolutePath());
                    downLoadDialog.show();
                    downLoadDialog = null;
                }
            });
        }

        @Override
        public void onProgress(AsyncHttpResponse response, long downloaded, long total) {
            super.onProgress(response, downloaded, total);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(downLoadDialog!=null)
                        downLoadDialog.setMessage(String.format(Locale.CHINA,"当前进度(%d / %d)",downloaded,total));
                }
            });
        }
    };

    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            int tagId = jsonObject.getInt("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPathChange(String path) {
        setTitle(path);
    }
}