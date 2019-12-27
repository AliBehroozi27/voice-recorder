package com.example.voicerecorder_mvp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements MainContract.View {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.timer)
    TextView timerTv;
    @BindView(R.id.recording_state)
    TextView recordingState;
    @BindView(R.id.linearLayoutRecorder)
    LinearLayout linearLayoutRecorder;
    @BindView(R.id.imageViewRecord)
    ImageView imageViewRecord;
    @BindView(R.id.imageViewStop)
    ImageView imageViewStop;
    @BindView(R.id.imageViewPlay)
    ImageView imageViewPlay;
    @BindView(R.id.linearLayoutPlay)
    LinearLayout linearLayoutPlay;
    @BindView(R.id.seekBar)
    SeekBar seekBar;


    private int REQUEST_PERMISSION = 123;
    private MainPresenter presenter;
    int time = 0;
    private CountDownTimer timer;
    private float positionY;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //butter_knife bind
        ButterKnife.bind(this);

        //initial views
        initViews();

        //initial timerTv
        initTimer();

        //init presenter
        presenter = new MainPresenter(this);
        presenter.requestPermission();
    }

    public String checkDigit(int number) {
        return number <= 9 ? "0" + number : String.valueOf(number);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);

        } else {
            presenter.initialMediaRecorder();
            //presenter.initialMediaPlayer();
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

                presenter.initialMediaRecorder();
                //presenter.initialMediaPlayer();

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

        imageViewRecord.setOnTouchListener(new View.OnTouchListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    recordingState.setVisibility(View.INVISIBLE);
                    if (event.getY() > positionY + 150) {
                        presenter.cancelRecording();

                    } else if (event.getY() < positionY - 150) {
                        //nothing
                    } else {
                        presenter.stopRecord();
                    }
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (event.getY() > positionY + 150) {
                        recordingState.setText("Cancel");
                        recordingState.setTextColor(Color.parseColor("#FF2196F3"));
                    } else if (event.getY() < positionY - 150) {
                        recordingState.setText("Lock");
                        recordingState.setTextColor(Color.parseColor("#FFCC1F1F"));
                    } else {
                        recordingState.setText("Recording");
                        recordingState.setTextColor(Color.parseColor("#FFA8A8A8"));
                    }
                    return true;
                }

                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    recordingState.setVisibility(View.VISIBLE);
                    positionY = event.getY();
                    presenter.record();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void initTimer() {
        timer = new CountDownTimer(300000, 1000) {
            public void onTick(long millisUntilFinished) {
                int minutes;
                int seconds = time;
                minutes = seconds / 60;
                seconds = seconds - (minutes * 60);
                timerTv.setText(minutes + ":" + checkDigit(seconds));
                time++;
            }

            public void onFinish() {
            }
        };
    }

    //@RequiresApi(api = Build.VERSION_CODES.KITKAT)
    //@OnClick(R.id.imageViewRecord)
    public void onRecordClick() {
        //
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @OnClick(R.id.imageViewStop)
    public void onStopClick() {
        presenter.stopRecord();
    }

    @OnClick(R.id.imageViewPlay)
    public void onPlayClick() {
        presenter.play();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForRecording() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.GONE);
        imageViewStop.setVisibility(View.VISIBLE);
        linearLayoutPlay.setVisibility(View.GONE);
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
        //stopPlaying();
        time = 0;

        //start timer with a bit delay
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                timer.start();
            }
        }, 400);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForStop() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.VISIBLE);
        imageViewStop.setVisibility(View.GONE);
        linearLayoutPlay.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForCancel() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.VISIBLE);
        imageViewStop.setVisibility(View.GONE);
        linearLayoutPlay.setVisibility(View.GONE);
        timerTv.setText("0:00");

    }

    @Override
    public void stopRecording() {
        timer.cancel();
        //Toast.makeText(this, "Recording saved successfully.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void startPlaying() {
        imageViewPlay.setImageResource(R.drawable.ic_pause);

        seekBar.setProgress(presenter.getLastProgress());
        presenter.seek(presenter.getLastProgress());
        seekBar.setMax(presenter.getMediaPlayerDuration());
    }

    @Override
    public void stopPlaying() {
        imageViewPlay.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void setSeekBarProgress(int currentPosition) {
        seekBar.setProgress(currentPosition);
    }

    @Override
    public void cancelRecording() {
        timer.cancel();
        time = 0;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_list:
//                Intent intent = new Intent(this, RecordingListActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

}
