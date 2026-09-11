package com.zinhao.kikoeru.utils;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import okhttp3.Call;
import okhttp3.OkHttpClient;

import java.io.InputStream;

@GlideModule
public class ProgressGlideModule extends AppGlideModule {
    public static final String TAG = "ProgressGlideModule";
    @Override
    public void registerComponents(@NonNull @org.jspecify.annotations.NonNull Context context, @NonNull @org.jspecify.annotations.NonNull Glide glide, @NonNull @org.jspecify.annotations.NonNull Registry registry) {
        Log.d(TAG, "registerComponents: ");
        OkHttpClient client = new OkHttpClient.Builder().addNetworkInterceptor(new ProgressInterceptor()).build();
        OkHttpUrlLoader.Factory factory = new OkHttpUrlLoader.Factory((Call.Factory) client);
        registry.replace(
                GlideUrl.class,
                InputStream.class,
                factory
                );
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
