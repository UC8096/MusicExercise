package com.example.music;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;
    private MusicController controller;

    public boolean musicBound = false;

    public MusicService musicSrv;
    private boolean paused = false;
    private Intent playIntent;
    private boolean playbackPaused = false;

    public ArrayList<Song> songList;
    private ListView songView;


    private ServiceConnection musicConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainActivity.this.musicSrv = ((MusicService.MusicBinder) service).getService();
            MainActivity.this.musicSrv.setSongs(MainActivity.this.songList);
            MainActivity.this.musicBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            MainActivity.this.musicBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setContentView((int) R.layout.activity_main);
        if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            this.songView = (ListView) findViewById(R.id.song_list);
            this.songList = new ArrayList<>();
            getSongList();
            Collections.sort(this.songList, new Comparator<Song>() {
                public int compare(Song a, Song b) {
                    return a.getTitle().compareTo(b.getTitle());
                }
            });
            this.songView.setAdapter(new SongAdapter(this, this.songList));
            setController();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_end) {
            stopService(this.playIntent);
            this.musicSrv = null;
            System.exit(0);
        } else if (itemId == R.id.action_shuffle) {
            this.musicSrv.setShuffle();
        }
        return super.onOptionsItemSelected(item);
    }

    public void getSongList() {
        Cursor musicCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex("title");
            int idColumn = musicCursor.getColumnIndex("_id");
            int artistColumn = musicCursor.getColumnIndex("artist");
            do {
                this.songList.add(new Song(musicCursor.getLong(idColumn), musicCursor.getString(titleColumn), musicCursor.getString(artistColumn)));
            } while (musicCursor.moveToNext());
        }
    }

    @Override
    public void start() {
        this.musicSrv.go();
    }

    @Override
    public void pause() {
        this.playbackPaused = true;
        this.musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        MusicService musicService = this.musicSrv;
        if (musicService == null || !this.musicBound || !musicService.isPng()) {
            return 0;
        }
        return this.musicSrv.getDur();
    }

    @Override
    public int getCurrentPosition() {
        MusicService musicService = this.musicSrv;
        if (musicService == null || !this.musicBound || !musicService.isPng()) {
            return 0;
        }
        return this.musicSrv.getPosn();
    }

    @Override
    public void seekTo(int pos) {
        this.musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        MusicService musicService = this.musicSrv;
        if (musicService == null || !this.musicBound) {
            return false;
        }
        return musicService.isPng();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public boolean checkPermissionREAD_EXTERNAL_STORAGE(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        String str = "android.permission.READ_EXTERNAL_STORAGE";
        if (ContextCompat.checkSelfPermission(context, str) == 0) {
            return true;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, str)) {
            showDialog("External storage", context, str);
        } else {
            ActivityCompat.requestPermissions((Activity) context, new String[]{str}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
        return false;
    }

    public void showDialog(String msg, final Context context, final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        sb.append(" permission is necessary");
        alertBuilder.setMessage(sb.toString());

        alertBuilder.setPositiveButton(getCurrentPosition(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{permission}, MainActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        });

        alertBuilder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 123) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        } else if (grantResults[0] != 0) {
            Toast.makeText(getApplicationContext(), "GET_ACCOUNTS Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.playIntent == null) {
            this.playIntent = new Intent(this, MusicService.class);
            bindService(this.playIntent, this.musicConnection, 0);
            startService(this.playIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.paused) {
            setController();
            this.paused = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.controller.hide();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(this.playIntent);
        this.musicSrv = null;
    }

    public void songPicked(View view) {
        this.musicSrv.setSongPosn(Integer.parseInt(view.getTag().toString()));
        this.musicSrv.playSong();
        if (this.playbackPaused) {
            setController();
            this.playbackPaused = false;
        }
        this.controller.show(0);
    }

    public void setController() {
        this.controller = new MusicController(this);
        this.controller.setPrevNextListeners(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.playNext();
            }
        }, new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.playPrev();
            }
        });
        this.controller.setMediaPlayer(this);
        this.controller.setAnchorView(findViewById(R.id.song_list));
        this.controller.setEnabled(true);
    }

    public void playNext() {
        this.musicSrv.playNext();
        if (this.playbackPaused) {
            setController();
            this.playbackPaused = false;
        }
        this.controller.show(0);
    }

    public void playPrev() {
        this.musicSrv.playPrev();
        if (this.playbackPaused) {
            setController();
            this.playbackPaused = false;
        }
        this.controller.show(0);
    }
}
