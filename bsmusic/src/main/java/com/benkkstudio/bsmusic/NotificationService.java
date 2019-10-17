package com.benkkstudio.bsmusic;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;

public class NotificationService extends IntentService {
    public static final String ACTION_FIRST_PLAY = "com.benkkstudio.bsmusic.action.ACTION_FIRST";
    public static final String ACTION_SEEKTO = "com.benkkstudio.bsmusic.action.ACTION_SEEKTO";
    public static final String ACTION_PLAY = "com.benkkstudio.bsmusic.action.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.benkkstudio.bsmusic.action.PAUSE";
    public static final String ACTION_STOP = "com.benkkstudio.bsmusic.action.STOP";
    public static final String ACTION_SKIP = "com.benkkstudio.bsmusic.action.SKIP";
    public static final String ACTION_REWIND = "com.benkkstudio.bsmusic.action.REWIND";
    public static final String ACTION_NOTI_PLAY_PAUSE = "com.benkkstudio.bsmusic.action.NOTI_PLAY_PAUSE";

    public static final String ACTION_NOTI_SKIP = "com.benkkstudio.bsmusic.action.NOTI_SKIP";
    public static final String ACTION_NOTI_REWIND = "com.benkkstudio.bsmusic.action.NOTI_REWIND";
    NotificationCompat.Builder notification;
    RemoteViews bigViews, smallViews;
    private String NOTIFICATION_CHANNEL_ID = "onlinemp3_ch_1";
    static NotificationManager mNotificationManager;
    MusicPlayer musicPlayer;
    public NotificationService() {
        super(null);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            registerReceiver(onCallIncome, new IntentFilter("android.intent.action.PHONE_STATE"));
            musicPlayer =  MusicPlayer.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (Objects.requireNonNull(intent.getAction())) {
            case ACTION_FIRST_PLAY:
                musicPlayer.onPlay();
                createNoti();
                break;
            case ACTION_SEEKTO:
                updateNoti();
                break;
            case ACTION_PLAY:
            case ACTION_PAUSE:
                if(notification != null){
                    updateNotiPlay(musicPlayer.isPlaying());
                } else {
                    createNoti();
                }
                break;
            case ACTION_STOP:
                musicPlayer.stop(null, false);
                unregisterReceiver(onCallIncome);
                stopService(intent);
                stopForeground(true);
                break;
            case ACTION_REWIND:
                if(notification != null){
                    updateNoti();
                } else {
                    createNoti();
                }
            case ACTION_SKIP:
                if(notification != null){
                    updateNoti();
                } else {
                    createNoti();
                }
                break;
            case ACTION_NOTI_PLAY_PAUSE:
                if(musicPlayer.isPlaying()){
                    musicPlayer.onPause();
                    updateNotiPlay(musicPlayer.isPlaying());
                    if (musicPlayer.currentSessionCallback != null) {
                        musicPlayer.currentSessionCallback.updatePlaybackState(musicPlayer.PAUSE_STATE);
                    }
                } else {
                    musicPlayer.onResume();
                    updateNotiPlay(musicPlayer.isPlaying());
                    if (musicPlayer.currentSessionCallback != null) {
                        musicPlayer.currentSessionCallback.updatePlaybackState(musicPlayer.PLAYING_STATE);
                    }
                }
                break;
            case ACTION_NOTI_SKIP:
                musicPlayer.onNext();
                break;
            case ACTION_NOTI_REWIND:
                musicPlayer.onPrev();
                break;
        }
        return START_STICKY;
    }



    private void createNoti() {
        bigViews = new RemoteViews(getPackageName(), R.layout.layout_notification);
        smallViews = new RemoteViews(getPackageName(), R.layout.layout_noti_small);

        Intent notificationIntent = new Intent(this, musicPlayer.getMainClass());
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.putExtra("isnoti", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent previousIntent = new Intent(this, NotificationService.class);
        previousIntent.setAction(ACTION_NOTI_REWIND);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0, previousIntent, 0);

        Intent playIntent = new Intent(this, NotificationService.class);
        playIntent.setAction(ACTION_NOTI_PLAY_PAUSE);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0, playIntent, 0);

        Intent nextIntent = new Intent(this, NotificationService.class);
        nextIntent.setAction(ACTION_NOTI_SKIP);
        PendingIntent pnextIntent = PendingIntent.getService(this, 0, nextIntent, 0);

