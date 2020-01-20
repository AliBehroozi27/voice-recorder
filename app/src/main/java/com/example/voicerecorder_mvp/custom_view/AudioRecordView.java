package com.example.voicerecorder_mvp.custom_view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voicerecorder_mvp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import rm.com.audiowave.AudioWaveView;


public class AudioRecordView extends FrameLayout {


    private float normalScaleY;
    private int h;
    private ConstraintLayout.LayoutParams arrowParams;
    private int ARROW_TOP_MARGIN;
    private ConstraintLayout layoutLockInternal;
    private boolean isRecording;
    private boolean isCanceled;


    public enum UserBehaviour {
        CANCELING,
        LOCKING,
        NONE
    }

    public enum RecordingBehaviour {
        CANCELED,
        LOCKED,
        LOCK_DONE,
        RELEASED,
        SEND, DONE
    }

    public interface RecordingListener {

        void onRecordingStarted();

        void onRecordingLocked();

        void onRecordingCompleted();

        void onRecordingCanceled();

        void onSendClick();

        void onDeleteClick();
    }

    private View imageViewAudio, imageViewDelete, imageViewShadow, imageViewLockArrow, imageViewLock, imageViewLockHandler, imageViewMic, dustin, dustin_cover, imageViewStop, layoutSend;
    private View layoutDustin, layoutMessage, imageViewAttachment;
    private View layoutSlideCancel, layoutLock;
    private EditText editTextMessage;
    private TextView timeText, cancel;
    private AudioWaveView wave;


    private ImageView stop, audio, send, play;

    private Animation animBlink, animJump, animJumpFast, animJumpSlow, animPressure;

    private boolean isDeleting;
    private boolean stopTrackingAction;
    private Handler handler;

    private int audioTotalTime;
    private TimerTask timerTask;
    private Timer audioTimer;
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("m:ss", Locale.getDefault());

    private float lastX, lastY;
    private float firstX, firstY;

    private float directionOffset, cancelOffset, lockOffset;
    private float dp = 0;
    private boolean isLocked = false;

    private UserBehaviour userBehaviour = UserBehaviour.NONE;
    private RecordingBehaviour recordingBehaviour;
    private RecordingListener recordingListener;

    public AudioRecordView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public AudioRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public AudioRecordView(Context context) {
        super(context);
        initView();
    }

    private void initView() {

        Log.e("AAA" , "init views");
        View view = inflate(getContext(), R.layout.recording_layout, null);
        addView(view);

        imageViewAttachment = view.findViewById(R.id.imageViewAttachment);
        editTextMessage = view.findViewById(R.id.editTextMessage);

        send = view.findViewById(R.id.imageSend);
        stop = view.findViewById(R.id.imageStop);
        audio = view.findViewById(R.id.imageAudio);
        play = view.findViewById(R.id.imageViewPlay);

        wave = view.findViewById(R.id.wave);

        cancel = view.findViewById(R.id.textViewCancel);

        imageViewDelete = view.findViewById(R.id.imageViewDelete);
        imageViewShadow = view.findViewById(R.id.imageAudioShadow);
        imageViewAudio = view.findViewById(R.id.imageViewAudio);
        imageViewStop = view.findViewById(R.id.imageViewStop);
        layoutSend = view.findViewById(R.id.imageViewSend);
        imageViewLock = view.findViewById(R.id.imageViewLock);
        imageViewLockHandler = view.findViewById(R.id.imageViewLockHandler);
        imageViewLockArrow = view.findViewById(R.id.imageViewLockArrow);
        layoutDustin = view.findViewById(R.id.layoutDustin);
        layoutMessage = view.findViewById(R.id.layoutMessage);
        timeText = view.findViewById(R.id.textViewTime);
        layoutSlideCancel = view.findViewById(R.id.layoutSlideCancel);
        layoutLock = view.findViewById(R.id.layoutLock);
        layoutLockInternal = view.findViewById(R.id.layoutLockInternal);
        imageViewMic = view.findViewById(R.id.imageViewMic);
        dustin = view.findViewById(R.id.dustin);
        dustin_cover = view.findViewById(R.id.dustin_cover);


        handler = new Handler(Looper.getMainLooper());

        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics());

