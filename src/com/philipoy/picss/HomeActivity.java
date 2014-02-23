package com.philipoy.picss;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.philipoy.picss.model.Picss;
import com.philipoy.picss.profiles.AbstractProfile;
import com.philipoy.picss.profiles.Constants;
import com.philipoy.picss.profiles.ProfileSDKFrom10;
import com.philipoy.picss.profiles.ProfileSDKFrom16;
import com.philipoy.picss.profiles.ProfileSDKFrom7;
import com.philipoy.picss.views.PicssDialogFragment;
import com.philipoy.picss.views.PicssDialogFragment.PicssDialogListener;
import com.philipoy.picss.views.PicssSurfaceView;

/**
 * Main activity that allows to take a picture and record a sound
 * @author paristote
 *
 */
public class HomeActivity extends FragmentActivity implements MediaRecorder.OnErrorListener, PicssDialogListener {
	
	/**
	 *  the device profile with options depending on the device and sdk that executes the app
	 */
	public static AbstractProfile profile;
	/**
	 *  custom surface view that prepares the camera and the preview, switch the camera if possible, and release the camera
	 */
	private PicssSurfaceView preview=null;
	/**
	 *  custom dialog to ask the name and label of the Picss
	 */
	private PicssDialogFragment dialog = null;
	/*
	 *  UI elements
	 */
	private ImageButton btnSwitch=null;
	private ImageButton btnCapture=null;
	private ImageView imageMic=null;
	private ProgressBar progressMic=null;
	private TextView textSeconds=null;
	/*
	 *  used to record the audio
	 */
	private MediaRecorder recorder = null;
	private boolean isRecording = false;
	private CounterTask counter = null;
	/**
	 *  Picss object contains photo, sound, name and label
	 */
	private Picss currentPicss = null;
	/**
	 * Mode of the selection of the sound : MODE_PICK_MUSIC or MODE_RECORD_AUDIO
	 */
	private int currentMode;

	  
	  /**
	   * Callback method when the camera took a picture.
	   * Stop the preview and save the photo data in RAM to reuse it in the endCapture method
	   */
	  Camera.PictureCallback photoCallback=new Camera.PictureCallback() {
		    public void onPictureTaken(byte[] data, Camera camera) {
		    	if (currentPicss != null) { 
		    		currentPicss.photo = data;
		    	}
		    	
		    }
		  };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// app in full screen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        					 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_home);
		
