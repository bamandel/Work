package com.example.smartwatchapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

public class UploadService extends Service implements SensorEventListener{
	public static final long SECOND = 1000;
	public static final long MINUTE = SECOND * 60;
	public static final long HOUR = MINUTE * 60;
	
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothService btService;
	
	private BluetoothDevice mDevice = null;
	//private static String deviceName = null;
	
	private String dataFile = null;
	private static String dataFileRef = "";
	private String deviceAddress = "00:00:00:00:00:00";
	
	private  Context mContext = null;
	
	private SharedPreferences settings;
	private SharedPreferences.Editor edit;
	
	private boolean isFirstVideo = true;
	
	private File sensorFile = null;
	private FileOutputStream sensorFos = null;
	private String sensorData = "";
	
	private WakeLock wakeLock = null;
	
	private Timer timer = new Timer();
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
	//private float moved = 0;
	
	//Total speed moved
	//private float speed = 0;
	
	//current accleretion (Without altercations/weights)
	private float accel = 0;
	
	//A weight to help acceleration more accurate
	private float accelDiff = 0;
	
	//Another weight to help with accuracy
	private float accelAvg = 0;
	
	//value used to determine accelAvg
	private float accelPrev = 0;
	
	//Distance we want the user to move
	//private float distanceNeeded = 0;
	
	//Arrays that hold the data for acceleration and direction
	private float[] a = new float[3];
	//private int[] directionCounter = {0, 0, 0}; // The higher a specific value is the further they moved that way
	
	//Values used as a weight to pinpoint true accleration
	private long lastUpdate = 0;
	private float curAccel = 0;
	private int accelUpdates = 0;
	
	/*
	 * Values used for storing angles and angle weights
	 */
	
	private float[] newAngles = new float[3];

	private float[] tempAngles = new float[3];
	
