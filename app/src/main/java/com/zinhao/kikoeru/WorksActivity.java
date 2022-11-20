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
import android.view.SubMenu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.MenuPopupWindow;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.ListPopupWindowCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
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
import java.util.Arrays;
import java.util.List;

public class WorksActivity extends BaseActivity implements MusicChangeListener,ServiceConnection,LrcRowChangeListener,TagsView.TagClickListener<JSONObject> {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private WorkAdapter workAdapter;
    private List<JSONObject> works;
    private RecyclerView.OnScrollListener scrollListener;
    private int page = 1;
    private int totalCount = 0;

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
    private String vaId  = "";
    private String vaName= "";

    private static final int TYPE_ALL_WORK = 491;
    private static final int TYPE_SELF_LISTENING = 492;
    private static final int TYPE_SELF_LISTENED = 493;
    private static final int TYPE_SELF_MARKED = 494;
    private static final int TYPE_SELF_REPLAY = 495;
    private static final int TYPE_SELF_POSTPONED = 496;
    private static final int TYPE_TAG_WORK = 497;
    private static final int TYPE_LOCAL_WORK = 498;
    private static final int TYPE_VA_WORK = 499;

    private int type = TYPE_ALL_WORK;
    private AudioService.CtrlBinder ctrlBinder;
    private static final int TAG_SELECT_RESULT = 14;
    private static final int VA_SELECT_RESULT = 15;

    private ImageButton bt1;
    private ImageButton bt2;
    private ImageButton bt3;

