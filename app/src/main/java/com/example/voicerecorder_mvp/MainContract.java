package com.example.voicerecorder_mvp;

import java.io.IOException;
import java.util.List;

import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public interface MainContract {
    interface Adapter {
        void startPlaying();

        void stopPlaying();
    }


    interface View {
        void initViews();

        void getPermission();

        void setTimerTv(String time);

        void prepareForRecording();

        void startRecording();

        void cancelRecording();

        void initRecyclerView(List<VoiceMessage> voiceMessages);
    }

    interface Presenter {

        void initViews();

        void requestPermission();

        void startRecord() throws IOException;

        void initialMediaRecorder();

        void initialMediaPlayer();

        void stopRecord();


        String checkSecondsDigit(int number);

        String checkMilliSecondsDigit(int number);

        void startPlay(int position);

        void stopPlay();

        void initTimer();

        void seek(int lastProgress);

        void cancelRecording();

        VoiceMessage getVoiceMessage();

        void setAdapterView(ChatRvAdapter chatAdapter);

        void deleteVoice(int deletingVoicePosition);

        void deleteVoice(String path);

    }
}
