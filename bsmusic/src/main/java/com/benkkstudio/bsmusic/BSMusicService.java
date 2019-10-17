package com.benkkstudio.bsmusic;

import android.app.Notification;
import android.app.NotificationChannel;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Logger;

public class BSMusicService extends Service implements NotificationManager.NotificationCenterDelegate{
    public static final String EXTRA_CONNECTED_CAST = "dm.audiostreaming.CAST_NAME";
    public static final String ACTION_CMD = "dm.audiostreaming.ACTION_CMD";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_PAUSE = "CMD_PAUSE";
    public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";
    private static final int STOP_DELAY = 30000;

    public static final String NOTIFY_PREVIOUS = "dm.audiostreamer.previous";
    public static final String NOTIFY_CLOSE = "dm.audiostreamer.close";
    public static final String NOTIFY_PAUSE = "dm.audiostreamer.pause";
    public static final String NOTIFY_PLAY = "dm.audiostreamer.play";
    public static final String NOTIFY_NEXT = "dm.audiostreamer.next";

    NotificationCompat.Builder notification;
    RemoteViews bigViews, smallViews;
    private String NOTIFICATION_CHANNEL_ID = "onlinemp3_ch_1";
    static NotificationManager mNotificationManager;
    ModelMusic modelMusic;
    public PendingIntent pendingIntent;
    private RemoteControlClient remoteControlClient;
    private AudioManager audioManager;
    private MusicPlayer musicPlayer;
    private PhoneStateListener phoneStateListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        musicPlayer = MusicPlayer.getInstance(BSMusicService.this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        NotificationManager.getInstance().addObserver(this, NotificationManager.audioProgressDidChanged);
        NotificationManager.getInstance().addObserver(this, NotificationManager.setAnyPendingIntent);
        NotificationManager.getInstance().addObserver(this, NotificationManager.audioPlayStateChanged);
        try {
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    if (state == TelephonyManager.CALL_STATE_RINGING) {
                        if (musicPlayer.isPlaying()) {
                            musicPlayer.onPause();
                        }
                    }
                    super.onCallStateChanged(state, incomingNumber);
                }
            };
            TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (mgr != null) {
                mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        } catch (Exception e) {
            Log.e("tmessages", e.toString());
        }
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        try {
            if (musicPlayer.getArrayList().size() == 0) {
                Handler handler = new Handler(BSMusicService.this.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
                    }
                });
                return START_STICKY;
            }
            modelMusic = musicPlayer.getArrayList().get(musicPlayer.getCurrentPos());
                ComponentName remoteComponentName = new ComponentName(getApplicationContext(), BSMusicService.class.getName());
                try {
                    if (remoteControlClient == null) {
                        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                        mediaButtonIntent.setComponent(remoteComponentName);
                        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
                        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
                        audioManager.registerRemoteControlClient(remoteControlClient);
                    }
                    remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_STOP
                            | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS | RemoteControlClient.FLAG_KEY_MEDIA_NEXT);

                } catch (Exception e) {
                    Log.e("tmessages", e.toString());
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        createNotification(modelMusic);

        return START_NOT_STICKY;
    }


    private void createNotification(ModelMusic modelMusic) {
        try {
            String channelId = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channelId = getNotificationChannelId();
            }


            String songName = modelMusic.getMusicTitle();
            String authorName = modelMusic.getMusicArtist();

            RemoteViews simpleContentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.layout_noti_small);
            RemoteViews expandedView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.layout_notification);


            Notification notification = null;
            if (pendingIntent != null) {
                notification = new NotificationCompat.Builder(getApplicationContext(), channelId)
                        .setSmallIcon(R.drawable.player)
                        .setContentIntent(pendingIntent)
                        .setContentTitle(songName)
                        .build();
            } else {
                notification = new NotificationCompat.Builder(getApplicationContext(), channelId)
                        .setSmallIcon(R.drawable.player)
                        .setContentTitle(songName)
                        .build();
            }

            notification.contentView = simpleContentView;
            notification.bigContentView = expandedView;


            setListeners(simpleContentView);
            setListeners(expandedView);


            Bitmap albumArt = getBitmapFromAsset(modelMusic.getMusicImage());
            if (albumArt != null) {
                notification.contentView.setImageViewBitmap(R.id.player_album_art, albumArt);
                notification.bigContentView.setImageViewBitmap(R.id.player_album_art, albumArt);

            } else {
                notification.contentView.setImageViewResource(R.id.player_album_art, R.drawable.bg_default_album_art);
                notification.bigContentView.setImageViewResource(R.id.player_album_art, R.drawable.bg_default_album_art);

            }
            notification.contentView.setViewVisibility(R.id.player_next, View.VISIBLE);
            notification.contentView.setViewVisibility(R.id.player_previous, View.VISIBLE);
            notification.bigContentView.setViewVisibility(R.id.player_next, View.VISIBLE);
            notification.bigContentView.setViewVisibility(R.id.player_previous, View.VISIBLE);

            Log.d("ABENK ", String.valueOf(musicPlayer.isPlaying()));
            if (!musicPlayer.isPlaying()) {
                notification.contentView.setViewVisibility(R.id.player_pause, View.GONE);
                notification.contentView.setViewVisibility(R.id.player_play, View.VISIBLE);
                notification.bigContentView.setViewVisibility(R.id.player_pause, View.GONE);
                notification.bigContentView.setViewVisibility(R.id.player_play, View.VISIBLE);
            } else {
                notification.contentView.setViewVisibility(R.id.player_pause, View.VISIBLE);
                notification.contentView.setViewVisibility(R.id.player_play, View.GONE);
                notification.bigContentView.setViewVisibility(R.id.player_pause, View.VISIBLE);
                notification.bigContentView.setViewVisibility(R.id.player_play, View.GONE);
            }

            notification.contentView.setTextViewText(R.id.player_song_name, songName);
            notification.contentView.setTextViewText(R.id.player_author_name, authorName);
            notification.bigContentView.setTextViewText(R.id.player_song_name, songName);
            notification.bigContentView.setTextViewText(R.id.player_author_name, authorName);

            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            startForeground(5, notification);

            if (remoteControlClient != null) {
                RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
                metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, authorName);
                metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, songName);
                if (albumArt != null) {
                    metadataEditor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, albumArt);
                }
                metadataEditor.apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @NonNull
    private String getNotificationChannelId() {
        NotificationChannel channel = new NotificationChannel("BENKKSTUDIO", "Playback", android.app.NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Objects.requireNonNull(notificationManager).createNotificationChannel(channel);
        return "BENKKSTUDIO";
    }

    private void setListeners(RemoteViews view) {
        try {
            PendingIntent prevIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, getIntentForNotification(NOTIFY_PREVIOUS), PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.player_previous, prevIntent);

            PendingIntent closeIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, getIntentForNotification(NOTIFY_CLOSE), PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.player_close, closeIntent);

            PendingIntent pauseIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, getIntentForNotification(NOTIFY_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.player_pause, pauseIntent);

            PendingIntent nextIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, getIntentForNotification(NOTIFY_NEXT), PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.player_next, nextIntent);

            PendingIntent playIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, getIntentForNotification(NOTIFY_PLAY), PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.player_play, playIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private Intent getIntentForNotification(@NonNull String action) {
        Intent intent = new Intent(action);
        intent.setClass(this, BSMusicReceiver.class);
        return intent;
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


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (remoteControlClient != null) {
            RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
            metadataEditor.clear();
            metadataEditor.apply();
            audioManager.unregisterRemoteControlClient(remoteControlClient);
        }
        try {
            TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (mgr != null) {
                mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        } catch (Exception e) {
            Log.e("tmessages", e.toString());
        }
        NotificationManager.getInstance().removeObserver(this, NotificationManager.audioProgressDidChanged);
        NotificationManager.getInstance().removeObserver(this, NotificationManager.audioPlayStateChanged);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {

    }

    @Override
    public void newSongLoaded(Object... args) {

    }
}
