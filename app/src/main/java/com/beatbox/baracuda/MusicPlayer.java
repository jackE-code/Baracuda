package com.beatbox.baracuda;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.widget.SeekBar;

import java.io.IOException;

public class MusicPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, ServiceConnection {

    private static MusicPlayer instance;
    private Context context;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private Handler handler;
    private Runnable updateSeekBar;

    private MusicPlayerService musicPlayerService;
    private boolean isBound = false;

    public void bindService(Context context) {
        Intent intent = new Intent(context, MusicPlayerService.class);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    public void unbindService(Context context) {
        if (isBound) {
            context.unbindService(this);
            isBound = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
        musicPlayerService = binder.getService();
        musicPlayerService.setOnPreparedListener(this);
        musicPlayerService.setOnCompletionListener(this);
        isBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicPlayerService = null;
        isBound = false;
    }

    private MusicPlayer(Context context) {
        this.context = context;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        handler = new Handler();
    }

    public static MusicPlayer getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayer(context.getApplicationContext());
        }
        return instance;
    }

    public void setSeekBar(SeekBar seekBar) {
        this.seekBar = seekBar;
    }

    public void playMusic(String musicFilePath) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        try {
            mediaPlayer.setDataSource(musicFilePath);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            seekBar.setProgress(0);
        }
    }

    public void pauseMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void seekToPosition(int position) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(position);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        if (seekBar != null) {
            seekBar.setMax(mp.getDuration());
            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer.isPlaying()) {
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.postDelayed(updateSeekBar, 1000);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (seekBar != null) {
            seekBar.setProgress(0);
        }
    }
}
