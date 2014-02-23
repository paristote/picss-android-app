package com.philipoy.picss;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

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
	 * Progress indicator
	 */
	private ProgressBar progress;
	/**
	 * The songs returned from the search
	 */
	private SongEntry[] currentSongs;
	/**
	 *  custom dialog to ask the name and label of the Picss
	 */
	private PicssDialogFragment dialog = null;
	/**
	 * The Picss object
	 */
	private Picss currentPicss = null;


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
		
		// set the action to execute when the user submits the search 
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
		// set the action to execute when the user selects an item in the list of songs
		songsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				SongEntry song = currentSongs[position];
				Log.d(Constants.LOG, "selected: "+song.songName);
				Log.d(Constants.LOG, "url : "+song.previewUrl);
				// stores the URL of the sound preview in the Picss object
				currentPicss.soundUrl = song.previewUrl;
				
//				DownloadPreviewTrackTask tsk = new DownloadPreviewTrackTask();
//				tsk.execute(song.previewUrl);
				
				// show the dialog to let user enter a name and label for the Picss
				askPicssNameAndLabel();
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pick_music, menu);
		return true;
	}
	
	/*
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
		
		// Call the async task if the Picss has all needed info
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
	
	/*
	 * Returns to the Home activity
	 */
	private void finishPicss() {
		startActivity(new Intent(getBaseContext(), HomeActivity.class));
	}
	
	@Override
	protected void onPause() {
		currentPicss = null;
		dialog = null;
		
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
	 * Model of Song Entry in the list
	 * @author paristote
	 *
	 */
	private static class SongEntry {
		public SongEntry(String song, String artist, String track) {
			songName = song;
			artistName = artist;
			previewUrl = track;
		}
		public String songName;
		public String artistName;
		public String previewUrl;
		public String toString() { return songName+" ("+artistName+")"; }
	}
	
	/**
	 * Execute the search
	 * @author paristote
	 *
	 */
	private class SearchSongsTask extends AsyncTask<String, Integer, SongEntry[]> {
		
		@Override
		protected void onPreExecute() {
			// show the progress indicator
			progress.setVisibility(View.VISIBLE);
			// hide the song list
			songsList.setVisibility(View.INVISIBLE);
			super.onPreExecute();
		}

		@Override
		protected SongEntry[] doInBackground(String... name) {
			// iTunes			
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
			List<SongEntry> result = null;

			try {
				// execute the HTTP request for the search 
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(url);
				HttpResponse resp = client.execute(get);
	        	
				// transform the response entity into a json document
        		String json = EntityUtils.toString(resp.getEntity());
        		// parse the json document
        		JSONObject obj = new JSONObject(json);
        		JSONArray res = obj.getJSONArray("data");

        		int length = res.length();
        		result = new ArrayList<PickMusicActivity.SongEntry>(length);
        		// add the song entry to the result list only if it has a preview URL
        		for (int i=0; i<length; i++) {
        			JSONObject songObj = res.getJSONObject(i);
        			if (songObj.has("preview")) {
	        			String song = songObj.getString("title");
	        			String artist = songObj.getJSONObject("artist").getString("name");
	        			String previewUrl = songObj.getString("preview");
	        			result.add(new SongEntry(song, artist, previewUrl));
        			}
        		}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			// return the list as an array
			SongEntry[] array = {};
			return result.toArray(array);
		}

		@Override
		protected void onPostExecute(SongEntry[] result) {
			// keep the array of songs in memory
			currentSongs = result;
			// updates the content of the list
			ListAdapter adapter = new ArrayAdapter<SongEntry>(getBaseContext(), android.R.layout.simple_list_item_1, result);
			songsList.setAdapter(adapter);
			// hide the progress indicator
			progress.setVisibility(View.INVISIBLE);
			// show the list
			songsList.setVisibility(View.VISIBLE);
			super.onPostExecute(result);
		}

	}
	
//	private class DownloadPreviewTrackTask extends AsyncTask<String, Integer, File> {
//
//		@Override
//		protected void onPostExecute(File result) {
//			MediaPlayer mp = new MediaPlayer();
//            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            mp.setOnPreparedListener(new OnPreparedListener() {
//				@Override
//				public void onPrepared(MediaPlayer mp) {
//					mp.start();
//				}
//			});
//            
//			try {
//				mp.setDataSource(result.getAbsolutePath());
//				mp.prepareAsync();
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				e.printStackTrace();
//			} catch (IllegalStateException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			super.onPostExecute(result);
//		}
//
//		@Override
//		protected void onPreExecute() {
//			// TODO Auto-generated method stub
//			super.onPreExecute();
//		}
//
//		@Override
//		protected File doInBackground(String... urls) {
//			HttpURLConnection conn = null;
//			URL url;
//			try {
//				url = new URL(urls[0]);
//				conn = (HttpURLConnection)url.openConnection();
//				conn.setDoInput(true);
//	        	conn.setRequestMethod("GET");
//	        	if (conn.getResponseCode() > 400) {
//	        		Log.e(Constants.LOG, "error " + conn.getResponseCode() + ": " + conn.getResponseMessage());
//	        	} else {
//		        	InputStream is = conn.getInputStream();
//		            BufferedInputStream bis = new BufferedInputStream(is);
//		            ByteArrayBuffer baf = new ByteArrayBuffer(50);
//		            int current = 0;
//		            while ((current = bis.read()) != -1) {
//		               baf.append((byte) current);
//		            }
//		            /* Convert the Bytes read to a String. */
//		            FileOutputStream fos = openFileOutput("tmp.m4a", MODE_PRIVATE);
//		            fos.write(baf.toByteArray());
//		            fos.close();
//	        	}
//	            
//			} catch (MalformedURLException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			return getFileStreamPath("tmp.m4a");
//		}
//	}

}
