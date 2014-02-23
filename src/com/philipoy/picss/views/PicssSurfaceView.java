package com.philipoy.picss.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.philipoy.picss.HomeActivity;
import com.philipoy.picss.profiles.Constants;

/**
 * Custom view that extends the Surface View and adds some handling methods
 * @author paristote
 *
 */
public class PicssSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

	/**
	 *  surface holder that displays the preview
	 */
	private SurfaceHolder sHolder;
	/**
	 *  camera used for the capture
	 */
	private Camera camera;
	private boolean isCameraConfigured = false;
	private boolean canAutoFocus = false;
	private boolean inPreview = false;
	private int currentCameraId = 0; // Back camera by default
	
	@SuppressWarnings("deprecation")
	public PicssSurfaceView(Context context, AttributeSet attr) {
		super(context, attr);
		
		sHolder = getHolder();
		sHolder.addCallback(this); // sets callback methods surfaceChanged/Created/Destroyed
		sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // needed to run on android < 11
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(Constants.LOG, "surface changed");
		initPreview(width, height);
		startPreview();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(Constants.LOG, "surface created");
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	
	/**
	 * Opens the camera identified by id (front or back)
	 * @param id id of the front camera or the back camera
	 * @return the Camera object
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private Camera openCameraSDK9(int id) {
		return Camera.open(id);
	}
	
	public void setCamera(int id) {
		currentCameraId = id;
	}
	
	/**
	   * Initialize the camera preview
	   * @param width The surface width.
	   * @param height The surface height.
	   */
	  public void initPreview(int width, int height) {
		  // opens the default camera if sdk < 9
		  if (HomeActivity.profile.SDK_VERSION < Build.VERSION_CODES.GINGERBREAD)
			  camera = Camera.open();
		  else // or opens the current selected camera (front or back)
			  camera = openCameraSDK9(currentCameraId);
		  try {
			  // display the camera preview on the surface (holder)
			  camera.setPreviewDisplay(sHolder);
		  }
		  catch (Throwable t) {
			  Log.e(Constants.LOG, "Exception in setPreviewDisplay()", t);
		  }
			
	    if (camera!=null && sHolder.getSurface()!=null) {

	      if (!isCameraConfigured) { // the camera is not yet configured
	    	  
	        Camera.Parameters parameters=camera.getParameters();
	        // get the largest size that this device supports, hoping it will be fullscreen
	        Camera.Size size=getBestPreviewSize(width, height,
	                                            parameters);
	        // set the size and finish the camera configuration
	        if (size!=null) {
		        parameters.setPreviewSize(size.width, size.height);
		        parameters.setPictureSize(size.width, size.height);
		        parameters.setJpegQuality(100);
	        	camera.setParameters(parameters);
	        	isCameraConfigured = true;
	        } else {
	        	Log.e(Constants.LOG, "No preview size that fits this device was found");
	        }
	      }
	    }
	  }
	  /**
	   * Start the preview of the camera on the surface view.
	   */
	  public void startPreview() {
		  if (isCameraConfigured && camera!=null) {
			  camera.startPreview();
			  String focusMode = camera.getParameters().getFocusMode();
			  Log.d(Constants.LOG, "autofocus mode: "+focusMode);
			  // if the camera supports autofocus, do it
			  if (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
				  camera.autoFocus(null);
				  canAutoFocus = true;
			  }
			  inPreview=true;
		  }
	  }
	  
	  /**
	   * Calculate the preview size that best fits the surface size.
	   * @param width The surface width.
	   * @param height The surface height.
	   * @param parameters The device's camera parameters.
	   * @return the best Camera.Size object for this screen.
	   */
	  private Camera.Size getBestPreviewSize(int width, int height,
	                                         Camera.Parameters parameters) {
	    Camera.Size result=null;
	    // check every preview size supported by this device
	    for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
	    	// if it's smaller or equal than the surface size
	      if (size.width<=width && size.height<=height) {
	        if (result==null) {
	          result=size;
	        } // keep the largest preview size that fits the surface size
	        else {
	          int resultArea=result.width*result.height;
	          int newArea=size.width*size.height;
	          
	          if (newArea>resultArea) {
	            result=size;
	          }
	        }
	      }
	    }
	    Log.d(Constants.LOG, "Final width:"+result.width+" | height:"+result.height);
	    return(result);
	  }
	  
	  public Camera getCamera() {
		  return camera;
	  }
	  
	  /**
	   * Switches between the front and back camera
	   */
	  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
	  public void switchCamera() {
		  // get the current camera parameters because we need the surface size
		  Camera.Parameters parameters=camera.getParameters();
		  // release the camera but don't set camera = null
		  releaseCamera(false);
		  // switch camera
		  if(currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
			  currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
		  }
		  else {
			  currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
		  }
		  // re-init the newly selected camera with the previous surface size as reference
		  // the camera is opened in that method
		  initPreview(parameters.getPreviewSize().width, parameters.getPreviewSize().height);
		  // start preview
		  startPreview();
	  }
	  
	  /**
	   * Util method that releases the camera properly
	   * @param setNull pass true if the camera must be set to null
	   */
	  public void releaseCamera(boolean setNull) {
		  if (camera != null) {
			  isCameraConfigured = false;
			  if (inPreview) {
			      camera.stopPreview();
			    }
			    camera.release();
			    if (setNull) camera=null;
			    inPreview=false;
		  }
	  }
	  
	  /**
	   * 
	   * @return true only if the camera supports autofocus and the camera is in preview
	   */
	  public boolean canAutoFocus() {
		  return (inPreview && canAutoFocus);
	  }

}
