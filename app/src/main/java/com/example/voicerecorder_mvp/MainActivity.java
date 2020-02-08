package com.example.voicerecorder_mvp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.voicerecorder_mvp.customView.AudioRecordView;
import com.example.voicerecorder_mvp.pojo.VoiceMessage;

import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import rm.com.audiowave.OnProgressListener;

public class MainActivity extends AppCompatActivity implements MainContract.View , AudioRecordView.RecordingListener {

    private static final String TAG = "MainActivity";
    @BindView(R.id.recycler_view)
    RecyclerView list;
    @BindView(R.id.recordingView)
    AudioRecordView audioRecordView;
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

        ButterKnife.bind(this);

        initViews();

        //init presenter
        presenter = new MainPresenter(this,this);
        presenter.requestPermission();
        presenter.getAllVoices();
        presenter.initViews();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);

        } else {
            Log.e(TAG, "" + (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED));
            Log.e("AAA" , "" + (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
            Log.e("AAA" , "" + (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
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
        audioRecordView.setRecordingListener(this);

        ((SimpleItemAnimator) list.getItemAnimator()).setSupportsChangeAnimations(false);
        
        audioRecordView.getAttachmentView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        audioRecordView.getSendView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = audioRecordView.getMessageView().getText().toString();
                audioRecordView.getMessageView().setText("");
            }
        });

        audioRecordView.getPlay().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!presenter.isPlaying()) {
                    if (presenter.isPlayRecording()) {
                        presenter.stopPlayRecording();
                        presenter.removeRecordingSeekBarUpdateCallbacks();
                    } else {
                        presenter.startPlayRecording();
                        presenter.updateRecordingOverviewSeekBar();
                    }
                }
            }
        });

        audioRecordView.getWave().setOnProgressListener(new OnProgressListener() {
            @Override
            public void onStartTracking(float v) {
            }

            @Override
            public void onStopTracking(float v) {
            }

            @Override
            public void onProgressChanged(float progress, boolean fromUser) {
                int position = (int) ((progress / 100) * presenter.getRecordingDuration());
                audioRecordView.getTimeText().setText(presenter.calculateTime(position));
                if (presenter.getMediaPlayer() != null && fromUser) {
                    presenter.isUserSeeking = true;
                    presenter.seek(position , presenter.getRecordingDuration());
                    presenter.setRecordingLastProgress(position);
                } else if (position == presenter.getPosition() || fromUser) {
                    presenter.setRecordingLastProgress(position);
                }
            }
        });
    }

    @Override
    public void prepareForRecording() {
    }

    @Override
    public void startRecording() {
    }


    @Override
    public void cancelRecording() {
    }

    @Override
    public void initRecyclerView(List<VoiceMessage> voiceMessages) {
        chatAdapter = new ChatRvAdapter(this, voiceMessages, presenter);
        list.setAdapter(chatAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        presenter.setAdapterView(chatAdapter);
    }

    @Override
    public void setShadowScale(double pressure) {
        float p = getShadowScale((float)pressure);
        audioRecordView.getImageViewShadow().animate().scaleY(p).scaleX(p).setDuration(500).start();
    }

    @Override
    public void playRecording() {
        audioRecordView.getPlay().setImageResource(R.drawable.ic_puase_white);
    }

    @Override
    public void stopPlayRecording() {
        audioRecordView.getPlay().setImageResource(R.drawable.ic_play_white);
    }

    @Override
    public void updateRecordingPlaySeekBar(int progress) {
        audioRecordView.getWave().setProgress(presenter.convertCurrentMediaPositionIntoPercent(progress, presenter.getRecordingDuration()));
    }
    
    @Override
    public AudioRecordView getRecordingView() {
        return audioRecordView;
    }
    
    private float getShadowScale(float pressure){
        return pressure / 1300 > 0.5 ? (float) 1.5 : pressure / 1300 + 1 < 1.3 ? 1 : pressure / 1300 + 1;
    }

    @Override
    public void onRecordingStarted() {
        presenter.startRecord();
    }

    @Override
    public void onRecordingLocked() {
    }

    @Override
    public void onSendClick() {
        Log.e("AAA" , "time : " + audioRecordView.getRecordingTime() + "  lim : " + MainPresenter.LOWER_LIMIT_RECORDING_TIME_MILLISECONDS);
        if (audioRecordView.getRecordingTime() < MainPresenter.LOWER_LIMIT_RECORDING_TIME_MILLISECONDS) {
            presenter.cancelRecording();
        } else {
            presenter.sendVoice();
        }
    }

    @Override
    public void onDeleteClick() {
        presenter.deleteVoice();
    }
    
    @Override
    public void onRecordingCompleted() {
        presenter.stopRecord();
        if (audioRecordView.getRecordingTime() < MainPresenter.LOWER_LIMIT_RECORDING_TIME_MILLISECONDS && !presenter.isRecording()) {
            audioRecordView.delete();
            presenter.deleteVoice();
        }else {
            audioRecordView.setWaveRawData(presenter.getRecordingRawData());
            audioRecordView.getWave().setProgress(0);
        }
    }

    @Override
    public void onRecordingCanceled() {
        presenter.cancelRecording();
    }
    
    
    @Override
    protected void onPause() {
        Log.e("AAA" , "onDestroy");
        if (presenter.isRecording()) {
            Log.e("AAA" , "canceling");
            audioRecordView.setRecordingBehaviour(AudioRecordView.RecordingBehaviour.LOCK_DONE);
            audioRecordView.stopRecording();
        }
        super.onPause();
    }
}
