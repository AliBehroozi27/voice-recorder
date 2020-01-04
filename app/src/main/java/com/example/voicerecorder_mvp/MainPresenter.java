package com.example.voicerecorder_mvp;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import top.oply.opuslib.OpusRecorder;

public class MainPresenter implements MainContract.Presenter {

    private static final int LOWER_LIMIT_RECORDING_TIME_MILLISECONDS = 900;
    static final int SEEK_BAR_UPDATE_DELAY = 5;
    private static final int INITIAL_PROGRESS = 0;
    private static final int START_TIMER_DELAY = 300;
    private static final int UPPER_LIMIT_RECORDING_TIME_MILLISECONDS = 600000;
    private static final String VOICE_FORMAT = ".ogg";
    private static final String SAVING_PATH = "/VoiceRecorderSimplifiedCoding/Audios";
    boolean isUserSeeking = false;
    private MainActivity view;
    private ChatRvAdapter adapterView;
    private OpusRecorder mediaRecorder;
    private String fileName;
    private int position;
    private MediaPlayer mediaPlayer;
    private boolean isRecording;
    private boolean isPlaying = false;
    private CountDownTimer timer;
    private int time;
    private List<VoiceMessage> voiceMessages = new ArrayList<VoiceMessage>();
    private VoiceMessage voiceMessage;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public MainPresenter(MainActivity view) {
        this.view = view;
        voiceMessages = getAllVoices();
    }

    @Override
    public void initViews() {
        view.initRecyclerView(voiceMessages);

    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void requestPermission() {
        view.getPermission();
    }

    @Override
    public void initialMediaRecorder() {
        initialFileName();
        mediaRecorder = OpusRecorder.getInstance();
    }

    private void initialFileName() {
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + SAVING_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        fileName = root.getAbsolutePath() + SAVING_PATH + System.currentTimeMillis() + VOICE_FORMAT;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void initialMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            if (isRecording) {
                mediaPlayer.setDataSource(fileName);
            } else {
                mediaPlayer.setDataSource(voiceMessage.getPath());
            }
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlay();
                voiceMessage.setLastProgress(INITIAL_PROGRESS);
                voiceMessages.set(position, voiceMessage);
                adapterView.notifyDataSetChanged();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void startRecord() {
        stopPlay();
        if (mediaRecorder == null)
            initialMediaRecorder();
        view.prepareForRecording();
        mediaRecorder.startRecording(fileName);
        isRecording = true;
        view.startRecording();
        if (timer == null)
            initTimer();
        startTimer();
    }

    private void startTimer() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                if (timer != null) {
                    timer.start();
                }
            }
        }, START_TIMER_DELAY);
    }

    @Override
    public void initTimer() {
        int randomInterval = 139;

        if (timer == null) {
            timer = new CountDownTimer(UPPER_LIMIT_RECORDING_TIME_MILLISECONDS, randomInterval) {
                public void onTick(long millisUntilFinished) {
                    int milliseconds = time;
                    int seconds = time / 1000;
                    int minutes = seconds / 60;
                    seconds = seconds - (minutes * 60);
                    view.setTimerTv(minutes + ":" + checkSecondsDigit(seconds) + ":" + checkMilliSecondsDigit(milliseconds));
                    time += randomInterval;
                }

                public void onFinish() {
                }
            };
        }
    }

    private List<VoiceMessage> getAllVoices() {
        String path = Environment.getExternalStorageDirectory() + "/" + SAVING_PATH;
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (File f : files) {
            VoiceMessage voiceMessage = new VoiceMessage(f.getPath(), 0, 0);
            voiceMessages.add(voiceMessage);
        }
        return voiceMessages;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void stopRecord() {

        if (getTime() < LOWER_LIMIT_RECORDING_TIME_MILLISECONDS) {
            cancelRecording();
        } else {
            timer.cancel();
            isRecording = false;
            mediaRecorder.stopRecording();
            mediaRecorder = null;
            timer = null;
            voiceMessages.add(new VoiceMessage(fileName, 0, 0));
        }
        adapterView.notifyDataSetChanged();
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void startPlay(int position) {
        setPosition(position);
        setVoiceMessage(voiceMessages.get(position));
        if (mediaPlayer == null && mediaRecorder == null)
            initialMediaPlayer();
        if (mediaPlayer != null && !voiceMessage.isPlaying() && voiceMessage.getPath() != null) {
            voiceMessage.setPlaying(true);
            mediaPlayer.start();
            adapterView.getViewHolder().startPlaying();
        }
    }

    @Override
    public void stopPlay() {
        if (voiceMessage != null && mediaPlayer != null) {
            voiceMessage.setPlaying(false);
            voiceMessage.setLastProgress(mediaPlayer.getCurrentPosition());
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
            adapterView.getViewHolder().stopPlaying();
        }
    }

    @Override
    public void seek(int progress) {
        mediaPlayer.seekTo(progress);
        isUserSeeking = false;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void cancelRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder.stopRecording();
        }
        if (timer != null) timer.cancel();
        mediaRecorder = null;
        timer = null;
        deleteVoice(fileName);
        view.cancelRecording();
    }

    @Override
    public VoiceMessage getVoiceMessage() {
        return voiceMessages.get(position);
    }

    @Override
    public void setAdapterView(ChatRvAdapter chatAdapter) {
        this.adapterView = chatAdapter;
    }

    @Override
    public void deleteVoice(int deletingVoicePosition) {
        stopPlay();
        File file = new File(voiceMessages.get(deletingVoicePosition).getPath());
        if (file.exists()) {
            if (file.delete()) {
                voiceMessages.remove(deletingVoicePosition);
                adapterView.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void deleteVoice(String path) {
        stopPlay();
        File file = new File(path);
        if (file.exists()) {
            if (file.delete()) {
                adapterView.notifyDataSetChanged();
            }
        }
    }

    @Override
    public String checkSecondsDigit(int number) {
        return number <= 9 ? "0" + number : String.valueOf(number);
    }

    @Override
    public String checkMilliSecondsDigit(int number) {
        return number <= 9 ? "00" + number : number <= 99 ? "0" + number : String.valueOf(number%1000) ;
    }


    public int getLastProgress() {
        return voiceMessage.getLastProgress();
    }

    public void setLastProgress(int lastProgress) {
        this.voiceMessage.setLastProgress(lastProgress);
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setVoiceMessage(VoiceMessage voiceMessage) {
        this.voiceMessage = voiceMessage;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }
}
