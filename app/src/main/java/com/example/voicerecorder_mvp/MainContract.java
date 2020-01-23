package com.example.voicerecorder_mvp;

import com.example.voicerecorder_mvp.pojo.VoiceMessage;

import java.io.IOException;
import java.util.List;

public interface MainContract {
    interface Adapter {
        void startPlaying();

        void stopPlaying();
    }


    interface View {
        void initViews();

        void getPermission();

        void prepareForRecording();

        void startRecording();

        void cancelRecording();

        void initRecyclerView(List<VoiceMessage> voiceMessages);

        void setShadowScale(double pressure);

        void playRecording();

        void stopPlayRecording();

        void updateRecordingPlaySeekBar(int progress);
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

        void deleteVoice();

        void getAllVoices();

        byte[] getRecordingRawData();

        byte[] getVoiceRawData(int position);

        void sendVoice();

        void startPlayRecording();
    }
}
