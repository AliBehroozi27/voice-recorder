package com.example.voicerecorder_mvp;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import top.oply.opuslib.OpusRecorder;

public class MainPresenter implements MainContract.Presenter {

    private static final int LOWER_LIMIT_RECORDING_TIME_MILLISECONDS = 900;
    private static final int SEEK_BAR_UPDATE_DELAY = 5;
    private static final int INITIAL_PROGRESS = 0;
    private static final int START_TIMER_DELAY = 300;
    private static final int UPPER_LIMIT_RECORDING_TIME_MILLISECONDS = 600000;
    public boolean isUserSeeking = false;
    private MainActivity view;
    private ChatRvAdapter adapterView;
    private OpusRecorder mediaRecorder;
    private String fileName;
    private int position;
    private boolean isPlaying;
    private MediaPlayer mediaPlayer;
    private int lastProgress = 0;
    private BehaviorSubject<Integer> seekBarSubject;
    private boolean isRecording;
    private CountDownTimer timer;
    private int time;
    private List<VoiceMessage> voiceMessages;
    private VoiceMessage voiceMessage;
    private int currentVoiceIndex = 1;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public MainPresenter(MainActivity view) {
        this.view = view;
        getAllVoices();

    }

    @Override
    public void initViews() {
        view.initRecyclerView(voiceMessages);

    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public BehaviorSubject<Integer> getSeekBarSubject() {
        if (seekBarSubject == null) {
            seekBarSubject = BehaviorSubject.create();
            seekBarSubject
                    .delay(SEEK_BAR_UPDATE_DELAY, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                        }

                        @Override
                        public void onNext(Integer integer) {
                            if (mediaPlayer != null && !isUserSeeking) {
                                voiceMessage.setLastProgress(integer);
                                voiceMessages.set(position, voiceMessage);
                                //adapterView.notifyDataSetChanged();
                                adapterView.notifyItemRangeChanged(position, 1);
                                getSeekBarSubject().onNext(mediaPlayer.getCurrentPosition());
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e("aaaa", e + "");
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }
        return seekBarSubject;
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
        File file = new File(root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios");
        if (!file.exists()) {
            file.mkdirs();
        }
        fileName = root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios/" + String.valueOf(System.currentTimeMillis() + ".ogg");
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

//        if (mediaPlayer.getDuration() < LOWER_LIMIT_RECORDING_TIME_MILLISECONDS) {
//            cancelRecording();
//        } else {
//            view.prepareForPlaying();
//        }

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
    public void record() {
        Log.e("AAA", "start recording");
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
                } else {
                    cancelRecording();
                }
            }
        }, START_TIMER_DELAY);
    }

    @Override
    public void initTimer() {
        if (timer == null) {
            timer = new CountDownTimer(UPPER_LIMIT_RECORDING_TIME_MILLISECONDS, 1000) {
                public void onTick(long millisUntilFinished) {
                    int seconds = time;
                    int minutes = seconds / 60;
                    seconds = seconds - (minutes * 60);
                    view.setTimerTv(minutes + ":" + checkDigit(seconds));
                    time++;
                }

                public void onFinish() {
                }
            };
        }
    }

    public void getAllVoices() {
        voiceMessages = new ArrayList<VoiceMessage>();
        String path = Environment.getExternalStorageDirectory() + "/VoiceRecorderSimplifiedCoding/Audios";
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (File f : files) {
            VoiceMessage voiceMessage = new VoiceMessage(f.getPath(), 0, 0);
            voiceMessages.add(voiceMessage);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void stopRecord() {
        Log.e("AAA", "stop recording");

        timer.cancel();
        view.prepareForStop();

        isRecording = false;
        mediaRecorder.stopRecording();

        mediaRecorder = null;
        timer = null;
        adapterView.notifyDataSetChanged();
        //initialMediaPlayer();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void play(int position) {
        setPosition(position);
        setVoiceMessage(voiceMessages.get(position));
        if (mediaPlayer == null)
            initialMediaPlayer();

        if (!voiceMessage.isPlaying() && voiceMessage.getPath() != null) {
            startPlay();
        } else {
            //stopPlay();
        }
    }

    @Override
    public void startPlay() {
        Log.e("AAA", "start playing ");
        voiceMessage.setPlaying(true);
        mediaPlayer.start();
        adapterView.getViewHolder().startPlaying();
        //getSeekBarSubject().onNext(lastProgress);
    }

    @Override
    public void stopPlay() {
        Log.e("AAA", "stop playing");
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
    public void seek(int lastProgress) {
        mediaPlayer.seekTo(lastProgress);
        //todo : seems change need
        isUserSeeking = false;

    }

    @Override
    public int getMediaPlayerDuration() {
        return mediaPlayer.getDuration();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void cancelRecording() {
        Log.e("AAA", "cancel recording");

        if (mediaRecorder != null) mediaRecorder.release();
        if (timer != null) timer.cancel();
        mediaRecorder = null;
        timer = null;
        view.prepareForCancel();
        view.cancelRecording();
    }

    @Override
    public VoiceMessage getVoiceMessage() {
        return voiceMessages.get(currentVoiceIndex);
    }

    @Override
    public void setAdapterView(ChatRvAdapter chatAdapter) {
        this.adapterView = chatAdapter;
    }


    private String checkDigit(int number) {
        return number <= 9 ? "0" + number : String.valueOf(number);
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
}
