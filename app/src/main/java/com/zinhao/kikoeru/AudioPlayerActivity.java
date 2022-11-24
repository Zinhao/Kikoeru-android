package com.zinhao.kikoeru;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.Player;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AudioPlayerActivity extends BaseActivity implements ServiceConnection,MusicChangeListener,LrcRowChangeListener, SeekBar.OnSeekBarChangeListener {
    private AudioService.CtrlBinder ctrlBinder;
    private ImageView imageView;
    private RequestOptions options;
    private ImageButton ibPrevious;
    private ImageButton ibPause;
    private ImageButton ibNext;
    private ImageButton ibWork;
    private ImageButton ibLoop;
    private TextView tvLrc;
    private TextView tvUpLrc;
    private TextView tvNextLrc;
    private TimeProgressView timeProgressView;
    private boolean needShowLrcWhenDestroy = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        imageView = findViewById(R.id.ivCover);
        RoundedCorners roundedCorners = new RoundedCorners(20);
        options = RequestOptions.bitmapTransform(roundedCorners);
        ibPrevious = findViewById(R.id.ib1);
        ibPause = findViewById(R.id.ib2);
        ibNext = findViewById(R.id.ib3);
        ibWork = findViewById(R.id.imageButton2);
        ibLoop = findViewById(R.id.ibLoop);
        tvLrc = findViewById(R.id.tvLrc);
        tvUpLrc = findViewById(R.id.tvUpLrc);
        tvNextLrc = findViewById(R.id.tvNextLrc);
        timeProgressView = findViewById(R.id.time_view);
        timeProgressView.setColor(ContextCompat.getColor(this,R.color.play_control_icon_color));
        ibPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ctrlBinder ==null)
                    return;
                if(ctrlBinder.getController() == null || ctrlBinder.getController().getTransportControls() == null)
                    return;
                PlaybackStateCompat playbackStateCompat = ctrlBinder.getController().getPlaybackState();
                if(playbackStateCompat != null && playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING){
                    ctrlBinder.getController().getTransportControls().pause();
                }else {
                    ctrlBinder.getController().getTransportControls().play();
                }

            }
        });
        ibPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ctrlBinder ==null)
                    return;
                if(ctrlBinder.getController() == null || ctrlBinder.getController().getTransportControls() == null)
                    return;
                ctrlBinder.getController().getTransportControls().skipToPrevious();
            }
        });
        ibNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ctrlBinder ==null)
                    return;
                if(ctrlBinder.getController() == null || ctrlBinder.getController().getTransportControls() == null)
                    return;
                ctrlBinder.getController().getTransportControls().skipToNext();
            }
        });
        ibLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ctrlBinder ==null)
                    return;
                if(ctrlBinder.getController() == null || ctrlBinder.getController().getTransportControls() == null)
                    return;
                if(ctrlBinder.getReapMode() == Player.REPEAT_MODE_ONE){
                    ctrlBinder.setReapAll();
                }else if(ctrlBinder.getReapMode() == Player.REPEAT_MODE_ALL){
                    ctrlBinder.setReapOff();
                }else if(ctrlBinder.getReapMode() == Player.REPEAT_MODE_OFF){
                    ctrlBinder.setReapOne();
                }
                if(ctrlBinder.getReapMode() == Player.REPEAT_MODE_ONE){
                    ibLoop.setImageResource(R.drawable.ic_baseline_flip_camera_android_24);
                }else if(ctrlBinder.getReapMode() == Player.REPEAT_MODE_ALL){
                    ibLoop.setImageResource(R.drawable.ic_baseline_loop_24);
                }else if(ctrlBinder.getReapMode() == Player.REPEAT_MODE_OFF){
                    ibLoop.setImageResource(R.drawable.ic_baseline_close_24);
                }

            }
        });
        timeProgressView.setOnSeekBarChangeListener(this);

        bindService(new Intent(this,AudioService.class),this,BIND_AUTO_CREATE);
    }

    private AsyncHttpClient.JSONObjectCallback searchWorkCallback = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            if(e!= null){
                alertException(e);
                return;
            }
            if(asyncHttpResponse.code() == 200){
                try {
                    int totalCount = jsonObject.getJSONObject("pagination").getInt("totalCount");
                    if(totalCount<1)
                        return;
                    JSONArray works = jsonObject.getJSONArray("works");
                    if(works.length() != 0){
                        JSONObject item = works.getJSONObject(0);
                        Intent intent = new Intent(AudioPlayerActivity.this, WorkTreeActivity.class);
                        intent.putExtra("work_json_str", item.toString());
                        ActivityCompat.startActivity(AudioPlayerActivity.this, intent,null);
                    }
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }

            }
        }
    };

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder) service;
        ctrlBinder.addMusicChangeListener(this);
        ctrlBinder.addLrcRowChangeListener(this);
        if(ctrlBinder.isLrcWindowShow()){
            needShowLrcWhenDestroy = true;
            ctrlBinder.showOrHideLrcFloatWindow();
        }
        ibWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Api.doGetWork(String.valueOf(ctrlBinder.getCurrentAlbumId()),1,searchWorkCallback);
            }
        });
        timeProgressView.setMax((int) ctrlBinder.getExoPlayer().getDuration());
        updateSeek();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onChange(Lrc.LrcRow currentRow) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvUpLrc.setText(currentRow.getUpRow().content);
                tvLrc.setText(currentRow.content);
                tvNextLrc.setText(currentRow.getNextRow().content);
            }
        });
    }

    @Override
    public void onAlbumChange(int rjNumber) {
        Glide.with(this).load(Api.HOST+String.format("/api/cover/%d?token=%s",rjNumber,Api.token)).apply(options).into(imageView);
    }

    @Override
    public void onAudioChange(JSONObject audio) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    timeProgressView.setMax((int) ctrlBinder.getExoPlayer().getDuration());
                    setTitle(ctrlBinder.getCurrentTitle());
                } catch (JSONException e) {
                    e.printStackTrace();
                    alertException(e);
                }
            }
        });
    }

    @Override
    public void onStatusChange(int status) {
        timeProgressView.setMax((int) ctrlBinder.getExoPlayer().getDuration());
        if(status == 0){
            ibPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }else {
            ibPause.setImageResource(R.drawable.ic_baseline_pause_24);
        }
    }

    private void updateSeek(){
        timeProgressView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(ctrlBinder!=null && ctrlBinder.getExoPlayer()!=null && ctrlBinder.getExoPlayer().isPlaying()){
                    long current = ctrlBinder.getExoPlayer().getCurrentPosition();
                    timeProgressView.setProgress((int) current);
                }
                if(!isDestroyed())
                    updateSeek();
            }
        }, 500);
    }

    @Override
    protected void onDestroy() {
        if(ctrlBinder!=null){
            ctrlBinder.removeLrcRowChangeListener(this);
            ctrlBinder.removeMusicChangeListener(this);
            unbindService(this);
            if(!ctrlBinder.isLrcWindowShow() && needShowLrcWhenDestroy)
                startActivity(new Intent(this,LrcFloatWindow.class));
        }
        super.onDestroy();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser){
            ctrlBinder.getController().getTransportControls().seekTo(progress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu subMenu = menu.addSubMenu(0,0,0, "stop after");
        subMenu.add(1,1,1, "30 minutes");
        subMenu.add(1,2,2, "60 minutes");
        subMenu.add(1,3,3, "90 minutes");
        subMenu.add(1,4,4, "120 minutes");
        subMenu.add(1,5,5, "240 minutes");
        subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            if(item.getItemId() == 1){
                ctrlBinder.stopAfterMinutes(30);
            }else if(item.getItemId() == 2){
                ctrlBinder.stopAfterMinutes(60);
            }else if(item.getItemId() == 3){
                ctrlBinder.stopAfterMinutes(90);
            }else if(item.getItemId() == 4){
                ctrlBinder.stopAfterMinutes(120);
            }else if(item.getItemId() == 5){
                ctrlBinder.stopAfterMinutes(240);
            }
        }catch (Exception e){

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
