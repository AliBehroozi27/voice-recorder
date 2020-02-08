package com.example.voicerecorder_mvp;

import android.content.Context;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.example.voicerecorder_mvp.customView.AudioRecordView;
import com.example.voicerecorder_mvp.pojo.VoiceMessage;
import com.example.voicerecorder_mvp.utils.MyOpusRecorder;
import com.example.voicerecorder_mvp.utils.OpusPlayer;

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


public class MainPresenter implements MainContract.Presenter {

    public static final int LOWER_LIMIT_RECORDING_TIME_MILLISECONDS = 1100;
    public static final int SEEK_BAR_UPDATE_DELAY = 5;
    private static final int INITIAL_PROGRESS = 0;
    private static final int START_TIMER_DELAY = 0;
    private static final int UPPER_LIMIT_RECORDING_TIME_MILLISECONDS = 600000;
    private static final String VOICE_FORMAT = ".opus";
    private static final String SAVING_PATH = "/NazdikaVoiceRecorder/Audios";
    private static final int DEVICE_ANDROID_VERSION = Build.VERSION.SDK_INT;
    private static final int PRESSURE_DELAY_OFFSET = 500;
    private final Context context;
    boolean isUserSeeking = false;
    private MainActivity view;
    private ChatRvAdapter adapterView;
    private MyOpusRecorder mediaRecorder;
    private String fileName;
    private int position;
    private OpusPlayer mediaPlayer;
    private boolean isRecording;
    private boolean isPlaying = false;
    private List<VoiceMessage> voiceMessages = new ArrayList<VoiceMessage>();
    private VoiceMessage voiceMessage;
    private boolean isPlayRecording;
    private int recordingDuration = -1;
    private int recordingLastProgress = 0;
    private AudioRecordView recordingView;
    private Handler handler = new Handler();
    
    
    public MainPresenter(MainActivity view , Context context) {
        this.view = view;
        this.context = context;
    }

    @Override
    public void initViews() {
        view.initRecyclerView(voiceMessages);
        recordingView = view.getRecordingView();
    
        //TODO: place it in a separate thread
//        if (recordingView.getRecordingTime() % PRESSURE_DELAY_OFFSET == 0)
//            view.setShadowScale(mediaRecorder.getPressure());
    }
    
    public OpusPlayer getMediaPlayer() {
        return mediaPlayer;
    }
    
    @Override
    public void requestPermission() {
        if (DEVICE_ANDROID_VERSION >= Build.VERSION_CODES.M) {
            view.getPermission();
        }
    }

    @Override
    public void initialMediaRecorder() {
        initialFileName();
        mediaRecorder = MyOpusRecorder.getInstance();
    }

    private void initialFileName() {
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root + SAVING_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        fileName = root + SAVING_PATH + "/" + System.currentTimeMillis() + VOICE_FORMAT;
    }

    @Override
    public void initialMediaPlayer() {
        mediaPlayer = OpusPlayer.getInstance();
    }

