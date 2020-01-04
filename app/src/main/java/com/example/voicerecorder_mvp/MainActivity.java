package com.example.voicerecorder_mvp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements MainContract.View {


    @BindView(R.id.recycler_view)
    RecyclerView messagesRv;
    @BindView(R.id.message_area)
    TextView messageArea;
    @BindView(R.id.state)
    TextView recordingState;
    @BindView(R.id.record_button)
    ImageView recordButton;
    @BindString(R.string.start_time)
    String START_TIME;
    @BindString(R.string.state_recording)
    String STATE_RECORDING;
    @BindString(R.string.state_cancel)
    String STATE_CANCEL;
    @BindString(R.string.state_lock)
    String STATE_LOCK;
    @BindString(R.string.record_message)
    String RECORD_MESSAGE;

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
                    recordingState.setVisibility(View.INVISIBLE);
                    messageArea.setText(RECORD_MESSAGE);
                    if (event.getX() < positionX - 150) {
                        //cancel recording
                        presenter.cancelRecording();

                    } else if (event.getY() < positionY - 150) {
                        //doing nothing to lock recording
                    } else {
                        //usual case
                        if (presenter.isRecording())
                            presenter.stopRecord();
                    }
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    //Log.e("AAA" , "x : " + event.getX() +  "||  y: " + event.getY());
                    if (event.getX() < positionX - 150) {
                        recordingState.setText(STATE_CANCEL);
                        recordingState.setTextColor(Color.parseColor("#FF2196F3"));
                    } else if (event.getY() < positionY - 150) {
                        recordingState.setText(STATE_LOCK);
                        recordingState.setTextColor(Color.parseColor("#12B318"));
                    } else {
                        messageArea.setTextColor(Color.parseColor("#FFA8A8A8"));
                        recordingState.setTextColor(Color.parseColor("#FFCC1F1F"));
                        recordingState.setText(STATE_RECORDING);
                    }
                    return true;
                }

                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    recordingState.setVisibility(View.VISIBLE);
                    positionY = event.getY();
                    positionX = event.getX();
                    if (presenter.isRecording()) {
                        presenter.stopRecord();
                    } else {
                        presenter.startRecord();
                    }
                    return true;
                }
                return false;
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void prepareForRecording() {
        messageArea.setText(START_TIME);
    }

    @Override
    public void startRecording() {
        presenter.setTime(0);
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
