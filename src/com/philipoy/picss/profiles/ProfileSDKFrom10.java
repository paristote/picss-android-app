package com.philipoy.picss.profiles;

import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.OutputFormat;
import android.os.Build;

public class ProfileSDKFrom10 extends AbstractProfile {
	
	private static ProfileSDKFrom10 instance;
	
	private ProfileSDKFrom10() {
		SDK_VERSION = Build.VERSION_CODES.GINGERBREAD_MR1; // 10
		AUDIO_ENCODER = AudioEncoder.AAC; // AAC Low Complexity : http://en.wikipedia.org/wiki/Advanced_Audio_Coding
		AUDIO_FORMAT = OutputFormat.MPEG_4; // AAC is only supported in the MP4 container in HTML 5
		                                    // https://developer.mozilla.org/en-US/docs/HTML/Supported_media_formats#Browser_compatibility (Notes)
		TEMP_AUDIO_FILE_EXT = ".mp4";
	}
	
	public static AbstractProfile getProfile() {
		if (instance == null) {
			instance = new ProfileSDKFrom10();
		}
		return instance;
	}
	
}
