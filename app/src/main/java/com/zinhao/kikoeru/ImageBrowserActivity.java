package com.zinhao.kikoeru;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

public class ImageBrowserActivity extends BaseActivity {
    private List<String> imageList;
    private ImagePagerAdapter<String> adapter;
    private ImageIndicator imageIndicator;
    private static final String TAG = "ImageBrowserActivity";

    private Animation outAnim;
    private Animation inAnim;
    private boolean shouldShowAnim = true;

    public static void start(Context context, List<String> list, int position) {
        Intent starter = new Intent(context, ImageBrowserActivity.class);
        starter.putExtra("images", (ArrayList<String>) list);
        starter.putExtra("position", position);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController windowInsetsController = getWindow().getDecorView().getWindowInsetsController();
            if(windowInsetsController!=null){
                windowInsetsController.hide(WindowInsets.Type.navigationBars());
            }
        }else{
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
        setContentView(R.layout.activity_image_browser);
        imageList = getIntent().getStringArrayListExtra("images");
        int position = getIntent().getIntExtra("position", 0);
        if (imageList == null)
            imageList = new ArrayList<>();
        ViewPager viewPager = findViewById(R.id.image_pager);
        imageIndicator = findViewById(R.id.imageIndicator);
        adapter = new ImagePagerAdapter<>(imageList);
        adapter.setListener(new ImagePagerAdapter.HideLayoutCallBack() {
            @Override
            public void hideLayout(View view) {
                if (shouldShowAnim && imageIndicator.getVisibility() == View.VISIBLE) {
                    shouldShowAnim = false;
                    imageIndicator.startAnimation(outAnim);
                    imageIndicator.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            imageIndicator.setVisibility(View.GONE);
                            shouldShowAnim = true;
                        }
                    }, outAnim.getDuration());
                } else if (shouldShowAnim && imageIndicator.getVisibility() == View.GONE) {
                    shouldShowAnim = false;
                    imageIndicator.setVisibility(View.VISIBLE);
                    imageIndicator.startAnimation(inAnim);
                    imageIndicator.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            shouldShowAnim = true;
                        }
                    }, inAnim.getDuration());
                }
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        imageIndicator.bindPreViewImage(imageList, position);
        imageIndicator.bindViewPager(viewPager);
        outAnim = AnimationUtils.loadAnimation(this, R.anim.move_bottom_out);
        outAnim.setDuration(100);
        inAnim = AnimationUtils.loadAnimation(this, R.anim.move_bottom_in);
        inAnim.setDuration(100);
    }
}