    private ListPopupWindow progressMenu;
    private ListPopupWindow moreMenu;
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
        bt1 = findViewById(R.id.bt1);
        bt2 = findViewById(R.id.bt2);
        bt3 = findViewById(R.id.bt3);
        dividerItemDecoration =  new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        startForegroundService(new Intent(this,AudioService.class));
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
                        reloadRecycleView();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        };
        works = new ArrayList<>();
        reloadRecycleView();

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = TYPE_ALL_WORK;
                clearWork();
                reloadRecycleView();
            }
        });
        progressMenu = new ListPopupWindow(this);
        progressMenu.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                Arrays.asList(getString(R.string.marked),
                        getString(R.string.listening),
                        getString(R.string.listened),
                        getString(R.string.replay),
                        getString(R.string.postponed))));
        progressMenu.setModal(true);
        progressMenu.setAnchorView(bt2);
        progressMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        type = TYPE_SELF_MARKED;
                        break;
                    case 1:
                        type = TYPE_SELF_LISTENING;
                        break;
                    case 2:
                        type = TYPE_SELF_LISTENED;
                        break;
                    case 3:
                        type = TYPE_SELF_REPLAY;
                        break;
                    case 4:
                        type = TYPE_SELF_POSTPONED;
                        break;
                }
                clearWork();
                reloadRecycleView();
            }
        });
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressMenu.show();
            }
        });

        moreMenu = new ListPopupWindow(this);
        moreMenu.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                Arrays.asList(getString(R.string.va_voicer),
                        getString(R.string.tag),
                        getString(R.string.local_works))));
        moreMenu.setModal(true);
        moreMenu.setAnchorView(bt3);
        moreMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        startActivityForResult(new Intent(view.getContext(),VasActivity.class),VA_SELECT_RESULT);
                        break;
                    case 1:
                        startActivityForResult(new Intent(view.getContext(),TagsActivity.class),TAG_SELECT_RESULT);
                        break;
                    case 2:
                        type = TYPE_LOCAL_WORK;
                        clearWork();
                        reloadRecycleView();
                        break;
                }
            }
        });
        bt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreMenu.show();
            }
        });
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

    public void reloadRecycleView() {
        if(type == TYPE_ALL_WORK){
            setTitle(getString(R.string.app_name));
            Api.doGetWorks(page,apisCallback);
        }else if(type == TYPE_SELF_LISTENING){
            setTitle(R.string.listening);
            Api.doGetReview(Api.FILTER_LISTENING,page,apisCallback);
        }else if(type == TYPE_SELF_LISTENED){
            setTitle(R.string.listened);
            Api.doGetReview(Api.FILTER_LISTENED,page,apisCallback);
        }else if(type == TYPE_SELF_MARKED){
            setTitle(R.string.marked);
            Api.doGetReview(Api.FILTER_MARKED,page,apisCallback);
        }else if(type == TYPE_SELF_REPLAY){
            setTitle(R.string.replay);
            Api.doGetReview(Api.FILTER_REPLAY,page,apisCallback);
        }else if(type == TYPE_SELF_POSTPONED){
            setTitle(R.string.postponed);
            Api.doGetReview(Api.FILTER_POSTPONED,page,apisCallback);
        }else if(type == TYPE_TAG_WORK){
            setTitle(tagStr);
            Api.doGetWorksByTag(page,tagId,apisCallback);
        }else if(type == TYPE_VA_WORK){
            setTitle(vaName);
            Api.doGetWorkByVa(page,vaId,apisCallback);
        }else if(type == TYPE_LOCAL_WORK){
            setTitle(String.format("%s",App.getInstance().isSaveExternal()?"外部公共目录":"内部私有目录"));
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
                if(ctrlBinder.getController().getPlaybackState() == null){
                    ctrlBinder.getController().getTransportControls().play();
                    return;
                }
                if(ctrlBinder.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                    ctrlBinder.getController().getTransportControls().pause();
                }else {
                    ctrlBinder.getController().getTransportControls().play();
                }
            }
        });
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(ctrlBinder.getCurrentTitle().endsWith("mp4")){
                        startActivity(new Intent(WorksActivity.this,VideoPlayerActivity.class));
                    }else {
                        startActivity(new Intent(WorksActivity.this,MusicPlayerActivity.class));
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
        menu.add(0,0,0, "切换账号");

        SubMenu layoutMenu = menu.addSubMenu(0,9,9, R.string.works_layout);
        layoutMenu.setIcon(R.drawable.ic_baseline_view_column_24);
        layoutMenu.add(2,10,10, R.string.list_layout);
        layoutMenu.add(2,11,11, R.string.cover_layout);
        layoutMenu.add(2,12,12, R.string.detail_layout);
        layoutMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0,15,15,R.string.more);

        SubMenu sortMenu = menu.addSubMenu(0,16,16, R.string.sort);
        sortMenu.add(3,17,17, R.string.release_date);
        sortMenu.add(3,18,18, R.string.rj_number);
        sortMenu.add(3,19,19, R.string.prize);
        sortMenu.add(3,20,20, R.string.last_in_lib);

        menu.add(0,22,22, R.string.download_mission);
        MenuItem searchMenu = menu.add(0,23,23, R.string.search);
        searchMenu.setIcon(R.drawable.ic_baseline_search_24);
        searchMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
     return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
                reloadRecycleView();
            }
            return true;
        }

        if(item.getItemId() == 0){
            App.getInstance().setValue(App.CONFIG_UPDATE_TIME,0);
            startActivity(new Intent(this,UserSwitchActivity.class));
        }else if(item.getItemId() == 1){
        }else if(item.getItemId() == 15){
            startActivity(new Intent(this, MoreActivity.class));
        }else if(item.getItemId() == 21){
            startActivityForResult(new Intent(this,VasActivity.class),VA_SELECT_RESULT);
        }else if(item.getItemId() == 22){
            startActivity(new Intent(this,DownLoadMissionActivity.class));
        }else if(item.getItemId() == 23){
            startActivity(new Intent(this,SearchActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == TAG_SELECT_RESULT || requestCode == VA_SELECT_RESULT){
            if(resultCode == RESULT_OK && data!=null){
                String resultType = data.getStringExtra("resultType");
                if(resultType == null){
                    return;
                }
                if(resultType.equals("va")){
                    String vaId = data.getStringExtra("id");
                    type = TYPE_VA_WORK;
                    if(!vaId.equals(this.vaId)){
                        vaName = data.getStringExtra("name");
                        clearWork();
                        this.vaId = vaId;
                    }
                }else if(resultType.equals("tag")){
                    int tagId = data.getIntExtra("id",-1);
                    type = TYPE_TAG_WORK;
                    if(tagId != this.tagId){
                        tagStr = data.getStringExtra("name");
                        clearWork();
                        this.tagId = tagId;
                    }
                }
                reloadRecycleView();
            }
        }
    }

    private DividerItemDecoration dividerItemDecoration;
    private void initLayout(int layoutType) {
        RecyclerView.LayoutManager layoutManager = null;
        if (layoutType == WorkAdapter.LAYOUT_LIST) {
            layoutManager = new LinearLayoutManager(WorksActivity.this);
            recyclerView.addItemDecoration(dividerItemDecoration);
        } else if (layoutType == WorkAdapter.LAYOUT_SMALL_GRID) {
            layoutManager = new GridLayoutManager(WorksActivity.this, 3);
            recyclerView.removeItemDecoration(dividerItemDecoration);
        } else if (layoutType == WorkAdapter.LAYOUT_BIG_GRID) {
            layoutManager = new GridLayoutManager(WorksActivity.this, 2);
            recyclerView.removeItemDecoration(dividerItemDecoration);
        }
        workAdapter = new WorkAdapter(works, layoutType);
        workAdapter.setTagClickListener(this);
        workAdapter.setVaClickListener(vaClickListener);
        workAdapter.setItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject item = (JSONObject) v.getTag();
                Intent intent = new Intent(v.getContext(), WorkTreeActivity.class);
                intent.putExtra("work_json_str", item.toString());
                ActivityCompat.startActivity(WorksActivity.this, intent,null);
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

        PlaybackStateCompat playbackStateCompat = ctrlBinder.getController().getPlaybackState();
        if(playbackStateCompat == null){
            stopService(new Intent(this,AudioService.class));
        }else {
            int state = ctrlBinder.getController().getPlaybackState().getState();
            if(state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_PAUSED){
                stopService(new Intent(this,AudioService.class));
            }
        }
        unbindService(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String resultType = intent.getStringExtra("resultType");
        if("va".equals(resultType)){
            String vaId = intent.getStringExtra("id");
            if(!vaId.equals(this.vaId)|| type != TYPE_VA_WORK){
                vaName = intent.getStringExtra("name");
                clearWork();
                this.vaId = vaId;
            }
            type = TYPE_VA_WORK;
        }else if("tag".equals(resultType)){
            int tagId = intent.getIntExtra("id",-1);
            if(tagId != this.tagId || type != TYPE_TAG_WORK){
                tagStr = intent.getStringExtra("name");
                clearWork();
                this.tagId = tagId;
            }
            type = TYPE_TAG_WORK;
        }else {
            type = TYPE_ALL_WORK;
            clearWork();
        }
        reloadRecycleView();
    }

    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            int tagId = jsonObject.getInt("id");
            Log.d(TAG, "onTagClick: "+ tagId);
            if(tagId != this.tagId || type != TYPE_TAG_WORK){
                tagStr = jsonObject.getString("name");
                clearWork();
                this.tagId = tagId;
            }
            type = TYPE_TAG_WORK;
            reloadRecycleView();
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }

    private final TagsView.TagClickListener<JSONObject> vaClickListener = new TagsView.TagClickListener<JSONObject>() {
        @Override
        public void onTagClick(JSONObject jsonObject) {
            try {
                String vaId = jsonObject.getString("id");
                if(!vaId.equals(WorksActivity.this.vaId) || type != TYPE_VA_WORK){
                    vaName = jsonObject.getString("name");
                    clearWork();
                    WorksActivity.this.vaId = vaId;
                }
                type = TYPE_VA_WORK;
                reloadRecycleView();
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
        }
    };

    private AsyncHttpClient.JSONObjectCallback apisCallback = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            if(e!=null){
                e.printStackTrace();
                alertException(e);
                /**
                 * why?
                 * javax.net.ssl.SSLHandshakeException: Read error: ssl=0x798b43bc58: Failure in SSL library, usually a protocol error
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err: error:10000065:SSL routines:OPENSSL_internal:ATTEMPT_TO_REUSE_SESSION_IN_DIFFERENT_CONTEXT (external/boringssl/src/ssl/tls13_client.cc:385 0x78de42cc60:0x00000000)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.SSLUtils.toSSLHandshakeException(SSLUtils.java:363)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.ConscryptEngine.convertException(ConscryptEngine.java:1134)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:919)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:747)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:712)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.Java8EngineWrapper.unwrap(Java8EngineWrapper.java:237)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncSSLSocketWrapper$6.onDataAvailable(AsyncSSLSocketWrapper.java:296)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.Util.emitAllData(Util.java:23)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncNetworkSocket.onReadable(AsyncNetworkSocket.java:160)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncServer.runLoop(AsyncServer.java:878)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncServer.run(AsyncServer.java:726)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncServer.access$800(AsyncServer.java:46)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncServer$8.run(AsyncServer.java:680)
                 */
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
                                reloadRecycleView();
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
                        if(type == TYPE_ALL_WORK){
                            setTitle(String.format("%s(%d)",getString(R.string.app_name),totalCount));
                        }
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            DownloadUtils.getInstance().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}