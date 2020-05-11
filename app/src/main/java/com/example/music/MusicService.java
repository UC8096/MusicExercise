package com.example.music;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final int NOTIFY_ID = 1;
    private final IBinder musicBind = new MusicBinder();
    private MediaPlayer player;
    private Random rand;
    private boolean shuffle = false;
    private int songPosn;
    private String songTitle = BuildConfig.FLAVOR;
    private ArrayList<Song> songs;


    public class MusicBinder extends Binder {
        public MusicBinder() {

        }

        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.songPosn = 0;
        this.rand = new Random();
        this.player = new MediaPlayer();
        initMusicPlayer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.player.stop();
        this.player.release();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
//        Intent notIntent = new Intent(this, MainActivity.class);
//        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        Notification.Builder builder = new Notification.Builder(this);
//        builder.setContentIntent(pendInt).setSmallIcon(R.drawable.play).setTicker(this.songTitle).setOngoing(true).setContentTitle("Playing").setContentText(this.songTitle);
//        startForeground(1, builder.build());
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.v("MUSIC PLAYER", "Playback Error");
        mp.reset();
        return false;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (this.player.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }
    }

    public void initMusicPlayer() {
        this.player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        this.player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        this.player.setOnPreparedListener(this);
        this.player.setOnCompletionListener(this);
        this.player.setOnErrorListener(this);
    }

    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public void setSongPosn(int songPosn) {
        this.songPosn = songPosn;
    }

    public void playSong() {
        this.player.reset();
        Song playSong = (Song) this.songs.get(this.songPosn);
        long currSong = playSong.getID();
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        try {
            this.player.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        this.player.prepareAsync();
    }

    public void playNext() {
        if (this.shuffle) {
            int newSong = this.songPosn;
            while (newSong == this.songPosn) {
                newSong = this.rand.nextInt(this.songs.size());
            }
            this.songPosn = newSong;
        } else {
            this.songPosn++;
            if (this.songPosn >= this.songs.size()) {
                this.songPosn = 0;
            }
        }
        playSong();
    }

    public void playPrev() {
        this.songPosn--;
        if (this.songPosn < 0) {
            this.songPosn = this.songs.size() - 1;
        }
        playSong();
    }

    public int getPosn() {
        return this.player.getCurrentPosition();
    }

    public int getDur() {
        return this.player.getDuration();
    }

    public boolean isPng() {
        return this.player.isPlaying();
    }

    public void go() {
        this.player.start();
    }

    public void pausePlayer() {
        this.player.pause();
    }

    public void seek(int posn) {
        this.player.seekTo(posn);
    }

    public void setShuffle() {
        if (this.shuffle) {
            this.shuffle = false;
        } else {
            this.shuffle = true;
        }
    }
}
