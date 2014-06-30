package com.example.smartwatchapp;

import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class VideoCameraActivity extends CameraSurfaceActivity{
	private MediaRecorder recorder;
	
	private LocalBroadcastManager broadcast;
	
	private ProgressBar timeBar;
	private TextView videoTimer;
	
	private long time = 0;
	private int progressStatus = 0;
	private int timer = 0;
	private boolean running = false;
	
	private Handler handler = new Handler();
	
	private BroadcastReceiver surfaceCreated = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("surface found")) {
				surfaceView.setClickable(true);
				surfaceView.performClick();
			}
		}
	};
	
	@Override
	protected void onResume() {
		super.onResume();
		
		recorder = new MediaRecorder();
		
		identifier = MEDIA_TYPE_VIDEO;
		
		timeBar = (ProgressBar) findViewById(R.id.pbTimer);
		videoTimer = (TextView) findViewById(R.id.tvVideoTime);
		
		broadcast = LocalBroadcastManager.getInstance(this);
		broadcast.registerReceiver(surfaceCreated, new IntentFilter("surface found"));
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (recorder != null) {
			releaseMediaRecorder();
		}
		broadcast.unregisterReceiver(surfaceCreated);
	}

	private void releaseMediaRecorder() {
		log("releasing mediaRecorder");
		if(recorder != null) {
			if(running) {
				recorder.stop();
			}
			recorder.reset();
			recorder.release();
			recorder = null;
			//camera.lock();
		}
	}
	
	private boolean initCamera() {
		log("Init camera entered");
		
		//Think bug here when retaking video
		camera.release();
		recorder.release();
		
		camera = getCameraInstance();
		recorder = new MediaRecorder();
		
		log("Items reinstantiated");
		
		//Is this needed?
		//camera.lock();
		camera.unlock();
		
		log("Camera unlocked");
		
		recorder.setCamera(camera);
		recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
		recorder.setOutputFile(getOutputMediaFile(CameraSurfaceActivity.MEDIA_TYPE_VIDEO).toString());
		
		recorder.setPreviewDisplay(surfaceHolder.getSurface());
		
		log("Settings set");
		
		try {
			recorder.prepare();
		} catch (IllegalStateException e) {
			log("Not Prepared correctly Illegal state");
			releaseMediaRecorder();
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			log("Not prepared correctly IO");
			releaseMediaRecorder();
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			log("Random error");
			e.printStackTrace();
		}
		
		log("Prepared correctly");
		
		return true;
	}
	
	public void onClick(View v) {
		super.onClick(v);
		if (v.getId() == R.id.ibCamera) {
			log("starting next activity");
			finish();
			startActivity(new Intent(this, PictureCameraActivity.class).putExtra("isVideo", false));
		}
		else if(surfaceView.isClickable()){
			if (isFirstClick) {
				if (initCamera()) {
					try {
						recorder.start();
						running = true;
						time = System.currentTimeMillis(); 
						new Thread(new Runnable() {
							@Override
							public void run() {
								while(progressStatus < 100) {
									progressStatus = startProgressBar();
									handler.post(new Runnable() {
										@Override
										public void run() {
											timeBar.setProgress(progressStatus);
											if ((timeBar.getProgress() / 7.5) > timer) {
												timer++;
											}
											videoTimer.setText(timer + "/15");
										}
									});
									if(!running) {
										break;
									}
									if(progressStatus >= 100) {
										VideoCameraActivity.this.runOnUiThread(new Runnable() {
											@Override
											public void run() {
												surfaceView.performClick();
											}
										});
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
						isFirstClick = false;
						log("Camera recording");
					} catch (Exception e) {
						log("Start didn't work");
						e.printStackTrace();
					}
				} else {
					log("Camera failed to record");
					finish();
				}
			} else {
				try {
					recorder.stop();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (RuntimeException ee) {
					// Delete bad file
					ee.printStackTrace();
				}
				isFirstClick = true;
				running = false;
				log("Recording stopped");
			}
		}
	}
	
	private int startProgressBar() {
		long curTime = System.currentTimeMillis();
		return (int) ((curTime - time) / 150);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if(!isFirstClick) {
			recorder.stop();
			isFirstClick = false;
		} else {
			finish();
		}
	}

	private void log(String data) {
		Log.d("VideoCameraActivity", data);
	}
	
}
