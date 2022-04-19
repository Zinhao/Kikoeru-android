package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements MusicChangeListener,ServiceConnection,LrcRowChangeListener,TagsView.TagClickListener<JSONObject> {
    private static final String TAG = "MainActivity";
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
    private int tagId = -1;
    private String tagStr = "";

    private static final int TYPE_ALL_WORK = 491;
    private static final int TYPE_SELF_LISTENING = 492;
    private static final int TYPE_SELF_LISTENED = 493;
    private static final int TYPE_SELF_MARKED = 494;
    private static final int TYPE_SELF_REPLAY = 495;
    private static final int TYPE_SELF_POSTPONED = 496;
    private static final int TYPE_TAG_WORK = 497;
    private static final int TYPE_LOCAL_WORK = 498;
    private int type = TYPE_ALL_WORK;

    private AsyncHttpClient.JSONObjectCallback apisCallback = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            if(e!=null){
                alertException(e);
                return;
            }
            if(asyncHttpResponse == null || asyncHttpResponse.code() !=200){
                if(jsonObject != null && jsonObject.has("works")){
                    Log.d(TAG, "onCompleted: load local cache!");
                }else {
                    Log.d(TAG, String.format("onCompleted:failed! "));
                    if(!isDestroyed()){
                        ivCover.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                retryCount++;
                                getNextPage();
                            }
                        },3000);
                    }
                    return;
                }
            }
            try {
                JSONArray jsonArray = jsonObject.getJSONArray("works");
                totalCount = jsonObject.getJSONObject("pagination").getInt("totalCount");
                page = jsonObject.getJSONObject("pagination").getInt("currentPage") +1;

                if(jsonArray.length() != 0){
                    page = Math.min(page,totalCount/jsonArray.length() + 1);
                }
                runOnUiThread(new Runnable() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void run() {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                works.add(jsonArray.getJSONObject(i));
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                                alertException(jsonException);
                            }
                        }
                        if(workAdapter == null){
                            initLayout((int) App.getInstance().getValue(App.CONFIG_LAYOUT_TYPE,WorkAdapter.LAYOUT_SMALL_GRID));
                            recyclerView.addOnScrollListener(scrollListener);
                        }else {
                            workAdapter.notifyItemRangeInserted(Math.max(0,works.size() - jsonArray.length()),jsonArray.length());
                            workAdapter.notifyItemRangeChanged(Math.max(0,works.size() - jsonArray.length()),jsonArray.length());
                        }
                    }
                });
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
                alertException(jsonException);
            }
        }
    };
    private AudioService.CtrlBinder ctrlBinder;
    private static final int TAG_SELECT_RESULT = 14;

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
        startService(new Intent(this,AudioService.class));
        bindService(new Intent(this, AudioService.class), this,BIND_AUTO_CREATE);
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
            }
        };
        works = new ArrayList<>();
        getNextPage();
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
        if(type == TYPE_ALL_WORK){
            setTitle(getString(R.string.app_name));
            Api.doGetWorks(page,apisCallback);
        }else if(type == TYPE_SELF_LISTENING){
            setTitle("在听");
            Api.doGetReview(Api.FILTER_LISTENING,page,apisCallback);
        }else if(type == TYPE_SELF_LISTENED){
            setTitle("听过");
            Api.doGetReview(Api.FILTER_LISTENED,page,apisCallback);
        }else if(type == TYPE_SELF_MARKED){
            setTitle("标记");
            Api.doGetReview(Api.FILTER_MARKED,page,apisCallback);
        }else if(type == TYPE_SELF_REPLAY){
            setTitle("重听");
            Api.doGetReview(Api.FILTER_REPLAY,page,apisCallback);
        }else if(type == TYPE_SELF_POSTPONED){
            setTitle("推迟");
            Api.doGetReview(Api.FILTER_POSTPONED,page,apisCallback);
        }else if(type == TYPE_TAG_WORK){
            setTitle(tagStr);
            Api.doGetWorksByTag(page,tagId,apisCallback);
        }else if(type == TYPE_LOCAL_WORK){
            setTitle("本地緩存");
            try {
                LocalFileCache.getInstance().readLocalWorks(this,apisCallback);
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onAlbumChange(int rjNumber) {
        if(rjNumber!=0 && bottomLayout.getVisibility() == View.GONE){
            toggleBottom();
        }
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
            ibStatus.setImageResource(R.drawable.ic_baseline_play_arrow_white_24);
        }else {
            ibStatus.setImageResource(R.drawable.ic_baseline_pause_white_24);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder)service;
        ibStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ctrlBinder.getCtrl().getPlaybackState() == null){
                    ctrlBinder.getCtrl().getTransportControls().play();
                    return;
                }
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
                    }else {
                        startActivity(new Intent(MainActivity.this,MusicPlayerActivity.class));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem1 = menu.add(0,0,0,"sign out");
        SubMenu menuProgress = menu.addSubMenu(0,1,1,"progress");
        menuProgress.setIcon(R.drawable.ic_baseline_favorite_24);
        MenuItem menuItem3 =menu.add(0,2,2,"about");
        MenuItem menuItem4 =menu.add(1,3,3,"work");
        menuItem4.setIcon(R.drawable.ic_baseline_widgets_24);
        menuItem4.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuProgress.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menuProgress.add(1,4,4,Api.FILTER_MARKED);
        menuProgress.add(1,5,5,Api.FILTER_LISTENING);
        menuProgress.add(1,6,6,Api.FILTER_LISTENED);
        menuProgress.add(1,7,7,Api.FILTER_REPLAY);
        menuProgress.add(1,8,8,Api.FILTER_POSTPONED);

        SubMenu layoutMenu = menu.addSubMenu(0,9,9,"layout");
        layoutMenu.setIcon(R.drawable.ic_baseline_view_column_24);
        layoutMenu.add(2,10,10,"list");
        layoutMenu.add(2,11,11,"small gird");
        layoutMenu.add(2,12,12,"gird");
        layoutMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem tagMenu = menu.add(0,13,13,"tags");
        tagMenu.setIcon(R.drawable.ic_baseline_tag_24);
        tagMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem localSaveMenu = menu.add(0,14,14,"local");
        localSaveMenu.setIcon(R.drawable.ic_baseline_storage_24);
        localSaveMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add(0,15,15,"setting");

        SubMenu sortMenu = menu.addSubMenu(0,16,16,"排序");
        sortMenu.add(3,17,17,"发布时间");
        sortMenu.add(3,18,18,"RJ号码");
        sortMenu.add(3,19,19,"价格");
        sortMenu.add(3,20,20,"最新收录");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getGroupId() == 1){
            boolean needGetNextPage = false;
            if(item.getItemId() == 4){
                if(type != TYPE_SELF_MARKED){
                    type = TYPE_SELF_MARKED;
                    needGetNextPage =true;
                }
            }else if(item.getItemId() == 5){
                if(type != TYPE_SELF_LISTENING){
                    type = TYPE_SELF_LISTENING;
                    needGetNextPage =true;
                }
            }else if(item.getItemId() == 6){
                if(type != TYPE_SELF_LISTENED){
                    type = TYPE_SELF_LISTENED;
                    needGetNextPage =true;
                }
            }else if(item.getItemId() == 7){
                if(type != TYPE_SELF_REPLAY){
                    type = TYPE_SELF_REPLAY;
                    needGetNextPage =true;
                }
            }else if(item.getItemId() == 8){
                if(type != TYPE_SELF_POSTPONED){
                    type = TYPE_SELF_POSTPONED;
                    needGetNextPage =true;
                }
            }else if (item.getItemId() == 3){
                if(type != TYPE_ALL_WORK){
                    type = TYPE_ALL_WORK;
                    needGetNextPage =true;
                }
            }
            if(needGetNextPage){
                clearWork();
                getNextPage();
            }
            return super.onOptionsItemSelected(item);
        }

        if(item.getGroupId() ==2){
            int layoutType = WorkAdapter.LAYOUT_SMALL_GRID;
            if(item.getItemId() == 10){
                layoutType = WorkAdapter.LAYOUT_LIST;
            }else if(item.getItemId() == 12){
                layoutType = WorkAdapter.LAYOUT_BIG_GRID;
            }
            App.getInstance().setValue(App.CONFIG_LAYOUT_TYPE,layoutType);
            initLayout(layoutType);
            return super.onOptionsItemSelected(item);
        }

        if(item.getGroupId() == 3){
            boolean update = false;
            if(item.getItemId() == 17){
                Api.setOrder("release");
                update = true;
            }else if(item.getItemId() == 18){
                Api.setOrder("id");
                update = true;
            }else if(item.getItemId() == 19){
                Api.setOrder("price");
                update = true;
            }else if(item.getItemId() == 20){
                Api.setOrder("create_date");
                update = true;
            }
            if(update){
                clearWork();
                getNextPage();
            }
            return true;
        }

        if(item.getItemId() == 0){
            App.getInstance().setValue(App.CONFIG_UPDATE_TIME,0);
            startActivity(new Intent(this,LoginAccountActivity.class));
            finish();
        }else if(item.getItemId() == 1){
        }else if(item.getItemId() == 2){
            startActivity(new Intent(this,AboutActivity.class));
        }else if(item.getItemId() == 13){
            startActivityForResult(new Intent(this,TagsActivity.class),TAG_SELECT_RESULT);
        }else if(item.getItemId() == 14){
            if(type != TYPE_LOCAL_WORK){
                type = TYPE_LOCAL_WORK;
                clearWork();
                getNextPage();
            }
        }else if(item.getItemId() == 15){
            startActivity(new Intent(this,SettingActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == TAG_SELECT_RESULT){
            if(resultCode == RESULT_OK && data!=null){
                int tagId = data.getIntExtra("id",-1);
                type = TYPE_TAG_WORK;
                if(tagId != this.tagId){
                    tagStr = data.getStringExtra("name");
                    clearWork();
                    this.tagId = tagId;
                }
                getNextPage();
            }
        }
    }

    private void initLayout(int layoutType){
        RecyclerView.LayoutManager layoutManager = null;
        if(layoutType == WorkAdapter.LAYOUT_LIST){
            layoutManager = new LinearLayoutManager(MainActivity.this);
        }else if( layoutType == WorkAdapter.LAYOUT_SMALL_GRID){
            layoutManager=new GridLayoutManager(MainActivity.this,3);
        }else if(layoutType == WorkAdapter.LAYOUT_BIG_GRID){
            layoutManager = new GridLayoutManager(MainActivity.this,2);
        }
        workAdapter = new WorkAdapter(works,layoutType);
        workAdapter.setTagClickListener(this);
        workAdapter.setItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject item = (JSONObject) v.getTag();
                Intent intent = new Intent(v.getContext(),WorkActivity.class);
                intent.putExtra("work_json_str",item.toString());
                ActivityCompat.startActivityForResult(MainActivity.this,intent,TAG_SELECT_RESULT,null);
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(workAdapter);
    }

    private void clearWork(){
        page = 1;
        if(workAdapter == null)
            return;
        workAdapter.notifyItemRangeRemoved(0,works.size());
        workAdapter.notifyItemRangeChanged(0,works.size());
        works.clear();
    }

    @Override
    protected void onDestroy() {
        ctrlBinder.removeMusicChangeListener(this);
        ctrlBinder.removeLrcRowChangeListener(this);
        unbindService(this);
        super.onDestroy();
    }


    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            int tagId = jsonObject.getInt("id");
            Log.d(TAG, "onTagClick: "+ tagId);
            type = TYPE_TAG_WORK;
            if(tagId != this.tagId){
                tagStr = jsonObject.getString("name");
                clearWork();
                this.tagId = tagId;
            }
            getNextPage();
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }
}