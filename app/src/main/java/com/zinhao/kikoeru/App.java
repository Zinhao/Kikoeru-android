package com.zinhao.kikoeru;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.kikoeru.db.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class App extends Application implements Application.ActivityLifecycleCallbacks {
    private static App instance;
    public static final String ID_PLAY_SERVICE = "com.zinhao.kikoeru.play_control";
    public static final String CONFIG_FILE_NAME = "app.config";
    public static final String CONFIG_UPDATE_TIME = "update_time";
    public static final String CONFIG_USER_DATABASE_ID = "current_user_database_id";
    public static final String CONFIG_LAYOUT_TYPE = "layout_type";
    public static final String CONFIG_ONLY_DISPLAY_LRC = "only_display_lrc";
    public static final String CONFIG_SORT = "sort";
    public static final String CONFIG_ORDER = "order";
    public static final String CONFIG_DEBUG = "debug";
    public static final String CONFIG_SAVE_EXTERNAL = "save_at_external_dir";


    public static App getInstance() {
        return instance;
    }

    private boolean saveExternal = false;
    private boolean appDebug = false;


    private final List<User> allUsers = new ArrayList<>();
    private final List<LocalWorkHistory> localWorkHistoryList = new ArrayList<>();
    private long currentUserId;

    private RequestOptions defaultPic;
    private UserDao userDao;
    private LocalWorkHistoryDao historyDao;

    private final List<Activity> activities = new ArrayList<>();
    private final HashMap<String,Long> circlesIdMap = new HashMap<>();

    public void setAppDebug(boolean appDebug) {
        this.appDebug = appDebug;
        setValue(CONFIG_DEBUG, appDebug ? 1 : 0);
    }

    public boolean isAppDebug() {
        return appDebug;
    }

    public boolean isSaveExternal() {
        return saveExternal;
    }

    public void setSaveExternal(boolean saveExternal) {
        this.saveExternal = saveExternal;
        setValue(CONFIG_SAVE_EXTERNAL, saveExternal ? 1 : 0);
    }

    public long getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(long currentUserId) {
        this.currentUserId = currentUserId;
    }

    public long mapCirclesId(String circlesName){
        if(circlesIdMap.containsKey(circlesName)){
            final Long id = circlesIdMap.get(circlesName);
            if(id == null){
                return -1;
            }
            return id;
        }
        return -1;
    }

    public void initCirclesIdMap(JSONArray circlesList) throws JSONException {
        /***
         *  {
         *         "id": 54978,
         *         "name": "#ハチゼロニ",
         *         "count": 2
         *     },
         */
        for (int i = 0; i < circlesList.length(); i++) {
            JSONObject j = circlesList.getJSONObject(i);
            circlesIdMap.put(j.getString("name"),j.getLong("id"));
        }
    }

    public HashMap<String, Long> getCirclesIdMap() {
        return circlesIdMap;
    }

    public RequestOptions getDefaultPic() {
        return defaultPic;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,"app.db")
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build();
        userDao = appDatabase.userDao();
        historyDao = appDatabase.historyDao();

        currentUserId = getValue(App.CONFIG_USER_DATABASE_ID, -1);
        appDebug = getValue(App.CONFIG_DEBUG, 0) == 1;
        saveExternal = getValue(App.CONFIG_SAVE_EXTERNAL, 0) == 1;

        getAllUsers();
        loadLocalHis();
        User user = App.getInstance().currentUser();
        if (user != null) {
            Api.init(user.getToken(), user.getHost());
        }

        defaultPic = new RequestOptions().placeholder(R.drawable.ic_no_cover).apply(RequestOptions.bitmapTransform(new RoundedCorners(10)));

        DownloadUtils.getInstance().init(this);
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

        Api.doGetCirclesList(new AsyncHttpClient.JSONArrayCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONArray jsonArray) {
                if(e!=null){
                    App.getInstance().alertException(e);
                    return;
                }
                try {
                    App.getInstance().initCirclesIdMap(jsonArray);
                } catch (JSONException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void alertException(Exception e) {
        if (activities.size() == 0)
            return;
        Activity activity = activities.get(activities.size() - 1);
        if (activity == null) {
            return;
        }
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).alertException(e);
        }
    }

    public static JSONArray getTagsList(JSONObject jsonObject) throws JSONException {
        return jsonObject.getJSONArray("tags");
    }

    public static JSONArray getVasList(JSONObject jsonObject) throws JSONException {
        return jsonObject.getJSONArray("vas");
    }

    public void setValue(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(CONFIG_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void setValue(String key, long value) {
        SharedPreferences sharedPreferences = getSharedPreferences(CONFIG_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public String getValue(String key, String def) {
        SharedPreferences sharedPreferences = getSharedPreferences(CONFIG_FILE_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(key, def);
    }

    public long getValue(String key, long def) {
        SharedPreferences sharedPreferences = getSharedPreferences(CONFIG_FILE_NAME, MODE_PRIVATE);
        return sharedPreferences.getLong(key, def);
    }

    public void savePosition(float x, float y) {
        SharedPreferences sharedPreferences = getSharedPreferences(CONFIG_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("WINDOW_X", x);
        editor.putFloat("WINDOW_Y", y);
        editor.apply();
    }

    public float[] getPosition() {
        float[] position = new float[2];
        SharedPreferences sharedPreferences = getSharedPreferences(CONFIG_FILE_NAME, MODE_PRIVATE);
        position[0] = sharedPreferences.getFloat("WINDOW_X", 145);
        position[1] = sharedPreferences.getFloat("WINDOW_Y", 160);
        return position;
    }

    public void insertLocalHis(LocalWorkHistory history, Runnable callback){
        LocalFileCache.getInstance().doSomething(()->{
            historyDao.insertOrReplace(history);
            boolean sameWorkRj = false;
            if(!localWorkHistoryList.isEmpty()){
                if(history.getRjNumber() == localWorkHistoryList.get(0).getRjNumber()){
                    sameWorkRj = true;
                }
            }
            if(!sameWorkRj){
                localWorkHistoryList.add(0,history);
            }
            callback.run();
        });
    }

    public void loadLocalHis(){
        LocalFileCache.getInstance().doSomething(()->{
            localWorkHistoryList.clear();
            localWorkHistoryList.addAll(historyDao.getAllHis());
            Log.i("App","getLocalWorkHistoryList:" + localWorkHistoryList.size());
        });
    }

    public List<LocalWorkHistory> getLocalWorkHistoryList() {
        return localWorkHistoryList;
    }

    public void insertUser(User user, Runnable callback) {
        LocalFileCache.getInstance().doSomething(()->{
            currentUserId = userDao.insert(user);
            user.setId(currentUserId);
            allUsers.add(user);
            callback.run();
        });
    }

    public void deleteUser(User user) {
        allUsers.remove(user);
        LocalFileCache.getInstance().doSomething(()->{
            userDao.delete(user);
        });
    }

    public void updateUser(User user) {
        LocalFileCache.getInstance().doSomething(()->{
            userDao.update(user);
        });
    }

    public List<User> getAllUsers() {
        LocalFileCache.getInstance().doSomething(()->{
            allUsers.clear();
            allUsers.addAll(userDao.getAllUser());
        });
        return allUsers;
    }

    public User currentUser() {
        for (User user : allUsers) {
            if(user.getId() == null){
                continue;
            }
            if (user.getId().equals(currentUserId)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        activities.add(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        activities.remove(activity);
        if (activities.isEmpty()) {
//            helper.close();
        }
    }
}
