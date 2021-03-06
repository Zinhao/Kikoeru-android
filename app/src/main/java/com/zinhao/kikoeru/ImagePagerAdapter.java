package com.zinhao.kikoeru;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.common.base.Utf8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
        return view==object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View)object);
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {

        final View v = LayoutInflater.from(container.getContext()).inflate(R.layout.item_image,container,false);
        final SubsamplingScaleImageView imageView =v.findViewById(R.id.imageView);
        RoundedCorners roundedCorners= new RoundedCorners(10);
        final RequestOptions requestOptions =RequestOptions.bitmapTransform(roundedCorners);
        T t = ts.get(position);
        CustomViewTarget<SubsamplingScaleImageView,File> target = new CustomViewTarget<SubsamplingScaleImageView,File>(imageView) {
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
        if(t instanceof String){
            if(((String) t).startsWith("http")){
                Glide.with(container.getContext()).asFile().load(t).apply(requestOptions)
                        .into(target);
            }else {
                Glide.with(container.getContext()).asFile().load(new File((String) t)).apply(requestOptions)
                        .into(target);
            }
        }

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                download(position,v.getContext());
                return true;
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.hideLayout(v);
            }
        });
        container.addView(v);
        return v;
    }

    public void download(final int position, final Context context){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(context,"?????????????????????",Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(context).setTitle("?????????????").setNegativeButton("???", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String urlStr = new String(ts.get(position).toString().getBytes(), StandardCharsets.UTF_8);
                    downLoadPic(urlStr ,pic);
                    Toast.makeText(context,"?????????!",Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    App.getInstance().alertException(e);
                }
            }
        }).setPositiveButton("???", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


            }
        }).create().show();

    }

    private void downLoadPic(final String urlStr,File into) throws IOException {
        File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if(!f.exists())
            if(!f.mkdir()){
                throw new FileNotFoundException("?????????????????????");
            }
        String[] strs=urlStr.split("/");
        final File pic =new File(f,URLDecoder.decode(strs[strs.length-1]));
        FileInputStream fi =new FileInputStream(into);
        FileOutputStream fo =new FileOutputStream(pic);
        FileChannel fic =fi.getChannel();
        FileChannel foc =fo.getChannel();
        foc.transferFrom(fic,0,fic.size());
        fo.close();
        fi.close();
        fic.close();
        foc.close();

    }

    public interface HideLayoutCallBack {
        void hideLayout(View view);
    }

    public interface UpAndNextPagerCallBack{
        void up(View view);
        void next(View view);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public int findPositionByName(T t){
        for(int i = 0; i< ts.size(); i++){
            if(ts.get(i)==t){
                return i;
            }
        }
        return 1;
    }

    public void remove(int position){
        ts.remove(position);
        notifyDataSetChanged();
    }
}
