package com.benkkstudio.bsmusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class BSMusicReceiver extends BroadcastReceiver {

    MusicPlayer musicPlayer;
    @Override
    public void onReceive(Context context, Intent intent) {
        musicPlayer = MusicPlayer.getInstance(context);
        if(musicPlayer ==null){
            return;
        }
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            if (intent.getExtras() == null) {
                return;
            }
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            if (keyEvent == null) {
                return;
            }
            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                return;
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:

                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    musicPlayer.onNext();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    musicPlayer.onPrev();
                    break;
            }
        } else {
            musicPlayer = MusicPlayer.getInstance(context);
            switch (intent.getAction()) {
                case BSMusicService.NOTIFY_PLAY:
                    musicPlayer.onResume();
                case BSMusicService.NOTIFY_PAUSE:
                    musicPlayer.onPause();
                    break;
                case BSMusicService.NOTIFY_NEXT:
                    musicPlayer.onNext();
                    break;
                case BSMusicService.NOTIFY_CLOSE:
                    musicPlayer.stop(context, true);
                    break;
                case BSMusicService.NOTIFY_PREVIOUS:
                    musicPlayer.onPrev();
                    break;
            }
        }
    }
}