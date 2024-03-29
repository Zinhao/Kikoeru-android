package com.zinhao.kikoeru;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.zinhao.kikoeru.db.DaoMaster;
import com.zinhao.kikoeru.db.DaoSession;
import com.zinhao.kikoeru.db.UserDao;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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


    private List<User> allUsers;
    private long currentUserId;

    private RequestOptions defaultPic;
    private UserDao userDao;
    private DaoMaster.OpenHelper helper;

    private final List<Activity> activities = new ArrayList<>();

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

    public RequestOptions getDefaultPic() {
        return defaultPic;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        helper = new DaoMaster.OpenHelper(App.instance, "app.db") {
        };
        SQLiteDatabase database = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        DaoSession daoSession = daoMaster.newSession();
        userDao = daoSession.getUserDao();

        currentUserId = getValue(App.CONFIG_USER_DATABASE_ID, -1);
        appDebug = getValue(App.CONFIG_DEBUG, 0) == 1;
        saveExternal = getValue(App.CONFIG_SAVE_EXTERNAL, 0) == 1;

        getAllUsers();
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

    public long insertUser(User user) {
        allUsers.add(user);
        currentUserId = userDao.insert(user);
        return currentUserId;
    }

    public void deleteUser(User user) {
        allUsers.remove(user);
        userDao.delete(user);
    }

    public void updateUser(User user) {
        userDao.update(user);
    }

    public List<User> getAllUsers() {
        if (allUsers == null)
            allUsers = userDao.loadAll();
        return allUsers;
    }

    public User currentUser() {
        for (User user : allUsers) {
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
            helper.close();
        }
    }
}
