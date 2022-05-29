package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.metadata.Metadata;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class AudioService extends Service{
    private static final String TAG= "AudioService";
    private static final String ACTION_PAUSE = "com.zinhao.kikoeru.ACTION_PAUSE";
    private static final String ACTION_PLAY = "com.zinhao.kikoeru.ACTION_PLAY";
    private static final String ACTION_PREVIOUS = "com.zinhao.kikoeru.ACTION_PREVIOUS";
    private static final String ACTION_NEXT="com.zinhao.kikoeru.ACTION_NEXT";
    private static final String ACTION_SHOW_LRC= "com.zinhao.kikoeru.ACTION_SHOW_LRC";
    private Handler mHandler;
    private ExoPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;

    private WindowManager.LayoutParams lrcWindowParams;

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
    };
    private static final int NOTIFICATION_ID = 12;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return ctrlBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this,getClass().getSimpleName());
        mediaSession.setCallback(callback);
        float[] position = App.getInstance().getPosition();
        lrcWindowParams = makeFloatWindowParams(position[0],position[1]);
        mHandler = new Handler(getMainLooper());
        mediaPlayer = new ExoPlayer.Builder(this).build();
        mediaPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "onIsPlayingChanged: "+isPlaying);
                ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                    @Override
                    public void accept(MusicChangeListener listener) {
                        listener.onStatusChange(mediaPlayer.isPlaying()?1:0);
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
                Log.d(TAG, String.format("onPlaybackStateChanged:playbackState: %d ,index: %d",playbackState,mediaPlayer.getCurrentMediaItemIndex()));
                ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                    @Override
                    public void accept(MusicChangeListener listener) {
                        listener.onStatusChange(mediaPlayer.isPlaying()?1:0);
                    }
                });
                try {
                    if(playbackState == Player.STATE_READY){
                        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, (String) mediaPlayer.getMediaMetadata().title)
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, null)
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, (String) mediaPlayer.getMediaMetadata().artist)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, null)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,mediaPlayer.getDuration())
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
                Log.d(TAG, "onMediaMetadataChanged: "+mediaMetadata.title);
                ctrlBinder.current = ctrlBinder.playList.get(mediaPlayer.getCurrentMediaItemIndex());

                MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, (String) mediaMetadata.title)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, null)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, null)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, null)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,mediaPlayer.getDuration())
                        .build();
                mediaSession.setMetadata(metadata);
                try {
                    updateNotificationState();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    String path = ctrlBinder.current.getString(JSONConst.WorkTree.MAP_FILE_PATH);
                    File audioFile = new File(path);
                    if(!LocalFileCache.getInstance().getLrcText(audioFile,ctrlBinder.lrcCallBack)){
                        Api.checkLrc(ctrlBinder.current.getString(JSONConst.WorkTree.HASH),ctrlBinder.checkLrcCallBack);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                    @Override
                    public void accept(MusicChangeListener listener) {
                        listener.onAudioChange(ctrlBinder.current);
                    }
                });
            }

            @Override
            public void onMetadata(@NonNull Metadata metadata) {
                Log.d(TAG, "onMetadata: "+metadata.toString());
            }
        });

        ctrlBinder = new CtrlBinder();
    }

    private static WindowManager.LayoutParams makeFloatWindowParams(float x, float y){
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

    PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();

    @SuppressLint("DefaultLocale")
    private void updateNotificationState() throws JSONException {
        if(mediaPlayer.getPlaybackState() == Player.STATE_BUFFERING){
            mediaSession.setActive(false);
            builder.setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1);
        }else if(mediaPlayer.isPlaying()){
            mediaSession.setActive(true);
            builder.setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1);
        }else if(mediaPlayer.getPlaybackState() == Player.STATE_READY){
            mediaSession.setActive(false);
            builder.setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1);
        }else {
            mediaSession.setActive(false);
            builder.setState(PlaybackStateCompat.STATE_STOPPED, mediaPlayer.getCurrentPosition(), 1);
        }
        builder.setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|
                PlaybackStateCompat.ACTION_STOP|
                PlaybackStateCompat.ACTION_PAUSE|
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_SET_RATING);
        mediaSession.setPlaybackState(builder.build());

        if(ctrlBinder.current !=null){
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,App.ID_PLAY_SERVICE);

            androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle();
            mediaStyle.setShowActionsInCompactView(1);
            mediaStyle.setMediaSession(mediaSession.getSessionToken());
            notificationBuilder.setStyle(mediaStyle);

            Intent previousIntent = new Intent(this,LrcFloatWindow.class);
            previousIntent.setAction(ACTION_SHOW_LRC);
            previousIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent showLrcPendingIntent = PendingIntent.getActivity(this, 1, previousIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent playIntent = new Intent(this,AudioService.class);
            playIntent.setAction(ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent pauseIntent = new Intent(this,AudioService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pausePendingIntent = PendingIntent.getService(this, 3, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent nextIntent = new Intent(this,AudioService.class);
            nextIntent.setAction(ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(this, 4, nextIntent, PendingIntent.FLAG_IMMUTABLE);


            notificationBuilder.addAction(R.drawable.ic_baseline_text_fields_24,"show lrc",showLrcPendingIntent);
            if(mediaPlayer.isPlaying()){
                notificationBuilder.addAction(R.drawable.ic_baseline_pause_24,"pause",pausePendingIntent);
            }else {
                notificationBuilder.addAction(R.drawable.ic_baseline_play_arrow_24,"play",playPendingIntent);
            }
            notificationBuilder.addAction(R.drawable.ic_baseline_skip_next_24,"next",nextPendingIntent);

            notificationBuilder.setContentTitle(ctrlBinder.current.getString("title"));
            notificationBuilder.setContentText(ctrlBinder.current.getString("title"));
            notificationBuilder.setSmallIcon(R.drawable.ic_baseline_audiotrack_24);
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);
            notificationBuilder.setShowWhen(true);
            notificationBuilder.setColorized(true);
            notificationBuilder.setContentIntent(null);
            Glide.with(this).asBitmap().load(Api.HOST+String.format("/api/cover/%d?type=sam",ctrlBinder.currentAlbumId)).into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                    notificationBuilder.setLargeIcon(bitmap);
                    startForeground(NOTIFICATION_ID, notificationBuilder.build());
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    super.onLoadFailed(errorDrawable);
                    alertException(new Exception("load notification cover failed!"));
                    startForeground(NOTIFICATION_ID, notificationBuilder.build());
                }
            });
            notificationBuilder.setColor(Color.WHITE);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null || intent.getAction() == null)
            return super.onStartCommand(intent, flags, startId);
        Log.d(TAG, String.format("onStartCommand : flag: %d, startId: %d action:%s",flags,startId,intent.getAction()));
        if(ACTION_NEXT.equals(intent.getAction())){
            mediaSession.getController().getTransportControls().skipToNext();
        }else if(ACTION_PAUSE.equals(intent.getAction())){
            mediaSession.getController().getTransportControls().pause();
        }else if(ACTION_PREVIOUS.equals(intent.getAction())){
            mediaSession.getController().getTransportControls().skipToPrevious();
        }else if(ACTION_PLAY.equals(intent.getAction())){
            mediaSession.getController().getTransportControls().play();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void alertException(Exception e){
        Activity activity = App.getInstance().getBackHelper().getLastActivity();
        if(activity == null){
            return;
        }
        if(activity instanceof BaseActivity){
            ((BaseActivity) activity).alertException(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
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
        private int currentAlbumId;
        private Lrc mLrc = Lrc.NONE;
        private Timer mLrcUpdateTimer;

        private WindowManager windowManager;
        private boolean lrcWindowShow = false;
        private View lrcView;
        private AsyncHttpClient.JSONObjectCallback lastPlayListCallback = new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
                if(e!= null){
                    alertException(e);
                    return;
                }
                if(asyncHttpResponse.code() == 200){
                    JSONArray jsonArray = null;
                    List<JSONObject> lastPlayList = new ArrayList<>();
                    try {
                        jsonArray = jsonObject.getJSONArray("playlist");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            lastPlayList.add(jsonArray.getJSONObject(i));
                        }
                        if(lastPlayList.size()!=0){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setReapAll();
                                    try {
                                        int index = jsonObject.getInt(JSONConst.LastPlayList.INDEX);
                                        long seek = jsonObject.getLong(JSONConst.LastPlayList.SEEK);
                                        play(lastPlayList,index);
                                        if(seek!= 0){
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
            LocalFileCache.getInstance().readLastPlayList(AudioService.this,lastPlayListCallback);
            mLrcUpdateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(mediaPlayer!=null && mediaPlayer.isPlaying()){
                                ctrlBinder.mLrc.update(mediaPlayer.getCurrentPosition());
                                if(lrcView instanceof TextView){
                                    ((TextView) lrcView).setText(mLrc.getCurrent().content);
                                }
                            }
                        }
                    });
                    lrcRowChangeListeners.forEach(new Consumer<LrcRowChangeListener>() {
                        @Override
                        public void accept(LrcRowChangeListener listener) {
                            listener.onChange(mLrc.getCurrent());
                        }
                    });
                }
            },0,500);
        }

        public boolean isLrcWindowShow() {
            return lrcWindowShow;
        }

        public void setLrcWindowShow(boolean lrcWindowShow) {
            this.lrcWindowShow = lrcWindowShow;
        }

        public WindowManager getWindowManager() {
            return windowManager;
        }

        public void setWindowManager(WindowManager windowManager) {
            this.windowManager = windowManager;
        }

        private AsyncHttpClient.JSONObjectCallback checkLrcCallBack = new AsyncHttpClient.JSONObjectCallback() {

            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject lrcResult) {
                if(e!=null){
                    alertException(e);
                    return;
                }
                if(lrcResult == null){
                    mLrc = Lrc.NONE;
                    return;
                }
                try {
                    boolean exist = lrcResult.getBoolean("result");
                    if(exist){
                        Api.doGetMediaString(lrcResult.getString(JSONConst.WorkTree.HASH),lrcCallBack);
                    }else{
                        mLrc = Lrc.NONE;
                    }
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                    alertException(jsonException);
                }
            }
        };

        private AsyncHttpClient.StringCallback lrcCallBack = new AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
                if(e != null){
                    alertException(e);
                    return;
                }
                if(asyncHttpResponse == null){
                    return;
                }
                if(asyncHttpResponse.code() == 200){
                    Log.d(TAG, "onCompleted: "+s);
                    mLrc = new Lrc(s);
                }else {
                    mLrc = Lrc.NONE;
                    Log.d(TAG, "onCompleted: "+asyncHttpResponse.code());
                }
            }
        };

        public ExoPlayer getExoPlayer(){
            return mediaPlayer;
        }

        private final List<MusicChangeListener> musicChangeListeners = new ArrayList<>();
        private final List<LrcRowChangeListener> lrcRowChangeListeners = new ArrayList<>();

        public MediaControllerCompat getController(){
            return mediaSession.getController();
        }

        public void addMusicChangeListener(MusicChangeListener listener){
            musicChangeListeners.add(listener);
            if(current != null)
                updateLastBottomView(listener);
        }

        public boolean equalsCurrentPlay(String lrcTitle,int workId){
            if(workId != currentAlbumId){
                return false;
            }
            if(lrcTitle == null)
                return false;
            if(!lrcTitle.toLowerCase(Locale.ROOT).endsWith(".lrc"))
                return false;
            if(current == null)
                return false;
            try {
                String musicTitle = current.getString("title");
                if(musicTitle.isEmpty())
                    return false;
                if(!musicTitle.contains("."))
                    return false;
                String beforeLrcTitle = lrcTitle.substring(0,lrcTitle.lastIndexOf('.'));
                String beforeMusicTitle =  musicTitle.substring(0,musicTitle.lastIndexOf('.'));
                return  beforeLrcTitle.equals(beforeMusicTitle);
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
            return false;
        }

        public void removeMusicChangeListener(MusicChangeListener listener){
            musicChangeListeners.remove(listener);
        }

        public void addLrcRowChangeListener(LrcRowChangeListener listener){
            lrcRowChangeListeners.add(listener);
        }

        public void removeLrcRowChangeListener(LrcRowChangeListener listener){
            lrcRowChangeListeners.remove(listener);
        }

        private void setCurrentAlbumId(int currentAlbumId) {
            this.currentAlbumId = currentAlbumId;
            ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                @Override
                public void accept(MusicChangeListener listener) {
                    listener.onAlbumChange(currentAlbumId);
                }
            });
        }

        public void play(List<JSONObject> playList, int index) throws JSONException {
            if(playList.size() ==0)
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
                if(audioFile.exists()){
                    builder.setUri(Uri.fromFile(audioFile));
                }else {
                    path =music.getString(JSONConst.WorkTree.MEDIA_STREAM_URL);
                    if(path.startsWith("http")){
                        path = path + "?token=" + Api.token;
                    }else {
                        path = Api.HOST + path + "?token=" + Api.token;
                    }
                    builder.setUri(path);
                }
                MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder();
                metaBuilder.setTitle(music.getString("title"));
                builder.setMediaMetadata(metaBuilder.build());

                mediaItem = builder.build();

                mediaItemList.add(mediaItem);
            }
            mediaPlayer.setMediaItems(mediaItemList,index,0);
            mediaPlayer.prepare();
            mediaPlayer.play();
        }

        public JSONObject getCurrent() {
            return current;
        }

        public int getCurrentAlbumId() {
            return currentAlbumId;
        }

        public Lrc getLrc(){
            return mLrc;
        }

        public WindowManager.LayoutParams getLrcWindowParams(){
            return lrcWindowParams;
        }

        public View getLrcFloatView(){
            return lrcView;
        }

        public void setLrcView(View lrcView) {
            this.lrcView = lrcView;
        }

        public void showOrHideLrcFloatWindow(){
            if(!lrcWindowShow){
                lrcWindowShow = true;
                windowManager.addView(lrcView, lrcWindowParams);
            }else {
                lrcWindowShow = false;
                windowManager.removeView(lrcView);
            }
        }

        public void setReapAll(){
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        }

        public void setReapOne(){
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        }

        public void setReapOff(){
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        }

        public String getCurrentTitle() throws JSONException {
            if(current == null)
                return "";
            return current.getString("title");
        }

        public String getCurrentWorkTitle() throws JSONException {
            if(current == null)
                return "";
            return current.getString("workTitle");
        }

        private void updateLastBottomView(MusicChangeListener listener){
            listener.onAlbumChange(currentAlbumId);
            listener.onStatusChange(mediaPlayer.isPlaying()?1:0);
            listener.onAudioChange(current);
        }


        @Override
        public void close() throws IOException {
            mLrcUpdateTimer.cancel();
            long seek = 0;
            PlaybackStateCompat playbackStateCompat = getController().getPlaybackState();
            if(playbackStateCompat != null){
                if(playbackStateCompat.getState() != PlaybackStateCompat.STATE_STOPPED){
                    seek = getExoPlayer().getCurrentPosition();
                    getController().getTransportControls().stop();
                }
                LocalFileCache.getInstance().savePlayList(AudioService.this,playList,currentIndex,seek);
            }

        }
    }
}
