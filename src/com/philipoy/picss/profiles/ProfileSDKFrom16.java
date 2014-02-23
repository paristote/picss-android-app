package com.philipoy.picss.profiles;

import android.annotation.TargetApi;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.OutputFormat;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ProfileSDKFrom16 extends AbstractProfile {

private static ProfileSDKFrom16 instance;
	
	private ProfileSDKFrom16() {
		SDK_VERSION = Build.VERSION_CODES.JELLY_BEAN; // 16
		AUDIO_ENCODER = AudioEncoder.HE_AAC; // AAC High Efficiency : http://en.wikipedia.org/wiki/HE-AAC
		AUDIO_FORMAT = OutputFormat.MPEG_4; // AAC is only supported in the MP4 container in HTML5
		                                    // https://developer.mozilla.org/en-US/docs/HTML/Supported_media_formats#Browser_compatibility (Notes)
		TEMP_AUDIO_FILE_EXT = ".mp4";
	}
	
	public static AbstractProfile getProfile() {
		if (instance == null) {
			instance = new ProfileSDKFrom16();
		}
		return instance;
	}
	
}
