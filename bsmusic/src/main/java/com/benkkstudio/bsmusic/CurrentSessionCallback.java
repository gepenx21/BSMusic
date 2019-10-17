/*
 * This is the source code of DMAudioStreaming for Android v. 1.0.0.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright @Dibakar_Mistry(dibakar.ece@gmail.com), 2017.
 */
package com.benkkstudio.bsmusic;


public interface CurrentSessionCallback {
    void updatePlaybackState(int state);

    void playSongComplete();

    void currentSeekBarPosition(int progress);

    void playCurrent(ModelMusic modelMusic);

    void playNext(ModelMusic modelMusic);

    void playPrevious(ModelMusic modelMusic);
}
