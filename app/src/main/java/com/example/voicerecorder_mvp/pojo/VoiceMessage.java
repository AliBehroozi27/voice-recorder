package com.example.voicerecorder_mvp.pojo;

public class VoiceMessage {
    private String path;
    private int duration;
    private int lastProgress;
    private boolean isUserSeeking;
    private boolean isPlaying;
    private String dateModified;
    private byte[] rawData;


    public VoiceMessage() {
    }

    public boolean isUserSeeking() {
        return isUserSeeking;
    }

    public void setUserSeeking(boolean userSeeking) {
        isUserSeeking = userSeeking;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getLastProgress() {
        return lastProgress;
    }

    public void setLastProgress(int lastProgress) {
        this.lastProgress = lastProgress;
    }


    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    public String getString(){
        return "  duration : " + duration + " | lastprogress : " + lastProgress + " | isPlaying :" + isPlaying;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public byte[] getRawData() {
        return rawData;
    }
}
