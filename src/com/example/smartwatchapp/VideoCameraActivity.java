package com.example.smartwatchapp;

import java.io.File;
import java.io.IOException;

import android.content.Intent;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class VideoCameraActivity extends CameraSurfaceActivity{
	private MediaRecorder recorder;
	
	private File videoFile = null;
	
	@Override
	protected void onResume() {
		super.onResume();
		
		recorder = new MediaRecorder();
	}
	
	private void releaseMediaRecorder() {
		log("releasing mediaRecorder");
		if(recorder != null) {
			recorder.reset();
			recorder.release();
			recorder = null;
			camera.lock();
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
		camera.lock();
		camera.unlock();
		
		log("Camera unlocked");
		
		recorder.setCamera(camera);
		recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
		
		videoFile = getOutputMediaFile(FinishedActivity.MEDIA_TYPE_VIDEO);
		recorder.setOutputFile(videoFile.toString());
		
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
		if(isFirstClick) {
			if(initCamera()) {
				try {
					recorder.start();
					isFirstClick = false;
					Toast.makeText(this, "Slowly pan camera until perimeter is yellow", Toast.LENGTH_SHORT).show();
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
			recorder.stop();
			isFirstClick = true;
			log("Recording stopped");
			startActivityForResult(new Intent(this, FinishedActivity.class)
				.putExtra("first file", Uri.fromFile(videoFile))
				.putExtra("second file", Uri.fromFile(videoFile))
				.putExtra("identifier", "video")
				, FinishedActivity.MEDIA_TYPE_VIDEO);
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log("in onActivityresult");
		
		if(resultCode == RESULT_OK && data != null) {
			if (requestCode == FinishedActivity.MEDIA_TYPE_VIDEO) {
				if (data.getStringExtra("response").equals("yes")) {
					// Store data and finish
					log("rsult OK Yes entered");

					startActivity(new Intent(this, PicVidActivity.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				}
				if (data.getStringExtra("response").equals("no")) {
					// Retake video
					log("result OK No entered");
				}
			}
		}
		else {
			log("result cancelled");
		}
		
	}
	
	private void log(String data) {
		Log.d("VideoCameraActivity", data);
	}
	
}