		preview = (PicssSurfaceView)findViewById(R.id.surfaceView);
		imageMic = (ImageView)findViewById(R.id.imageMic);
		progressMic = (ProgressBar)findViewById(R.id.progressMic);
		textSeconds = (TextView)findViewById(R.id.textSeconds);
		btnSwitch = (ImageButton)findViewById(R.id.btn_switch);
		btnCapture = (ImageButton)findViewById(R.id.btn_capture);
		btnCapture.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO better UXP needed to handle btw click and long-press
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					currentMode = Constants.MODE_PICK_MUSIC;
					currentPicss = new Picss();
					takePhoto();
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					// TODO allow user to choose the best of the captured photos
					if (currentMode == Constants.MODE_RECORD_AUDIO) {
						// stops recording audio
						endCapture();
						// opens a dialog to ask the user the name and label for this Picss
						askPicssNameAndLabel();
					} else if (currentMode == Constants.MODE_PICK_MUSIC) {
						Log.d(Constants.LOG, "pick music");
						// launch Pick Music Activity
						Intent pickMusic = new Intent(getBaseContext(), PickMusicActivity.class);
						pickMusic.putExtra("picssPhoto", currentPicss.photo);
						startActivity(pickMusic);
					}
				}
				return false;
			}
		});
		// if the user keeps the capture button pressed, he's recording audio
		btnCapture.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// start recording audio and capture 1 photo ; TODO: capture 1 photo every 1 or 2 sec
				currentMode = Constants.MODE_RECORD_AUDIO;
				startCapture();
				return false;
			}
		});
		
		// configure app parameters according to the device's sdk
		configure();
	}
	
	/*
	 * Configure the app according to the SDK of the device
	 */
	private void configure() {
		int sdk = Build.VERSION.SDK_INT;
		if (sdk >= Build.VERSION_CODES.ECLAIR_MR1 && sdk < Build.VERSION_CODES.GINGERBREAD_MR1) { // 7-9
			profile = ProfileSDKFrom7.getProfile();
		} else if (sdk >= Build.VERSION_CODES.GINGERBREAD_MR1 && sdk < Build.VERSION_CODES.JELLY_BEAN) { // 10-15
			profile = ProfileSDKFrom10.getProfile();
		} else if (sdk >= Build.VERSION_CODES.JELLY_BEAN) { // 16+
			profile = ProfileSDKFrom16.getProfile();
		}
		// configure back or front camera for devices with sdk >= 9
		if (profile.SDK_VERSION >= Build.VERSION_CODES.GINGERBREAD) {
			configureCameraForSDK9();
		}
	}
	
	/*
	 *  set back camera by default
	 *  displays the switch camera button if there is a 2nd camera
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void configureCameraForSDK9() {
		preview.setCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
		if (Camera.getNumberOfCameras() > 1) {
			btnSwitch.setVisibility(View.VISIBLE);
		}
	}

	
	  @Override
	  public void onResume() {
	    super.onResume();
	    preview.startPreview();
	  }
	    
	  @Override
	  public void onPause() {
	    
	    preview.releaseCamera(true);
	    releaseRecorder();

	    super.onPause();
	  }
	  
	  /**
	   * Capture the photo
	   */
	  private void takePhoto() {
		  if (preview.getCamera() != null) {
			  Log.d(Constants.LOG, "taking photo");
			  preview.getCamera().takePicture(null, null, photoCallback);
		  }
	  }

	  /**
	   * Called when the user presses and keeps down the capture button
	   * Start recording audio
	   */
	  private void startCapture() {
		  if (!isRecording) {
			  recorder = new MediaRecorder();
			  recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			  // format and encoder depend on the profile i.e. the SDK of the device
			  recorder.setOutputFormat(profile.AUDIO_FORMAT);
			  recorder.setAudioEncoder(profile.AUDIO_ENCODER);
			  // file where the sound will be saved
			  String filepath = getFilesDir().getAbsolutePath()+"/"+Constants.TEMP_AUDIO_FILE_NAME+profile.TEMP_AUDIO_FILE_EXT;
			  recorder.setOutputFile(filepath);
			  try {
				  recorder.prepare();
			  } catch (IOException ioe) {
				  Log.e(Constants.LOG, ioe.getMessage(), ioe);
			  }
			  
			  Log.d(Constants.LOG, "start recording audio");
			  recorder.start();
			  isRecording = true;
			  // inform user he is recording audio
			  // mic icon
			  imageMic.setVisibility(View.VISIBLE);
			  // activity indicator
			  progressMic.setVisibility(View.VISIBLE);
			  // counter TODO show a progress bar instead (as in mindie)
			  counter = new CounterTask(getBaseContext());
			  counter.execute(Integer.valueOf(0));
			  
		  }
	  }
	  
	  /**
	   * Called when the user releases the capture button
	   * Stop recording audio
	   */
	  private void endCapture() {
		  byte[] sound = null; // byte array of the audio file
	      if (isRecording) {
	    	  // stop recording and release the recorder
	    	  releaseRecorder();
	    	  // stop and hide the counter
	    	  counter.stop();
	    	  counter = null;
	    	  Log.d(Constants.LOG, "done recording audio");
	    	  // hide the mic icon and the progress to inform user he stopped recording audio
	    	  imageMic.setVisibility(View.INVISIBLE);
	    	  progressMic.setVisibility(View.INVISIBLE);
	      }
	      try {
	    	  // load the sound from the saved file into the byte array
	    	  File tmp = new File(getFilesDir().getAbsolutePath()+"/"+Constants.TEMP_AUDIO_FILE_NAME+profile.TEMP_AUDIO_FILE_EXT);
	    	  if (tmp.length() < Integer.MAX_VALUE) {
	    		  sound = new byte[(int)tmp.length()];
	    		  Log.d(Constants.LOG, "Sound file size: "+tmp.length());
	    	  }
	    	  BufferedInputStream buf = new BufferedInputStream(new FileInputStream(tmp));
	          buf.read(sound, 0, sound.length);
	          buf.close();
	          // reference the sound in the Picss object
	          currentPicss.sound = sound;
	      } catch (FileNotFoundException e) {
	    	  Log.e(Constants.LOG, e.getMessage(), e);
	      } catch (IOException ioe) {
	    	  Log.e(Constants.LOG, ioe.getMessage(), ioe);
	      }
	  }
	  
	  /*
	   * re-init the state of the activity to create a new Picss
	   */
	  private void finishPicss() {
		  // set to null to be able to reopen the dialog for another Picss
		  dialog = null;
		  // restart the camera preview
		  preview.getCamera().startPreview();
	      // reset the model for a new Picss
	      currentPicss = null;
	  }
	  
	  /**
	   * Called when the switch camera is tapped
	   * @param v
	   */
	  public void switchCamera(View v) {
		  preview.switchCamera();
	  }
	  
	  /*
	   * Util method to stop recording, release the recorder properly and reinit the state of the app
	   */
	  private void releaseRecorder() {
		  if (recorder != null) {
			  if (isRecording) {
				  recorder.stop();
				  isRecording = false;
			  }
			  recorder.release();
			  recorder = null;
		  }
	  }

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		Log.e(Constants.LOG, "Error "+what+" with MediaRecorder. Extra "+extra);
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
	 * Send the Picss to the server and re-inits the activity
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
	    	Toast.makeText(HomeActivity.this, R.string.sending, Toast.LENGTH_SHORT).show();
	    } else {
	    	Log.e(Constants.LOG, "Cannot upload Picss to the server.");
	    	Log.e(Constants.LOG, currentPicss.toString());
	    	Toast.makeText(HomeActivity.this, R.string.sent_ko, Toast.LENGTH_LONG).show();
	    }
		// re-init the activity to create a new Picss
		finishPicss();
	}
	/**
	 * Called from the dialog Cancel button
	 */
	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		// Nothing else to do than re-init the activity to start creating a new Picss
		finishPicss();
	}
	
	/**
	 * Called when the user taps the image preview
	 * If the device supports it, start auto-focus of the camera
	 * @param v
	 */
	public void autofocus(View v) {
		if (preview.canAutoFocus())
			preview.getCamera().autoFocus(null);
	}
	
	/**
	 * Counter used to show how many secs of audio are recorded
	 */
	private class CounterTask extends AsyncTask<Integer, Integer, Integer> {
		
		private Context c;
		private boolean shouldContinue = true;
		
		public CounterTask(Context ctx) {
			c = ctx;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			// hides the counter text
			textSeconds.setVisibility(View.INVISIBLE);
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			int secs = values[0].intValue();
			if (secs == 1) { // show counter from 1 sec and more
				textSeconds.setTextColor(Color.BLACK);
				textSeconds.setVisibility(View.VISIBLE);
			} else if (secs == 20) { // stop the counter and continue the process at maximum 20 sec
				endCapture();
				shouldContinue = false;
				askPicssNameAndLabel();
			}
			if (secs <= 1) textSeconds.setText(secs+" "+c.getString(R.string.sec)); // 0 or 1 sec
			else { // 2 or more secs
				if (secs >= 15 && secs < 19) textSeconds.setTextColor(Color.rgb(255, 144, 0)); // orange
				else if (secs == 19) textSeconds.setTextColor(Color.RED);
				textSeconds.setText(secs+" "+c.getString(R.string.secs));
			}
			super.onProgressUpdate(values);
		}

		@Override
		protected Integer doInBackground(Integer... params) {
			int i = params[0].intValue();
			while (shouldContinue) {
				// update the counter value in the label
				publishProgress(Integer.valueOf(i));
				try {
					// wait 1 sec
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e(Constants.LOG, "Counter thread interrupted at "+i+" secs because of "+e.getMessage(), e);
				}
				i++;
			}
			return Integer.valueOf(i);
		}
		// called when the user releases the capture button
		// stop the counter
		public void stop() { shouldContinue = false; }
		
	}
}
