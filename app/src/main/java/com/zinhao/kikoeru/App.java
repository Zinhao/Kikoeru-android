package com.zinhao.kikoeru;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.bumptech.glide.request.RequestOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class App extends Application {
    public static final String ID_PLAY_SERVICE = "com.zinhao.kikoeru.play_control";
    private static App instance;
    public static final String CONFIG_UPDATE_TIME = "update_time";
    public static final String CONFIG_TOKEN = "token";
    public static final String CONFIG_USER_ACCOUNT = "user";
    public static final String CONFIG_USER_PASSWORD = "password";

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
        Api.HOST = getValue("host",Api.REMOTE_HOST);
        channelMusicService.setDescription(getString(R.string.channel_description));
        channelMusicService.enableLights(false);
        channelMusicService.enableVibration(false);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channelMusicService);
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
