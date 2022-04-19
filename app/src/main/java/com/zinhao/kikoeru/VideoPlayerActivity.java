package com.zinhao.kikoeru;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.json.JSONObject;

public class VideoPlayerActivity extends BaseActivity implements ServiceConnection,MusicChangeListener {
    private StyledPlayerView playerView;
    private AudioService.CtrlBinder ctrlBinder;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        playerView = findViewById(R.id.exoplayer);
        bindService(new Intent(this, AudioService.class),this,BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder)service;
        ctrlBinder.addMusicChangeListener(this);
        playerView.setPlayer(ctrlBinder.getExoPlayer());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        ctrlBinder.removeMusicChangeListener(VideoPlayerActivity.this);
    }

    @Override
    public void onAlbumChange(int rjNumber) {

    }

    @Override
    public void onAudioChange(JSONObject audio) {

    }

    @Override
    public void onStatusChange(int status) {

    }

    @Override
    protected boolean enableSlide() {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }
}
