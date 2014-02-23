package com.philipoy.picss;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.philipoy.picss.model.Picss;
import com.philipoy.picss.profiles.Constants;

/**
 * Send the Picss to the server
 * @author paristote
 *
 */
public class SendPicssTask extends AsyncTask<Picss, String, Integer> {

	/*
	 * elements of the request data
	 */
	private final String LINE_END = "\r\n";
	private final String HYPHENS = "--";
	private final String BOUNDARY = "*****";
	
	
	private Context context;
	
	public SendPicssTask(Context ctx) {
		context = ctx;
	}
	
	@Override
	protected Integer doInBackground(Picss... data) {
		Log.d(Constants.LOG, "sending photo");
        if (data.length < 1) return null;
        // get the Picss name and label
        String picssName = data[0].name;
        String picssLabel = (data[0].label==null ? "" : data[0].label);
        HttpURLConnection conn = null;
        int result = 505;
        try {
        	// prepare a HTTP POST request to the server
        	URL url = new URL("http://"+Constants.PICSS_SERVER_HOST+"/"+
        						Constants.PICSS_SERVER_SERVICE);
        	conn = (HttpURLConnection)url.openConnection();
        	conn.setChunkedStreamingMode(0);
        	// we will read from this request's response
        	conn.setDoInput(true);
        	// we will write data in the request
        	conn.setDoOutput(true);
        	conn.setRequestMethod("POST");
        	conn.setRequestProperty("Connection", "Keep-Alive");
        	conn.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + BOUNDARY);
        	DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        	// name
        	write("name", picssName, dos);
        	// label
        	write("label", picssLabel, dos);
        	// image
        	writeFile("image", "image"+picssName+".jpg", "image/jpeg", data[0].photo, dos);
        	// sound
        	if (data[0].sound != null) { // sound is a recorded audio (byte array)
	        	
        		writeFile("sound", "sound"+picssName+HomeActivity.profile.TEMP_AUDIO_FILE_EXT, "application/octet-stream", data[0].sound, dos);
	            
        	} else if (!data[0].soundUrl.equals("")) { // sound if a music preview (url string)
        		
        		write("soundUrl", data[0].soundUrl, dos);
        		
        	}
        	dos.writeBytes(HYPHENS + BOUNDARY + HYPHENS + LINE_END);
        	// done
            dos.flush();
            dos.close();
            Log.d(Constants.LOG, "photo sent");
            result = conn.getResponseCode();
            Log.d(Constants.LOG, conn.getResponseMessage());
        } catch (Exception e) {
        	Log.e(Constants.LOG, e.getMessage(), e);
        } finally {
        	if (conn != null) conn.disconnect();
        }
	    return Integer.valueOf(result);
	}
	
	/**
	 * Write the form data part in the Data Output Stream
	 * @param name The name of the form field
	 * @param data The data as a String
	 * @param dos The Output Stream
	 * @throws IOException
	 */
	private void write(String name, String data, DataOutputStream dos) throws IOException {
		dos.writeBytes(HYPHENS + BOUNDARY + LINE_END);
    	dos.writeBytes("Content-Disposition: form-data; name=\""+name+"\""+LINE_END);
    	dos.writeBytes(LINE_END);
    	dos.writeBytes(data);
    	dos.writeBytes(LINE_END);
	}
	
	/**
	 * Write the file in the Data Output Stream
	 * @param name The name of the form field
	 * @param fileName The name of file as it is uploaded
	 * @param contentType The content type of the file
	 * @param data The array of bytes
	 * @param dos The Output Stream
	 * @throws IOException
	 */
	private void writeFile(String name, String fileName, String contentType, byte[] data, DataOutputStream dos) throws IOException {
		dos.writeBytes(HYPHENS + BOUNDARY + LINE_END);
    	dos.writeBytes("Content-Disposition: form-data; name=\""+name+"\"; filename=\""+fileName+"\""+LINE_END);
    	dos.writeBytes("Content-Type: "+contentType+LINE_END);
    	dos.writeBytes(LINE_END);
    	dos.write(data);
    	dos.writeBytes(LINE_END);
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if (result.intValue()<400) {
			Log.d(Constants.LOG, "done!");
			Toast.makeText(context, R.string.sent_ok, Toast.LENGTH_SHORT).show();
		}
		super.onPostExecute(result);
	}

}
