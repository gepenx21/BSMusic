package com.benkkstudio.bsmusic;

import android.os.Parcel;
import android.os.Parcelable;

public class ModelMusic  implements Parcelable {
    private String MusicTitle;
    private String MusicArtist;
    private String MusicDuration;
    private String MusicUrl;
    private String MusicImage;
    private String MusicLirik;
    private int playState;

    public ModelMusic(String musicTitle, String musicArtist, String musicDuration, String musicUrl, String musicImage, String musicLirik) {
        MusicTitle = musicTitle;
        MusicArtist = musicArtist;
        MusicDuration = musicDuration;
        MusicUrl = musicUrl;
        MusicImage = musicImage;
        MusicLirik = musicLirik;
    }

    public ModelMusic() {
    }

    private ModelMusic(Parcel in) {
        MusicTitle = in.readString();
        MusicArtist = in.readString();
        MusicDuration = in.readString();
        MusicUrl = in.readString();
        MusicImage = in.readString();
        MusicLirik = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(MusicTitle);
        dest.writeString(MusicArtist);
        dest.writeString(MusicDuration);
        dest.writeString(MusicUrl);
        dest.writeString(MusicImage);
        dest.writeString(MusicLirik);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ModelMusic> CREATOR = new Creator<ModelMusic>() {
        @Override
        public ModelMusic createFromParcel(Parcel in) {
            return new ModelMusic(in);
        }

        @Override
        public ModelMusic[] newArray(int size) {
            return new ModelMusic[size];
        }
    };

    public String getMusicTitle() {
        return MusicTitle;
    }

    public String getMusicArtist() {
        return MusicArtist;
    }

    public String getMusicDuration() {
        return MusicDuration;
    }

    public String getMusicUrl() {
        return MusicUrl;
    }

    public String getMusicImage() {
        return MusicImage;
    }

    public String getMusicLirik() {
        return MusicLirik;
    }

    public int getPlayState() {
        return playState;
    }

    public void setPlayState(int playState) {
        this.playState = playState;
    }
}

