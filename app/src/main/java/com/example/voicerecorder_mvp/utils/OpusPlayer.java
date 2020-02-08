package com.example.voicerecorder_mvp.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.annotation.StyleableRes;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import top.oply.opuslib.OpusEvent;
import top.oply.opuslib.OpusTool;
import top.oply.opuslib.Utils;

/**
 * Created by young on 2015/7/2.
 */
public class OpusPlayer {
    
    public static final int SAMPLE_RATE_HRTZ = 48000;
    private static final int BUFFER_SIZE = 1000;
    
    private OpusPlayer(){
    }
    private static volatile OpusPlayer oPlayer ;
    public static OpusPlayer getInstance(){
        if(oPlayer == null)
            synchronized(OpusPlayer.class){
                if(oPlayer == null)
                    oPlayer = new OpusPlayer();
            }
        return oPlayer;
    }
    
    private OpusTool opusLib = new OpusTool();
    private static final String TAG = OpusPlayer.class.getName();
    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_PAUSED = 2;
    
    private volatile int state = STATE_NONE;
    private Lock libLock = new ReentrantLock();
    private AudioTrack audioTrack;
    private static final int minBufferSize = 1024 * 8 * 8;
    int bufferSize = 0;
    private int channel = 0;
    
    private long lastNotificationTime = 0;
    private String currentFileName = "";
    
    private volatile Thread playTread = new Thread();
    private OpusEvent mEventSender = null;
    
    public void setEventSender(OpusEvent es) {
        mEventSender = es;
    }
    
    class PlayThread implements Runnable {
        public void run() {
            Log.e("AAA" , "run");
            readAudioDataFromFile();
        }
    }
    
    public void setFile(String fileName) {
        //if already playing, stop current playback
        if (state != STATE_NONE) {
            stop();
        }
        state = STATE_NONE;
        currentFileName = fileName;
        if(!Utils.isFileExist(currentFileName) || opusLib.isOpusFile(currentFileName) == 0) {
            Log.e(TAG, "File does not exist, or it is not an opus file!");
            if(mEventSender != null)
                mEventSender.sendEvent(OpusEvent.PLAYING_FAILED);
            return;
        }
        libLock.lock();
        int res = opusLib.openOpusFile(currentFileName);
        libLock.unlock();
        if (res == 0) {
            Log.e(TAG, "Open opus file error!");
            if(mEventSender != null)
                mEventSender.sendEvent(OpusEvent.PLAYING_FAILED);
            return;
        }
    }
    
    public void play(String fileName) {
        //if already playing, stop current playback
        if (state != STATE_NONE) {
            stop();
        }
        state = STATE_NONE;
        currentFileName = fileName;
        if(!Utils.isFileExist(currentFileName) || opusLib.isOpusFile(currentFileName) == 0) {
            Log.e(TAG, "File does not exist, or it is not an opus file!");
            if(mEventSender != null)
                mEventSender.sendEvent(OpusEvent.PLAYING_FAILED);
            return;
        }
        libLock.lock();
        int res = opusLib.openOpusFile(currentFileName);
        libLock.unlock();
        if (res == 0) {
            Log.e(TAG, "Open opus file error!");
            if(mEventSender != null)
                mEventSender.sendEvent(OpusEvent.PLAYING_FAILED);
            return;
        }
        try {
            channel = opusLib.getChannelCount();
            int trackChannel = 0;
            if (channel == 1)
                trackChannel = AudioFormat.CHANNEL_OUT_MONO;
            else
                trackChannel = AudioFormat.CHANNEL_OUT_STEREO;
            
//            bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_HRTZ, trackChannel, AudioFormat.ENCODING_PCM_16BIT);
//            bufferSize = (bufferSize > minBufferSize)? bufferSize : minBufferSize;
            bufferSize = BUFFER_SIZE;
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE_HRTZ, trackChannel, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
            
            audioTrack.play();
        } catch (Exception e) {
            Utils.printE(TAG, e);
            destroyPlayer();
            return;
        }
        
        state = STATE_STARTED;
        playTread = new Thread( new OpusPlayer.PlayThread(),"OpusPlay Thrd");
        playTread.start();
        
        if(mEventSender != null)
            mEventSender.sendEvent(OpusEvent.PLAYING_STARTED);
    }
    
