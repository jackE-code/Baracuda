package com.beatbox.baracuda;

import android.os.Handler;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.widget.AppCompatImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MusicPlayerLayout extends AppCompatActivity {

    private MusicPlayer musicPlayer;
    private SeekBar seekBar;
    private AppCompatImageButton playButton;
    private TextView musicTitleTextView;

    private boolean isServiceBound = false;

    private Handler handler = new Handler();


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player_layout);

        musicPlayer = MusicPlayer.getInstance(this);

        seekBar = findViewById(R.id.seekBar);
        playButton = findViewById(R.id.playPauseButton);
        musicTitleTextView = findViewById(R.id.musicTitleTextView);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.stopMusic();
                    playButton.setBackgroundResource(R.drawable.play);
                } else {
                    String musicFilePath = getIntent().getStringExtra("musicFilePath");
                    if (musicFilePath != null) {
                        musicPlayer.playMusic(musicFilePath);
                        playButton.setBackgroundResource(R.drawable.pause);
                    }
                }
            }
        });

        musicPlayer.setSeekBar(seekBar);

        // Retrieve the music title from intent extras
        String musicTitle = getIntent().getStringExtra("musicTitle");
        if (musicTitle != null) {
            musicTitleTextView.setText(musicTitle);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        musicPlayer.bindService(this);
        isServiceBound = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            musicPlayer.unbindService(this);
            isServiceBound = false;
        }
    }

    // ...

    private void updateSeekBar() {
        if (isServiceBound && musicPlayer.isPlaying()) {
            int progress = musicPlayer.getCurrentPosition();
            seekBar.setProgress(progress);
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        musicPlayer.stopMusic();
    }
}
