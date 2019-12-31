package com.example.voicerecorder_mvp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements MainContract.View {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.timer)
    TextView timerTv;
    @BindView(R.id.recording_state)
    TextView recordingStateTv;
    @BindView(R.id.linear_layout_recorder)
    LinearLayout recorderLl;
    @BindView(R.id.image_view_record)
    ImageView recordIv;
    @BindView(R.id.image_view_stop)
    ImageView stopIv;
    @BindView(R.id.image_view_play)
    ImageView playIv;
    @BindView(R.id.playLl)
    LinearLayout playLl;
    @BindView(R.id.seekBar)
    SeekBar seekBar;
    @BindString(R.string.start_time)
    String startTime;

    private int REQUEST_PERMISSION = 123;
    private float positionY;
    private MainPresenter presenter;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //butter_knife bind
        ButterKnife.bind(this);

        //initial views
        initViews();

        //init presenter
        presenter = new MainPresenter(this);
        presenter.requestPermission();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);

        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length == 3 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initViews() {
        toolbar.setTitle("Voice Recorder");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
        setSupportActionBar(toolbar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (presenter.getMediaPlayer() != null && fromUser) {
                    presenter.isUserSeeking = true;
                    presenter.seek(progress);
                    presenter.setLastProgress(progress);
                } else if (fromUser) {
                    presenter.setLastProgress(progress);
                }

                int seconds = progress / 1000;
                int minutes = seconds / 60;
                seconds = seconds - (minutes * 60);

                timerTv.setText(minutes + ":" + checkDigit(seconds));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        recordIv.setOnTouchListener(new View.OnTouchListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    recordingStateTv.setVisibility(View.INVISIBLE);
                    if (event.getY() > positionY + 150) {
                        //cancel recording
                        presenter.cancelRecording();

                    } else if (event.getY() < positionY - 150) {
                        //doing nothing to lock recording
                    } else {
                        //usual case
                        presenter.stopRecord();
                    }
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (event.getY() > positionY + 150) {
                        recordingStateTv.setText("Cancel");
                        recordingStateTv.setTextColor(Color.parseColor("#FF2196F3"));
                    } else if (event.getY() < positionY - 150) {
                        recordingStateTv.setText("Lock");
                        recordingStateTv.setTextColor(Color.parseColor("#FFCC1F1F"));
                    } else {
                        recordingStateTv.setText("Recording");
                        recordingStateTv.setTextColor(Color.parseColor("#FFA8A8A8"));
                    }
                    return true;
                }

                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    recordingStateTv.setVisibility(View.VISIBLE);
                    positionY = event.getY();
                    presenter.record();
                    return true;
                }
                return false;
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @OnClick(R.id.image_view_stop)
    public void onStopClick() {
        presenter.stopRecord();
    }

    @OnClick(R.id.image_view_play)
    public void onPlayClick() {
        presenter.play();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForRecording() {
        TransitionManager.beginDelayedTransition(recorderLl);
        recordIv.setVisibility(View.GONE);
        stopIv.setVisibility(View.VISIBLE);
        playLl.setVisibility(View.GONE);
    }

    @Override
    public void prepareForPlaying() {
        seekBar.setProgress(presenter.getLastProgress());
        presenter.seek(presenter.getLastProgress());
        seekBar.setMax(presenter.getMediaPlayerDuration());
    }

    @Override
    public void startRecording() {
        presenter.setLastProgress(0);
        seekBar.setProgress(0);
        presenter.setTime(0);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForStop() {
        TransitionManager.beginDelayedTransition(recorderLl);
        recordIv.setVisibility(View.VISIBLE);
        stopIv.setVisibility(View.GONE);
        playLl.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForCancel() {
        TransitionManager.beginDelayedTransition(recorderLl);
        recordIv.setVisibility(View.VISIBLE);
        stopIv.setVisibility(View.GONE);
        playLl.setVisibility(View.GONE);
        timerTv.setText(startTime);
    }

    private String checkDigit(int number) {
        return number <= 9 ? "0" + number : String.valueOf(number);
    }

    @Override
    public void startPlaying() {
        playIv.setImageResource(R.drawable.ic_pause);
        seekBar.setProgress(presenter.getLastProgress());
        presenter.seek(presenter.getLastProgress());
        seekBar.setMax(presenter.getMediaPlayerDuration());
    }

    @Override
    public void stopPlaying() {
        playIv.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void setSeekBarProgress(int currentPosition) {
        seekBar.setProgress(currentPosition);
    }

    @Override
    public void cancelRecording() {
        presenter.setTime(0);
    }

    @Override
    public void setTimerTv(String time) {
        timerTv.setText(time);
    }

}
