package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class LrcShowActivity extends AppCompatActivity implements ServiceConnection,LrcRowChangeListener {
    private boolean isCurrent;

    public static void start(Context context, String lrc, boolean isCurrent) {
        Intent starter = new Intent(context, LrcShowActivity.class);
        starter.putExtra("lrc_text",lrc);
        starter.putExtra("is_current",isCurrent);
        context.startActivity(starter);
    }

    private RecyclerView mRecyclerView;
    private Lrc mLrc;
    private LrcAdapter adapter;
    private AudioService.CtrlBinder ctrlBinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lrc_show);
        mRecyclerView = findViewById(R.id.recyclerView);
        isCurrent = getIntent().getBooleanExtra("is_current",false);
        if(isCurrent){
            bindService(new Intent(this, AudioService.class),this,BIND_AUTO_CREATE);
        }else{
            String lrcText = getIntent().getStringExtra("lrc_text");
            if(lrcText!=null && !lrcText.isEmpty()){
                mLrc = new Lrc(lrcText);
            }else {
                finish();
                return;
            }
            adapter = new LrcAdapter(mLrc);
            mRecyclerView.setAdapter(adapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isCurrent){
            unbindService(this);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        ctrlBinder = (AudioService.CtrlBinder)iBinder;
        mLrc = ctrlBinder.getLrc();
        adapter = new LrcAdapter(mLrc);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ctrlBinder.addLrcRowChangeListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        ctrlBinder.removeLrcRowChangeListener(this);
    }

    @Override
    public void onChange(Lrc.LrcRow currentRow) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(currentRow.content);
                adapter.notifyDataSetChanged();
            }
        });
    }
}
