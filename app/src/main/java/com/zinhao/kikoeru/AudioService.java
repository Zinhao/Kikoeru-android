package com.zinhao.kikoeru;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.kikoeru.db.AudioLrcBind;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class AudioService extends Service {
    private static final String TAG = "AudioService";
    private static final String ACTION_PAUSE = "com.zinhao.kikoeru.ACTION_PAUSE";
    private static final String ACTION_PLAY = "com.zinhao.kikoeru.ACTION_PLAY";
    private static final String ACTION_PREVIOUS = "com.zinhao.kikoeru.ACTION_PREVIOUS";
    private static final String ACTION_NEXT = "com.zinhao.kikoeru.ACTION_NEXT";
    private static final String ACTION_SHOW_LRC = "com.zinhao.kikoeru.ACTION_SHOW_LRC";
    private Handler mHandler;
    private ExoPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;

    private WindowManager.LayoutParams lrcWindowParams;
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat SSDF = new SimpleDateFormat("MM-dd HH:mm:ss");
    private final MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            mediaPlayer.seekTo(pos);
        }

        @Override
        public void onPause() {
            super.onPause();
            mediaPlayer.pause();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            mediaPlayer.play();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.d(TAG, "onSkipToNext: ");
            mediaPlayer.seekToNextMediaItem();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.d(TAG, "onSkipToPrevious: ");
            mediaPlayer.seekToPreviousMediaItem();
        }

        @Override
        public void onStop() {
            super.onStop();
            mediaPlayer.stop();
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent == null || keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            int keyCode = keyEvent.getKeyCode();
            Log.d(TAG, "onMediaButtonEvent: " + keyCode);
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPrepare() {
            Log.d(TAG, "onPrepare: ");
            super.onPrepare();
        }
    };

    private static final int NOTIFICATION_ID = 12;
    private static final int NOTIFICATION_ID_CLOSE_DELAY = 13;

    private final HeadsetActionReceiver headsetActionReceiver = new HeadsetActionReceiver();

    private class HeadsetActionReceiver extends BroadcastReceiver {
        final IntentFilter intentFilter;
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isActionPause = false;

        public HeadsetActionReceiver() {
            intentFilter = new IntentFilter();
            intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
            intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null)
                return;
            if (intent.hasExtra("state")) {
                final boolean isPlugIn = intent.getExtras().getInt("state") == 1;
                Log.d(TAG, "onReceive: isPlugIn:" + isPlugIn);
                if (isPlugIn && isActionPause) {
                    mediaSession.getController().getTransportControls().play();
                } else {
                    mediaSession.getController().getTransportControls().pause();
                    isActionPause = true;
                }
            }
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);
            if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                if (ActivityCompat.checkSelfPermission(AudioService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    mediaSession.getController().getTransportControls().pause();
                    isActionPause = true;
                    return;
                }
                if (bluetoothAdapter != null && bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_DISCONNECTED) {
                    mediaSession.getController().getTransportControls().pause();
                    isActionPause = true;
                }
            } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                mediaSession.getController().getTransportControls().pause();
                isActionPause = true;
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return ctrlBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, getClass().getSimpleName());
        mediaSession.setCallback(callback);
        float[] position = App.getInstance().getPosition();
        lrcWindowParams = makeFloatWindowParams(position[0], position[1]);
        mHandler = new Handler(getMainLooper());
        mediaPlayer = new ExoPlayer.Builder(this).build();
        mediaPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                alertException(error);
                if (error instanceof ExoPlaybackException) {
                    ExoPlaybackException exoError = (ExoPlaybackException) error;

                    // 检查是否是源码层面的错误（网络/文件读取）
                    if (exoError.type == ExoPlaybackException.TYPE_SOURCE) {
                        IOException cause = exoError.getSourceException();

                        if (cause instanceof HttpDataSource.HttpDataSourceException) {
                            // 提示用户检查网络或稍后重试
                            Log.d(TAG, "onPlayerError: "+"网络连接异常，请检查网络设置");
                            // 可以尝试自动重试，或者让用户手动点击刷新
                            return;
                        }
                    }
                }
                // 处理其他类型的错误
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "onIsPlayingChanged: " + isPlaying);
                ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                    @Override
                    public void accept(MusicChangeListener listener) {
                        listener.onStatusChange(mediaPlayer.isPlaying() ? 1 : 0);
                    }
                });
                try {
                    updateNotificationState();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.d(TAG, String.format("onPlaybackStateChanged:playbackState: %d ,index: %d", playbackState, mediaPlayer.getCurrentMediaItemIndex()));
                if (ctrlBinder == null) {
                    return;
                }
                ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                    @Override
                    public void accept(MusicChangeListener listener) {
                        listener.onStatusChange(mediaPlayer.isPlaying() ? 1 : 0);
                    }
                });
                try {
                    if (playbackState == Player.STATE_READY) {
                        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, (String) mediaPlayer.getMediaMetadata().title)
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, null)
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, (String) mediaPlayer.getMediaMetadata().artist)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, null)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration())
                                .build();
                        mediaSession.setMetadata(metadata);
                    }
                    updateNotificationState();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
                Log.d(TAG, "onMediaMetadataChanged: " + mediaMetadata.title + ", play state:"+mediaPlayer.getPlaybackState());
                ctrlBinder.current = ctrlBinder.playList.get(mediaPlayer.getCurrentMediaItemIndex());

                MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, (String) mediaMetadata.title)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, null)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, null)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, null)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration())
                        .build();
                mediaSession.setMetadata(metadata);
                try {
                    updateNotificationState();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                    @Override
                    public void accept(MusicChangeListener listener) {
                        listener.onAudioChange(ctrlBinder.current);
                    }
                });
                loadLrc();
            }

            @Override
            public void onMetadata(@NonNull Metadata metadata) {
                Log.d(TAG, "onMetadata: " + metadata.toString());
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(headsetActionReceiver, headsetActionReceiver.intentFilter, Context.RECEIVER_NOT_EXPORTED);
        }else{
            registerReceiver(headsetActionReceiver, headsetActionReceiver.intentFilter);
        }
        ctrlBinder = new CtrlBinder();
        try {
            updateNotificationState();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        setupFloatLrc();
    }

    private void setupFloatLrc(){
        if (ctrlBinder.getLrcFloatView() == null) {
            TextView view = (TextView) LayoutInflater.from(this).inflate(R.layout.lrc_layout, null, false);
            view.setOnTouchListener(new View.OnTouchListener() {
                private float downX, downY;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // 歌词手势
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        downX = event.getRawX();
                        downY = event.getRawY();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (ctrlBinder != null)
                            App.getInstance().savePosition(ctrlBinder.getLrcWindowParams().x, ctrlBinder.getLrcWindowParams().y);
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

                        float nowX = event.getRawX();
                        float nowY = event.getRawY();
                        float moveX = nowX - downX;
                        float moveY = nowY - downY;
                        if (ctrlBinder != null) {
                            ctrlBinder.getLrcWindowParams().x += moveX;
                            ctrlBinder.getLrcWindowParams().y += moveY;
                            ctrlBinder.windowManager.updateViewLayout(ctrlBinder.getLrcFloatView(), ctrlBinder.getLrcWindowParams());
                        }
                        downX = nowX;
                        downY = nowY;
                    }
                    return true;
                }
            });
            ctrlBinder.setLrcView(view);
        }
    }

    private void loadLrc(){
        Log.d(TAG, "loadLrc: " + ctrlBinder.current.optString(JSONConst.WorkTree.TITLE));
        try {
            String path = ctrlBinder.current.getString(JSONConst.WorkTree.MAP_FILE_PATH);
            File audioFile = new File(path);
            if (!LocalFileCache.getInstance().getLrcText(audioFile, ctrlBinder.lrcCallBack)) {
                // Local lrc file not exist
                if(ctrlBinder.current.has("lrc_info")){
                    JSONObject lrcInfo = ctrlBinder.current.getJSONObject(JSONConst.WorkTree.LRC_INFO);
                    Log.d(TAG, "loadLrc: local check:" + lrcInfo.optString(JSONConst.WorkTree.HASH));
                    Api.doGetMediaString(lrcInfo.getString(JSONConst.WorkTree.HASH),ctrlBinder.lrcCallBack);
                }else{
                    Log.d(TAG, "loadLrc: server check:" + ctrlBinder.current.optString(JSONConst.WorkTree.HASH));
                    String hash = ctrlBinder.current.optString(JSONConst.WorkTree.HASH);
                    Api.checkLrc(hash, ctrlBinder.checkLrcCallBack);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace(System.err);
        }
    }

    private static WindowManager.LayoutParams makeFloatWindowParams(float x, float y) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params.gravity = Gravity.START | Gravity.TOP;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        params.format = PixelFormat.RGBA_8888;
        params.x = (int) x;
        params.y = (int) y;
        return params;
    }

    private final PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();

    @SuppressLint("DefaultLocale")
    private void updateNotificationState() throws JSONException {
        if (mediaPlayer.getPlaybackState() == Player.STATE_BUFFERING) {
            mediaSession.setActive(false);
            builder.setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1);
        } else if (mediaPlayer.isPlaying()) {
            mediaSession.setActive(true);
            builder.setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1);
        } else if (mediaPlayer.getPlaybackState() == Player.STATE_READY) {
            mediaSession.setActive(false);
            builder.setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1);
        } else {
            mediaSession.setActive(false);
            builder.setState(PlaybackStateCompat.STATE_STOPPED, mediaPlayer.getCurrentPosition(), 1);
        }
        builder.setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_STOP |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_SET_RATING);
        mediaSession.setPlaybackState(builder.build());

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, App.ID_PLAY_SERVICE);
        androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle();
        mediaStyle.setShowActionsInCompactView(1);
        mediaStyle.setMediaSession(mediaSession.getSessionToken());
        notificationBuilder.setStyle(mediaStyle);

        Intent previousIntent = new Intent(this, AudioService.class);
        previousIntent.setAction(ACTION_SHOW_LRC);
        PendingIntent showLrcPendingIntent = PendingIntent.getService(this, 1, previousIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, AudioService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, AudioService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 3, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, AudioService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 4, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder.addAction(R.drawable.ic_baseline_text_fields_24, "show lrc", showLrcPendingIntent);
        if (mediaPlayer.isPlaying()) {
            notificationBuilder.addAction(R.drawable.ic_baseline_pause_24, "pause", pausePendingIntent);
        } else {
            notificationBuilder.addAction(R.drawable.ic_baseline_play_arrow_24, "play", playPendingIntent);
        }
        notificationBuilder.addAction(R.drawable.ic_baseline_skip_next_24, "next", nextPendingIntent);
        notificationBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        notificationBuilder.setSmallIcon(R.drawable.ic_baseline_audiotrack_24);
        notificationBuilder.setShowWhen(true);
        notificationBuilder.setColorized(true);
        if (ctrlBinder.current != null) {
            notificationBuilder.setContentTitle(ctrlBinder.current.getString("title"));
            notificationBuilder.setContentText(ctrlBinder.current.getString("title"));
            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, AudioPlayerActivity.class), PendingIntent.FLAG_IMMUTABLE));
            Glide.with(this).asBitmap().load(Api.minCoverImageUrl(ctrlBinder.currentAlbumId)).into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                    notificationBuilder.setLargeIcon(bitmap);
                    startForeground(NOTIFICATION_ID, notificationBuilder.build());
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    super.onLoadFailed(errorDrawable);
                    alertException(new Exception("load notification cover failed!"+errorDrawable));
                    startForeground(NOTIFICATION_ID, notificationBuilder.build());
                }
            });
            notificationBuilder.setColor(Color.WHITE);
        } else {
            notificationBuilder.setContentTitle("");
            notificationBuilder.setContentText("");
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null)
            return super.onStartCommand(intent, flags, startId);
        Log.d(TAG, String.format("onStartCommand : flag: %d, startId: %d action:%s", flags, startId, intent.getAction()));
        if (ACTION_NEXT.equals(intent.getAction())) {
            mediaSession.getController().getTransportControls().skipToNext();
        } else if (ACTION_PAUSE.equals(intent.getAction())) {
            mediaSession.getController().getTransportControls().pause();
        } else if (ACTION_PREVIOUS.equals(intent.getAction())) {
            mediaSession.getController().getTransportControls().skipToPrevious();
        } else if (ACTION_PLAY.equals(intent.getAction())) {
            mediaSession.getController().getTransportControls().play();
        } else if (ACTION_SHOW_LRC.equals(intent.getAction())) {
            if (!ctrlBinder.isLrcWindowShow()) {
                ctrlBinder.showLrcFloatWindow();
            } else {
                ctrlBinder.hideLrcFloatWindow();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void alertException(Exception e) {
        App.getInstance().alertException(e);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(headsetActionReceiver);
            ctrlBinder.hideLrcFloatWindow();
            ctrlBinder.close();
            ctrlBinder = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CtrlBinder ctrlBinder;

    public class CtrlBinder extends Binder implements Closeable {
        private final List<JSONObject> playList = new ArrayList<>();
        private JSONObject current;
        private int currentIndex;
        //和 rjNumber 相同
        private long currentAlbumId;
        private Lrc mLrc = Lrc.NONE;
        private final Timer mLrcUpdateTimer;
        private final WindowManager windowManager;
        private boolean lrcWindowShow = false;
        private View lrcView;
        private Lrc.LrcRow currentLrcRow = null;
        private final Runnable stopTask;
        private final Runnable updateLrcTask;
        private final AsyncHttpClient.JSONObjectCallback lastPlayListCallback = new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
                if (e != null) {
                    return;
                }
                if (asyncHttpResponse.code() == 200) {
                    JSONArray jsonArray = null;
                    List<JSONObject> lastPlayList = new ArrayList<>();
                    try {
                        jsonArray = jsonObject.getJSONArray("playlist");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            lastPlayList.add(jsonArray.getJSONObject(i));
                        }
                        if (lastPlayList.size() != 0) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setReapAll();
                                    try {
                                        int index = jsonObject.getInt(JSONConst.LastPlayList.INDEX);
                                        long seek = jsonObject.getLong(JSONConst.LastPlayList.SEEK);
                                        play(lastPlayList, index);
                                        if (seek != 0) {
                                            getController().getTransportControls().seekTo(seek);
                                        }
                                        getController().getTransportControls().pause();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        alertException(e);
                                    }
                                }
                            });

                        }
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                        alertException(jsonException);
                    }
                }
            }
        };

        public CtrlBinder() {
            mLrcUpdateTimer = new Timer();
            windowManager = getSystemService(WindowManager.class);
            LocalFileCache.getInstance().readLastPlayList(AudioService.this, lastPlayListCallback);
            updateLrcTask = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.getPlaybackState() == Player.STATE_READY && mLrc!=null) {
                        Lrc.LrcRow lrcRow = mLrc.update(mediaPlayer.getCurrentPosition());
                        if (currentLrcRow != lrcRow) {
                            currentLrcRow = lrcRow;
                            if (lrcView instanceof TextView) {
                                ((TextView) lrcView).setText(currentLrcRow.content);
                            }
                            lrcRowChangeListeners.forEach(new Consumer<LrcRowChangeListener>() {
                                @Override
                                public void accept(LrcRowChangeListener listener) {
                                    listener.onSeekChange(currentLrcRow);
                                }
                            });
                        }

                    }
                }
            };
            mLrcUpdateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(updateLrcTask);
                }
            }, 0, 200);

            stopTask = new Runnable() {
                @Override
                public void run() {
                    MediaControllerCompat controllerCompat = getController();
                    if (controllerCompat == null)
                        return;
                    MediaControllerCompat.TransportControls transportControls = controllerCompat.getTransportControls();
                    if (transportControls == null)
                        return;
                    if(App.getInstance().noActiveActivity()){
                        Log.d(TAG, "timer stop: stop service");
                        transportControls.stop();
                        stopSelf();
                    }else{
                        Log.d(TAG, "timer stop: pause media");
                        transportControls.pause();
                    }
                }
            };
        }

        public void stopAfterMinutes(int minute) {
            mHandler.removeCallbacks(stopTask);
            mHandler.postDelayed(stopTask, minute * 60 * 1000L);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(AudioService.this, App.ID_PLAY_SERVICE);
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_STATUS);
            notificationBuilder.setSmallIcon(R.drawable.ic_baseline_audiotrack_24);
            notificationBuilder.setContentTitle("delay stop");
            notificationBuilder.setAutoCancel(true);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, minute);
            notificationBuilder.setContentText(SSDF.format(new Date(calendar.getTimeInMillis())));
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(NOTIFICATION_ID_CLOSE_DELAY, notificationBuilder.build());
        }

        public boolean isLrcWindowShow() {
            return lrcWindowShow;
        }

        public void setLrcWindowShow(boolean lrcWindowShow) {
            this.lrcWindowShow = lrcWindowShow;
        }

        private final AsyncHttpClient.JSONObjectCallback checkLrcCallBack = new AsyncHttpClient.JSONObjectCallback() {

            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject lrcResult) {
                boolean findLrc = false;
                if (e != null) {
                    mLrc = Lrc.NONE;
                    alertException(e);
                    Log.i(TAG,"LRC RESULT: ERR");
                }else if (lrcResult == null) {
                    mLrc = Lrc.NONE;
                    Log.i(TAG,"LRC RESULT: NULL");
                }else{
                    Log.i(TAG,"LRC RESULT:"+ lrcResult);
                    try {
                        boolean exist = lrcResult.optBoolean("result");
                        if (exist) {
                            findLrc = true;
                            Api.doGetMediaString(lrcResult.getString(JSONConst.WorkTree.HASH), lrcCallBack);
                        } else {
                            mLrc = Lrc.NONE;
                        }
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                        alertException(jsonException);
                        mLrc = Lrc.NONE;
                    }
                }
                if(!findLrc){
                    Log.d(TAG, "onCompleted: use audio lrc bind");
                    App.getInstance().getLrcBind(currentAlbumId, current.optString(JSONConst.WorkTree.MEDIA_STREAM_URL), new App.DatabaseResultCallback() {
                        @Override
                        public void onResult(Object result) {
                            if(result instanceof AudioLrcBind){
                                AudioLrcBind lrcBind = (AudioLrcBind) result;
                                Log.d(TAG, "onCompleted: find bind:"+lrcBind.getAudioPath() +", lrc_hash:"+ lrcBind.getLrcPath());
                                Api.doGetMediaString(lrcBind.getLrcPath(), lrcCallBack);
                            }

                        }
                    });
                }
                lrcRowChangeListeners.forEach(new Consumer<LrcRowChangeListener>() {
                    @Override
                    public void accept(LrcRowChangeListener listener) {
                        listener.onLrcChange(mLrc);
                    }
                });
            }
        };

        private AsyncHttpClient.StringCallback lrcCallBack = new AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
                if (e != null) {
                    mLrc = Lrc.NONE;
                    alertException(e);
                    return;
                }
                if (asyncHttpResponse == null) {
                    return;
                }
                if (asyncHttpResponse.code() == 200) {
                    mLrc = new Lrc(s);
                } else {
                    mLrc = Lrc.NONE;
                    Log.e(TAG, "onCompleted: " + asyncHttpResponse.code());
                }
                lrcRowChangeListeners.forEach(new Consumer<LrcRowChangeListener>() {
                    @Override
                    public void accept(LrcRowChangeListener listener) {
                        listener.onLrcChange(mLrc);
                    }
                });
            }
        };

        public ExoPlayer getExoPlayer() {
            return mediaPlayer;
        }

        private final List<MusicChangeListener> musicChangeListeners = new ArrayList<>();
        private final List<LrcRowChangeListener> lrcRowChangeListeners = new ArrayList<>();

        public MediaControllerCompat getController() {
            return mediaSession.getController();
        }

        public void addMusicChangeListener(MusicChangeListener listener) {
            musicChangeListeners.add(listener);
            if (current != null)
                updateLastBottomView(listener);
        }

        public boolean equalsCurrentPlay(String lrcTitle, int workId) {
            if (workId != currentAlbumId) {
                return false;
            }
            if (lrcTitle == null)
                return false;
            if (!lrcTitle.toLowerCase(Locale.ROOT).endsWith(".lrc"))
                return false;
            if (current == null)
                return false;
            try {
                String musicTitle = current.getString("title");
                if (musicTitle.isEmpty())
                    return false;
                if (!musicTitle.contains("."))
                    return false;
                String beforeLrcTitle = lrcTitle.substring(0, lrcTitle.lastIndexOf('.'));
                String beforeMusicTitle = musicTitle.substring(0, musicTitle.lastIndexOf('.'));
                return beforeLrcTitle.equals(beforeMusicTitle);
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
            return false;
        }

        public void removeMusicChangeListener(MusicChangeListener listener) {
            musicChangeListeners.remove(listener);
        }

        public void addLrcChangeListener(LrcRowChangeListener listener) {
            lrcRowChangeListeners.add(listener);
        }

        public void removeLrcChangeListener(LrcRowChangeListener listener) {
            lrcRowChangeListeners.remove(listener);
        }

        private void setCurrentAlbumId(long currentAlbumId) {
            this.currentAlbumId = currentAlbumId;
            musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                @Override
                public void accept(MusicChangeListener listener) {
                    listener.onAlbumChange(currentAlbumId);
                }
            });
        }

        public void play(List<JSONObject> playList, int index) throws JSONException {
            if (playList.isEmpty())
                return;
            this.playList.clear();
            this.playList.addAll(playList);
            currentIndex = index;
            current = playList.get(index);
            int currentAlbumId = current.getInt(JSONConst.WorkTree.WORK_ID);
            setCurrentAlbumId(currentAlbumId);
            List<MediaItem> mediaItemList = new ArrayList<>();
            for (int i = 0; i < playList.size(); i++) {
                MediaItem mediaItem;
                MediaItem.Builder builder = new MediaItem.Builder();
                JSONObject music = playList.get(i);
                String path = music.getString(JSONConst.WorkTree.MAP_FILE_PATH);
                File audioFile = new File(path);
                if (audioFile.exists()) {
                    builder.setUri(Uri.fromFile(audioFile));
                } else {
                    path = music.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
                    if (path.startsWith("http")) {
                        path = path + "?token=" + Api.token;
                    } else {
                        path = App.getInstance().currentUser().getHost() + path + "?token=" + Api.token;
                    }
                    builder.setUri(path);
                }
                MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder();
                metaBuilder.setTitle(music.getString("title"));
                builder.setMediaMetadata(metaBuilder.build());

                mediaItem = builder.build();

                mediaItemList.add(mediaItem);
            }

            mediaPlayer.setMediaItems(mediaItemList, index, 0);
            mediaPlayer.prepare();
            mediaPlayer.play();
        }

        public JSONObject getCurrent() {
            return current;
        }

        public long getCurrentAlbumId() {
            return currentAlbumId;
        }

        public Lrc getLrc() {
            return mLrc;
        }

        public void setLrc(String lrcText){
            mLrc = new Lrc(lrcText);
        }

        public void insertLrcBind(String lrcPath){
            if(current==null){
                return;
            }
            String audioUrl = current.optString(JSONConst.WorkTree.MEDIA_STREAM_URL);
            long riNumber = currentAlbumId;
            AudioLrcBind audioLrcBind = new AudioLrcBind(riNumber,audioUrl,lrcPath);
            App.getInstance().insertLrcBind(audioLrcBind);
        }

        public WindowManager.LayoutParams getLrcWindowParams() {
            return lrcWindowParams;
        }

        public View getLrcFloatView() {
            return lrcView;
        }

        public void setLrcView(View lrcView) {
            this.lrcView = lrcView;
        }

        public void showLrcFloatWindow() {
            if (!Settings.canDrawOverlays(getApplicationContext()) || lrcView == null) {
                Intent rqIntent = new Intent(AudioService.this, LrcFloatWindow.class);
                rqIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(rqIntent);
            } else {
                if (!lrcWindowShow) {
                    windowManager.addView(lrcView, lrcWindowParams);
                }
                lrcWindowShow = true;
            }
        }

        public void hideLrcFloatWindow() {
            if (lrcView == null)
                return;
            if (lrcWindowShow) {
                windowManager.removeView(lrcView);
            }
            lrcWindowShow = false;
        }

        public void setReapAll() {
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        }

        public void setReapOne() {
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        }

        public void setReapOff() {
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        }

        public int getReapMode() {
            return mediaPlayer.getRepeatMode();
        }

        public String getCurrentTitle() throws JSONException {
            if (current == null)
                return "";
            return current.getString("title");
        }

        public String getCurrentWorkTitle() throws JSONException {
            if (current == null)
                return "";
            return current.getString("workTitle");
        }

        private void updateLastBottomView(MusicChangeListener listener) {
            listener.onAlbumChange(currentAlbumId);
            listener.onStatusChange(mediaPlayer.isPlaying() ? 1 : 0);
            listener.onAudioChange(current);
        }


        @Override
        public void close() throws IOException {
            mLrcUpdateTimer.cancel();
            long seek = 0;
            PlaybackStateCompat playbackStateCompat = getController().getPlaybackState();
            if (playbackStateCompat != null) {
                if (playbackStateCompat.getState() != PlaybackStateCompat.STATE_STOPPED) {
                    seek = getExoPlayer().getCurrentPosition();
                    getController().getTransportControls().stop();
                }
                LocalFileCache.getInstance().savePlayList(AudioService.this, playList, currentIndex, seek);
            }

        }
    }
}
