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

import top.oply.opuslib.OpusService;


public class MainPresenter implements MainContract.Presenter {

    public static final int LOWER_LIMIT_RECORDING_TIME_MILLISECONDS = 900;
    public static final int SEEK_BAR_UPDATE_DELAY = 5;
    private static final int INITIAL_PROGRESS = 0;
    private static final int START_TIMER_DELAY = 0;
    private static final int UPPER_LIMIT_RECORDING_TIME_MILLISECONDS = 600000;
    private static final String VOICE_FORMAT = ".opus";
    private static final String SAVING_PATH = "/VoiceRecorderSimplifiedCoding/Audios";
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
    private CountDownTimer timer;
    private int time;
    private List<VoiceMessage> voiceMessages = new ArrayList<VoiceMessage>();
    private VoiceMessage voiceMessage;
    private boolean isPlayRecording;
    private int recordingDuration = -1;
    private int recordingLastProgress = 0;
    
    @RequiresApi(api = Build.VERSION_CODES.M)
    public MainPresenter(MainActivity view , Context context) {
        this.view = view;
        this.context = context;
    }

    @Override
    public void initViews() {
        view.initRecyclerView(voiceMessages);
    }
    
    public OpusPlayer getMediaPlayer() {
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

    @Override
    public void initialMediaPlayer() {
        mediaPlayer = OpusPlayer.getInstance();

//        try {
//            if (isRecording || isPlayRecording) {
//                mediaPlayer.setDataSource(fileName);
//            } else {
//                mediaPlayer.setDataSource(voiceMessage.getPath());
//            }
//            mediaPlayer.prepare();
//            Log.e("AAA" , "prepare");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


//        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                stopPlay();
//                if (isPlayRecording) {
//                    stopPlayRecording();
//                    setRecordingLastProgress(0);
//                    view.stopPlayRecording();
//                } else {
//                    voiceMessage.setLastProgress(INITIAL_PROGRESS);
//                    voiceMessages.set(position, voiceMessage);
//                    adapterView.notifyItemChanged(position);
//                }
//            }
//        });

    }

    @Override
    public void startRecord() {
        mediaPlayer = null;
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
                    if (time%500 == 0)
                        view.setShadowScale(mediaRecorder.getPressure());
                    //view.setTimerTv(minutes + ":" + checkSecondsDigit(seconds) + ":" + checkMilliSecondsDigit(milliseconds));
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

        String path = Environment.getExternalStorageDirectory() + "/" +SAVING_PATH;
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (File f : files) {
            VoiceMessage voiceMessage = new VoiceMessage();
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    
            OpusPlayer opusPlayer = OpusPlayer.getInstance();
            opusPlayer.setFile(f.getPath());
    
    
            mmr.setDataSource(f.getPath());
            Date modifiedTime = new Date(f.lastModified());
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
            voiceMessage.setDateModified(sdf.format(modifiedTime.getTime()));
            voiceMessage.setPath(f.getPath());
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String date = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
    
            Log.e("AAA", "dur : " + opusPlayer.getDuration());
    
            voiceMessage.setDuration((int) opusPlayer.getDuration());
            voiceMessage.setLastProgress(0);
            voiceMessages.add(voiceMessage);
            mmr.release();
        }
    }


    @Override
    public void stopRecord() {
        Log.e("AAA", "stop");
        if (isRecording) {
            mediaRecorder.stopRecording();
        }
        if (timer != null){
            timer.cancel();
        }
        isRecording = false;
        mediaRecorder = null;
        timer = null;
    
        OpusService.decode(context
                , fileName
                , Environment.getExternalStorageDirectory()
                        + SAVING_PATH
                        + "/decode"
                        + System.currentTimeMillis()
                        + ".wav"
                , null);
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
        //voiceMessage.setDuration(Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        voiceMessage.setLastProgress(0);
        voiceMessage.setPath(fileName);

        voiceMessages.add(voiceMessage);
        adapterView.notifyItemInserted(voiceMessages.size() - 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void startPlayRecording() {
        view.playRecording();
        setPlayRecording(true);
        if (mediaPlayer == null && mediaRecorder == null) {
            initialMediaPlayer();
        }
        if (mediaPlayer != null) {
//            mediaPlayer.seekTo(recordingLastProgress);
//            mediaPlayer.start();
            mediaPlayer.seekOpusFile(recordingLastProgress);
            mediaPlayer.play(fileName);
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
        setPlayRecording(false);
        if (mediaPlayer != null) {
//            recordingLastProgress = mediaPlayer.getCurrentPosition();
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
        if (voiceMessage != null && mediaPlayer != null) {
            voiceMessage.setPlaying(false);
//            voiceMessage.setLastProgress(mediaPlayer.getCurrentPosition());
            voiceMessage.setLastProgress((int) mediaPlayer.getPosition());
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
    public void seek(int progress , int duration) {
//        mediaPlayer.seekTo(progress);
        Log.e("AAAA" , "last : " + (float)progress / duration);
        float scale = (float)progress / duration;
        mediaPlayer.seekOpusFile(scale);
        isUserSeeking = false;
    }


    @Override
    public void cancelRecording() {
        //Log.e("AAA" , "CANCEL " + mediaRecorder);
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder.stopRecording();
        }
        if (timer != null) timer.cancel();
        mediaRecorder = null;
        timer = null;
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
        stopPlay();
        File file = new File(fileName);
        if (file.exists()) {
            if (file.delete()) {
                //adapterView.notifyDataSetChanged();
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
            Log.e("AAA", "" + mediaPlayer.isWorking());
            if (!mediaPlayer.isWorking()) {
                stopPlay();
                if (isPlayRecording) {
                    stopPlayRecording();
                    setRecordingLastProgress(0);
                    view.stopPlayRecording();
                } else {
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

    public int getRecordingDuration() {
        if (recordingDuration == -1) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            File f = new File(fileName);
            mmr.setDataSource(context, Uri.parse(f.getPath()));
            try{
            recordingDuration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));}
            catch (Exception e){
                e.printStackTrace();
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
}
