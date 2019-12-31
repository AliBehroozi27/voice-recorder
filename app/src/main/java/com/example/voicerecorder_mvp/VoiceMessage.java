package com.example.voicerecorder_mvp;

public class VoiceMessage {
    private String path;
    private int duration;
    private int lastProgress;
    private boolean isUserSeeking;
    private boolean isPlaying;
    private int dateModified;


    public VoiceMessage(String path, int duration, int lastProgress) {
        this.path = path;
        this.duration = duration;
        this.lastProgress = lastProgress;
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


    public int getDateModified() {
        return dateModified;
    }

    public void setDateModified(int dateModified) {
        this.dateModified = dateModified;
    }
}
