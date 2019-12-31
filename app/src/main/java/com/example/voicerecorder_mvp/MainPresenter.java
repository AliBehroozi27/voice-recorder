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
    private OpusRecorder mediaRecorder;
    private String fileName;
    private boolean isPlaying;
    private MediaPlayer mediaPlayer;
    private int lastProgress = 0;
    private BehaviorSubject<Integer> seekBarSubject;
    private boolean isRecording;
    private CountDownTimer timer;
    private int time;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public MainPresenter(MainActivity view) {
        this.view = view;
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
                                view.setSeekBarProgress(integer);
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
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaPlayer.getDuration() < LOWER_LIMIT_RECORDING_TIME_MILLISECONDS) {
            cancelRecording();
        } else {
            view.prepareForPlaying();
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlay();
                setLastProgress(INITIAL_PROGRESS);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void record() {
        Log.e("AAA" , "start recording");
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
                    //cancelRecording();
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


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void stopRecord() {
        Log.e("AAA" , "stop recording");

        timer.cancel();
        view.prepareForStop();

        isRecording = false;
        mediaRecorder.stopRecording();

        mediaRecorder = null;
        timer = null;
        initialMediaPlayer();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void play() {
        if (mediaPlayer == null)
            initialMediaPlayer();

        if (!isPlaying && fileName != null) {
            isPlaying = true;
            startPlay();
        } else {
            isPlaying = false;
            stopPlay();
        }
    }

    @Override
    public void startPlay() {
        Log.e("AAA" , "start playing");

        isPlaying = true;
        mediaPlayer.start();
        view.startPlaying();
        getSeekBarSubject().onNext(lastProgress);
    }

    @Override
    public void stopPlay() {
        Log.e("AAA" , "top playing");

        isPlaying = false;

        setLastProgress(mediaPlayer.getCurrentPosition());
        try {
            mediaPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer = null;
        view.stopPlaying();
    }

    @Override
    public void seek(int lastProgress) {
        mediaPlayer.seekTo(lastProgress);
        isUserSeeking = false;

    }

    @Override
    public int getMediaPlayerDuration() {
        return mediaPlayer.getDuration();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void cancelRecording() {
        Log.e("AAA" , "cancel recording");

        if (mediaRecorder != null) mediaRecorder.release();
        if (timer != null) timer.cancel();
        mediaRecorder = null;
        timer = null;
        view.prepareForCancel();
        view.cancelRecording();
    }


    private String checkDigit(int number) {
        return number <= 9 ? "0" + number : String.valueOf(number);
    }


    public int getLastProgress() {
        return lastProgress;
    }

    public void setLastProgress(int lastProgress) {
        this.lastProgress = lastProgress;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