        Intent closeIntent = new Intent(this, NotificationService.class);
        closeIntent.setAction(ACTION_STOP);
        PendingIntent pcloseIntent = PendingIntent.getService(this, 0, closeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notification = new NotificationCompat.Builder(this)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_play))
                .setContentTitle(getString(R.string.app_name))
                .setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_action_play)
                .setTicker(musicPlayer.getModelMusic().getMusicTitle())
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(true);

        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Online Song";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            mNotificationManager.createNotificationChannel(mChannel);

            MediaSessionCompat mMediaSession;
            mMediaSession = new MediaSessionCompat(musicPlayer.getActivity(), "ONLINEMP3");
            mMediaSession.setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

            notification.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mMediaSession.getSessionToken())
                    .setShowCancelButton(true)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setCancelButtonIntent(
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    musicPlayer.getActivity(), PlaybackStateCompat.ACTION_STOP)))
                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_noti_previous, "Previous",
                            ppreviousIntent))
                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_noti_pause, "Pause",
                            pplayIntent))
                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_noti_next, "Next",
                            pnextIntent))
                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_noti_close, "Close",
                            pcloseIntent));
        } else {
            Bitmap albumArt = getBitmapFromAsset(musicPlayer.getModelMusic().getMusicImage());
            bigViews.setImageViewBitmap(R.id.player_album_art, albumArt);
            smallViews.setImageViewBitmap(R.id.player_album_art, albumArt);

            bigViews.setOnClickPendingIntent(R.id.player_pause, pplayIntent);

            bigViews.setOnClickPendingIntent(R.id.player_next, pnextIntent);

            bigViews.setOnClickPendingIntent(R.id.player_previous, ppreviousIntent);

            bigViews.setOnClickPendingIntent(R.id.player_close, pcloseIntent);
            smallViews.setOnClickPendingIntent(R.id.player_close, pcloseIntent);

            bigViews.setImageViewResource(R.id.player_pause, R.drawable.ic_action_pause);

            bigViews.setTextViewText(R.id.player_song_name, musicPlayer.getModelMusic().getMusicTitle());
            smallViews.setTextViewText(R.id.player_song_name, musicPlayer.getModelMusic().getMusicTitle());

            bigViews.setTextViewText(R.id.player_author_name, musicPlayer.getModelMusic().getMusicArtist());
            smallViews.setTextViewText(R.id.player_author_name, musicPlayer.getModelMusic().getMusicArtist());

            notification.setCustomContentView(smallViews).setCustomBigContentView(bigViews);
        }
        startForeground(230997, notification.build());
    }

    public Bitmap getBitmapFromAsset(String filePath) {
        AssetManager assetManager = getAssets();
        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath.replace("file:///android_asset/", ""));
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void updateNoti() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.setContentTitle(musicPlayer.getModelMusic().getMusicTitle());
            notification.setContentText(musicPlayer.getModelMusic().getMusicArtist());
        } else {
            bigViews.setTextViewText(R.id.player_song_name, musicPlayer.getModelMusic().getMusicTitle());
            smallViews.setTextViewText(R.id.player_song_name, musicPlayer.getModelMusic().getMusicTitle());
            bigViews.setTextViewText(R.id.player_author_name, musicPlayer.getModelMusic().getMusicArtist());
            smallViews.setTextViewText(R.id.player_author_name, musicPlayer.getModelMusic().getMusicArtist());
            Bitmap albumArt = getBitmapFromAsset(musicPlayer.getModelMusic().getMusicImage());
            bigViews.setImageViewBitmap(R.id.player_album_art, albumArt);
            smallViews.setImageViewBitmap(R.id.player_album_art, albumArt);
        }
        updateNotiPlay(musicPlayer.isPlaying());
    }

    private void updateNotiPlay(Boolean isPlay) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.mActions.remove(1);
            Intent playIntent = new Intent(this, NotificationService.class);
            playIntent.setAction(ACTION_NOTI_PLAY_PAUSE);
            PendingIntent ppreviousIntent = PendingIntent.getService(this, 0, playIntent, 0);
            if (isPlay) {
                notification.mActions.add(1, new NotificationCompat.Action(
                        R.drawable.ic_noti_pause, "Pause",
                        ppreviousIntent));

            } else {
                notification.mActions.add(1, new NotificationCompat.Action(
                        R.drawable.ic_noti_play, "Play",
                        ppreviousIntent));
            }
        } else {
            if (isPlay) {
                bigViews.setImageViewResource(R.id.player_pause, R.drawable.ic_action_pause);
            } else {
                bigViews.setImageViewResource(R.id.player_pause, R.drawable.ic_action_play);
            }
        }
        mNotificationManager.notify(230997, notification.build());
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    BroadcastReceiver onCallIncome = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (musicPlayer.isPlaying()) {
                if (a.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) || a.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    musicPlayer.getMediaPlayer().pause();
                } else if (a.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    musicPlayer.getMediaPlayer().start();
                }
            }
        }
    };
}
