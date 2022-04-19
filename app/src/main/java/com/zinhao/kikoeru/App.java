package com.zinhao.kikoeru;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.bumptech.glide.request.RequestOptions;
import com.jil.swipeback.SwipeBackApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class App extends SwipeBackApplication {
    public static final String ID_PLAY_SERVICE = "com.zinhao.kikoeru.play_control";
    private static App instance;
    public static final String CONFIG_UPDATE_TIME = "update_time";
    public static final String CONFIG_TOKEN = "token";
    public static final String CONFIG_USER_ACCOUNT = "user";
    public static final String CONFIG_USER_PASSWORD = "password";
    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_LAYOUT_TYPE = "layout_type";
    public static final String CONFIG_ONLY_DISPLAY_LRC = "only_display_lrc";
    public static final String CONFIG_SORT = "sort";
    public static final String CONFIG_ORDER = "order";

    public static App getInstance() {
        return instance;
    }

    private RequestOptions defaultPic;

    public RequestOptions getDefaultPic() {
        return defaultPic;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        defaultPic = new RequestOptions().placeholder(R.drawable.ic_no_cover);
        NotificationChannel channelMusicService =
                new NotificationChannel(
                        ID_PLAY_SERVICE,
                        getString(R.string.channel_description), NotificationManager.IMPORTANCE_LOW
                );
        channelMusicService.setDescription(getString(R.string.channel_description));
        channelMusicService.enableLights(false);
        channelMusicService.enableVibration(false);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channelMusicService);
    }

    public void alertException(Exception e){
        Activity activity = getBackHelper().getLastActivity();
        if(activity == null){
            return;
        }
        if(activity instanceof BaseActivity){
            ((BaseActivity) activity).alertException(e);
        }
    }

    public static String getTagsStr(JSONObject jsonObject) throws JSONException {
        JSONArray tagArray = jsonObject.getJSONArray("tags");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < tagArray.length(); i++) {
            JSONObject tag = tagArray.getJSONObject(i);
            stringBuilder.append(tag.getString("name"));
            stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }

    public static JSONArray getTagsList(JSONObject jsonObject) throws JSONException {
        return  jsonObject.getJSONArray("tags");
    }

    public static String getArtStr(JSONObject jsonObject) throws JSONException{
        JSONArray tagArray = jsonObject.getJSONArray("vas");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < tagArray.length(); i++) {
            JSONObject tag = tagArray.getJSONObject(i);
            stringBuilder.append(tag.getString("name"));
            stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }

    public void setValue(String key, String value){
        SharedPreferences sharedPreferences = getSharedPreferences("app.config",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key,value);
        editor.apply();
    }

    public void setValue(String key, long value){
        SharedPreferences sharedPreferences = getSharedPreferences("app.config",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key,value);
        editor.apply();
    }

    public String getValue(String key,String def){
        SharedPreferences sharedPreferences = getSharedPreferences("app.config",MODE_PRIVATE);
        return sharedPreferences.getString(key,def);
    }

    public long getValue(String key,long def){
        SharedPreferences sharedPreferences = getSharedPreferences("app.config",MODE_PRIVATE);
        return sharedPreferences.getLong(key,def);
    }

    public void savePosition(float x,float y){
        SharedPreferences sharedPreferences = getSharedPreferences("config",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("WINDOW_X",x);
        editor.putFloat("WINDOW_Y",y);
        editor.apply();
    }

    public float[] getPosition(){
        float[] position = new float[2];
        SharedPreferences sharedPreferences = getSharedPreferences("config",MODE_PRIVATE);
        position[0] = sharedPreferences.getFloat("WINDOW_X",145);
        position[1] = sharedPreferences.getFloat("WINDOW_Y",160);
        return position;
    }
}
