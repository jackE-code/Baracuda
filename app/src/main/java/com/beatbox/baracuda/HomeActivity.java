package com.beatbox.baracuda;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements TextWatcher {

    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private ListView musicListView;
    private ArrayList<String> musicList;
    private ArrayList<String> filteredMusicList;
    private ArrayAdapter<String> musicListAdapter;


    private MusicPlayerService musicPlayerService;

    private boolean isServiceBound = false;




    // Create a ServiceConnection instance
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicPlayerService = null;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        musicListView = findViewById(R.id.musicListView);
        musicList = new ArrayList<>();
        filteredMusicList = new ArrayList<>();
        musicListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredMusicList);
        musicListView.setAdapter(musicListAdapter);

        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String musicTitle = filteredMusicList.get(position);
                String musicFilePath = getMusicFilePath(musicTitle);

                if (musicPlayerService != null) {
                    musicPlayerService.playMusic(musicFilePath);
                }

                // Create an intent to open the MusicPlayerLayout activity
                Intent intent = new Intent(HomeActivity.this, MusicPlayerLayout.class);
                intent.putExtra("musicTitle", musicTitle);
                startActivity(intent);
            }
        });
        EditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(this);

        if (checkPermission()) {
            loadMusicList();
            bindMusicPlayerService();
        } else {
            requestPermission();
        }
    }

    // Bind to the MusicPlayerService
    // Bind to the MusicPlayerService
    private void bindMusicPlayerService() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    // Unbind from the MusicPlayerService
    private void unbindMusicPlayerService() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }



    // Create a ServiceConnection instance
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusicPlayerService();  // Stop the service if needed
        unbindMusicPlayerService();
    }


    // Method to start the MusicPlayerService with the selected music file path
    private void startMusicPlayerService(String musicFilePath) {
        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        serviceIntent.putExtra("musicFilePath", musicFilePath);
        startService(serviceIntent);
    }

    // Method to stop the MusicPlayerService
    private void stopMusicPlayerService() {
        Intent stopIntent = new Intent(this, MusicPlayerService.class);
        stopIntent.setAction("STOP");
        stopService(stopIntent);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_READ_EXTERNAL_STORAGE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicList();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadMusicList() {
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        Cursor cursor = getContentResolver().query(musicUri, null, selection, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int filePathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            do {
                String title = cursor.getString(titleColumn);
                String filePath = cursor.getString(filePathColumn);
                musicList.add(title);
                // Store the file path instead of the title if you want to play the music later
                // musicList.add(filePath);
            } while (cursor.moveToNext());
            cursor.close();
            musicListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String query = s.toString().trim().toLowerCase(Locale.getDefault());
        performSearch(query);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void performSearch(String query) {
        filteredMusicList.clear();
        for (String music : musicList) {
            if (music.toLowerCase(Locale.getDefault()).startsWith(query)) {
                filteredMusicList.add(music);
            }
        }
        musicListAdapter.notifyDataSetChanged();
    }

    private String getMusicFilePath(String musicTitle) {
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.TITLE + "=?";
        String[] selectionArgs = { musicTitle };
        Cursor cursor = getContentResolver().query(musicUri, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            int filePathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            String filePath = cursor.getString(filePathColumn);
            cursor.close();
            return filePath;
        }
        return null;
    }
}
