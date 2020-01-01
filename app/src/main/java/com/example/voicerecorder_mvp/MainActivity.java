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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements MainContract.View {


    @BindView(R.id.recycler_view)
    RecyclerView messagesRv;
    @BindView(R.id.message_area)
    TextView messageArea;
    @BindView(R.id.record_button)
    ImageView recordButton;
    @BindString(R.string.start_time)
    String startTime;

    private int REQUEST_PERMISSION = 123;
    private float positionY,positionX;
    private MainPresenter presenter;
    private ChatRvAdapter chatAdapter;


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
        presenter.initViews();
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


        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    messageArea.setVisibility(View.INVISIBLE);
                    if (event.getX() < positionX - 150) {
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
                    Log.e("AAA" , "x : " + event.getX() +  "||  y: " + event.getY());
                    if (event.getX() < positionX - 150) {
                        messageArea.setText("Cancel");
                        messageArea.setTextColor(Color.parseColor("#FF2196F3"));
                    } else if (event.getY() < positionY - 150) {
                        messageArea.setText("Lock");
                        messageArea.setTextColor(Color.parseColor("#FFCC1F1F"));
                    } else {
                        //messageArea.setText("Recording");
                        messageArea.setTextColor(Color.parseColor("#FFA8A8A8"));
                    }
                    return true;
                }

                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    messageArea.setVisibility(View.VISIBLE);
                    positionY = event.getY();
                    positionX = event.getX();
                    presenter.record();
                    return true;
                }
                return false;
            }
        });
    }


//    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//    @OnClick(R.id.image_view_stop)
//    public void onStopClick() {
//        presenter.stopRecord();
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//    @OnClick(R.id.image_view_play)
//    public void onPlayClick() {
//        presenter.play();
//    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForRecording() {
//        TransitionManager.beginDelayedTransition(recorderLl);
//        recordIv.setVisibility(View.GONE);
//        stopIv.setVisibility(View.VISIBLE);
//        playLl.setVisibility(View.GONE);
        messageArea.setText(startTime);
    }

    @Override
    public void prepareForPlaying() {
//        seekBar.setProgress(presenter.getLastProgress());
//        presenter.seek(presenter.getLastProgress());
//        seekBar.setMax(pr     esenter.getMediaPlayerDuration());
    }

    @Override
    public void startRecording() {
//        presenter.setLastProgress(0);
//        seekBar.setProgress(0);
//        presenter.setTime(0);
        presenter.setTime(0);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForStop() {
//        TransitionManager.beginDelayedTransition(recorderLl);
//        recordButton.setVisibility(View.VISIBLE);
//        stopIv.setVisibility(View.GONE);
//        playLl.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForCancel() {
//        TransitionManager.beginDelayedTransition(recorderLl);
//        recordButton.setVisibility(View.VISIBLE);
//        stopIv.setVisibility(View.GONE);
//        playLl.setVisibility(View.GONE);
//        timerTv.setText(startTime);
    }

    private String checkDigit(int number) {
        return number <= 9 ? "0" + number : String.valueOf(number);
    }

    @Override
    public void startPlaying() {
//        playIv.setImageResource(R.drawable.ic_pause);
//        seekBar.setProgress(presenter.getLastProgress());
//        presenter.seek(presenter.getLastProgress());
//        seekBar.setMax(presenter.getMediaPlayerDuration());
    }

    @Override
    public void stopPlaying() {

//        playIv.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void setSeekBarProgress(int currentPosition) {

//        seekBar.setProgress(currentPosition);
    }

    @Override
    public void cancelRecording() {
        presenter.setTime(0);
    }

    @Override
    public void initRecyclerView(List<VoiceMessage> voiceMessages) {
        chatAdapter = new ChatRvAdapter(this, voiceMessages, presenter);
        messagesRv.setAdapter(chatAdapter);
        messagesRv.setLayoutManager(new LinearLayoutManager(this));
        presenter.setAdapterView(chatAdapter);
    }

    @Override
    public void setTimerTv(String time) {
        messageArea.setText(time);
    }

}
