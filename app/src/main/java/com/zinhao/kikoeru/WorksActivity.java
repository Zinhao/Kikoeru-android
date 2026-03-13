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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;

import com.bumptech.glide.Glide;
import com.zinhao.kikoeru.data.model.Tag;
import com.zinhao.kikoeru.data.model.Va;
import com.zinhao.kikoeru.data.model.Work;
import com.zinhao.kikoeru.ui.adapter.WorksAdapter;
import com.zinhao.kikoeru.ui.viewmodel.WorksViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 作品列表页面 - MVVM 重构版
 */
public class WorksActivity extends BaseActivity implements MusicChangeListener, ServiceConnection, TagsView.TagClickListener<Tag> {
    private static final String TAG = "WorksActivity";
    private static final String CONFIG_TYPE = "last_type";
    private static final String CONFIG_PAGE = "last_page";
    private static final String CONFIG_PARAM_INT = "last_param_int";
    private static final String CONFIG_PARAM_STR = "last_param_str";
    
    // ViewModel
    private WorksViewModel viewModel;
    
    // Views
    private RecyclerView recyclerView;
    private WorksAdapter worksAdapter;
    private RecyclerView.OnScrollListener scrollListener;
    
    // Bottom player
    private View bottomLayout;
    private Animation outAnim;
    private Animation inAnim;
    private boolean shouldShowAnim = true;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvWorkTitle;
    private ImageButton ibStatus;
    private ImageButton ibFloatLrcWindow;
    
    // Popup menus
    private ListPopupWindow progressMenu;
    private ListPopupWindow moreMenu;
    private RecyclerView.ItemDecoration itemDecoration;
    
    // Audio service
    private AudioService.CtrlBinder ctrlBinder;
    
    // Request codes
    private static final int TAG_SELECT_RESULT = 14;
    private static final int VA_SELECT_RESULT = 15;
    private static final int CIRCLES_SELECT_RESULT = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(WorksViewModel.class);
        
        initViews();
        initBottomPlayer();
        initViewModelObservers();
        initPopupMenus();
        initListeners();
        
