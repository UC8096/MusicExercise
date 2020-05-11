package com.example.music;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {

    private LayoutInflater songInf;
    private ArrayList<Song> songs;

    public SongAdapter(Context c, ArrayList<Song> theSongs) {
        this.songs = theSongs;
        this.songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return this.songs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout songLay = (LinearLayout) this.songInf.inflate(R.layout.song, parent, false);
        TextView artistView = (TextView) songLay.findViewById(R.id.song_artist);
        Song currSong = (Song) this.songs.get(position);
        ((TextView) songLay.findViewById(R.id.song_title)).setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        songLay.setTag(Integer.valueOf(position));
        return songLay;
    }
}
