package com.example.voicerecorder_mvp;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaTimestamp;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class MainPresenter implements MainContract.Presenter {

    public boolean isUserSeeking = false;
    private MainActivity view;
    private MediaRecorder mediaRecorder;
    private String fileName;
    private boolean isPlaying;
    private MediaPlayer mediaPlayer;
    private int lastProgress = 0;
    private BehaviorSubject<Integer> seekBarSubject;

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
                    .delay(5, TimeUnit.MILLISECONDS)
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
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(44100);
        mediaRecorder.setAudioSamplingRate(48000);
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios");
        if (!file.exists()) {
            file.mkdirs();
        }
        fileName = root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios/" + String.valueOf(System.currentTimeMillis() + ".ogg");
        mediaRecorder.setOutputFile(fileName);

    }

    @Override
    public void initialMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        view.prepareForPlaying();

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlay();
                setLastProgress(0);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void record() {
        if (mediaRecorder == null)
            initialMediaRecorder();
        view.prepareForRecording();

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        view.startRecording();
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void stopRecord() {
        view.prepareForStop();

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaRecorder = null;

        view.stopRecording();
        initialMediaPlayer();
    }

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
        isPlaying = true;

        mediaPlayer.start();
        view.startPlaying();
        getSeekBarSubject().onNext(lastProgress);
    }

    @Override
    public void stopPlay() {
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
        mediaRecorder.stop();
        mediaRecorder = null;
        view.prepareForCancel();
        view.cancelRecording();
    }

    public int getLastProgress() {
        return lastProgress;
    }

    public void setLastProgress(int lastProgress) {
        this.lastProgress = lastProgress;
    }
}