        // Start audio service
        startForegroundService(new Intent(this, AudioService.class));
        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);
        
        // Load initial data
        viewModel.loadWorks();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        bottomLayout = findViewById(R.id.bottomLayout);
        ivCover = bottomLayout.findViewById(R.id.imageView);
        tvTitle = bottomLayout.findViewById(R.id.textView);
        tvWorkTitle = bottomLayout.findViewById(R.id.textView2);
        ibStatus = bottomLayout.findViewById(R.id.button);
        ibFloatLrcWindow = bottomLayout.findViewById(R.id.imageButton);
        
        itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        outAnim = AnimationUtils.loadAnimation(this, R.anim.move_bottom_out);
        inAnim = AnimationUtils.loadAnimation(this, R.anim.move_bottom_in);
        
        // RecyclerView scroll listener for pagination
        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    boolean canScrollDown = recyclerView.canScrollVertically(1);
                    Log.d(TAG, "Scroll state idle, canScrollDown=" + canScrollDown);
                    if (!canScrollDown) {
                        Log.d(TAG, "Reached bottom, loading next page...");
                        viewModel.loadNextPage();
                    }
                }
            }
        };
    }
    
    private void initBottomPlayer() {
        ibFloatLrcWindow.setOnClickListener(v -> {
            if (ctrlBinder != null) {
                if (ctrlBinder.isLrcWindowShow()) {
                    ctrlBinder.hideLrcFloatWindow();
                } else {
                    ctrlBinder.showLrcFloatWindow();
                }
            }
        });
    }
    
    private void initViewModelObservers() {
        // Observe works data
        viewModel.getWorks().observe(this, works -> {
            Log.d(TAG, "===== Works data changed: " + (works != null ? works.size() : 0) + " items =====");
            if (worksAdapter == null) {
                initLayout((int) App.getInstance().getValue(App.CONFIG_LAYOUT_TYPE, WorkAdapter.LAYOUT_SMALL_GRID));
                recyclerView.addOnScrollListener(scrollListener);
            }
            // 确保数据设置到适配器
            if (works != null) {
                Log.d(TAG, "Setting data to adapter: " + works.size() + " items");
                worksAdapter.setData(works);
            }
        });
        
        // Observe total count for title
        viewModel.getTotalCount().observe(this, count -> {
            Log.d(TAG, "Total count changed: " + count);
            updateTitle();
        });
        
        // Observe work type change for title
        viewModel.getWorkType().observe(this, type -> {
            Log.d(TAG, "Work type changed: " + type);
            updateTitle();
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            // Show/hide loading indicator if needed
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                showToast(errorMsg);
            }
        });
        
        // Observe navigation to detail
        viewModel.getNavigateToDetail().observe(this, work -> {
            if (work != null) {
                openWorkDetail(work);
            }
        });
    }
    
    private void initPopupMenus() {
        ImageButton bt1 = findViewById(R.id.bt1);
        ImageButton bt2 = findViewById(R.id.bt2);
        ImageButton bt3 = findViewById(R.id.bt3);
        
        // All works
        bt1.setOnClickListener(v -> {
            viewModel.setWorkType(WorksViewModel.TYPE_ALL_WORK);
        });
        
        // Progress menu
        progressMenu = new ListPopupWindow(this);
        progressMenu.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Arrays.asList(getString(R.string.marked),
                        getString(R.string.listening),
                        getString(R.string.listened),
                        getString(R.string.replay),
                        getString(R.string.postponed))));
        progressMenu.setModal(true);
        progressMenu.setAnchorView(bt2);
        progressMenu.setOnItemClickListener((parent, view, position, id) -> {
            progressMenu.dismiss();
            switch (position) {
                case 0:
                    viewModel.setWorkType(WorksViewModel.TYPE_SELF_MARKED);
                    break;
                case 1:
                    viewModel.setWorkType(WorksViewModel.TYPE_SELF_LISTENING);
                    break;
                case 2:
                    viewModel.setWorkType(WorksViewModel.TYPE_SELF_LISTENED);
                    break;
                case 3:
                    viewModel.setWorkType(WorksViewModel.TYPE_SELF_REPLAY);
                    break;
                case 4:
                    viewModel.setWorkType(WorksViewModel.TYPE_SELF_POSTPONED);
                    break;
            }
        });
        bt2.setOnClickListener(v -> progressMenu.show());
        
        // More menu
        moreMenu = new ListPopupWindow(this);
        moreMenu.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Arrays.asList(getString(R.string.va_voicer),
                        getString(R.string.tag), "Circles",
                        getString(R.string.local_works))));
        moreMenu.setModal(true);
        moreMenu.setAnchorView(bt3);
        moreMenu.setOnItemClickListener((parent, view, position, id) -> {
            moreMenu.dismiss();
            switch (position) {
                case 0:
                    startActivityForResult(new Intent(this, VasActivity.class), VA_SELECT_RESULT);
                    break;
                case 1:
                    startActivityForResult(new Intent(this, TagsActivity.class), TAG_SELECT_RESULT);
                    break;
                case 2:
                    startActivityForResult(new Intent(this, CirclesActivity.class), CIRCLES_SELECT_RESULT);
                    break;
                case 3:
                    viewModel.setWorkType(WorksViewModel.TYPE_LOCAL_WORK);
                    break;
            }
        });
        bt3.setOnClickListener(v -> moreMenu.show());
    }
    
    private void initListeners() {
        // Already set in initBottomPlayer and initPopupMenus
    }
    
    private void initLayout(int layoutType) {
        RecyclerView.LayoutManager layoutManager = null;
        recyclerView.removeItemDecoration(itemDecoration);
        
        if (layoutType == WorkAdapter.LAYOUT_LIST) {
            layoutManager = new LinearLayoutManager(this);
            recyclerView.addItemDecoration(itemDecoration);
        } else if (layoutType == WorkAdapter.LAYOUT_SMALL_GRID) {
            int col = Math.max(getResources().getDisplayMetrics().widthPixels / 395, 3);
            layoutManager = new GridLayoutManager(this, col);
        } else if (layoutType == WorkAdapter.LAYOUT_BIG_GRID) {
            int col = Math.max(getResources().getDisplayMetrics().widthPixels / 395, 2);
            layoutManager = new GridLayoutManager(this, col);
        } else if (layoutType == WorkAdapter.LAYOUT_STAGGERED) {
            int col = Math.max(getResources().getDisplayMetrics().widthPixels / 395, 2);
            layoutManager = new StaggeredGridLayoutManager(col, StaggeredGridLayoutManager.VERTICAL);
        }
        
        // 创建空列表，数据将通过 LiveData 观察者更新
        worksAdapter = new WorksAdapter(new ArrayList<>(), layoutType);
        worksAdapter.setTagClickListener(this);
        worksAdapter.setVaClickListener(vaClickListener);
        worksAdapter.setCirclesClickListener(circlesClickListener);
        worksAdapter.setOnItemClickListener((v, position, work) -> {
            viewModel.onWorkClick(work);
        });
        worksAdapter.setOnItemLongClickListener((v, position, work) -> {
            if (viewModel.getWorkType().getValue() != null && 
                viewModel.getWorkType().getValue() == WorksViewModel.TYPE_LOCAL_WORK) {
                showDeleteLocalWorkDialog(position, work);
                return true;
            }
            return false;
        });
        
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(worksAdapter);
        
        // 如果有当前数据，立即更新
        List<Work> currentWorks = viewModel.getWorks().getValue();
        if (currentWorks != null && !currentWorks.isEmpty()) {
            worksAdapter.setData(currentWorks);
        }
    }
    
    private void openWorkDetail(Work work) {
        try {
            JSONObject workJson = new JSONObject();
            workJson.put("id", work.getId());
            workJson.put("title", work.getTitle());
            workJson.put("name", work.getCircleName());
            workJson.put("release", work.getReleaseDate());
            workJson.put("price", work.getPrice());
            workJson.put("dl_count", work.getDlCount());
            
            // 转换 tags 列表为 JSONArray
            JSONArray tagsArray = new JSONArray();
            if (work.getTags() != null) {
                for (Tag tag : work.getTags()) {
                    JSONObject tagJson = new JSONObject();
                    tagJson.put("id", tag.getId());
                    tagJson.put("name", tag.getName());
                    tagsArray.put(tagJson);
                }
            }
            workJson.put("tags", tagsArray);
            
            // 转换 vas 列表为 JSONArray
            JSONArray vasArray = new JSONArray();
            if (work.getVas() != null) {
                for (Va va : work.getVas()) {
                    JSONObject vaJson = new JSONObject();
                    vaJson.put("id", va.getId());
                    vaJson.put("name", va.getName());
                    vasArray.put(vaJson);
                }
            }
            workJson.put("vas", vasArray);
            
            if (work.isLocalWork()) {
                workJson.put(JSONConst.Work.IS_LOCAL_WORK, true);
                workJson.put(JSONConst.Work.HOST, work.getHost());
            }
            
            Intent intent = new Intent(this, WorkTreeActivity.class);
            intent.putExtra("work_json_str", workJson.toString());
            ActivityCompat.startActivity(this, intent, null);
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }
    
    private void showDeleteLocalWorkDialog(int position, Work work) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_cache)
                .setMessage(work.getTitle())
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    LocalFileCache.getInstance().removeWork(work.getId());
                    worksAdapter.remove(position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void updateTitle() {
        Integer totalCount = viewModel.getTotalCount().getValue();
        String title = getCurrentTitle();
        if (totalCount != null && totalCount > 0) {
            setTitle(String.format("%s (%d)", title, totalCount));
        } else {
            setTitle(title);
        }
    }
    
    private String getCurrentTitle() {
        Integer type = viewModel.getWorkType().getValue();
        if (type == null) type = WorksViewModel.TYPE_ALL_WORK;
        
        switch (type) {
            case WorksViewModel.TYPE_ALL_WORK:
                return getString(R.string.app_name);
            case WorksViewModel.TYPE_SELF_LISTENING:
                return getString(R.string.listening);
            case WorksViewModel.TYPE_SELF_LISTENED:
                return getString(R.string.listened);
            case WorksViewModel.TYPE_SELF_MARKED:
                return getString(R.string.marked);
            case WorksViewModel.TYPE_SELF_REPLAY:
                return getString(R.string.replay);
            case WorksViewModel.TYPE_SELF_POSTPONED:
                return getString(R.string.postponed);
            case WorksViewModel.TYPE_TAG_WORK:
                return "Tag";
            case WorksViewModel.TYPE_VA_WORK:
                return "VA";
            case WorksViewModel.TYPE_CIRCLES_WORK:
                String name = viewModel.getCirclesName().getValue();
                return name != null ? name : "Circles";
            case WorksViewModel.TYPE_LOCAL_WORK:
                return getString(R.string.local_works);
            default:
                return getString(R.string.app_name);
        }
    }
    
    private void toggleBottom() {
        if (shouldShowAnim && bottomLayout.getVisibility() == View.VISIBLE) {
            shouldShowAnim = false;
            bottomLayout.startAnimation(outAnim);
            bottomLayout.postDelayed(() -> {
                bottomLayout.setVisibility(View.GONE);
                shouldShowAnim = true;
            }, outAnim.getDuration());
        } else if (shouldShowAnim && bottomLayout.getVisibility() == View.GONE) {
            shouldShowAnim = false;
            bottomLayout.setVisibility(View.VISIBLE);
            bottomLayout.startAnimation(inAnim);
            bottomLayout.postDelayed(() -> shouldShowAnim = true, inAnim.getDuration());
        }
    }
    
    @Override
    public void onAlbumChange(int rjNumber) {
        if (rjNumber != 0 && bottomLayout.getVisibility() == View.GONE) {
            toggleBottom();
        }
        Glide.with(this).load(App.getInstance().currentUser().getHost() + 
                String.format("/api/cover/%d?type=sam", rjNumber)).into(ivCover);
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
        if (status == 0) {
            ibStatus.setImageResource(R.drawable.ic_baseline_play_arrow_white_24);
        } else {
            ibStatus.setImageResource(R.drawable.ic_baseline_pause_white_24);
        }
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder) service;
        ibStatus.setOnClickListener(v -> {
            if (ctrlBinder.getController().getPlaybackState() == null) {
                ctrlBinder.getController().getTransportControls().play();
                return;
            }
            if (ctrlBinder.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                ctrlBinder.getController().getTransportControls().pause();
            } else {
                ctrlBinder.getController().getTransportControls().play();
            }
        });
        bottomLayout.setOnClickListener(v -> {
            try {
                if (ctrlBinder.getCurrentTitle().endsWith("mp4")) {
                    startActivity(new Intent(WorksActivity.this, VideoPlayerActivity.class));
                } else {
                    startActivity(new Intent(WorksActivity.this, AudioPlayerActivity.class));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        ctrlBinder.addMusicChangeListener(this);
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "切换账号");
        
        SubMenu layoutMenu = menu.addSubMenu(0, 9, 9, R.string.works_layout);
        layoutMenu.setIcon(R.drawable.ic_baseline_view_column_24);
        layoutMenu.add(2, 10, 10, R.string.list_layout);
        layoutMenu.add(2, 11, 11, R.string.cover_layout);
        layoutMenu.add(2, 12, 12, R.string.detail_layout);
        layoutMenu.add(2, 13, 13, "staggered");
        layoutMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        menu.add(0, 15, 15, R.string.more);
        
        SubMenu sortMenu = menu.addSubMenu(0, 16, 16, R.string.sort);
        sortMenu.add(3, 17, 17, R.string.release_date);
        sortMenu.add(3, 18, 18, R.string.rj_number);
        sortMenu.add(3, 19, 19, R.string.prize);
        sortMenu.add(3, 20, 20, R.string.last_in_lib);
        
        menu.add(0, 22, 22, R.string.download_mission);
        MenuItem searchMenu = menu.add(0, 23, 23, R.string.search);
        searchMenu.setIcon(R.drawable.ic_baseline_search_24);
        searchMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getGroupId() == 2) {
            int layoutType = WorkAdapter.LAYOUT_SMALL_GRID;
            if (item.getItemId() == 10) {
                layoutType = WorkAdapter.LAYOUT_LIST;
            } else if (item.getItemId() == 12) {
                layoutType = WorkAdapter.LAYOUT_BIG_GRID;
            } else if (item.getItemId() == 13) {
                layoutType = WorkAdapter.LAYOUT_STAGGERED;
            }
            App.getInstance().setValue(App.CONFIG_LAYOUT_TYPE, layoutType);
            initLayout(layoutType);
            return super.onOptionsItemSelected(item);
        }
        
        if (item.getGroupId() == 3) {
            if (item.getItemId() == 17) {
                Api.setOrder("release");
            } else if (item.getItemId() == 18) {
                Api.setOrder("id");
            } else if (item.getItemId() == 19) {
                Api.setOrder("price");
            } else if (item.getItemId() == 20) {
                Api.setOrder("create_date");
            }
            viewModel.refresh();
            return true;
        }
        
        if (item.getItemId() == 0) {
            App.getInstance().setValue(App.CONFIG_UPDATE_TIME, 0);
            startActivity(new Intent(this, UserSwitchActivity.class));
        } else if (item.getItemId() == 15) {
            startActivity(new Intent(this, MoreActivity.class));
        } else if (item.getItemId() == 22) {
            startActivity(new Intent(this, DownLoadMissionActivity.class));
        } else if (item.getItemId() == 23) {
            startActivity(new Intent(this, SearchActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String resultType = data.getStringExtra("resultType");
            if (resultType == null) return;
            
            switch (requestCode) {
                case VA_SELECT_RESULT:
                    if ("va".equals(resultType)) {
                        String vaId = data.getStringExtra("id");
                        viewModel.setVaId(vaId);
                    }
                    break;
                case TAG_SELECT_RESULT:
                    if ("tag".equals(resultType)) {
                        int tagId = data.getIntExtra("id", -1);
                        viewModel.setTagId(tagId);
                    }
                    break;
                case CIRCLES_SELECT_RESULT:
                    if ("circles".equals(resultType)) {
                        long circlesId = data.getLongExtra("id", -1);
                        String circlesName = data.getStringExtra("name");
                        viewModel.setCirclesId(circlesId, circlesName);
                    }
                    break;
            }
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String resultType = intent.getStringExtra("resultType");
        if ("va".equals(resultType)) {
            String vaId = intent.getStringExtra("id");
            viewModel.setVaId(vaId);
        } else if ("tag".equals(resultType)) {
            int tagId = intent.getIntExtra("id", -1);
            viewModel.setTagId(tagId);
        } else {
            viewModel.setWorkType(WorksViewModel.TYPE_ALL_WORK);
        }
    }
    
    @Override
    public void onTagClick(Tag tag) {
        viewModel.setTagId(tag.getId());
    }
    
    private final TagsView.TagClickListener<Va> vaClickListener = new TagsView.TagClickListener<Va>() {
        @Override
        public void onTagClick(Va va) {
            viewModel.setVaId(va.getId());
        }
    };
    
    private final TagsView.TagClickListener<String> circlesClickListener = circlesName -> {
        long circlesId = App.getInstance().mapCirclesId(circlesName);
        if (circlesId != -1) {
            viewModel.setCirclesId(circlesId, circlesName);
        }
    };
    
    @Override
    protected void onDestroy() {
        if (ctrlBinder != null) {
            ctrlBinder.removeMusicChangeListener(this);
            
            PlaybackStateCompat playbackStateCompat = ctrlBinder.getController().getPlaybackState();
            if (playbackStateCompat == null) {
                stopService(new Intent(this, AudioService.class));
            } else {
                int state = ctrlBinder.getController().getPlaybackState().getState();
                if (state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_PAUSED) {
                    stopService(new Intent(this, AudioService.class));
                }
            }
        }
        unbindService(this);
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            App.getInstance().setValue(CONFIG_TYPE, viewModel.getWorkType().getValue());
            App.getInstance().setValue(CONFIG_PAGE, viewModel.getCurrentPage());
            DownloadUtils.getInstance().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
