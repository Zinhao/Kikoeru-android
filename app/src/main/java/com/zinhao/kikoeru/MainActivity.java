package com.zinhao.kikoeru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
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
import com.google.android.exoplayer2.Player;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MusicChangeListener {
    private static final String TAG = "MainActivity";
    public static final String HOST = "http://192.168.1.47:8888";
    private RecyclerView recyclerView;
    private WorkAdapter workAdapter;
    private List<JSONObject> works;
    private RecyclerView.OnScrollListener scrollListener;
    private int page = 1;
    private int totalCount = 0;
    private int retryCount =0;

    private View bottomLayout;
    private Animation outAnim;
    private Animation inAnim;
    private boolean shouldShowAnim = true;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvWorkTitle;
    private ImageButton ibStatus;
    private ImageButton ibCloseOrOpen;

    private AsyncHttpClient.StringCallback apisCallback = new AsyncHttpClient.StringCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
            Log.d(TAG, "onCompleted: "+s);
            if(asyncHttpResponse == null || asyncHttpResponse.code() !=200){
                Log.d(TAG, String.format("onCompleted:failed! "));
                ivCover.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        retryCount++;
                        setTitle(String.format("retry: %d",retryCount));
                        getNextPage();
                    }
                },3000);
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(s);
                JSONArray jsonArray = jsonObject.getJSONArray("works");
                totalCount = jsonObject.getJSONObject("pagination").getInt("totalCount");
                page = jsonObject.getJSONObject("pagination").getInt("currentPage") +1;
                page = Math.min(page,totalCount/12 + 1);
                for (int i = 0; i < jsonArray.length(); i++) {
                    works.add(jsonArray.getJSONObject(i));
                }
                runOnUiThread(new Runnable() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void run() {
                        setTitle(String.format("下次加载第%d页 ,当前已加载%d项",page,works.size()));
                        if(workAdapter == null){
                            workAdapter = new WorkAdapter(works);
                            recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this,2));
                            recyclerView.setAdapter(workAdapter);
                            recyclerView.addOnScrollListener(scrollListener);
                        }else {
                            workAdapter.notifyItemInserted(works.size()-jsonArray.length());
                        }

                    }
                });
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }

        }
    };
    private AudioService.CtrlBinder ctrlBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        startService(new Intent(this,AudioService.class));
        bindService(new Intent(this, AudioService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ctrlBinder = (AudioService.CtrlBinder)service;
                ctrlBinder.addListener(MainActivity.this);
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
                                startActivity(new Intent(MainActivity.this,VideoPlayerActivity.class));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        },BIND_AUTO_CREATE);
        outAnim = AnimationUtils.loadAnimation(this,R.anim.move_bottom_out);
        inAnim = AnimationUtils.loadAnimation(this,R.anim.move_bottom_in);
        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(works.size()>=totalCount){
                    return;
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!recyclerView.canScrollVertically(1)) {
                        getNextPage();
                    }
                }
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
        works = new ArrayList<>();
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST+"/api/works?order=id&sort=desc&page=1&seed=35"),"GET");
        AsyncHttpClient.getDefaultInstance().executeString(request, apisCallback);
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

    public void getNextPage() {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST+String.format("/api/works?order=release&sort=desc&page=%d&seed=35",page)),"GET");
        Log.d(TAG, "getNextPage: "+request.getUri());
        request.setTimeout(5000);
        AsyncHttpClient.getDefaultInstance().executeString(request, apisCallback);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onAlbumChange(int rjNumber) {
        Glide.with(this).load(HOST+String.format("/api/cover/%d?type=sam",rjNumber)).into(ivCover);
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
        ctrlBinder.removeListener(this);
    }
}