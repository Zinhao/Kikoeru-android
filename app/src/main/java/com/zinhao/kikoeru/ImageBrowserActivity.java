package com.zinhao.kikoeru;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

public class ImageBrowserActivity extends BaseActivity {
    private List<String> imageList;
    private ImagePagerAdapter<String> adapter;
    private ImageIndicator imageIndicator;

    public static void start(Context context,List<String> list,int position) {
        Intent starter = new Intent(context, ImageBrowserActivity.class);
        starter.putExtra("images",(ArrayList<String>)list);
        starter.putExtra("position",position);
        context.startActivity(starter);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image_browser);
        imageList = getIntent().getStringArrayListExtra("images");
        int position = getIntent().getIntExtra("position",0);
        if(imageList == null)
            imageList = new ArrayList<>();
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        ViewPager viewPager = findViewById(R.id.image_pager);
        imageIndicator = findViewById(R.id.imageIndicator);
        adapter = new ImagePagerAdapter<>(imageList);
        adapter.setListener(new ImagePagerAdapter.HideLayoutCallBack() {
            @Override
            public void hideLayout(View view) {
                if(imageIndicator.getVisibility()== View.GONE){
                    imageIndicator.setVisibility(View.VISIBLE);
                }else if(imageIndicator.getVisibility()== View.VISIBLE){
                    imageIndicator.setVisibility(View.GONE);
                }
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        imageIndicator.bindPreViewImage(imageList,position);
        imageIndicator.bindViewPager(viewPager);
    }
}
