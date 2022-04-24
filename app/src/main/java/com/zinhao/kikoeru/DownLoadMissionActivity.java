package com.zinhao.kikoeru;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

public class DownLoadMissionActivity extends BaseActivity implements Runnable{
    private RecyclerView recyclerView;
    private MissionAdapter missionAdapter;
    private boolean isStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_down_load_mission);
        recyclerView = findViewById(R.id.recyclerView);
        missionAdapter = new MissionAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(missionAdapter);
        recyclerView.setItemAnimator(null);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(DownLoadMissionActivity.this,DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
    }

    @Override
    protected void onStart() {
        super.onStart();
        isStop = false;
        update();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStop = true;
    }

    private void update(){
        recyclerView.postDelayed(this,200);
    }

    @Override
    public void run() {
        if(isStop){
           return;
        }
        for (int i = 0; i < DownloadUtils.getInstance().missionList.size(); i++) {
            DownloadUtils.Mission mission = DownloadUtils.getInstance().missionList.get(i);
            if(mission.isUpdate()){
                missionAdapter.notifyItemChanged(i);
                mission.setUpdate(false);
            }
        }
        update();
    }
}