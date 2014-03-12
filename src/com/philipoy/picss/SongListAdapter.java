package com.philipoy.picss;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.philipoy.picss.model.DeezerSongEntry;

/**
 * Custom ArrayAdapter for PickMusicActivity
 * Data of type DeezerSongEntry
 * @author paristote
 */
public class SongListAdapter extends ArrayAdapter<DeezerSongEntry> {

	private static LayoutInflater inflater = null;
	/**
	 * Position of the song being played or -1
	 * When different than -1 and the item is displayed, the right button will be displayed with a stop icon
	 */
	private int playingSongPosition;
	/**
	 * Position of the song being downloaded or -1
	 * When different than -1 and the item is displayed, a progress indicator will be displayed instead of the ImageButton
	 */
	private int downloadingSongPosition;
	/**
	 * Size of the list of songs
	 */
	private int listSize;
	/**
	 * Parent activity
	 */
	private PickMusicActivity activity;
	
	public SongListAdapter(PickMusicActivity act, List<DeezerSongEntry> list) {
		super(act, R.layout.song_list_item, list);
		inflater = (LayoutInflater)act.getLayoutInflater();
		playingSongPosition = -1;
		downloadingSongPosition = -1;
		listSize = list.size();
		activity = act;
	}
	
	/**
	 * Set the variable playingSongPosition to the given position
	 * @param position
	 */
	public void setPLayingSongPosition(int position) {
		if (position >= 0 && position < listSize)
			playingSongPosition = position;
	}
	
	/**
	 * Set playingSongPosition back to -1 => no song is playing
	 */
	public void cancelPlayingSongPosition() {
		playingSongPosition = -1;
	}
	
	/**
	 * Set the variable downloadingSongPosition to the given position
	 * @param position
	 */
	public void setDownloadingSongPosition(int position) {
		if (position >= 0 && position < listSize)
			downloadingSongPosition = position;
	}
	
	/**
	 * Set downloadingSongPosition back to -1 => no song is being downloaded
	 */
	public void cancelDownloadingSongPosition() {
		downloadingSongPosition = -1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		// Using ViewHolder based on http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.song_list_item, null);
			holder = new ViewHolder();
			holder.cover = (ImageView)convertView.findViewById(R.id.imgAlbumCover);
			holder.title = (TextView)convertView.findViewById(R.id.songTitle);
			holder.artist = (TextView)convertView.findViewById(R.id.songArtist);
			holder.playStopBtn = (ImageButton)convertView.findViewById(R.id.btnPlayLoadStop);
			holder.downloadProgress = (ProgressBar)convertView.findViewById(R.id.progressDownload);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}
		// listen to taps on the play/stop button
		holder.playStopBtn.setOnClickListener(new StartStopClickListener(position));
		
		// set progress or play/stop button for the current item, depending on the state
		if (position == downloadingSongPosition) {
			// downloading song => hide button and show progress indicator
			holder.downloadProgress.setVisibility(View.VISIBLE);
			holder.playStopBtn.setVisibility(View.INVISIBLE);
		} else if (position == playingSongPosition) {
			// playing song => sho stop button and hide progress indicator
			holder.playStopBtn.setImageResource(R.drawable.icon_stop);
			holder.downloadProgress.setVisibility(View.INVISIBLE);
			holder.playStopBtn.setVisibility(View.VISIBLE);
		} else {
			// nothing => show play button and hide progress indicator
			holder.playStopBtn.setImageResource(R.drawable.icon_play);
			holder.downloadProgress.setVisibility(View.INVISIBLE);
			holder.playStopBtn.setVisibility(View.VISIBLE);
		}
		
		// set song cover and information via the view holder
		DeezerSongEntry song = getItem(position);
		UrlImageViewHelper.setUrlDrawable(holder.cover, song.coverUrl+"?size=medium", R.drawable.icon_about, 60000);
		holder.title.setText(song.songName);
		holder.artist.setText(song.artistName);
		
		return convertView;
	}
	
	/**
	 * Holds subviews of each item view to avoid too many calls to findViewById()
	 * @author paristote
	 */
	private static class ViewHolder {
		public ImageView cover;
		public TextView title;
		public TextView artist;
		public ImageButton playStopBtn;
		public ProgressBar downloadProgress;
	}
	
	/**
	 * Calls PickMusicActivity.startStopAudioPreview() with the selected song position
	 * @author paristote
	 */
	private class StartStopClickListener implements View.OnClickListener {

		private int position;

		public StartStopClickListener(int pos) {
			position = pos;
		}
		
		@Override
		public void onClick(View v) {
			activity.startStopAudioPreview(position);
		}
	}
}