        animBlink = AnimationUtils.loadAnimation(getContext(),
                R.anim.blink);
        animJump = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump);
        animJumpFast = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump_fast);
        animJumpSlow = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump_slow);


        setupRecording();
    }

    public void setAudioRecordButtonImage(int imageResource) {
        audio.setImageResource(imageResource);
    }

    public void setStopButtonImage(int imageResource) {
        stop.setImageResource(imageResource);
    }

    public RecordingListener getRecordingListener() {
        return recordingListener;
    }

    public void setRecordingListener(RecordingListener recordingListener) {
        this.recordingListener = recordingListener;
    }

    public View getSendView() {
        return layoutSend;
    }

    public AudioWaveView getWave() {
        return wave;
    }

    public TextView getTimeText() {
        return timeText;
    }

    public ImageView getPlay() {
        return play;
    }

    public View getAttachmentView() {
        return imageViewAttachment;
    }

    public View getImageViewShadow() {
        return imageViewShadow;
    }

    public EditText getMessageView() {
        return editTextMessage;
    }

    private void setupRecording() {
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                recordingBehaviour = RecordingBehaviour.CANCELED;
                stopRecording();
            }
        });

        imageViewDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("AAA", "DELETE : " + recordingBehaviour);
                recordingListener.onDeleteClick();
                delete();
            }
        });

        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("AAA", "OnSend : " + recordingBehaviour);
                //recordingListener.onRecordingCompleted();
                recordingBehaviour = RecordingBehaviour.SEND;
                stopRecording();
            }
        });


        imageViewAudio.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isDeleting) {
                    return true;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    cancelOffset = (float) (imageViewAudio.getX() / 2.8);
                    lockOffset = (float) (imageViewAudio.getX() / 4);

                    if (firstX == 0) {
                        firstX = motionEvent.getRawX();
                    }

                    if (firstY == 0) {
                        firstY = motionEvent.getRawY();
                    }
                    startRecord();

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP
                        || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                        Log.e("AAA" , "fuckkk  " + recordingBehaviour +"  "+isLocked);
                        if (!isLocked && recordingBehaviour != RecordingBehaviour.CANCELED) {
                            recordingBehaviour = RecordingBehaviour.RELEASED;
                            stopRecording();
                        }
                    }

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (stopTrackingAction) {
                        return true;
                    }

                    UserBehaviour direction = UserBehaviour.NONE;

                    float motionX = Math.abs(firstX - motionEvent.getRawX());
                    float motionY = Math.abs(firstY - motionEvent.getRawY());

                    if (motionX > directionOffset &&
                            motionX > directionOffset &&
                            lastX < firstX && lastY < firstY) {

                        if (motionX > motionY && lastX < firstX) {
                            direction = UserBehaviour.CANCELING;

                        } else if (motionY > motionX && lastY < firstY) {
                            direction = UserBehaviour.LOCKING;
                        }

                    } else if (motionX > motionY && motionX > directionOffset && lastX < firstX) {
                        direction = UserBehaviour.CANCELING;
                    } else if (motionY > motionX && motionY > directionOffset && lastY < firstY) {
                        direction = UserBehaviour.LOCKING;
                    }

                    if (direction == UserBehaviour.CANCELING) {
                        if (userBehaviour == UserBehaviour.NONE || motionEvent.getRawY() + imageViewAudio.getWidth() / 2 > firstY) {
                            userBehaviour = UserBehaviour.CANCELING;
                        }

                        if (userBehaviour == UserBehaviour.CANCELING || userBehaviour == UserBehaviour.LOCKING) {
                            translateX(-(firstX - motionEvent.getRawX()));
                        }
                    } else if (direction == UserBehaviour.LOCKING) {
                        if (userBehaviour == UserBehaviour.NONE || motionEvent.getRawX() + imageViewAudio.getWidth() / 2 > firstX) {
                            userBehaviour = UserBehaviour.LOCKING;
                        }

                        if (userBehaviour == UserBehaviour.LOCKING || userBehaviour == UserBehaviour.CANCELING) {
                            translateY(-(firstY - motionEvent.getRawY()));
                        }
                    }

                    lastX = motionEvent.getRawX();
                    lastY = motionEvent.getRawY();
                }
                view.onTouchEvent(motionEvent);
                return true;
            }
        });

        imageViewStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLocked = false;
                recordingBehaviour = RecordingBehaviour.LOCK_DONE;
                stopRecording();
            }
        });

        layoutSend.animate().scaleX(0f).scaleY(0f).setDuration(100).setInterpolator(new AccelerateInterpolator()).start();
    }


    private void translateY(float y) {
        if (y < -lockOffset && userBehaviour != UserBehaviour.CANCELING) {
            locked();
            imageViewAudio.setTranslationY(0);
            imageViewLock.setTranslationY(0);
            return;
        }

        if (layoutLock.getVisibility() != View.VISIBLE) {
            layoutLock.setVisibility(View.VISIBLE);
        }
        if (normalScaleY == 0) {
            normalScaleY = y;
            arrowParams = (ConstraintLayout.LayoutParams) imageViewLockArrow.getLayoutParams();
            ARROW_TOP_MARGIN = arrowParams.topMargin;
        }

        imageViewLock.clearAnimation();
        imageViewLockHandler.clearAnimation();
        imageViewLock.setTranslationY(y / 20);
        imageViewAudio.setTranslationY(y);
        layoutLock.setTranslationY((int) (y * 1.1));
        arrowParams.setMargins(0, ARROW_TOP_MARGIN + arrowMarginInterpolator(y), 0, 0);
        imageViewLockArrow.setLayoutParams(arrowParams);
        imageViewLockArrow.setAlpha(arrowAlphaInterpolator(y));
        imageViewAudio.setTranslationX(0);
    }

    private void translateX(float x) {
        if (x < -cancelOffset) {
            canceled();
            imageViewAudio.setTranslationX(0);
            layoutSlideCancel.setTranslationX(0);
            return;
        }

        layoutSlideCancel.setTranslationX(x);

        if (Math.abs(x) < imageViewMic.getWidth() / 2) {
            if (layoutLock.getVisibility() != View.VISIBLE) {
                layoutLock.setVisibility(View.VISIBLE);
            }
        }
    }


    private int arrowMarginInterpolator(float y) {
        return (int) (y / 2.8);
    }

    private float arrowAlphaInterpolator(float y) {
        return 1 - (y / -180);
    }

    private void locked() {
        stopTrackingAction = true;
        recordingBehaviour = RecordingBehaviour.LOCKED;
        stopRecording();
        isLocked = true;
    }

    private void canceled() {
        stopTrackingAction = true;
        recordingBehaviour = RecordingBehaviour.CANCELED;
        stopRecording();
    }

    private void stopRecording() {
        Log.e("AAAA", "STOP " + recordingBehaviour);
        stopTrackingAction = true;
        firstX = 0;
        firstY = 0;
        lastX = 0;
        lastY = 0;

        userBehaviour = UserBehaviour.NONE;

        imageViewShadow.animate().scaleY(1f).scaleX(1f).setDuration(100).start();
        imageViewAudio.animate().scaleX(1f).scaleY(1f).translationX(0).translationY(0).setDuration(100).setInterpolator(new AccelerateInterpolator()).start();
        layoutSlideCancel.setTranslationX(0);
        layoutSlideCancel.setVisibility(View.GONE);

        layoutLock.setVisibility(View.GONE);
        layoutLock.setTranslationY(0);
        imageViewLockArrow.setAlpha(1f);
        if (arrowParams != null) {
            arrowParams.setMargins(0, ARROW_TOP_MARGIN, 0, 0);
            imageViewLockArrow.setLayoutParams(arrowParams);
        }
        imageViewLock.setTranslationY(0);
        imageViewLockArrow.clearAnimation();
        imageViewLock.clearAnimation();


        if (recordingBehaviour == RecordingBehaviour.LOCKED) {
            imageViewStop.setVisibility(VISIBLE);
            layoutSend.setVisibility(VISIBLE);
            cancel.setVisibility(VISIBLE);
            layoutSend.animate().scaleY(2f).scaleX(2f).setDuration(100).start();
            imageViewAudio.animate().scaleY(0f).scaleX(0f).setDuration(100).start();


            if (recordingListener != null)
                recordingListener.onRecordingLocked();

        } else if (recordingBehaviour == RecordingBehaviour.CANCELED) {
            Log.e("AAA", "cancel");
            timeText.clearAnimation();
            imageViewStop.setVisibility(View.GONE);

            timerTask.cancel();
            timerTask = null;
            delete();

            if (recordingListener != null)
                recordingListener.onRecordingCanceled();
            recordingBehaviour = null;

        } else if (recordingBehaviour == RecordingBehaviour.RELEASED) {
            timeText.clearAnimation();
            timeText.setVisibility(View.INVISIBLE);
            imageViewMic.setVisibility(View.INVISIBLE);
            imageViewAttachment.setVisibility(View.VISIBLE);
            imageViewStop.setVisibility(View.GONE);
            editTextMessage.setVisibility(VISIBLE);

            timerTask.cancel();
            timerTask = null;


            if (recordingListener != null){
                recordingListener.onRecordingCompleted();
                recordingListener.onSendClick();
            }
        }else if (recordingBehaviour == RecordingBehaviour.LOCK_DONE) {
            timeText.clearAnimation();

            wave.setVisibility(VISIBLE);
            play.setVisibility(VISIBLE);
            imageViewDelete.setVisibility(VISIBLE);
            layoutSend.setVisibility(VISIBLE);
            timeText.setVisibility(VISIBLE);
            imageViewDelete.setVisibility(VISIBLE);
            layoutSend.animate().scaleX(1f).scaleY(1f).setDuration(100).start();

            imageViewMic.setVisibility(GONE);
            imageViewAttachment.setVisibility(GONE);
            editTextMessage.setVisibility(GONE);
            imageViewStop.setVisibility(GONE);
            imageViewAudio.setVisibility(GONE);

            timerTask.cancel();
            timerTask = null;


            if (recordingListener != null)
                recordingListener.onRecordingCompleted();
        } else if (recordingBehaviour == RecordingBehaviour.SEND) {

            timeText.clearAnimation();
            timeText.animate().translationX(1000).setDuration(100).start();
            imageViewMic.animate().translationX(1000).setDuration(100).start();
            wave.animate().translationX(1000).setDuration(100).start();
            play.animate().translationX(1000).setDuration(100).start();
            imageViewDelete.animate().translationX(1000).setDuration(100).start();
            cancel.animate().translationX(1000).setDuration(100).start();

            layoutSend.setVisibility(GONE);
            imageViewStop.setVisibility(GONE);

            imageViewAttachment.setVisibility(VISIBLE);
            editTextMessage.setVisibility(VISIBLE);
            imageViewAudio.setVisibility(VISIBLE);
            imageViewAudio.animate().scaleX(1f).scaleY(1f).setDuration(100).start();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    timeText.setVisibility(GONE);
                    imageViewMic.setVisibility(GONE);
                    wave.setVisibility(GONE);
                    play.setVisibility(GONE);
                    cancel.setVisibility(GONE);
                    imageViewDelete.setVisibility(GONE);

                }
            }, 100);

            if (timerTask != null) {
                timerTask.cancel();
            }
            timerTask = null;


            if (recordingListener != null) {
                recordingListener.onRecordingCompleted();
                recordingListener.onSendClick();
            }
        }

    }

    private void startRecord() {
        isRecording = true;
        Log.e("AAAA", "START");
        if (recordingListener != null)
            recordingListener.onRecordingStarted();


        stopTrackingAction = false;

        imageViewAttachment.setVisibility(INVISIBLE);
        imageViewDelete.setVisibility(GONE);
        editTextMessage.setVisibility(GONE);
        imageViewAudio.animate().scaleX(2f).scaleY(2f).setDuration(100).setInterpolator(new OvershootInterpolator()).start();
        timeText.setVisibility(View.VISIBLE);
        layoutLock.setVisibility(View.VISIBLE);
        layoutSlideCancel.setVisibility(View.VISIBLE);
        imageViewMic.setVisibility(View.VISIBLE);
        timeText.animate().translationX(0).setDuration(100).start();
        imageViewMic.animate().translationX(0).setDuration(100).start();
        wave.animate().translationX(0).setDuration(100).start();
        play.animate().translationX(0).setDuration(100).start();
        imageViewDelete.animate().translationX(0).setDuration(100).start();
        layoutSend.animate().scaleY(1f).scaleX(1f).setDuration(100).start();
        cancel.animate().translationX(0).setDuration(100).start();
        timeText.startAnimation(animBlink);
        imageViewLockArrow.clearAnimation();
        imageViewLock.clearAnimation();
        imageViewLockArrow.startAnimation(animJumpFast);
        imageViewLock.startAnimation(animJump);
        imageViewLockHandler.startAnimation(animJumpSlow);


        if (audioTimer == null) {
            audioTimer = new Timer();
            timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        timeText.setText(timeFormatter.format(new Date(audioTotalTime * 1000)));
                        audioTotalTime++;
                    }
                });
            }
        };

        audioTotalTime = 0;
        audioTimer.schedule(timerTask, 0, 1000);
    }

    public void delete() {
        isDeleting = true;
        imageViewAudio.setEnabled(false);

        timeText.animate().translationX(1000).setDuration(100).start();
        imageViewMic.animate().translationX(1000).setDuration(100).start();
        wave.animate().translationX(1000).setDuration(100).start();
        play.animate().translationX(1000).setDuration(100).start();
        imageViewDelete.animate().translationX(1000).setDuration(100).start();
        cancel.animate().translationX(1000).setDuration(100).start();

        dustin.setVisibility(GONE);
        dustin_cover.setVisibility(GONE);

        layoutSend.setVisibility(GONE);
        imageViewStop.setVisibility(GONE);
        imageViewAudio.setVisibility(VISIBLE);
        imageViewAudio.animate().scaleX(1f).scaleY(1f).setDuration(100).start();


        imageViewAttachment.setVisibility(VISIBLE);
        editTextMessage.setVisibility(VISIBLE);
        imageViewAudio.setVisibility(VISIBLE);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isDeleting = false;
                imageViewAudio.setEnabled(true);
                timeText.setVisibility(GONE);
                imageViewMic.setVisibility(GONE);
                wave.setVisibility(GONE);
                play.setVisibility(GONE);
                cancel.setVisibility(GONE);
                imageViewDelete.setVisibility(GONE);

            }
        }, 100);
    }

    public void setWaveRawData(byte[] raw){
        wave.setRawData(raw);
    }
}
