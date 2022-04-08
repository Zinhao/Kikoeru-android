package com.zinhao.kikoeru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class WorkActivity extends AppCompatActivity implements View.OnClickListener,MusicChangeListener,ServiceConnection {
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
    private RecyclerView.OnScrollListener scrollListener;

    private View bottomLayout;
    private Animation outAnim;
    private Animation inAnim;
    private boolean shouldShowAnim = true;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvWorkTitle;
    private ImageButton ibStatus;
    private ImageButton ibCloseOrOpen;

    private Timer timer;

    private AsyncHttpClient.StringCallback apisCallback = new AsyncHttpClient.StringCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
            Log.d(TAG, "onCompleted: "+s);
            workTrees = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(s);
                for (int i = 0; i < jsonArray.length(); i++) {
                    workTrees.add(jsonArray.getJSONObject(i));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        workTreeAdapter = new WorkTreeAdapter(workTrees);
                        workTreeAdapter.setMusicClickListener(WorkActivity.this);
                        workTreeAdapter.setHeaderInfo(work);
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(ctrlBinder.getLrcText().equals(s)){
                        LrcShowActivity.start(WorkActivity.this,ctrlBinder.getLrcText(),true);
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
            }
        }
        recyclerView = findViewById(R.id.recyclerView);
        bottomLayout = findViewById(R.id.bottomLayout);
        ivCover = bottomLayout.findViewById(R.id.imageView);
        tvTitle = bottomLayout.findViewById(R.id.textView);
        tvWorkTitle = bottomLayout.findViewById(R.id.textView2);
        ibStatus = bottomLayout.findViewById(R.id.button);
        ibCloseOrOpen = bottomLayout.findViewById(R.id.imageButton);
        ibCloseOrOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBottom();
            }
        });

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
                if (dy > 0 && shouldShowAnim && bottomLayout.getVisibility() == View.VISIBLE){
                    toggleBottom();
                }else if(dy<0 && shouldShowAnim && bottomLayout.getVisibility() == View.GONE){
                    toggleBottom();
                }
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
        AsyncHttpRequest request = null;
        try {
            request = new AsyncHttpRequest(Uri.parse(MainActivity.HOST+String.format("/api/tracks/%d",work.getInt("id"))),"GET");
            AsyncHttpClient.getDefaultInstance().executeString(request, apisCallback);
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
    public void onClick(View v) {
        JSONObject item = (JSONObject) v.getTag();
        try {
            if("image".equals(item.getString("type"))){

            }else if("audio".equals(item.getString("type"))){
                List<JSONObject> musicArray = new ArrayList<>();
                int index = 0;
                workTreeAdapter.getData().forEach(new Consumer<JSONObject>() {
                    @Override
                    public void accept(JSONObject jsonObject) {
                        try {
                            if("audio".equals(jsonObject.getString("type"))){
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
                    ctrlBinder.setReap();
                    ctrlBinder.setCurrentAlbumId(work.getInt("id"));
                    ctrlBinder.play(musicArray,index);
                    if(item.getString("title").toLowerCase(Locale.ROOT).endsWith("mp4")){
                        startActivity(new Intent(this,VideoPlayerActivity.class));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if("text".equals(item.getString("type"))){
                if(item.getString("title").toLowerCase(Locale.ROOT).endsWith("lrc")){
                    AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(App.HOST+String.format("/media/stream/%s",item.getInt("hash"))),"GET");
                    AsyncHttpClient.getDefaultInstance().executeString(request, lrcTextCallback);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAlbumChange(int rjNumber) {
        Glide.with(this).load(MainActivity.HOST+String.format("/api/cover/%d?type=sam",rjNumber)).into(ivCover);
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
        unbindService(this);
        ctrlBinder.removeListener(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder)service;
        ctrlBinder.addListener(WorkActivity.this);
        ibStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(ctrlBinder.getCtrl().getPlaybackState() == null){
                    return;
                }
                if(ctrlBinder.getCtrl().getPlaybackState().getState()== PlaybackStateCompat.STATE_PLAYING){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setTitle(ctrlBinder.getLrcText());
                        }
                    });
                }
            }
        },200,200);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        timer.cancel();
    }
}