package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class LrcShowActivity extends AppCompatActivity {

    private Timer timer;

    public static void start(Context context, String lrc, boolean isCurrent) {
        Intent starter = new Intent(context, LrcShowActivity.class);
        starter.putExtra("lrc_text",lrc);
        starter.putExtra("is_current",isCurrent);
        context.startActivity(starter);
    }

    private RecyclerView mRecyclerView;
    private Lrc mLrc;
    private LrcAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lrc_show);
        mRecyclerView = findViewById(R.id.recyclerView);
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
        boolean isCurrent = getIntent().getBooleanExtra("is_current",false);
        if(isCurrent){
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            },200,200);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timer!=null)
            timer.cancel();
    }
}
