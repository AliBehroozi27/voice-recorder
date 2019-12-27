package com.example.voicerecorder_mvp;

import java.io.IOException;

import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public interface MainContract {
    interface View {
        void initViews();

        void initTimer();

        void getPermission();

        void prepareForRecording();

        void prepareForPlaying();

        void startRecording();

        void prepareForStop();

        void stopRecording();

        void startPlaying();

        void stopPlaying();

        void prepareForCancel();

        void setSeekBarProgress(int currentPosition);

        void cancelRecording();
    }

    interface Presenter {
        void requestPermission();

        void record() throws IOException;

        void initialMediaRecorder();

        void initialMediaPlayer();

        BehaviorSubject<Integer> getSeekBarSubject();

        void stopRecord();

        void play();

        void startPlay();

        void stopPlay();

        void seek(int lastProgress);

        int getMediaPlayerDuration();

        void cancelRecording();
    }
}