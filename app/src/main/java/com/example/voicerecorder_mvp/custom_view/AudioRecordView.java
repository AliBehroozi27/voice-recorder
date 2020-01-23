package com.example.voicerecorder_mvp.custom_view;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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

    private static final int ANIMATION_DURATION = 200;
    private static final float LOCK_LAYOUT_TRANSLATION_Y = 200;
    private static int WIDTH;
    private float normalScaleY;
    private ConstraintLayout.LayoutParams arrowParams;
    private int ARROW_TOP_MARGIN;
    private boolean isCanceling;

    public void setRecordingBehaviour(RecordingBehaviour recordingBehaviour) {
        this.recordingBehaviour = recordingBehaviour;
    }


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
    private View imageViewAttachment;
    private View layoutSlideCancel, layoutLock;
    private EditText editTextMessage;
    private TextView timeText, cancel;
    private AudioWaveView wave;
    private CardView cardViewTheme;


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

        View view = inflate(getContext(), R.layout.recording_layout, null);
        addView(view);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        WIDTH = displayMetrics.widthPixels;

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
        timeText = view.findViewById(R.id.textViewTime);
        layoutSlideCancel = view.findViewById(R.id.layoutSlideCancel);
        layoutLock = view.findViewById(R.id.layoutLock);
        imageViewMic = view.findViewById(R.id.imageViewMic);

        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics());

        animBlink = AnimationUtils.loadAnimation(getContext(),
                R.anim.blink);
        animJump = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump);
        animJumpFast = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump_fast);
        animJumpSlow = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump_slow);

        timeText.animate().translationX(WIDTH).start();
        imageViewMic.animate().translationX(WIDTH).start();
        wave.animate().translationX(WIDTH).start();
        play.animate().translationX(WIDTH).start();
        imageViewDelete.animate().translationX(WIDTH).start();
        cancel.animate().translationX(WIDTH).start();
        layoutLock.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
        imageViewLockArrow.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
        imageViewLock.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
        imageViewLockHandler.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
        imageViewStop.setScaleY(0f);
        imageViewStop.setScaleX(0f);

        handler = new Handler(Looper.getMainLooper());

        setupRecording();
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
                isLocked = false;
                recordingBehaviour = RecordingBehaviour.SEND;
                stopRecording();
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

        imageViewAudio.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isDeleting) {
                    return true;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.e("AAAAA" , "DOWN");
                    isCanceling = false;
                    cancelOffset = (float) (imageViewAudio.getX() / 2.8);
                    lockOffset = (float) (imageViewAudio.getX() / 4);

                    if (firstX == 0) {
                        firstX = motionEvent.getRawX();
                    }

                    if (firstY == 0) {
                        firstY = motionEvent.getRawY();
                    }
                    startRecord();

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
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP
                        || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    Log.e("AAAAA" , "UP");

                    if (motionEvent.getAction() == MotionEvent.ACTION_UP && !isLocked) {
                        if (!isCanceling) {
                            Log.e("AAA", "if  " + recordingBehaviour);
                            recordingBehaviour = RecordingBehaviour.RELEASED;
                            stopRecording();
                        } else {
                            Log.e("AAA", "else  " + recordingBehaviour);
                            isCanceling = false;
                            recordingBehaviour = null;
                        }
                    }

                }
                view.onTouchEvent(motionEvent);
                return true;
            }
        });

        layoutSend.animate().scaleX(0f).scaleY(0f).setDuration(ANIMATION_DURATION).start();
    }


    private void translateY(float y) {
        if (y < -lockOffset && userBehaviour != UserBehaviour.CANCELING) {
            locked();
            imageViewAudio.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
            layoutLock.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
            imageViewLockArrow.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
            imageViewLock.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
            imageViewLockHandler.setTranslationY(LOCK_LAYOUT_TRANSLATION_Y);
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
            isCanceling = true;
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
        Log.e("AAAAA" , "cancel()");
        stopTrackingAction = true;
        recordingBehaviour = RecordingBehaviour.CANCELED;
        stopRecording();
    }

    public void stopRecording() {
        Log.e("AAAAA", "STOP " + recordingBehaviour);
        stopTrackingAction = true;
        firstX = 0;
        firstY = 0;
        lastX = 0;
        lastY = 0;

        userBehaviour = UserBehaviour.NONE;

        layoutSlideCancel.setTranslationX(0);

        imageViewShadow.animate().scaleY(1f).scaleX(1f).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewAudio.animate().scaleX(1f).scaleY(1f).translationX(0).translationY(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

        layoutLock.animate().translationY(LOCK_LAYOUT_TRANSLATION_Y).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewLockArrow.animate().translationY(LOCK_LAYOUT_TRANSLATION_Y).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewLock.animate().translationY(LOCK_LAYOUT_TRANSLATION_Y).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewLockHandler.animate().translationY(LOCK_LAYOUT_TRANSLATION_Y).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

        layoutSlideCancel.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

        imageViewLockArrow.setAlpha(1f);
        imageViewLockArrow.clearAnimation();
        imageViewLock.clearAnimation();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                layoutSlideCancel.setVisibility(View.GONE);
                layoutLock.setVisibility(View.GONE);
            }
        }, 100);

        if (arrowParams != null) {
            arrowParams.setMargins(0, ARROW_TOP_MARGIN, 0, 0);
            imageViewLockArrow.setLayoutParams(arrowParams);
        }

        if (recordingBehaviour == RecordingBehaviour.LOCKED) {
            imageViewStop.setVisibility(VISIBLE);
            layoutSend.setVisibility(VISIBLE);
            cancel.setVisibility(VISIBLE);
            imageViewStop.animate().scaleY(1f).scaleX(1f).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
            layoutSend.animate().scaleY(2f).scaleX(2f).setInterpolator(new AccelerateInterpolator()).start();
            imageViewAudio.animate().scaleY(0f).scaleX(0f).setInterpolator(new AccelerateInterpolator()).start();

            if (recordingListener != null)
                recordingListener.onRecordingLocked();

        } else if (recordingBehaviour == RecordingBehaviour.CANCELED) {
            timeText.clearAnimation();
            imageViewStop.animate().scaleY(0f).scaleX(0f).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

            delete();

            timerTask.cancel();
            timerTask = null;

            recordingBehaviour = null;
            isLocked = false;

            if (recordingListener != null)
                recordingListener.onRecordingCanceled();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    imageViewStop.setVisibility(View.GONE);
                }
            }, 100);

        } else if (recordingBehaviour == RecordingBehaviour.RELEASED) {
            timeText.clearAnimation();
            timeText.setVisibility(GONE);
            imageViewMic.setVisibility(GONE);
            imageViewStop.setVisibility(GONE);
            imageViewAttachment.setVisibility(VISIBLE);
            editTextMessage.setVisibility(VISIBLE);

            timerTask.cancel();
            timerTask = null;


            if (recordingListener != null){
                recordingListener.onRecordingCompleted();
                recordingListener.onSendClick();
            }
        }else if (recordingBehaviour == RecordingBehaviour.LOCK_DONE) {
            timeText.clearAnimation();

            imageViewStop.animate().scaleX(0f).scaleY(0f).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
            imageViewMic.setVisibility(GONE);
            imageViewAttachment.setVisibility(GONE);

            wave.setVisibility(VISIBLE);
            play.setVisibility(VISIBLE);
            imageViewDelete.setVisibility(VISIBLE);
            layoutSend.setVisibility(VISIBLE);
            timeText.setVisibility(VISIBLE);
            layoutSend.animate().scaleX(1f).scaleY(1f).start();

            timerTask.cancel();
            timerTask = null;

            if (recordingListener != null)
                recordingListener.onRecordingCompleted();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    imageViewStop.setVisibility(GONE);
                }
            }, 100);


        } else if (recordingBehaviour == RecordingBehaviour.SEND) {
            timeText.clearAnimation();

            timeText.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
            imageViewMic.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
            wave.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
            play.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
            imageViewDelete.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
            cancel.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

            layoutSend.setVisibility(GONE);
            imageViewStop.setVisibility(GONE);

            imageViewAttachment.setVisibility(VISIBLE);
            editTextMessage.setVisibility(VISIBLE);
            imageViewAudio.setVisibility(VISIBLE);
            imageViewAudio.animate().scaleX(1f).scaleY(1f).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

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

            isLocked = false;

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
        Log.e("AAAAA", "START " + recordingBehaviour);
        if (recordingListener != null)
            recordingListener.onRecordingStarted();

        stopTrackingAction = false;

        timeText.setVisibility(View.VISIBLE);
        layoutLock.setVisibility(View.VISIBLE);
        layoutSlideCancel.setVisibility(View.VISIBLE);
        imageViewMic.setVisibility(View.VISIBLE);
        timeText.startAnimation(animBlink);
        imageViewLockArrow.startAnimation(animJumpFast);
        imageViewLock.startAnimation(animJump);
        imageViewLockHandler.startAnimation(animJumpSlow);

        timeText.animate().translationX(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewMic.animate().translationX(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        wave.animate().translationX(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        play.animate().translationX(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewDelete.animate().translationX(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        layoutSend.animate().scaleY(1f).scaleX(1f).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        cancel.animate().translationX(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        layoutSlideCancel.animate().translationX(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        layoutLock.animate().translationY(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewLockArrow.animate().translationY(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewLock.animate().translationY(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewLockHandler.animate().translationY(0).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

        imageViewAttachment.setVisibility(INVISIBLE);
        imageViewDelete.setVisibility(GONE);
        editTextMessage.setVisibility(GONE);
        imageViewAudio.animate().scaleX(2f).scaleY(2f).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).setInterpolator(new OvershootInterpolator()).start();

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

        timeText.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewMic.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        wave.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        play.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        imageViewDelete.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        cancel.animate().translationX(WIDTH).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

        layoutSend.setVisibility(GONE);
        imageViewStop.setVisibility(GONE);
        imageViewAudio.setVisibility(VISIBLE);
        imageViewAudio.animate().scaleX(1f).scaleY(1f).setDuration(ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();

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
}
