package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.List;

public class ImagePagerAdapter<T> extends PagerAdapter {
    private static final String TAG = "ImagePagerAdapter";
    public List<T> ts;
    private HideLayoutCallBack listener;
    File pic;

    public void setListener(HideLayoutCallBack listener) {
        this.listener = listener;
    }

    public ImagePagerAdapter(List<T> ts) {
        this.ts = ts;
    }

    @Override
    public int getCount() {
        return ts.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        final View v = LayoutInflater.from(container.getContext()).inflate(R.layout.item_image, container, false);
        final SubsamplingScaleImageView imageView = v.findViewById(R.id.imageView);
        RoundedCorners roundedCorners = new RoundedCorners(10);
        final RequestOptions requestOptions = RequestOptions.bitmapTransform(roundedCorners);
        T t = ts.get(position);
        CustomViewTarget<SubsamplingScaleImageView, File> target = new CustomViewTarget<SubsamplingScaleImageView, File>(imageView) {
            @Override
            public void onLoadFailed(@Nullable Drawable drawable) {
            }

            @Override
            public void onResourceReady(@NonNull File file, @Nullable Transition<? super File> transition) {
                imageView.setImage(ImageSource.uri(Uri.fromFile(file)));
                pic = file;
            }

            @Override
            protected void onResourceCleared(@Nullable Drawable drawable) {

            }
        };
        if (t instanceof String) {
            if (((String) t).startsWith("http")) {
                Glide.with(container.getContext()).asFile().load(t).apply(requestOptions)
                        .into(target);
            } else {
                Glide.with(container.getContext()).asFile().load(new File((String) t)).apply(requestOptions)
                        .into(target);
            }
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.hideLayout(v);
            }
        });
        container.addView(v);
        return v;
    }

    public interface HideLayoutCallBack {
        void hideLayout(View view);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
