package com.zinhao.kikoeru;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.kikoeru.db.LocalWorkHistory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WorkTreeActivity extends BaseActivity implements View.OnClickListener, MusicChangeListener,
        ServiceConnection, View.OnLongClickListener, TagsView.TagClickListener<JSONObject>,
        WorkTreeAdapter.RelativePathChangeListener {
    /**
     * <a href="http://localhost:8888/api/tracks/357844">...</a>
     */
    private static final String TAG = "WorkActivity";
    private RecyclerView recyclerView;
    private WorkTreeAdapter workTreeAdapter;
    private AudioService.CtrlBinder ctrlBinder;

    private JSONObject work;
    private JSONArray jsonWorkTrees;

    private View bottomLayout;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvWorkTitle;
    private ImageButton ibStatus;
    private ImageButton ibFloatLrc;

    private HeaderViewCompat headerViewCompat;

    private class HeaderViewCompat{
        private TagsView.TagClickListener tagClickListener;
        private TagsView.TagClickListener vaClickListener;
        private TagsView.TagClickListener circlesClickListener;
        private ImageView ivCover;
        private TextView tvTitle;
        private TagsView<JSONArray> tvArt;
        private TagsView<JSONArray> tvTags;
        private TextView tvDate;
        private TextView tvPrice;
        private TextView tvSaleCount;
        private TextView tvHost;
        private TagsView<List<String>> tvCircles;

        public void setCirclesClickListener(TagsView.TagClickListener circlesClickListener) {
            this.circlesClickListener = circlesClickListener;
            tvCircles.setTagClickListener(circlesClickListener);

        }
        public void setTagClickListener(TagsView.TagClickListener<?> tagClickListener) {
            this.tagClickListener = tagClickListener;
            tvTags.setTagClickListener(tagClickListener);
        }
        public void setVaClickListener(TagsView.TagClickListener<?> vaClickListener) {
            this.vaClickListener = vaClickListener;
            tvArt.setTagClickListener(vaClickListener);
        }

        public HeaderViewCompat(@NonNull View itemView) {
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArt = itemView.findViewById(R.id.tvArt);
            tvTags = itemView.findViewById(R.id.tvTags);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSaleCount = itemView.findViewById(R.id.tvSaleCount);
            tvHost = itemView.findViewById(R.id.tvHost);
            tvCircles = itemView.findViewById(R.id.tvCircles);
        }
    }

    private final AsyncHttpClient.JSONArrayCallback docTreeCallback = new AsyncHttpClient.JSONArrayCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONArray jsonArray) {
            if (e != null) {
                alertException(e);
                return;
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                return;
            }
            jsonWorkTrees = jsonArray;
            // TODO 来自不同服务器的同一个作品（RJ号码相同），当用户执行下载操作时，目录树不一致。
            runOnUiThread(() -> {
                workTreeAdapter = new WorkTreeAdapter(jsonWorkTrees,work.optInt("id"));
                workTreeAdapter.setItemClickListener(WorkTreeActivity.this);
                workTreeAdapter.setParentDirClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (workTreeAdapter != null) {
                            boolean r = workTreeAdapter.parentDir();
                            if(r){
                                finish();
                            }
                        }
                    }
                });

                workTreeAdapter.setItemLongClickListener(WorkTreeActivity.this);
                workTreeAdapter.setPathChangeListener(WorkTreeActivity.this);
                RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(WorkTreeActivity.this, DividerItemDecoration.VERTICAL);
                recyclerView.addItemDecoration(itemDecoration);
                recyclerView.setItemAnimator(null);
                recyclerView.setLayoutManager(new LinearLayoutManager(WorkTreeActivity.this));
                recyclerView.setAdapter(workTreeAdapter);
            });
        }
    };

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);
        String workStr = getIntent().getStringExtra("work_json_str");
        if (workStr != null && !workStr.isEmpty()) {
            try {
                work = new JSONObject(workStr);
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
                finish();
                return;
            }
        }

        App app = (App)getApplication();
        try {
            LocalWorkHistory localWorkHistory = new LocalWorkHistory(
                    work.getInt("id")
                    ,System.currentTimeMillis(),"",
                    work.getString("title"));
            app.insertLocalHis(localWorkHistory, new Runnable() {
                @Override
                public void run() {

                }
            });
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        inAnim = AnimationUtils.loadAnimation(this, R.anim.move_bottom_in);
        recyclerView = findViewById(R.id.recyclerView);
        bottomLayout = findViewById(R.id.bottomLayout);
        ivCover = bottomLayout.findViewById(R.id.imageView);
        tvTitle = bottomLayout.findViewById(R.id.textView);
        tvWorkTitle = bottomLayout.findViewById(R.id.textView2);
        ibStatus = bottomLayout.findViewById(R.id.button);
        ibFloatLrc = bottomLayout.findViewById(R.id.imageButton);

        View header = findViewById(R.id.header_info);
        headerViewCompat = new HeaderViewCompat(header);
        headerViewCompat.setTagClickListener(WorkTreeActivity.this);
        headerViewCompat.setVaClickListener(vaClickListener);
        headerViewCompat.setCirclesClickListener(circlesClickListener);
        initHeader();

        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);
        init();
    }

    private void initHeader(){
        try {
            Glide.with(this).load(
                    App.getInstance().currentUser().getHost() + String.format("/api/cover/%d?token=%s", work.getInt("id"), Api.token))
                    .apply(App.getInstance().getDefaultPic())
                    .into(headerViewCompat.ivCover);
            headerViewCompat.tvTitle.setText(work.getString("title"));
            headerViewCompat.tvArt.setTags(App.getVasList(work), TagsView.JSON_TEXT_GET.setKey("name"));
            headerViewCompat.tvTags.setTags(App.getTagsList(work), TagsView.JSON_TEXT_GET.setKey("name"));
            headerViewCompat.tvCircles.setTags(Collections.singletonList(work.getString("name")),TagsView.STRING_TEXT_GET);
            String dateStr = work.optString("release");
            if(dateStr.isEmpty()){
                headerViewCompat.tvDate.setVisibility(View.GONE);
            }else{
                headerViewCompat.tvDate.setVisibility(View.VISIBLE);
                headerViewCompat.tvDate.setText(dateStr);
            }
            headerViewCompat.tvPrice.setText(String.format("%d 日元", work.getInt("price")));
            headerViewCompat.tvSaleCount.setText(String.format("售出：%d", work.getInt("dl_count")));
            if (work.has(JSONConst.Work.HOST)) {
                headerViewCompat.tvHost.setVisibility(View.VISIBLE);
                headerViewCompat.tvHost.setText(work.getString(JSONConst.Work.HOST));
            } else {
                headerViewCompat.tvHost.setVisibility(View.INVISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            App.getInstance().alertException(e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String workStr = intent.getStringExtra("work_json_str");
        Log.d(TAG, "onNewIntent: " + workStr);
        if (workStr != null && !workStr.isEmpty()) {
            try {
                work = new JSONObject(workStr);
                init();
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
        }
    }

    private void init() {
        try {
            if (work.has(JSONConst.Work.IS_LOCAL_WORK)) {
                boolean isLocalWork = work.getBoolean(JSONConst.Work.IS_LOCAL_WORK);
                if (isLocalWork) {
                    LocalFileCache.getInstance().readLocalWorkTree(this, work.getInt("id"), docTreeCallback);
                    return;
                }
            }
            Api.doGetDocTree(work.getInt("id"), docTreeCallback);
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }

    @Override
    public void onBackPressed() {
        if(recyclerView.getAdapter() == workAdapter){
            RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(WorkTreeActivity.this, DividerItemDecoration.VERTICAL);
            recyclerView.addItemDecoration(itemDecoration);
            recyclerView.setLayoutManager(new LinearLayoutManager(WorkTreeActivity.this));
            recyclerView.setAdapter(workTreeAdapter);
            workAdapter = null;
            if(workTreeAdapter == null){
                super.onBackPressed();
            }
        }else{
            super.onBackPressed();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu subMenu = menu.addSubMenu(0, 0, 0, R.string.mark_action);
        subMenu.add(1, 1, 1, R.string.marked);
        subMenu.add(1, 2, 2, R.string.listening);
        subMenu.add(1, 3, 3, R.string.listened);
        subMenu.add(1, 4, 4, R.string.replay);
        subMenu.add(1, 5, 5, R.string.postponed);
        subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            if (item.getItemId() == 1) {
                Api.doPutReview(work.getInt("id"), Api.FILTER_MARKED, actionCallBack);
            } else if (item.getItemId() == 2) {
                Api.doPutReview(work.getInt("id"), Api.FILTER_LISTENING, actionCallBack);
            } else if (item.getItemId() == 3) {
                Api.doPutReview(work.getInt("id"), Api.FILTER_LISTENED, actionCallBack);
            } else if (item.getItemId() == 4) {
                Api.doPutReview(work.getInt("id"), Api.FILTER_REPLAY, actionCallBack);
            } else if (item.getItemId() == 5) {
                Api.doPutReview(work.getInt("id"), Api.FILTER_POSTPONED, actionCallBack);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
        return super.onOptionsItemSelected(item);
    }

    private final AsyncHttpClient.JSONObjectCallback actionCallBack = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            if (e != null) {
                alertException(e);
                return;
            }
            if (asyncHttpResponse == null)
                return;
            if (asyncHttpResponse.code() == 200) {
                try {
                    String message = jsonObject.getString("message");
                    runOnUiThread(() -> Toast.makeText(WorkTreeActivity.this, message, Toast.LENGTH_SHORT).show());
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                    alertException(jsonException);
                }
            }
        }
    };

    @Override
    public void onClick(View v) {
        JSONObject item = (JSONObject) v.getTag();
        try {
            String itemType = item.getString("type");
            if ("image".equals(itemType)) {
                openImage(item);
            } else if ("audio".equals(itemType)) {
                openAudioOrVideo(item);
            } else if ("text".equals(itemType)) {
                openText(item);
            } else {
                openOther(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }

    private void openImage(JSONObject item) throws JSONException {
        List<String> imageList = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < workTreeAdapter.getData().length(); i++) {
            JSONObject _item = workTreeAdapter.getData().getJSONObject(i);
            if (_item.getString("type").equals("image")) {
                String url;
                try {
                    url = _item.getString(JSONConst.WorkTree.MAP_FILE_PATH);
                    if (!new File(url).exists()) {
                        url = _item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
                        url = Api.formatGetUrl(url, true);
                    }
                    imageList.add(url);
                    if (_item.getString(JSONConst.WorkTree.HASH).equals(item.getString(JSONConst.WorkTree.HASH))) {
                        index = imageList.size() - 1;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    alertException(e);
                }
            }
        }
        ImageBrowserActivity.start(WorkTreeActivity.this, imageList, index);
    }

    private void openText(JSONObject item) {
        TextRowActivity.start(this, item.toString());
    }

    private void openOther(JSONObject item) throws JSONException {
        String url = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (!url.startsWith("http")) {
            url = String.format("%s%s", App.getInstance().currentUser().getHost(), url);
        }
        intent.setData(Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            alertException(e);
        }
    }

    private void openAudioOrVideo(JSONObject item) throws JSONException {
        String itemHash = item.getString(JSONConst.WorkTree.HASH);
        String itemTitle = item.getString("title");
        String itemMediaStreamUrl = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
        List<JSONObject> musicArray = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < workTreeAdapter.getData().length(); i++) {
            JSONObject _item = workTreeAdapter.getData().getJSONObject(i);
            if ("audio".equals(_item.getString("type"))) {
                musicArray.add(_item);
                if (_item.getString(JSONConst.WorkTree.HASH).equals(itemHash)) {
                    index = musicArray.size() - 1;
                }
            }
        }
        if (ctrlBinder.getCurrent() != null && ctrlBinder.getCurrent().getString(JSONConst.WorkTree.MEDIA_STREAM_URL).equals(itemMediaStreamUrl)) {
            if (itemTitle.toLowerCase(Locale.ROOT).endsWith(".mp4")) {
                startActivity(new Intent(WorkTreeActivity.this, VideoPlayerActivity.class));
            } else {
                startActivity(new Intent(WorkTreeActivity.this, AudioPlayerActivity.class));
            }
        } else {
            ctrlBinder.play(musicArray, index);
        }
    }

    @Override
    public void onAlbumChange(int rjNumber) {
        Glide.with(this).load(App.getInstance().currentUser().getHost() + String.format(Locale.US, "/api/cover/%d?type=sam", rjNumber)).into(ivCover);
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
        if (status == 0) {
            ibStatus.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        } else {
            if(bottomLayout.getVisibility() == View.GONE){
                showBottomLayout();
            }
            ibStatus.setImageResource(R.drawable.ic_baseline_pause_24);
        }
    }

    private boolean shouldShowAnim = true;
    private Animation inAnim;
    private void showBottomLayout() {
        if(shouldShowAnim){
            shouldShowAnim = false;
            bottomLayout.setVisibility(View.VISIBLE);
            bottomLayout.startAnimation(inAnim);
            bottomLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    shouldShowAnim = true;
                }
            }, inAnim.getDuration());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ctrlBinder.removeMusicChangeListener(this);
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder) service;
        if (ctrlBinder.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            bottomLayout.setVisibility(View.VISIBLE);
        }
        ibStatus.setOnClickListener(v -> {
            if (ctrlBinder.getController().getPlaybackState() == null)
                return;
            if (ctrlBinder.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                ctrlBinder.getController().getTransportControls().pause();
            } else {
                ctrlBinder.getController().getTransportControls().play();
            }
        });
        ibFloatLrc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ctrlBinder.isLrcWindowShow()){
                    ctrlBinder.hideLrcFloatWindow();
                }else{
                    ctrlBinder.showLrcFloatWindow();
                }
            }
        });
        bottomLayout.setOnClickListener(v -> {
            try {
                if (ctrlBinder.getCurrentTitle().endsWith("mp4")) {
                    startActivity(new Intent(WorkTreeActivity.this, VideoPlayerActivity.class));
                } else {
                    Intent intent =new Intent(v.getContext(), AudioPlayerActivity.class);
                    View view = v.findViewById(R.id.imageView);
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            WorkTreeActivity.this, view, "hero_bottom" // 这里的字符串必须匹配 transitionName
                    );
                    startActivity(intent,options.toBundle());
                }
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
        });
        ctrlBinder.addMusicChangeListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}

    @Override
    public boolean onLongClick(View v) {
        JSONObject item = (JSONObject) v.getTag();
        if (item == null)
            return false;
        if (work == null)
            return false;
        try {
            String itemType = item.getString("type");
            if (!item.has(JSONConst.WorkTree.MAP_FILE_PATH)) {
                return false;
            }
            File itemFile = new File(item.getString(JSONConst.WorkTree.MAP_FILE_PATH));
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(itemFile.getAbsolutePath());
            if (itemFile.exists()) {
                DownloadUtils.Mission mapMission = DownloadUtils.mapMission(item);
                if (mapMission != null) {
                    builder.setTitle(R.string.downloading);
                    builder.setNegativeButton(R.string.cancel_download, (dialog, which) -> mapMission.stop());
                    builder.setPositiveButton(R.string.check_mission, (dialogInterface, i) -> {
                        startActivity(new Intent(WorkTreeActivity.this, DownLoadMissionActivity.class));
                        dialogInterface.dismiss();
                    });
                } else {
                    builder.setNegativeButton(R.string.open, (dialog, which) -> {
                        try {
                            if (itemType.equals("audio")) {
                                openAudioOrVideo(item);
                            } else if (itemType.equals("text")) {
                                openText(item);
                            } else if (itemType.equals("image")) {
                                openImage(item);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    });
                    builder.setPositiveButton("open with", (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = FileProvider.getUriForFile(WorkTreeActivity.this, getPackageName() + ".fileProvider", itemFile);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        intent.setDataAndType(uri, String.format("%s/*", itemType));
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            alertException(e);
                        }

                    });
                }

            } else {
                builder.setTitle(getString(R.string.not_download));
                builder.setMessage(itemFile.getAbsolutePath());
                builder.setNegativeButton(R.string.download, (dialog, which) -> {
                    if (work.has(JSONConst.Work.IS_LOCAL_WORK)) {
                        // 从本地目录树开始下载
                        if (!work.has(JSONConst.Work.HOST)) {
                            return;
                        }
                        try {
                            String workHost = work.getString(JSONConst.Work.HOST);
                            if (!App.getInstance().currentUser().getHost().equals(workHost)) {
                                Toast.makeText(WorkTreeActivity.this, "switch host user then start download!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    boolean havePermission;
                    final DownloadUtils.Mission downLoadMission = new DownloadUtils.Mission(item);
                    downLoadMission.setSuccessCallback(() -> runOnUiThread(() -> {
                        if (!isDestroyed()) {
                            workTreeAdapter.mapFileExistValue();
                        }
                    }));
                    if (App.getInstance().isSaveExternal()) {
                        havePermission = requestReadWriteExternalPermission(() -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                if (!Environment.isExternalStorageManager()) {
                                    return;
                                }
                            } else {
                                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                                    return;
                                }
                            }
                            saveWorkWithTree();
                            downLoadMission.start();
                        });
                    } else {
                        havePermission = true;
                    }
                    if (havePermission) {
                        saveWorkWithTree();
                        downLoadMission.start();
                        runOnUiThread(()->{
                            workTreeAdapter.notifyDataSetChanged();
                        });
                    }
                    dialog.dismiss();
                });
                final String itemStreamUrl = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
                builder.setPositiveButton("open in browser", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    String readableUrl;
                    if (!itemStreamUrl.startsWith("http")) {
                        readableUrl = String.format("%s%s?token=%s", App.getInstance().currentUser().getHost(), itemStreamUrl, Api.token);
                    } else {
                        readableUrl = String.format("%s?token=%s", itemStreamUrl, Api.token);
                    }
                    intent.setData(Uri.parse(readableUrl));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        alertException(e);
                    }
                });
            }
            builder.create().show();
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
        return true;
    }

    private void saveWorkWithTree() {
        if (jsonWorkTrees != null) {
            try {
                work.put(JSONConst.Work.HOST, App.getInstance().currentUser().getHost());
                LocalFileCache.getInstance().saveWork(work, jsonWorkTrees);
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
        }
    }

    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            int tagId = jsonObject.getInt("id");
            String tagName = jsonObject.getString("name");
            setTitle(tagName);
            Intent intent = new Intent(WorkTreeActivity.this, WorksActivity.class);
            intent.putExtra("resultType", "tag");
            intent.putExtra("id", tagId);
            intent.putExtra("name", tagName);
            startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }

    private WorkAdapter workAdapter;
    private List<JSONObject> works;
    private int page = 1;

    private final AsyncHttpClient.JSONObjectCallback apisCallback = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            if (e != null) {
                e.printStackTrace();
                alertException(e);
                return;
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                return;
            }
            try {
                JSONArray jsonArray = jsonObject.getJSONArray("works");
                int totalCount = jsonObject.getJSONObject("pagination").getInt("totalCount");
                page = jsonObject.getJSONObject("pagination").getInt("currentPage") + 1;

                if (jsonArray.length() != 0) {
                    page = Math.min(page, totalCount / jsonArray.length() + 1);
                }
                runOnUiThread(new Runnable() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void run() {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                if(works == null){
                                    works = new ArrayList<>();
                                }
                                works.add(jsonArray.getJSONObject(i));
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                                alertException(jsonException);
                            }
                        }
                        initLayout();
                        workAdapter.notifyItemRangeInserted(Math.max(0, works.size() - jsonArray.length()), jsonArray.length());
                        workAdapter.notifyItemRangeChanged(Math.max(0, works.size() - jsonArray.length()), jsonArray.length());
                    }
                });
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
                alertException(jsonException);
            }
        }
    };

    private final TagsView.TagClickListener<JSONObject> vaClickListener = jsonObject -> {
        try {
            String vaId = jsonObject.getString("id");
            Log.d(TAG, "onTagClick: " + vaId);
            String vaName = jsonObject.getString("name");
            setTitle(vaName);
            Api.doGetWorkByVa(page, vaId, apisCallback);
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    };

    private final TagsView.TagClickListener<String> circlesClickListener = circlesName -> {
        //todo
        //http://localhost:8980/api/circles/
        //http://localhost:8980/api/circles/54978/works?order=release&sort=desc&page=1&seed=59
        long circlesId = App.getInstance().mapCirclesId(circlesName);
        if(circlesId!=-1){
            Api.doGetWorkByCircles(page,circlesId,apisCallback);
        }
        setTitle(circlesName);
        Log.d(TAG, "onTagClick: " + circlesName);
    };

    private void initLayout() {
        RecyclerView.LayoutManager layoutManager = null;
        int col = Math.max(getResources().getDisplayMetrics().widthPixels/395,3);
        layoutManager = new GridLayoutManager(this, col);
        workAdapter = new WorkAdapter(works, WorkAdapter.LAYOUT_SMALL_GRID);
        workAdapter.setItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject item = (JSONObject) v.getTag();
                Intent intent = new Intent(v.getContext(), WorkTreeActivity.class);
                intent.putExtra("work_json_str", item.toString());
                ActivityCompat.startActivity(v.getContext(), intent, null);
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(workAdapter);
    }

    @Override
    public void onPathChange(String path) {
        if (path.length() > 15) {
            path = "..." + path.substring(path.length() - 12);
        }
        setTitle(path);
    }
}