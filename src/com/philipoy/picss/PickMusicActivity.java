package com.philipoy.picss;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.philipoy.picss.model.DeezerSongEntry;
import com.philipoy.picss.model.Picss;
import com.philipoy.picss.profiles.Constants;
import com.philipoy.picss.views.PicssDialogFragment;
import com.philipoy.picss.views.PicssDialogFragment.PicssDialogListener;

/**
 * User can search for a sound or a song in popular sources (iTunes, Deezer, SoundCloud)
 * @author paristote
 *
 */
public class PickMusicActivity extends FragmentActivity implements PicssDialogListener  {
	
	/**
	 * Name of the sound to search
	 */
	private EditText name;
	/**
	 * The list of results
	 */
	private ListView songsList;
	/**
	 * Progress indicator of the search task
	 */
	private ProgressBar progress;
	/**
	 * Songs list adapter
	 */
	private SongListAdapter adapter;
	/**
	 *  Custom dialog to ask the name and label of the Picss
	 */
	private PicssDialogFragment dialog = null;
	/**
	 * The Picss object
	 */
	private Picss currentPicss = null;
	/**
	 * Media Player used when a song is played from the list
	 */
	private MediaPlayer player = null;
	/**
	 * Song currently playing or -1
	 */
	private int playingSong;
	/**
	 * Current in progress downloading task or null
	 */
	private DownloadPreviewTrackTask currentDownload = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pick_music);
		
		name = (EditText)findViewById(R.id.text_song_name);
		songsList = (ListView)findViewById(R.id.list_songs);
		progress = (ProgressBar)findViewById(R.id.progressBar);
		currentPicss = new Picss();
		// loads the content of the Picss already created in the Home Activity
		Bundle extras = getIntent().getExtras();
		if (extras.containsKey("picssPhoto") && extras.getByteArray("picssPhoto") != null) {
			Log.d(Constants.LOG, "Picss photo was transferred from Home Activity");
			currentPicss.photo = extras.getByteArray("picssPhoto");
		}
		
		// Start the SearchSongsTask when the user submits the search
		name.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		            SearchSongsTask tsk = new SearchSongsTask();
		            tsk.execute(v.getText().toString());
		        }
		        return false;
		    }
		});
		
		// Action to execute when the user selects a song
		songsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				DeezerSongEntry song = adapter.getItem(position);
				Log.d(Constants.LOG, "selected: "+song.songName);
				Log.d(Constants.LOG, "url : "+song.previewUrl);
				// stores the URL of the sound preview in the Picss object
				currentPicss.soundUrl = song.previewUrl;
				
				// show the dialog to let user enter a name and label for the Picss
				askPicssNameAndLabel();
			}
		});
		
		playingSong = -1;
		player = new MediaPlayer();
	}
	
	/**
	 * Handle taps on the pay / stop button of each item
	 * @param position The position of the song in the list
	 */
	public void startStopAudioPreview(int position) {
		
		if (position == playingSong) // song is already playing, we should stop it
		{
			stopSong();
		} 
		else  // song isn't playing, we download it and play it after
		{
			if (currentDownload != null) {
				// we must cancel any current download before starting a new one
				currentDownload.cancel(true);
			}
			// Download the song preview; TODO get from cache or internal storage
			currentDownload = new DownloadPreviewTrackTask(position);
			currentDownload.execute();
			// Hide the button and show the loading indicator
			adapter.setDownloadingSongPosition(position);
			adapter.notifyDataSetChanged();
		}
	}
	/**
	 * Stop the song currently playing, reset the player and update the view
	 */
	private void stopSong() {
		if (player != null) {
			if (player.isPlaying())
				player.stop(); // Stop the song currently playing
			player.reset(); // Reset the player 
			playingSong = -1;
			// Update the view
			adapter.cancelPlayingSongPosition();
			adapter.notifyDataSetChanged();
		}
	}
	/**
	 * Start the song at the given position and update the view
	 * @param position
	 */
	private void startSong(int position) {
		if (player != null) {
			if (player.isPlaying())
				stopSong(); // if another song is currently playing, we stop it first
			
			// start preparing the player
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					stopSong(); // stop the song and reset the player when the song playback is finished
				}
			});
			try {
				player.setDataSource(getFilesDir().getAbsolutePath()+"/tmp-"+position+".m4a");
				player.prepare();
				player.start(); // start playing the song
				playingSong = position;
				// update the view
				adapter.setPLayingSongPosition(position);
				adapter.notifyDataSetChanged();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Opens the dialog to ask the user a name and label for this Picss
	 */
	private void askPicssNameAndLabel() {
		if (dialog == null) { // can start the dialog only once at a time
			dialog = new PicssDialogFragment();
			dialog.show(getSupportFragmentManager(), "PicssDialogListener");
		}
	}
	
	/**
	 * Called from the dialog Send button
	 */
	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		// Get and store the name of the Picss
		EditText nameF = (EditText)dialog.getDialog().findViewById(R.id.picssName);
		String name = nameF.getText().toString();
		if (name != null && !name.trim().equals(""))
			currentPicss.name = name.trim();
		// Get and store the label of the Picss
		EditText labelF = (EditText)dialog.getDialog().findViewById(R.id.picssLabel);
		String label = labelF.getText().toString();
		if (label != null && !label.trim().equals(""))
			currentPicss.label = label.trim();
		
		// Send the Picss to server if it has all needed info
		// TODO do not send immediately, open the screen to recap all info instead
		if (currentPicss.isReady()) {
			SendPicssTask tsk = new SendPicssTask(this.getBaseContext());
	    	tsk.execute(currentPicss);
	    	Toast.makeText(PickMusicActivity.this, R.string.sending, Toast.LENGTH_SHORT).show();
	    } else {
	    	Log.e(Constants.LOG, "Cannot upload Picss to the server.");
	    	Log.e(Constants.LOG, currentPicss.toString());
	    	Toast.makeText(PickMusicActivity.this, R.string.sent_ko, Toast.LENGTH_LONG).show();
	    }
		// returns to the Home Activity
		finishPicss();
	}
	
	/**
	 * Returns to the Home activity
	 */
	private void finishPicss() {
		startActivity(new Intent(getBaseContext(), HomeActivity.class));
	}
	
	@Override
	protected void onPause() {
		// Stop current download before leaving the activity
		if (currentDownload != null) currentDownload.cancel(true);
		currentDownload = null;
		currentPicss = null;
		dialog = null;
		// Stop playing the song and release the player
		if (player != null) player.release();
		super.onPause();
	}

	/**
	 * Called from the dialog Cancel button
	 */
	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		// set to null to be able to reopen the dialog for another Picss
		this.dialog = null;
	}
	
	/**
	 * Execute the search
	 * @author paristote
	 */
	private class SearchSongsTask extends AsyncTask<String, Integer, List<DeezerSongEntry>> {
		
		@Override
		protected void onPreExecute() {
			// show the progress indicator
			progress.setVisibility(View.VISIBLE);
			// hide the song list
			songsList.setVisibility(View.INVISIBLE);
			super.onPreExecute();
		}

		@Override
		protected List<DeezerSongEntry> doInBackground(String... name) {
			// TODO allow search from different sources
//			iTunes			
//			String url1 = "https://itunes.apple.com/search?term=";
//			String url2 = "&media=music&entity=musicTrack";
			// Deezer
			String url1 = "http://api.deezer.com/search?q=";
			String url2 = "&order=RATING_DESC&nb_items=20";
			
			String url = url1;
			try {
				url = url + URLEncoder.encode(name[0], "UTF-8") + url2;
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			// the list of SongEntry
			List<DeezerSongEntry> result = null;

			try {
				// execute the HTTP request for the search 
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(url);
				HttpResponse resp = client.execute(get);
	        	
				// transform the response entity into a json document
        		String json = EntityUtils.toString(resp.getEntity());
        		// parse the json document returned by Deezer API
        		// TODO improve to be able to parse responses from different sources (Deezer, iTunes, etc)
        		JSONObject obj = new JSONObject(json);
        		JSONArray res = obj.getJSONArray("data");

        		int length = res.length();
        		result = new ArrayList<DeezerSongEntry>(length);
        		// add the song entry to the result list only if it has a preview URL
        		for (int i=0; i<length; i++) {
        			JSONObject songObj = res.getJSONObject(i);
        			if (songObj.has("preview")) {
        				int id = songObj.getInt("id");
	        			String song = songObj.getString("title");
	        			String artist = songObj.getJSONObject("artist").getString("name");
	        			String previewUrl = songObj.getString("preview");
	        			String albumCover = songObj.getJSONObject("album").getString("cover");
	        			result.add(new DeezerSongEntry(id, song, artist, previewUrl, albumCover));
        			}
        		}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			// return the list
			return result;
		}

		@Override
		protected void onPostExecute(List<DeezerSongEntry> result) {
			// updates the content of the list
			SongListAdapter adapt = new SongListAdapter(PickMusicActivity.this, result);
			adapter = adapt;
			songsList.setAdapter(adapt);
			// hide the progress indicator
			progress.setVisibility(View.INVISIBLE);
			// show the list
			songsList.setVisibility(View.VISIBLE);
			super.onPostExecute(result);
		}

	}
	
	/**
	 * Download and temporarily stores a preview song from its URL.
	 * When the task is cancelled, there is no post operation because  
	 *  either a new task will replace it and will update the views,
	 *  or the activity is being paused
	 * @author paristote
	 */
	private class DownloadPreviewTrackTask extends AsyncTask<Void, Void, Void> {
		/**
		 * Position of the song to download
		 */
		private int position;
		
		public DownloadPreviewTrackTask(int pos) {
			position = pos;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			currentDownload = null; // we don't need the reference anymore
			adapter.cancelDownloadingSongPosition(); // update the adapter to hide the progress indicator
			startSong(position); // automatically starts the song
			super.onPostExecute(v);
		}

		@Override
		protected Void doInBackground(Void... v) {
			HttpURLConnection conn = null;
			URL url;
			// get the song info and set a temp filename
			DeezerSongEntry song = adapter.getItem(position);
			String fileName = "tmp-"+position+".m4a";
			try {
				url = new URL(song.previewUrl); // get the URL of the song preview
				conn = (HttpURLConnection)url.openConnection();
				conn.setDoInput(true);
	        	conn.setRequestMethod("GET");
	        	if (conn.getResponseCode() > 400) {
	        		Log.e(Constants.LOG, "error " + conn.getResponseCode() + ": " + conn.getResponseMessage());
	        	} else if (!isCancelled()) {
		        	InputStream is = conn.getInputStream();
		            BufferedInputStream bis = new BufferedInputStream(is);
		            ByteArrayBuffer baf = new ByteArrayBuffer(50);
		            int current = 0;
		            while ((current = bis.read()) != -1 && !isCancelled()) { // read the file unless download is cancelled
		               baf.append((byte) current);
		            }
		            // convert the bytes read to the temp file unless the download is cancelled
		            if (!isCancelled()) {
			            FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
			            fos.write(baf.toByteArray());
			            fos.close();
		            }
	        	}
	            
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
