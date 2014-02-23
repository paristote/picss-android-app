package com.philipoy.picss.profiles;


public abstract class AbstractProfile {
	
	/**
	 * http://developer.android.com/reference/android/os/Build.VERSION_CODES.html
	 */
	public int SDK_VERSION;
	
	/**
	 * http://developer.android.com/reference/android/media/MediaRecorder.AudioEncoder.html
	 */
	public int AUDIO_ENCODER;
	
	/**
	 * http://developer.android.com/reference/android/media/MediaRecorder.OutputFormat.html
	 */
	public int AUDIO_FORMAT;
	
	/**
	 * Extension of the temp audio file, depending on AUDIO_FORMAT
	 */
	public String TEMP_AUDIO_FILE_EXT;
	
	/**
	 * To overwrite in concrete profile classes
	 * @return the profile object
	 */
	public static AbstractProfile getProfile() {
		return null;
	}

}
