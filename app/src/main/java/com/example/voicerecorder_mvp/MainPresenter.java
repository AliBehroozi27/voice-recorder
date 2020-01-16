package com.example.voicerecorder_mvp;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.example.voicerecorder_mvp.pojo.VoiceMessage;
import com.example.voicerecorder_mvp.utils.MyOpusRecorder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import top.oply.opuslib.OpusEvent;

public class MainPresenter implements MainContract.Presenter {

    private static final int LOWER_LIMIT_RECORDING_TIME_MILLISECONDS = 900;
    static final int SEEK_BAR_UPDATE_DELAY = 5;
    private static final int INITIAL_PROGRESS = 0;
    private static final int START_TIMER_DELAY = 0;
    private static final int UPPER_LIMIT_RECORDING_TIME_MILLISECONDS = 600000;
    private static final String VOICE_FORMAT = ".ogg";
    private static final String SAVING_PATH = "/VoiceRecorderSimplifiedCoding/Audios";
    private final Context context;
    boolean isUserSeeking = false;
    private MainActivity view;
    private ChatRvAdapter adapterView;
    private MyOpusRecorder mediaRecorder;
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
    public MainPresenter(MainActivity view , Context context) {
        this.view = view;
        this.context = context;
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
        mediaRecorder = MyOpusRecorder.getInstance();
    }

    private void initialFileName() {
        File root = Environment.getExternalStorageDirectory();
        File file = new File(Environment.getExternalStorageDirectory()+ SAVING_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        fileName = Environment.getExternalStorageDirectory()+ SAVING_PATH + "/" + System.currentTimeMillis() + VOICE_FORMAT;
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
        int randomInterval = 100;

        if (timer == null) {
            timer = new CountDownTimer(UPPER_LIMIT_RECORDING_TIME_MILLISECONDS, randomInterval) {
                public void onTick(long millisUntilFinished) {
                    int milliseconds = time;
                    int seconds = time / 1000;
                    int minutes = seconds / 60;
                    seconds = seconds - (minutes * 60);
                    //Log.e("AAA" , mediaRecorder.getPressure()+"");
                    if (time%500 == 0)
                        view.setShadowScale(mediaRecorder.getPressure());
                    view.setTimerTv(minutes + ":" + checkSecondsDigit(seconds) + ":" + checkMilliSecondsDigit(milliseconds));
                    time += randomInterval;
                }

                public void onFinish() {
                }
            };
        }
    }

    @Override
    public void getAllVoices() {
        File file = new File(Environment.getExternalStorageDirectory() + SAVING_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        String path = Environment.getExternalStorageDirectory() + "/" +SAVING_PATH;
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (File f : files) {
            mmr.setDataSource(context, Uri.parse(f.getPath()));
            VoiceMessage voiceMessage = new VoiceMessage();
            Date modifiedTime = new Date(f.lastModified());
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            voiceMessage.setDateModified(sdf.format(modifiedTime.getTime()));
            voiceMessage.setPath(f.getPath());
            voiceMessage.setDuration(Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
            voiceMessage.setLastProgress(0);
            voiceMessages.add(voiceMessage);
        }
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
        }
    }

    @Override
    public void sendVoice() {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        File f = new File(fileName);
        mmr.setDataSource(context, Uri.parse(f.getPath()));


        Date modifiedTime = new Date(f.lastModified());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        VoiceMessage voiceMessage = new VoiceMessage();
        voiceMessage.setDateModified(sdf.format(modifiedTime.getTime()));
        voiceMessage.setDuration(Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        voiceMessage.setLastProgress(0);
        voiceMessage.setPath(fileName);

        voiceMessages.add(voiceMessage);
        adapterView.notifyDataSetChanged();
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void startPlay(int position) {
        setPosition(position);
        setVoiceMessage(voiceMessages.get(position));
        if (mediaPlayer == null && mediaRecorder == null) {
            initialMediaPlayer();
        }
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
        deleteVoice();
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
    public void deleteVoice() {
        stopPlay();
        File file = new File(fileName);
        if (file.exists()) {
            if (file.delete()) {
                adapterView.notifyDataSetChanged();
            }
        }
    }

    @Override
    public byte[] getRecordingRawData() {
        File file = new File(fileName);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int  nonZeroBytesCounter = 0;
        for (int i=0 ; i < bytes.length ;i++) {
            if (bytes[i] != 0) {
                nonZeroBytesCounter++;
            }
        }

        byte[] voiceBytes = new byte[nonZeroBytesCounter];

        nonZeroBytesCounter = 0 ;
        for (int i=0 ; i < bytes.length ;i++) {
            if (bytes[i] != 0) {
                voiceBytes[nonZeroBytesCounter] = bytes[i];
                nonZeroBytesCounter++;
            }
        }

        return voiceBytes;
    }

    @Override
    public byte[] getVoiceRawData(int position) {
        File file = new File(voiceMessages.get(position).getPath());
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        int  nonZeroBytesCounter = 0;
//        for (int i=0 ; i < bytes.length ;i++) {
//            if (bytes[i] != 0) {
//                nonZeroBytesCounter++;
//            }
//        }
//
//        byte[] voiceBytes = new byte[nonZeroBytesCounter];
//
//        nonZeroBytesCounter = 0 ;
//        for (int i=0 ; i < bytes.length ;i++) {
//            if (bytes[i] != 0) {
//                voiceBytes[nonZeroBytesCounter] = bytes[i];
//                nonZeroBytesCounter++;
//            }
//        }

        return bytes;
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
