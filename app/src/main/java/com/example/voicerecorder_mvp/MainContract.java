package com.example.voicerecorder_mvp;

import java.io.IOException;
import java.util.List;

import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public interface MainContract {
    interface Adapter {
        void startPlaying();

        void setSeekBarProgress(Integer integer);

        void stopPlaying();
    }


    interface View {
        void initViews();

        void getPermission();

        void setTimerTv(String time);

        void prepareForRecording();

        void prepareForPlaying();

        void startRecording();

        void prepareForStop();

        void startPlaying();

        void stopPlaying();

        void prepareForCancel();

        void setSeekBarProgress(int currentPosition);

        void cancelRecording();

        void initRecyclerView(List<VoiceMessage> voiceMessages);
    }

    interface Presenter {

        void initViews();

        void requestPermission();

        void record() throws IOException;

        void initialMediaRecorder();

        void initialMediaPlayer();

        BehaviorSubject<Integer> getSeekBarSubject();

        void stopRecord();

        void play(int position);

        void startPlay();

        void stopPlay();

        void initTimer();

        void seek(int lastProgress);

        int getMediaPlayerDuration();

        void cancelRecording();

        VoiceMessage getVoiceMessage();

        void setAdapterView(ChatRvAdapter chatAdapter);
    }
}
