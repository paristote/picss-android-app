package com.philipoy.picss.model;

/**
 * Song Entry from Deezer
 * @author paristote
 *
 */
public class DeezerSongEntry {

	public DeezerSongEntry(int i, String song, String artist, String track, String albumCoverUrl) {
		id = i;
		songName = song;
		artistName = artist;
		previewUrl = track;
		coverUrl = albumCoverUrl;
	}
	
	public int id;
	public String songName;
	public String artistName;
	public String previewUrl;
	public String coverUrl;
	
	public String toString() { return songName+" ("+artistName+")"; }
	
}
