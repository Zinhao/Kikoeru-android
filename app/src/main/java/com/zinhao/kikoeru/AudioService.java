package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AudioService extends Service {
    private static final String TAG= "AudioService";
    private static final String ACTION_PAUSE = "com.zinhao.kikoeru.ACTION_PAUSE";
    private static final String ACTION_PLAY = "com.zinhao.kikoeru.ACTION_PLAY";
    private static final String ACTION_PREVIOUS = "com.zinhao.kikoeru.ACTION_PREVIOUS";
    private static final String ACTION_NEXT="com.zinhao.kikoeru.ACTION_NEXT";
    private ExoPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
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
        ctrlBinder = new CtrlBinder();
        mediaPlayer = new ExoPlayer.Builder(this).build();

        mediaPlayer.addListener(new Player.Listener() {
            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {
                Log.d(TAG, "onTimelineChanged: ");
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "onIsPlayingChanged: ");
                try {
                    updateMediaSessionMetaData();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.d(TAG, "onPlaybackStateChanged: "+playbackState);
                if(playbackState == Player.STATE_ENDED && mediaPlayer.getRepeatMode() != Player.REPEAT_MODE_ONE){
                    ctrlBinder.skipToNext();
                }
            }

            @Override
            public void onSeekBackIncrementChanged(long seekBackIncrementMs) {
                Log.d(TAG, "onSeekBackIncrementChanged: "+seekBackIncrementMs);
            }

            @Override
            public void onSeekForwardIncrementChanged(long seekForwardIncrementMs) {
                Log.d(TAG, "onSeekForwardIncrementChanged: "+seekForwardIncrementMs);
            }
        });
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
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, MainActivity.HOST+ctrlBinder.current.getString("mediaStreamUrl"))
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


            Intent previousIntent = new Intent(this,AudioService.class);
            previousIntent.setAction(ACTION_PREVIOUS);
            PendingIntent previousPendingIntent = PendingIntent.getService(this, 1, previousIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent playIntent = new Intent(this,AudioService.class);
            playIntent.setAction(ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent pauseIntent = new Intent(this,AudioService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pausePendingIntent = PendingIntent.getService(this, 3, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent nextIntent = new Intent(this,AudioService.class);
            nextIntent.setAction(ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(this, 4, nextIntent, PendingIntent.FLAG_IMMUTABLE);


            notificationBuilder.addAction(android.R.drawable.ic_media_previous,"previous",previousPendingIntent);
            if(mediaPlayer.isPlaying()){
                notificationBuilder.addAction(R.drawable.ic_baseline_pause_24,"pause",pausePendingIntent);
            }else {
                notificationBuilder.addAction(R.drawable.ic_baseline_play_arrow_24,"play",playPendingIntent);
            }
            notificationBuilder.addAction(android.R.drawable.ic_media_next,"next",nextPendingIntent);

            notificationBuilder.setContentTitle(ctrlBinder.current.getString("title"));
            notificationBuilder.setContentText(ctrlBinder.current.getString("title"));
            notificationBuilder.setSmallIcon(R.drawable.ic_baseline_audiotrack_24);
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);
            notificationBuilder.setShowWhen(true);
            notificationBuilder.setColorized(true);
            notificationBuilder.setContentIntent(null);
            Glide.with(this).asBitmap().load(MainActivity.HOST+String.format("/api/cover/%d?type=sam",ctrlBinder.currentAlbumId)).into(new SimpleTarget<Bitmap>() {
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
            ctrlBinder.listeners.forEach(new Consumer<MusicChangeListener>() {
                @Override
                public void accept(MusicChangeListener listener) {
                    listener.onAudioChange(ctrlBinder.current);
                    listener.onStatusChange(mediaPlayer.isPlaying()?1:0);
                }
            });
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

    public class CtrlBinder extends Binder{
        private List<JSONObject> playList;
        private JSONObject current;
        private int currentIndex;
        private int currentAlbumId;
        private Lrc mLrc;
        private AsyncHttpClient.StringCallback checkLrcCallBack = new AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
                try {
                    JSONObject lrcResult = new JSONObject(s);
                    boolean exist = lrcResult.getBoolean("result");
                    if(exist){
                        downLoadLrc(lrcResult.getString("hash"));
                    }else{
                        mLrc = null;
                    }
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
        };

        private AsyncHttpClient.StringCallback lrcCallBack = new AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
                if(asyncHttpResponse == null)
                    return;
                if(asyncHttpResponse.code() == 200){
                    mLrc = new Lrc(s);
                    Log.d(TAG, "onCompleted: "+s);
                }else {
                    Log.d(TAG, "onCompleted: "+asyncHttpResponse.code());
                }
            }
        };

        public ExoPlayer getExoPlayer(){
            return mediaPlayer;
        }

        private List<MusicChangeListener> listeners = new ArrayList<>();

        public MediaControllerCompat getCtrl(){
            return mediaSession.getController();
        }

        public void addListener(MusicChangeListener listener){
            listeners.add(listener);
            if(current != null)
                updateLastBottomView(listener);
        }

        public void removeListener(MusicChangeListener listener){
            listeners.remove(listener);
        }

        public void play(JSONObject music) throws JSONException {
            current = music;
            mediaPlayer.setMediaItem(MediaItem.fromUri(MainActivity.HOST+ current.getString("mediaStreamUrl")));
            mediaPlayer.prepare();
            mediaPlayer.play();
            checkLrc();
        }

        private void checkLrc() throws JSONException {
            //检查歌词 http://localhost:8888/api/media/check-lrc/363822/5
            AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(App.HOST+String.format("/api/media/check-lrc/%s",current.getString("hash"))),"GET");
            Log.d(TAG, "checkLrc: "+request.getUri());
            request.setTimeout(5000);
            AsyncHttpClient.getDefaultInstance().executeString(request, checkLrcCallBack);
        }

        private void downLoadLrc(String hash){
            // http://localhost:8888/api/media/stream/363822/4
            AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(App.HOST+String.format("/api/media/stream/%s",hash)),"GET");
            Log.d(TAG, "downLoadLrc: "+request.getUri());
            request.setTimeout(5000);
            AsyncHttpClient.getDefaultInstance().executeString(request, lrcCallBack);
        }

        public void setCurrentAlbumId(int currentAlbumId) {
            this.currentAlbumId = currentAlbumId;
            ctrlBinder.listeners.forEach(new Consumer<MusicChangeListener>() {
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
            }
        }

        public String getLrcText() {
            if(mLrc == null){
                return "无歌词";
            }
            Lrc.LrcRow row = mLrc.update(mediaPlayer.getCurrentPosition());
            if (row!=null){
                return row.content;
            }
            return "无歌词";
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

    }
}
