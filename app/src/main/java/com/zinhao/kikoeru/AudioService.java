package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
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

    private MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
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
            ctrlBinder.skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            ctrlBinder.skipToPrevious();
        }

        @Override
        public void onStop() {
            super.onStop();
            mediaPlayer.stop();
        }
    };

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
                Log.d(TAG, "onIsPlayingChanged: ");
                try {
                    updateMediaSessionMetaData();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                    @Override
                    public void accept(MusicChangeListener listener) {
                        listener.onStatusChange(mediaPlayer.isPlaying()?1:0);
                    }
                });
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.d(TAG, "onPlaybackStateChanged: "+playbackState);
                if(playbackState == Player.STATE_ENDED && mediaPlayer.getRepeatMode() != Player.REPEAT_MODE_ONE){
                    ctrlBinder.skipToNext();
                }
                if(playbackState == Player.STATE_READY){
                    ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                        @Override
                        public void accept(MusicChangeListener listener) {
                            listener.onAudioChange(ctrlBinder.current);
                        }
                    });
                }
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
    private void updateMediaSessionMetaData() throws JSONException {
        MediaMetadataCompat metadata = null;
        try {
            metadata = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,ctrlBinder.current.getString("title"))
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, null)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, null)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, null)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, Api.HOST+ctrlBinder.current.getString("mediaStreamUrl"))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,mediaPlayer.getDuration())
                    .build();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mediaSession.setMetadata(metadata);
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
                    startForeground(12, notificationBuilder.build());
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    super.onLoadFailed(errorDrawable);
                    Log.e(TAG, "onLoadFailed: load notification large icon fail.");
                    startForeground(12, notificationBuilder.build());
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

    private CtrlBinder ctrlBinder;

    public class CtrlBinder extends Binder implements Closeable {
        private List<JSONObject> playList;
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
                if(asyncHttpResponse.code() == 200){
                    JSONArray jsonArray = null;
                    if(playList == null)
                        playList = new ArrayList<>();
                    try {
                        jsonArray = jsonObject.getJSONArray("playlist");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            playList.add(jsonArray.getJSONObject(i));
                        }
                        if(playList.size()!=0){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setReap();
                                    try {
                                        setCurrentAlbumId(jsonObject.getInt("id"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        play(playList,0);
                                        getCtrl().getTransportControls().pause();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
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
                if(lrcResult ==null){
                    mLrc = Lrc.NONE;
                    return;
                }
                try {
                    boolean exist = lrcResult.getBoolean("result");
                    if(exist){
                        Api.doGetMediaString(lrcResult.getString("hash"),lrcCallBack);
                    }else{
                        mLrc = Lrc.NONE;
                    }
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
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

        private void alertException(Exception e){
            Activity activity = App.getInstance().getBackHelper().getLastActivity();
            if(activity == null){
                return;
            }
            if(activity instanceof BaseActivity){
                ((BaseActivity) activity).alertException(e);
            }
        }

        public ExoPlayer getExoPlayer(){
            return mediaPlayer;
        }

        private List<MusicChangeListener> musicChangeListeners = new ArrayList<>();
        private List<LrcRowChangeListener> lrcRowChangeListeners = new ArrayList<>();

        public MediaControllerCompat getCtrl(){
            return mediaSession.getController();
        }

        public void addMusicChangeListener(MusicChangeListener listener){
            musicChangeListeners.add(listener);
            if(current != null)
                updateLastBottomView(listener);
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

        private void play(JSONObject music) throws JSONException {
            current = music;
            MediaItem mediaItem;
            String path =current.getString("mediaStreamUrl");
            if(path.startsWith("http")){
                path = path + "?token=" + Api.token;
            }else {
                path = Api.HOST + path + "?token=" + Api.token;
            }
            if(music.has("local_file_path")){
                path = music.getString("local_file_path");
                File audioFile = new File(path);
                LocalFileCache.getInstance().getLrcText(AudioService.this,audioFile,lrcCallBack);
                mediaItem = MediaItem.fromUri(Uri.fromFile(audioFile));
            }else {
                Log.d(TAG, "play: network file");
                Api.checkLrc(current.getString("hash"),checkLrcCallBack);
                mediaItem = MediaItem.fromUri(path);
            }
            Log.d(TAG, "play: "+path);
            mediaPlayer.setMediaItem(mediaItem);
            mediaPlayer.prepare();
            mediaPlayer.play();
        }

        public void setCurrentAlbumId(int currentAlbumId) {
            this.currentAlbumId = currentAlbumId;
            ctrlBinder.musicChangeListeners.forEach(new Consumer<MusicChangeListener>() {
                @Override
                public void accept(MusicChangeListener listener) {
                    listener.onAlbumChange(currentAlbumId);
                }
            });
        }

        public void play(List<JSONObject> playList, int index) throws JSONException {
            this.playList = playList;
            currentIndex = index;
            play(playList.get(index));
        }

        public void skipToNext(){
            if(currentIndex == playList.size()-1){
                currentIndex = 0;
            }else{
                currentIndex++;
            }
            try {
                play(playList.get(currentIndex));
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
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

        public void skipToPrevious(){
            if(currentIndex == 0){
                currentIndex = playList.size()-1;
            }else{
                currentIndex--;
            }
            try {
                play(playList.get(currentIndex));
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
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

        public void setReap(){
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
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
            LocalFileCache.getInstance().savePlayList(AudioService.this,playList,currentAlbumId);
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
}
