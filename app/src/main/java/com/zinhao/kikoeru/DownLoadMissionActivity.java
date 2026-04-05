package com.zinhao.kikoeru;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DownLoadMissionActivity extends BaseActivity implements Runnable {
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
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(DownLoadMissionActivity.this, DividerItemDecoration.VERTICAL);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem clear = menu.add(0,0,0,"清除已下载项目");
        clear.setIcon(R.drawable.ic_baseline_delete_forever_white_24);
        clear.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == 0){
            DownloadUtils.getInstance().removeCompleteMission();
            missionAdapter.notifyDataSetChanged();
        }
        return super.onOptionsItemSelected(item);
    }

    private void update() {
        recyclerView.postDelayed(this, 200);
    }

    @Override
    public void run() {
        if (isStop) {
            return;
        }
        for (int i = 0; i < DownloadUtils.getInstance().missionList.size(); i++) {
            DownloadUtils.Mission mission = DownloadUtils.getInstance().missionList.get(i);
            if (mission.isUpdate()) {
                missionAdapter.notifyItemChanged(i);
                mission.setUpdate(false);
            }
        }
        update();
    }
}