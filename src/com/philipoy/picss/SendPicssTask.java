package com.philipoy.picss;

import java.io.ByteArrayInputStream;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.philipoy.picss.model.Picss;
import com.philipoy.picss.profiles.Constants;
import com.philipoy.picss.utils.HttpRequest;

/**
 * Send the Picss to the server
 * @author paristote
 *
 */
public class SendPicssTask extends AsyncTask<Picss, String, Integer> {	
	
	private Context context;
	
	public SendPicssTask(Context ctx) {
		context = ctx;
	}
	
	@Override
	protected Integer doInBackground(Picss... data) {
		Log.d(Constants.LOG, "sending picss");
        if (data.length < 1) return null;
        // get the Picss name and label
        String picssName = data[0].name;
        String picssLabel = (data[0].label==null ? "" : data[0].label);
        
        HttpRequest request = HttpRequest.post("http://"+Constants.PICSS_SERVER_HOST+"/"+
				Constants.PICSS_SERVER_SERVICE);
        request.part("name", picssName);
        request.part("label", picssLabel);
        request.part("image","image.jpg", "image/jpeg", new ByteArrayInputStream(data[0].photo));
        if (data[0].sound != null) { // sound is a recorded audio (byte array)
        	
        	request.part("sound", "sound"+HomeActivity.profile.TEMP_AUDIO_FILE_EXT, "application/octet-stream", new ByteArrayInputStream(data[0].sound));
            
    	} else if (!data[0].soundUrl.equals("")) { // sound is a music preview (url string)
    		
    		request.part("soundUrl", data[0].soundUrl);
    		
    	}
        
        if (request.ok()) {
        	Log.d(Constants.LOG, "picss sent");
        } else {
        	Log.e(Constants.LOG, request.code() + " : " + request.message());
        }
        
        return Integer.valueOf(request.code());
        

	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if (result.intValue()>=200 && result.intValue()<300) {
			Log.d(Constants.LOG, "done!");
			Toast.makeText(context, R.string.sent_ok, Toast.LENGTH_SHORT).show();
		} else {
			//TODO handle different response code
		}
		super.onPostExecute(result);
	}

}