	//Strings to store data for deciphering pictures
	@SuppressWarnings("unused")
	private String accelAccuracy;
	@SuppressWarnings("unused")
	private String gyroAccuracy;
	@SuppressWarnings("unused")
	private String direction;
	
	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler() {		
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case BluetoothService.MESSAGE_STATE_CHANGE:
				switch(msg.arg1) {
				case BluetoothService.STATE_CONNECTED:
					Log.d("Service", "Connected");
					if(!btService.getDevice().getAddress().equals(deviceAddress)) {
						Log.d("Service", "Settingn up new address");
						deviceAddress = btService.getDevice().getAddress();
						edit.putString("device address", deviceAddress);
						edit.apply();
					}
					break;
				case BluetoothService.STATE_CONNECTING:
					Log.d("Service", "Connecting");
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:
					Log.d("Service", "Not Connected");
					break;
				}
				break;
			case BluetoothService.MESSAGE_WRITE:
				//byte[] writeBuf = (byte[]) msg.obj;
				break;
			case BluetoothService.MESSAGE_READ:
				//byte[] readBuf = (byte[]) msg.obj;
				//String readMessage = new String(readBuf, 0, msg.arg1);
				//Do something with read message
				break;
			case BluetoothService.MESSAGE_DEVICE_NAME:
				//deviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
				break;
			case BluetoothService.MESSAGE_TOAST:
				Toast.makeText(mContext, msg.getData().getString(BluetoothService.TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	private BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			Log.d("Service", "Broadcast " + action + " recieved");
			
			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
				Log.d("Service", "new device found");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				mDevice = device;
				
				if(!device.getAddress().equals(deviceAddress)) {
					deviceAddress = device.getAddress();
					edit.putString("device address", deviceAddress);
					edit.apply();
				}
				
				connectDevice(mDevice, true);
				
				Log.d("Service", "Found device");
			} else if(action.equals("data")) {
				Log.d("Service", "getting data");
				dataFile = intent.getStringExtra("datafile");
				
				if(!deviceAddress.equals("00:00:00:00:00:00") && btService.getState() != BluetoothService.STATE_CONNECTED) {
					Log.d("Service", "device connecting to " + deviceAddress);
					connectDevice(deviceAddress, false);
				} else {
					Log.d("Service", deviceAddress);
					Log.d("Service", dataFile);
				}
				uploadData();
				
			}else if(action.equals(Intent.ACTION_SCREEN_OFF)) {
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						unregisterListener();
						registerListener();
					}
				}, 500);
			}
		}
	};
	
	public UploadService() {
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("Service", "Service started");
		mContext = getApplicationContext();
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		dataFile = intent.getStringExtra("datafile");
		dataFileRef = dataFile;
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		
		sensorFile = new File(dataFile + File.separator + "TXT_" + timeStamp + ".txt");
		
		try {
			sensorFos = new FileOutputStream(sensorFile);
		} catch (FileNotFoundException e) {
			Log.d("Service", "Faield opening output stream");
			e.printStackTrace();
		}
		
		registerReceiver(deviceReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		registerReceiver(deviceReceiver, new IntentFilter("data"));
		
		settings = mContext.getSharedPreferences("settings", MODE_PRIVATE);
		edit = settings.edit();
		
		btService = new BluetoothService(handler);
		
		if(btService != null) {
			if(btService.getState() == BluetoothService.STATE_NONE) {
				btService.start();
				Log.d("Service", "Bluetoothservice started");
			}
		}
		
		deviceAddress = settings.getString("device address", "00:00:00:00:00:00");
		
		if(!deviceAddress.equals("00:00:00:00:00:00") && btService.getState() != BluetoothService.STATE_CONNECTED) {
			Log.d("Service", "device connecting");
			connectDevice(deviceAddress, false);
		} else {
			Log.d("Service", deviceAddress);
		}
		
		sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccel = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
		sensorGyro = sensorMgr.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
		
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Service");
		
		registerReceiver(deviceReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		
		registerListener();
		wakeLock.acquire();
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				isFirstVideo = false;
				uploadData();
			}
		}, HOUR);
		
		return START_NOT_STICKY;
	}
	
	private void connectDevice (BluetoothDevice newDevice, boolean secure) {
		BluetoothDevice device = bluetoothAdapter.getRemoteDevice(newDevice.getAddress());
		btService.connect(device, secure);
	}
	
	private void connectDevice (String newAddress, boolean secure) {
		BluetoothDevice device = bluetoothAdapter.getRemoteDevice(newAddress);
		btService.connect(device, secure);
	}
	
	private void sendData(File data) {
		if(btService.getState() != BluetoothService.STATE_CONNECTED) {
			Log.d("Service", "Not connected");
			return;
		}
		btService.write(data);
	}
	
	private void registerListener() {
		boolean accelSupported = sensorMgr.registerListener(this, sensorAccel,
				SensorManager.SENSOR_DELAY_NORMAL);
		boolean gyroSupported = sensorMgr.registerListener(this, sensorGyro,
				SensorManager.SENSOR_DELAY_NORMAL);

		if (!accelSupported) {
			Toast.makeText(this, "No accelerometer detected",Toast.LENGTH_SHORT).show();
		} else if (!gyroSupported) {
			Toast.makeText(this, "No gyroscope detected", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void unregisterListener() {
		if (sensorMgr != null) {
			sensorMgr.unregisterListener(this, sensorAccel);
			sensorMgr.unregisterListener(this, sensorGyro);
		}
	}
	
	private void uploadData() {
		Log.d("Service", "uploading data");
		
		if (btService.getState() == BluetoothService.STATE_CONNECTED) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Log.d("Service", "Getting remaining files");
					String[] parent = dataFile.split("/DAT_");
					File directory = new File(parent[0]);
					File[] directories = directory.listFiles();
					
					if(directories == null) {
						Log.d("Service", "Not a directory");
					} else if(directories.length == 0) {
						Log.d("Service", "No directories");
					} else {
						for(File files : directories) {
							File[] contents = files.listFiles();
							if(contents == null) {
								Log.d("Service", "Not a directory");
							} else if(contents.length == 0) {
								Log.d("Service", "No files in directory");
							} else {
								for(File newFiles : contents) {
									if (!isFirstVideo) {
										sendData(newFiles);
										newFiles.delete();
									}
								}
							}
							files.delete();
						}
					}
					if (isFirstVideo) {
						isFirstVideo = false;
					} else {
						UploadService.this.stopSelf();
					}
				}
			}).start();
		} else {
			Log.d("Service", "Not connected when trying to send data");
			if (isFirstVideo) {
				isFirstVideo = false;
			} else {
				stopSelf();
			}
		}
	}
	
	public static String getDataPath() {
		return dataFileRef;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d("Service", "Ending Service");
		
		try {
			sensorFos.flush();
			sensorFos.close();
		} catch (IOException e) {
			Log.d("Service", "Failed to close output stream");
			e.printStackTrace();
		}
		
		if(btService != null)
			btService.stop();
		
		unregisterReceiver(deviceReceiver);
		unregisterListener();
		wakeLock.release();
		timer.cancel();
		
		Log.d("Service", "Service ended");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void onSensorChanged(SensorEvent event) {
		int sensorType = event.sensor.getType();
		float[] accelResult = new float[3];
		float[] acceleration;
		float[] gyroResult = new float[3];
		float[] angles;
		
		if (sensorType == Sensor.TYPE_ACCELEROMETER) {
			acceleration = event.values;
			float kFilteringFactor = .88f;

			a[0] = acceleration[0] * kFilteringFactor + a[0] * (1.0f - kFilteringFactor);
			a[1] = acceleration[1] * kFilteringFactor + a[1] * (1.0f - kFilteringFactor);
			a[2] = acceleration[2] * kFilteringFactor + a[2] * (1.0f - kFilteringFactor);
			accelResult[0] = acceleration[0] - a[0];
			accelResult[1] = acceleration[1] - a[1];
			accelResult[2] = acceleration[2] - a[2];
			
			sensorData += "accl:" + " x:" + accelResult[0] + " y:" + accelResult[1] + " z:" + accelResult[2] + "\n";
			
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
			
			sensorData += "gyro:" + " x:" + newAngles[0] + " y:" + newAngles[1] + " z:" + newAngles[2] + "\n";
			
			long curTime = (event.timestamp) - lastUpdate;
			if (curTime >= 1000000000) {
				
				lastUpdate = event.timestamp;
			}
		}
		
		try {
			sensorFos.write(sensorData.getBytes());
		} catch (IOException e) {
			Log.d("Service", "Failed to write string to file");
			e.printStackTrace();
		}
		sensorData = "";
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
}