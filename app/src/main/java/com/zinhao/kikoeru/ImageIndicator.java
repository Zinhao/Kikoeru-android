package com.zinhao.kikoeru;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import java.util.List;

public class ImageIndicator extends RecyclerView implements ViewPager.OnPageChangeListener{
    private ViewPager viewPager;
    private static final int HALE_DARK_COLOR = Color.argb(0,0,0,0);
    private int imageSelectedColor = Color.GREEN;
    private int selectedPosition = -1;
    private boolean stateStopSelect = false;
    public ImageIndicator(@NonNull Context context) {
        this(context,null,-1);
    }

    public ImageIndicator(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,-1);
    }

    public ImageIndicator(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        imageSelectedColor = ContextCompat.getColor(context,R.color.play_control_icon_color);
    }

    public ImageIndicator bindViewPager(ViewPager viewPager){
        this.viewPager = viewPager;
        this.viewPager.addOnPageChangeListener(this);
        return this;
    }

    public void bindPreViewImage(List<String> list,int selectedPosition){
        this.selectedPosition = selectedPosition;
        if(list.isEmpty())
            return;
        setAdapter(new SuperRecyclerAdapter<String>(list) {
            @Override
            public void bindData(@NonNull SuperVHolder holder, int position) {
                holder.setImage(data.get(position),R.id.preview);
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewPager.setCurrentItem(position);
                    }
                });
                if(selectedPosition!=position)
                    holder.itemView.setBackgroundColor(HALE_DARK_COLOR);
                else
                    holder.itemView.setBackgroundColor(imageSelectedColor);
            }

            @Override
            public int setLayout(int viewType) {
                return R.layout.item_image_indicator;
            }
        });
        setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if(getLayoutManager() == null)
            return;
        if(selectedPosition != -1){
            View view = getLayoutManager().findViewByPosition(selectedPosition);
            if(view !=null){
                view.setBackgroundColor(HALE_DARK_COLOR);
            }
        }
        selectedPosition = position;
        getLayoutManager().scrollToPosition(position);
        View currentSelectedView = getLayoutManager().findViewByPosition(position);
        if(currentSelectedView != null){
            currentSelectedView.setBackgroundColor(imageSelectedColor);
        }else {
            stateStopSelect = true;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if(stateStopSelect && state == 0){
            if(getLayoutManager() == null)
                return;
            View view = getLayoutManager().findViewByPosition(selectedPosition);
            if(view == null)
                return;
            view.setBackgroundColor(imageSelectedColor);
            stateStopSelect = false;
        }
    }
}
