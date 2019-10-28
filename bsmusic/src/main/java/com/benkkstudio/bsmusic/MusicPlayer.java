package com.benkkstudio.bsmusic;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MusicPlayer implements CurrentSessionCallback{
    private static volatile MusicPlayer instance = null;
    private Context context;
    private ModelMusic modelMusic;
    private PendingIntent pendingIntent;
    private ArrayList<ModelMusic> arrayList = new ArrayList<>();
    private int currentPos, currentProgress;
    private boolean Lirik = false, Repeate = false, RandomMusic = false;
    private static MediaPlayer mediaPlayer = new MediaPlayer();
    public CurrentSessionCallback currentSessionCallback;
    private Class<?> mainClass;
    static volatile Handler applicationHandler = null;
    private int NONE_STATE = PlaybackStateCompat.STATE_NONE;
    public int PLAYING_STATE = PlaybackStateCompat.STATE_PLAYING;
    private int STOP_STATE = PlaybackStateCompat.STATE_STOPPED;
    public int PAUSE_STATE = PlaybackStateCompat.STATE_PAUSED;
    private int SKIP_STATE = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
    private int PREV_STATE = PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
    private Activity activity;
    public static MusicPlayer getInstance(Context context) {
        if (instance == null) {
            synchronized (MusicPlayer.class) {
                instance = new MusicPlayer();
                instance.context = context;
                applicationHandler = new Handler(context.getMainLooper());
                if (instance.currentSessionCallback != null) {
                    instance.currentSessionCallback.updatePlaybackState(instance.NONE_STATE);
                }
            }
        }
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(instance.Repeate){
                    instance.onPlay();
                    if (instance.currentSessionCallback != null) {
                        instance.currentSessionCallback.updatePlaybackState(instance.PLAYING_STATE);
                    }
                } else {
                    if (instance.currentPos != (instance.arrayList.size() - 1)) {
                        instance.currentPos++;
                        instance.onPlay();
                        if (instance.currentSessionCallback != null) {
                            instance.currentSessionCallback.updatePlaybackState(instance.PLAYING_STATE);
                            instance.currentSessionCallback.updatePlaybackState(instance.SKIP_STATE);
                        }
                    }
                }
                if (instance.currentSessionCallback != null) {
                    instance.currentSessionCallback.playSongComplete();
                }
            }
        });
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
        return instance;
    }

    @Override
    public void updatePlaybackState(int state) {

    }

    @Override
    public void playSongComplete() {

    }

    @Override
    public void currentSeekBarPosition(int progress) {

    }

    @Override
    public void playCurrent(ModelMusic modelMusic) {

    }

    @Override
    public void playNext(ModelMusic modelMusic) {

    }

    @Override
    public void playPrevious(ModelMusic modelMusic) {

    }



    private void setPendingIntent() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (pendingIntent != null) {
                    NotificationManager.getInstance().postNotificationName(NotificationManager.setAnyPendingIntent, pendingIntent);
                }
            }
        }, 400);
    }

    public void onResume() {
        try {
            modelMusic = arrayList.get(currentPos);
            onPrepare();
            mediaPlayer.seekTo(getCurrentProgress());
            mediaPlayer.start();
            if (instance.currentSessionCallback != null) {
                instance.currentSessionCallback.updatePlaybackState(instance.PLAYING_STATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    setCurrentProgress(mediaPlayer.getCurrentPosition());
                    if (instance.currentSessionCallback != null) {
                        instance.currentSessionCallback.updatePlaybackState(instance.PAUSE_STATE);
                    }
                }
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
    }


    public void stop(Context context, boolean stopService) {
        setCurrentProgress(mediaPlayer.getCurrentPosition());
        mediaPlayer.stop();
        if (instance.currentSessionCallback != null) {
            instance.currentSessionCallback.updatePlaybackState(instance.STOP_STATE);
        }
    }


    public void onPlay() {
        if (instance.currentSessionCallback != null && mediaPlayer != null && mediaPlayer.isPlaying()) {
            instance.currentSessionCallback.updatePlaybackState(instance.PAUSE_STATE);
        }
        try {
            modelMusic = arrayList.get(currentPos);
            onPrepare();
            mediaPlayer.start();
            //showNotification();
            if (instance.currentSessionCallback != null) {
                currentSessionCallback.playCurrent(modelMusic);
                instance.currentSessionCallback.updatePlaybackState(instance.PLAYING_STATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void onNext() {
        if (instance.currentSessionCallback != null && mediaPlayer != null && mediaPlayer.isPlaying()) {
            instance.currentSessionCallback.updatePlaybackState(instance.PAUSE_STATE);
        }
        if (currentPos != (arrayList.size() - 1)) {
            currentPos++;
            try {
                modelMusic = arrayList.get(currentPos);
                onPrepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (instance.currentSessionCallback != null) {
                currentSessionCallback.playNext(modelMusic);
                instance.currentSessionCallback.updatePlaybackState(instance.SKIP_STATE);
            }
        }
    }

    public void onPrev() {
        if (instance.currentSessionCallback != null && mediaPlayer != null && mediaPlayer.isPlaying()) {
            instance.currentSessionCallback.updatePlaybackState(instance.PAUSE_STATE);
        }
        if (currentPos != 0) {
            currentPos--;
            try {
                modelMusic = arrayList.get(currentPos);
                onPrepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (instance.currentSessionCallback != null) {
                currentSessionCallback.playPrevious(modelMusic);
                instance.currentSessionCallback.updatePlaybackState(instance.PREV_STATE);
            }
        }
    }

    private void onPrepare() throws IOException {
        handleMediaPlayer();
        AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(arrayList.get(getCurrentPos()).getMusicUrl());
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
        assetFileDescriptor.close();
        mediaPlayer.prepare();
    }

    public void handleMediaPlayer(){
        if(mediaPlayer == null){
            mediaPlayer = new MediaPlayer();
        } else {
            mediaPlayer.reset();
        }
    }

    public Context getContext() {
        return context;
    }

    public ModelMusic getModelMusic() {
        return modelMusic;
    }

    public MediaPlayer getMediaPlayer(){
        return mediaPlayer;
    }
    public void subscribesCallBack(CurrentSessionCallback callback) {
        this.currentSessionCallback = callback;
    }

    public void unSubscribeCallBack() {
        this.currentSessionCallback = null;
    }

    private static int getRandomNumber(int min,int max) {
        return (new Random()).nextInt((max - min) + 1) + min;
    }

    public void setCurrentPos(int currentPos) {
        this.currentPos = currentPos;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }


    public void setLirik(boolean lirik) {
        Lirik = lirik;
    }

    public void setRepeate(boolean repeate) {
        Repeate = repeate;
    }

    public void setRandom(boolean random) {
        RandomMusic = random;
    }

    public void setMediaList(ModelMusic model) {
        arrayList.add(model);
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public int getCurrentPos() {
        return currentPos;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public boolean isLirik() {
        return Lirik;
    }

    public boolean isRepeate() {
        return Repeate;
    }

    public boolean isRandom() {
        return RandomMusic;
    }

    public ArrayList<ModelMusic> getArrayList() {
        return arrayList;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Class<?> getMainClass() {
        return mainClass;
    }

    public void setMainClass(Class<?> mainClass) {
        this.mainClass = mainClass;
    }
}