    @Override
    public void startRecord() {
        isRecording = true;
        stopPlay();
        if (adapterView.getViewHolder() != null)
            adapterView.getViewHolder().notifyLastItem();
        if (mediaRecorder == null)
            initialMediaRecorder();
        view.prepareForRecording();
        mediaRecorder.startRecording(fileName);
        view.startRecording();
    }
    
    
    @Override
    public void getAllVoices() {
        File file = new File(Environment.getExternalStorageDirectory() + SAVING_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        String path = Environment.getExternalStorageDirectory() + "/" +SAVING_PATH;
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (File f : files) {
            VoiceMessage voiceMessage = new VoiceMessage();
    
            //init opus player to get duration
            OpusPlayer opusPlayer = OpusPlayer.getInstance();
            int duration = opusPlayer.getDuration(f.getPath());
    
            //getting date modified
            Date modifiedTime = new Date(f.lastModified());
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
            voiceMessage.setDateModified(sdf.format(modifiedTime.getTime()));
            voiceMessage.setPath(f.getPath());
            voiceMessage.setDuration(duration);
            voiceMessage.setLastProgress(0);
            voiceMessages.add(voiceMessage);
        }
    }
    
    @Override
    public void stopRecord() {
        Log.e("AAA", "stop");
        if (isRecording) {
            mediaRecorder.stopRecording();
        }
        isRecording = false;
        mediaRecorder = null;
    }

    @Override
    public void sendVoice() {
        recordingLastProgress = 0;
    
        //getting date modified
        File f = new File(fileName);
        Date modifiedTime = new Date(f.lastModified());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        VoiceMessage voiceMessage = new VoiceMessage();
        voiceMessage.setDateModified(sdf.format(modifiedTime.getTime()));
        voiceMessage.setLastProgress(0);
        voiceMessage.setPath(fileName);
    
        //init opus player to get duration
        OpusPlayer opusPlayer = OpusPlayer.getInstance();
        int duration = opusPlayer.getDuration(fileName);
        voiceMessage.setDuration(duration);

        voiceMessages.add(voiceMessage);
        adapterView.notifyItemInserted(voiceMessages.size() - 1);
    }

    @Override
    public void startPlayRecording() {
        view.playRecording();
        setPlayRecording(true);
        if (mediaPlayer == null && mediaRecorder == null) {
            initialMediaPlayer();
        }
        if (mediaPlayer != null) {
            mediaPlayer.play(fileName);
            seek(recordingLastProgress, (int) mediaPlayer.getDuration());
        }
    }

    @Override
    public void startPlay(int position) {
        setPosition(position);
        setVoiceMessage(voiceMessages.get(position));
        if (mediaPlayer == null && mediaRecorder == null) {
            initialMediaPlayer();
        }
        if (mediaPlayer != null && !voiceMessage.isPlaying() && voiceMessage.getPath() != null) {
            voiceMessage.setPlaying(true);
            mediaPlayer.play(voiceMessage.getPath());
            adapterView.getViewHolder().startPlaying();
        }
    }

    public void stopPlayRecording() {
        view.stopPlayRecording();
        handler.removeCallbacks(overviewPlaySeekBarUpdate);
        setPlayRecording(false);
        if (mediaPlayer != null) {
            recordingLastProgress = (int) mediaPlayer.getPosition();
            mediaPlayer.stop();
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }
    }

    @Override
    public void stopPlay() {
        if (adapterView.getViewHolder() != null)
            adapterView.getViewHolder().stopPlaying();
        if (voiceMessage != null && mediaPlayer != null) {
            voiceMessage.setPlaying(false);
            voiceMessage.setLastProgress((int) mediaPlayer.getPosition());
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mediaPlayer = null;
    }

    @Override
    public void seek(int progress , int duration) {
        float scale = (float)progress / duration;
        mediaPlayer.seekOpusFile(scale);
        isUserSeeking = false;
    }
    
    @Override
    public void cancelRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder.stopRecording();
        }
        mediaRecorder = null;
        isRecording = false;
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
                adapterView.notifyItemRemoved(deletingVoicePosition);
                voiceMessages.remove(deletingVoicePosition);
            }
        }
    }

    @Override
    public void deleteVoice() {
        recordingLastProgress = 0;
        stopPlay();
        File file = new File(fileName);
        if (file.exists()) {
            if (file.delete()) {
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

        return bytes;
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

        voiceMessages.get(position).setRawData(bytes);
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

    public float convertCurrentMediaPositionIntoPercent(int currentPosition, int duration) {
        if (duration < 1) {
            return 0;
        }
        if (mediaPlayer != null) {
            if (!mediaPlayer.isWorking()) {
                Log.e("AAA", " stop ");
                if (isPlayRecording) {
                    stopPlayRecording();
                    setRecordingLastProgress(0);
                    view.stopPlayRecording();
                } else {
                    stopPlay();
                    voiceMessage.setLastProgress(INITIAL_PROGRESS);
                    voiceMessages.set(position, voiceMessage);
                    adapterView.notifyItemChanged(position);
                }
            }
        }
        return (float)currentPosition * 100 / (float)duration;
    }

    public String calculateTime(Integer progress) {
        int seconds = progress / OpusPlayer.SAMPLE_RATE_HRTZ;
        int minutes = seconds / 60;
        seconds = seconds - (minutes * 60);
        return minutes + ":" + checkSecondsDigit(seconds);

    }
    
    Runnable overviewPlaySeekBarUpdate = new Runnable() {
        @Override
        public void run() {
            updateRecordingOverviewSeekBar();
        }
    };
    
    public void updateRecordingOverviewSeekBar() {
        if (getMediaPlayer() != null && isPlayRecording) {
            int currentPosition = (int) getMediaPlayer().getPosition();
            recordingView.getWave().setProgress(convertCurrentMediaPositionIntoPercent(currentPosition, getRecordingDuration()));
            setRecordingLastProgress(currentPosition);
            handler.postDelayed(overviewPlaySeekBarUpdate, MainPresenter.SEEK_BAR_UPDATE_DELAY);
        }
    }

    public int getLastProgress() {
        return voiceMessage.getLastProgress();
    }

    public void setLastProgress(int lastProgress) {
        this.voiceMessage.setLastProgress(lastProgress);
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

    public int getRecordingDuration() {
        if (recordingDuration == -1) {
            //init opus player to get duration
            if (mediaPlayer == null) {
                OpusPlayer opusPlayer = OpusPlayer.getInstance();
                recordingDuration = opusPlayer.getDuration(fileName);
            } else {
                recordingDuration = (int) getMediaPlayer().getDuration();
            }
        }
        return recordingDuration;

    }

    public void setRecordingLastProgress(int currentPosition) {
        recordingLastProgress = currentPosition;
    }

    public boolean isPlayRecording() {
        return isPlayRecording;
    }

    public void setPlayRecording(boolean playRecording) {
        isPlayRecording = playRecording;
    }

    public int getRecordingLastProgress() {
        return recordingLastProgress;
    }
    
    public void removeRecordingSeekBarUpdateCallbacks() {
        handler.removeCallbacks(overviewPlaySeekBarUpdate);
    }
    
    public int downloadVoice() {
        //faking download for 1 seconds
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }
}
