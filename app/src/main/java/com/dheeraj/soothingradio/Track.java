package com.dheeraj.soothingradio;

public class Track {
    private String songName ;
    private String artist ;

    public Track(String songName, String artist) {
        this.songName = songName;
        this.artist = artist;
    }

    public String getSongName() {
        return songName;
    }

    public String getArtist() {
        return artist;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
