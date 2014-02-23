package com.philipoy.picss.profiles;

import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.OutputFormat;
import android.os.Build;

public class ProfileSDKFrom7 extends AbstractProfile {

private static ProfileSDKFrom7 instance;
	
	private ProfileSDKFrom7() {
		SDK_VERSION = Build.VERSION_CODES.ECLAIR_MR1; // 7
		AUDIO_ENCODER = AudioEncoder.AMR_NB; // AMR Narrow Band : http://en.wikipedia.org/wiki/Adaptive_Multi-Rate_audio_codec
		AUDIO_FORMAT = OutputFormat.THREE_GPP; // AMR/3GP is not supported by browser -> converted to AAC/MP4 on server
		TEMP_AUDIO_FILE_EXT = ".3gp";
	}
	
	public static AbstractProfile getProfile() {
		if (instance == null) {
			instance = new ProfileSDKFrom7();
		}
		return instance;
	}
	
}
