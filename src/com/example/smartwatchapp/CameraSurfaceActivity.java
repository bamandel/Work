package com.example.smartwatchapp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public abstract class CameraSurfaceActivity extends Activity implements OnClickListener, SurfaceHolder.Callback, SensorEventListener{
	public static final int RESPONSE_YES = 0;
	public static final int RESPONSE_NO = 1;
	
	public static final float INCHES_FOUR = .4f;
	
	/*
	 * Sensor variables, Gyroscope and Accelerometer
	 */
	
	private SensorManager sensorMgr;
	private Sensor sensorAccel;
	private Sensor sensorGyro;
	
	/*
	 * values used to determine distance a user has moved
	 */
	
	//Total distance moved
	private float moved = 0;
	
	//Total speed moved
	private float speed = 0;
	
	//current accleretion (Without altercations/weights)
	private float accel = 0;
	
	//A weight to help acceleration more accurate
	private float accelDiff = 0;
	
	//Another weight to help with accuracy
	private float accelAvg = 0;
	
	//value used to determine accelAvg
	private float accelPrev = 0;
	
	//Distance we want the user to move
	private float distanceNeeded = 0;
	
	//Arrays that hold the data for acceleration and direction
	private float[] a = new float[3];
	private int[] directionCounter = {0, 0, 0}; // The higher a specific value is the further they moved that way
	
	//Values used as a weight to pinpoint true accleration
	private long lastUpdate = 0;
	private float curAccel = 0;
	private int accelUpdates = 0;
	
	/*
	 * Values used for storing angles and angle weights
	 */
	
	private float[] newAngles = new float[3];

	private float[] tempAngles = new float[3];
	
	private static ImageView dot;
	
	protected static SurfaceHolder surfaceHolder;
	protected static Camera camera;
	private static SurfaceView surfaceView;
	
	private boolean previewRunning = false;
	protected static Boolean isFirstClick = true;
	private static Boolean isSecondClick = false;
	
	//Strings to store data for deciphering pictures
	protected String accelAccuracy;
	protected String gyroAccuracy;
	protected String direction;
	protected String beforeAfter;
	
	private File dataFile = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_surface);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		beforeAfter = this.getIntent().getStringExtra("before after");
		
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
			
			surfaceView.setClickable(true);
			surfaceView.setOnClickListener((OnClickListener) getActivity());
			
			dot = (ImageView) rootView.findViewById(R.id.ivDot);
			
			return rootView;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		sensorMgr.unregisterListener(this, sensorAccel);
		sensorMgr.unregisterListener(this, sensorGyro);
		sensorMgr = null;
		//finish();
	}
 
	@Override
	protected void onResume() {
		super.onResume();
		
		dot.setImageResource(R.drawable.red_dot);
		
		sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccel = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
		sensorGyro = sensorMgr.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
		
		boolean accelSupported = sensorMgr.registerListener(this, sensorAccel, SensorManager.SENSOR_DELAY_NORMAL);
		boolean gyroSupported = sensorMgr.registerListener(this, sensorGyro, SensorManager.SENSOR_DELAY_NORMAL);
		
		if(!accelSupported) {
			Toast.makeText(this, "No accelerometer detected", Toast.LENGTH_SHORT).show();;
		}
		else if(!gyroSupported) {
			Toast.makeText(this, "No gyroscope detected", Toast.LENGTH_SHORT).show();;
		}
	}
	
	protected void calibrate() {
		moved = 0;
		speed = 0;
		accelDiff = accel+accelDiff;
		accel = 0;
		distanceNeeded = 0;
	}
	
	public void onAccuracyChanged(Sensor sensor, int sensorAccuracy) {
		// this method is called very rarely, so we don't have to
		// limit our updates as we do in onSensorChanged(...)
		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			switch (sensorAccuracy) {
			case SensorManager.SENSOR_STATUS_UNRELIABLE:
				accelAccuracy = "unreliable";
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
				accelAccuracy = "low";
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
				accelAccuracy = "medium";
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
				accelAccuracy = "high";
				break;
			}
		}
		if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			switch (sensorAccuracy) {
			case SensorManager.SENSOR_STATUS_UNRELIABLE:
				gyroAccuracy = "unreliable";
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
				gyroAccuracy = "low";
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
				gyroAccuracy = "medium";
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
				gyroAccuracy = "high";
				break;
			}
		}
	}
 
	public void onSensorChanged(SensorEvent event) {
		int sensorType = event.sensor.getType();
		float[] accelResult = new float[3];
		float[] acceleration;
		float[] gyroResult = new float[3];
		float[] angles;
		float move = 0;

		if (sensorType == Sensor.TYPE_ACCELEROMETER) {
			acceleration = event.values;
			float kFilteringFactor = .88f;

			a[0] = acceleration[0] * kFilteringFactor + a[0] * (1.0f - kFilteringFactor);
			a[1] = acceleration[1] * kFilteringFactor + a[1] * (1.0f - kFilteringFactor);
			a[2] = acceleration[2] * kFilteringFactor + a[2] * (1.0f - kFilteringFactor);
			accelResult[0] = acceleration[0] - a[0];
			accelResult[1] = acceleration[1] - a[1];
			accelResult[2] = acceleration[2] - a[2];

			accel = (float) (Math.sqrt((accelResult[0] * accelResult[0]) + (accelResult[1]
					* accelResult[1]) + (accelResult[2] * accelResult[2])) - accelDiff);
			
			curAccel += accel;
			accelAvg = (accelAvg + ((accelPrev + curAccel) / 2) / 2);
			accelPrev = curAccel;
			
			accelUpdates++;
			long curTime = (event.timestamp) - lastUpdate;
			if (curTime >= 1000000000) {
				curAccel = curAccel / accelUpdates;
				//curAccel += .02;

				if (curAccel <= 3 && curAccel >= -3) {
					if(curAccel <= .01 && curAccel >= -.01) {
						curAccel = 0;
					}
					
					lastUpdate = event.timestamp;
					speed = (float) (curAccel * curTime * 10e-10);
					move = (float) (speed * curTime * 10e-10);
					moved += move;
					
					curAccel = 0;
					accelUpdates = 0;
				}
			}
		}
		if(sensorType == Sensor.TYPE_GYROSCOPE) {
			angles = event.values;
			float kFilteringFactor = .88f;

			tempAngles[0] = angles[0] * kFilteringFactor + tempAngles[0] * (1.0f - kFilteringFactor);
			tempAngles[1] = angles[1] * kFilteringFactor + tempAngles[1] * (1.0f - kFilteringFactor);
			tempAngles[2] = angles[2] * kFilteringFactor + tempAngles[2] * (1.0f - kFilteringFactor);
			gyroResult[0] = angles[0] - tempAngles[0];
			gyroResult[1] = angles[1] - tempAngles[1];
			gyroResult[2] = angles[2] - tempAngles[2];
			
			newAngles[0] = gyroResult[0];
			newAngles[1] = gyroResult[1];
			newAngles[2] = gyroResult[2];
		}
	}
	
	/*
	 * Measures distance in meters
	 */
	
	public void measureDistanceMoved(float distance) {
		calibrate();
		distanceNeeded = distance;
		final Handler handler = new Handler();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							CameraSurfaceActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (proximity(moved, .1f)) {
										dot.setImageResource(R.drawable.yellow_dot);
									} else if (proximity(moved, 1f)) {
										dot.setImageResource(R.drawable.orange_dot);
									} else {
										dot.setImageResource(R.drawable.red_dot);
									}
								}
							});
						}
					});
					if (isSecondClick) {
						break;
					}
					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	private void captureAngles() {
		if(isFirstClick) {
			//add before modifiers to angles
		} else {
			//add after modifiers to angles
		}
	}
	
	private boolean proximity(float distance, float gap) {
		if((distanceNeeded - distance) > gap )
			return false;
		
		return true;
	}
	
	public static Camera getCameraInstance() {
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
				if (result == null)
					result = size;
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
	
	public void startPreview() {
		camera.stopPreview();
		try {
			camera.setPreviewDisplay(surfaceHolder);
		} catch (IOException e) {
			log("exception thrown in called startPreview");
			e.printStackTrace();
		}
		camera.startPreview();
	}
	
	private void releaseCamera() {
		if(camera != null) {
			camera.release();
			camera = null;
		}
	}
	
	protected File getOutputMediaFile(int type) {
		/*
		 * log("Saving media file"); File mediaStorageDir = new File
		 * (Environment.getExternalStoragePublicDirectory(
		 * Environment.DIRECTORY_DCIM), "MyTestCamera");
		 * if(!mediaStorageDir.exists()) { log("New sotrage doesnt exist");
		 * if(!mediaStorageDir.mkdirs()) {
		 * log("null returned when making directory"); return null; } }
		 * if(Environment
		 * .getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
		 * log("Storage is mounted"); } log(mediaStorageDir.getPath());
		 */
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		
		if (type == FinishedActivity.MEDIA_TYPE_IMAGE) {
			try {
				//mediaFile = new File(mediaStorageDir.getPath() + File.separator + beforeAfter + "IMG_" + timeStamp + ".jpg");
				return File.createTempFile(File.separator + beforeAfter + "_IMG_" + timeStamp, ".jpg");
			} catch (IOException e) {
				log("Image saving IOException thrown");
				e.printStackTrace();
			}
		} else if(type == FinishedActivity.MEDIA_TYPE_VIDEO) {
			try {
				//mediaFile = new File(mediaStorageDir.getPath() + File.separator + beforeAfter + "VID_" + timeStamp + ".mp4");
				//mediaFile = File.createTempFile(File.separator + beforeAfter + "VID_" + timeStamp, ".mp4", mediaStorageDir);
				return File.createTempFile(File.separator + beforeAfter + "_VID_" + timeStamp, ".mp4");
			} catch (IOException e) {
				log("Video saving IOException thrown");
				e.printStackTrace();
			}
		} else if(type == FinishedActivity.MEDIA_TYPE_TEXT) {
			try {
				return File.createTempFile(File.separator + beforeAfter + "_TXT_" + timeStamp, ".txt");
			} catch (IOException e) {
				log("Text saving IOException thrown");
				e.printStackTrace();
			}
		}
		
		return null;
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		camera = getCameraInstance();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(surfaceHolder.getSurface() == null) {
			return;
		}
		
		Camera.Parameters params = camera.getParameters();
		Camera.Size size = getBestPreviewSize(params, width, height);
		
		if(size != null) {
			params.setPreviewSize(size.width, size.height);
			camera.setParameters(params);
		}
		
		try {
			camera.stopPreview();
			previewRunning = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			camera.setPreviewDisplay(surfaceHolder);
			camera.startPreview();
			previewRunning = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(previewRunning)
			camera.stopPreview();
		releaseCamera();
		previewRunning = false;
	}
	
	@Override
	public void onClick(View v) {
		if(isFirstClick) {
			dot.setImageResource(R.drawable.red_dot);
			measureDistanceMoved(INCHES_FOUR);
		} else
			isSecondClick = true;
		captureAngles();
	}
	
	@Override
	public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
		//intent.putExtra("data file", Uri.fromFile(dataFile));
		super.startActivityForResult(intent, requestCode, options);
	}
	
	private static void log(String data) {
		Log.d("CameraSurfaceActivity", data);
	}
	
}