    protected void readAudioDataFromFile() {
        if (state != STATE_STARTED) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        boolean isFinished = false;
        while (state != STATE_NONE) {
            if (state == STATE_PAUSED){
    
                try {
                    Thread.sleep(10);
                    continue;
                }
                catch (Exception e) {
                    Log.e(TAG, e.toString());
                    continue;
                }
                
            }
            else  if (state == STATE_STARTED) {
                libLock.lock();
                opusLib.readOpusFile(buffer, bufferSize);
                int size = opusLib.getSize();
                libLock.unlock();
                if (size != 0) {
                    buffer.rewind();
                    byte[] data = new byte[size];
                    buffer.get(data);
    
                    audioTrack.write(data, 0, size);
                }
                
                notifyProgress();
                isFinished = opusLib.getFinished() != 0;
                if (isFinished) {
                    break;
                }
            }
        }
        if (state != STATE_NONE)
            state = STATE_NONE;
        if(mEventSender != null)
            mEventSender.sendEvent(OpusEvent.PLAYING_FINISHED);
    }
    
    public void pause() {
        if (state == STATE_STARTED) {
            audioTrack.pause();
            state = STATE_PAUSED;
            if(mEventSender != null)
                mEventSender.sendEvent(OpusEvent.PLAYING_PAUSED);
        }
        notifyProgress();
    }
    
    public void resume() {
        if (state == STATE_PAUSED) {
            audioTrack.play();
            state = STATE_STARTED;
            if(mEventSender != null)
                mEventSender.sendEvent(OpusEvent.PLAYING_STARTED);
        }
    }
    
    public void stop() {
        state = STATE_NONE;
        while (true) {
            try {
                Thread.sleep(20);
            }
            catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            
            if(!playTread.isAlive())
                break;
        }
        Thread.yield();
        destroyPlayer();
    }
    
    public String toggle(String fileName) {
        if (state == STATE_PAUSED && currentFileName.equals(fileName)) {
            resume();
            return "Pause";
        } else if (state == STATE_STARTED && currentFileName.equals(fileName)) {
            pause();
            return "Resume";
        } else {
            play(fileName);
            return "Pause";
        }
    }
    
    
    /**
     * Get duration, whose unit is second
     * @return duration
     */
    public long getDuration() {
        return opusLib.getTotalPcmDuration();
    }
    
    public int getDuration(String path) {
        setFile(path);
        return (int)opusLib.getTotalPcmDuration();
    }
    
    
    public long getPosition() {
        return opusLib.getPcmOffset();
    }
    
    public void seekOpusFile(float scale) {
        if (state == STATE_PAUSED || state == STATE_STARTED) {
            libLock.lock();
            int r = opusLib.seekOpusFile(scale);
            Log.e("AAA" , " is seekinggg " + r);
            libLock.unlock();
        }
    }
    
    private void notifyProgress() {
        //notify every 1 second
        if(System.currentTimeMillis() - lastNotificationTime >= 1000) {
            if(mEventSender != null)
                mEventSender.sendProgressEvent(getPosition(), getDuration());
        }
    }
    
    private void destroyPlayer() {
        
        libLock.lock();
        opusLib.closeOpusFile();
        libLock.unlock();
        try {
            if (audioTrack != null ) {
                audioTrack.pause();
                audioTrack.flush();
                audioTrack.release();
                audioTrack = null;
            }
        } catch (Exception e) {
            Utils.printE(TAG, e);
        }
    }
    
    public boolean isWorking() {
        return state != STATE_NONE;
    }
    public void release() {
        if(state != STATE_NONE)
            stop();
    }
}
