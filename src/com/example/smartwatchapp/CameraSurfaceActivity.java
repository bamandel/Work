package com.example.smartwatchapp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

public abstract class CameraSurfaceActivity extends Activity implements OnClickListener, SurfaceHolder.Callback {
	public static final int RESPONSE_YES = 0;
	public static final int RESPONSE_NO = 1;
	
	public static final float INCHES_FOUR = .4f;
	
	public static final String IDENTIFIER = "id";
	public static final String FIRST_FILE = "first file";
	public static final String SECOND_FILE = "second file";
	public static final String DATA_FILE = "data file";
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	public static final int MEDIA_TYPE_TEXT = 3;
	public static final int MEDIA_TYPE_AUDIO = 4;
	
	public static final int MESSAGE_STATE_CHANGED = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MEASSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICENAME = 4;
	public static final int MESSAGE_TOAST = 5;
	
	public static final int STATE_NONE = 1;
	public static final int STATE_LISTEN = 2;
	public static final int STATE_CONNECTING = 3;
	public static final int STATE_CONNECTED = 4;
	
	public static final int REQUEST_CONNECT_DEVICE = 1;
	public static final int REQUEST_ENABLE_BT = 2;
	
	private BluetoothAdapter bluetoothAdapter = null;
	
	private static ImageView dot;
	private static ImageButton cameraButton;
	
	protected static SurfaceHolder surfaceHolder;
	protected static Camera camera = null;
	protected static SurfaceView surfaceView;
	
	protected boolean isVideo = true;
	private boolean previewRunning = false;
	protected boolean isFirstClick = true;
	protected boolean isSecondClick = false;
	
	private String dataFile = null;
	private String dataPath = "temp_store";
	protected static int identifier = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_surface);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		
		if(!isServiceRunning(UploadService.class)) {
			dataPath += "/DAT_" + timeStamp;
		} else {
			String[] loc = UploadService.getDataPath().split("/DAT_");
			dataPath += "/DAT_" + loc[1];
		}
		
		makeDirectory();
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.flVideoPerimeter, new PlaceholderFragment()).commit();
		}
	}
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_camera_surface, container, false);
			
			surfaceView = (SurfaceView) rootView.findViewById(R.id.svVideoView);
			surfaceHolder = surfaceView.getHolder();
			surfaceHolder.addCallback((Callback) getActivity());
			
			surfaceView.setClickable(false);
			surfaceView.setOnClickListener((OnClickListener) getActivity());
			
			dot = (ImageView) rootView.findViewById(R.id.ivDot);
			cameraButton = (ImageButton) rootView.findViewById(R.id.ibCamera);
			
			cameraButton.setVisibility(View.INVISIBLE);
			
			return rootView;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if (camera != null) {
			camera.stopPreview();
			previewRunning = false;
			releaseCamera();
		}
		
	}
 
	@Override
	protected void onResume() {
		super.onResume();

		dot.setImageResource(R.drawable.red_dot);
	}
	
	protected File getOutputMediaFile(int type) {
		makeDirectory();
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		
		if (type == MEDIA_TYPE_IMAGE) {
			return new File(dataFile + File.separator + "IMG_" + timeStamp + ".jpg");
		} else if(type == MEDIA_TYPE_VIDEO) {
			return new File(dataFile + File.separator + "VID_" + timeStamp + ".mp4");
		} else if(type == MEDIA_TYPE_TEXT) {
			return new File(dataFile + File.separator + "TXT_" + timeStamp + ".txt");
		}
		
		return null;
	}
	
	private void makeDirectory() {
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), dataPath);
		if(!mediaStorageDir.exists()) {
			if(mediaStorageDir.mkdirs()) {
				log("Created new Directory");
				dataFile = mediaStorageDir.getPath();
			}
		} else {
			dataFile = mediaStorageDir.getPath();
		}
	}
	
	public Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}
	
	private Camera.Size getBestPreviewSize(Camera.Parameters parameters, int w, int h) {
		Camera.Size result = null;
		
		for(Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= w && size.height <= h) {
				if (result == null) {
					log("Size set");
					result = size;
				}
				else {
					int resultDelta = w - result.width + h - result.height;
					int newDelta = w - size.width + h - size.height;
					
					if(newDelta < resultDelta)
						result = size;
				}
			}
		}
		
		return result;
	}
	
	private void releaseCamera() {
		if(camera != null) {
			camera.release();
			camera = null;
		}
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		log("surfaceCreated");
		
		camera = getCameraInstance();
		
		try {
			camera.setPreviewDisplay(surfaceHolder);
			camera.startPreview();
			log("setting Preview and display");
		} catch (IOException e) {
			log("error with first Preview");
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
		
		if(surfaceHolder.getSurface() == null) {
			log("No surface detected");
			return;
		}
		
		Camera.Parameters params = null;
		Camera.Size size = null;
		if (camera != null) {
			log("camera not null");
			params = camera.getParameters();
			size = getBestPreviewSize(params, width, height);
		}
		
		if(size != null) {
			params.setPreviewSize(size.width, size.height);
			camera.setParameters(params);
		}
		
		try {
			camera.setPreviewDisplay(surfaceHolder);
			log("PreviewDisplay set");
		} catch (Exception e) {
			log("camera preview failed");
			e.printStackTrace();
		}
		
		try {
			camera.stopPreview();
			previewRunning = false;
			log("Preview stopped");
		} catch (Exception e) {
			e.printStackTrace();
			log("camera preview stop failed");
		}
		
		camera.startPreview();
		log("Preview started");
		previewRunning = true;
		
		LocalBroadcastManager broadcast = LocalBroadcastManager.getInstance(this);
		broadcast.sendBroadcast(new Intent("surface found"));
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(previewRunning) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			log("Camera Preview stopped in SurfaceDestroyed");
		}
		
		previewRunning = false;
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.svVideoView && v.getId() != R.id.ibCamera) {
			log("running start camera");
			if (isFirstClick) {
				dot.setImageResource(R.drawable.red_dot);
			} else {
				isSecondClick = true;
				
				Intent data = new Intent("data");

				data.putExtra("datafile", dataFile);
				data.putExtra("file", getOutputMediaFile(MEDIA_TYPE_TEXT).getPath());

				sendBroadcast(data);
				
				finish();
			}
			
		
		}
	}
	
	public boolean isServiceRunning(Class<?> serviceClass) {
    	ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    	for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
    		if(serviceClass.getName().equals(service.service.getClassName())) {
    			return true;
    		}
    	}
    	
    	return false;
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		if(!bluetoothAdapter.isEnabled())
        	startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
		else {
			if(!isServiceRunning(UploadService.class)) {
				Intent service = new Intent(this, UploadService.class);
				service.putExtra("datafile", dataFile);
				
				startService(service);
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_ENABLE_BT:
			if(resultCode == RESULT_OK) {
				if(!isServiceRunning(UploadService.class)) {
					Intent service = new Intent(this, UploadService.class);
					service.putExtra("datafile", dataFile);
					
					startService(service);
				}
				break;
			} else {
				Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private static void log(String data) {
		Log.d("CameraSurfaceActivity", data);
	}
	
}
