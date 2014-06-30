package com.example.smartwatchapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class PictureCameraActivity extends CameraSurfaceActivity{
	private static File firstPic, secondPic;
	private byte[] tempPic;
	
	private final Camera.PictureCallback callBack = new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			log("entered picture taken");
			if (isFirstClick) {
				log("setting up first pic");

				tempPic = data;
				firstPic = getOutputMediaFile(FinishedActivity.MEDIA_TYPE_IMAGE);

				log("First pic setup");
			} else {
				log("Setting up second pic");
				
				
				secondPic = getOutputMediaFile(FinishedActivity.MEDIA_TYPE_IMAGE);
				
				/*
				 * This could probably be implemented better
				 * but is working for now (I THINK)
				 */
				
				try {
					FileOutputStream outputFirst = new FileOutputStream(firstPic);
					outputFirst.write(tempPic);
					outputFirst.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					FileOutputStream outputSecond = new FileOutputStream(secondPic);
					outputSecond.write(data);
					outputSecond.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				log("second pic setup");
			}
		}
	};

	@Override
	public void onClick(View v) {
		super.onClick(v);
		if (v.getId() == R.id.ibCamera) {
			finish();
			startActivity(new Intent(this, VideoCameraActivity.class).putExtra("isVideo", true));
		} else {
			log("Taking Picture");
			takePicture();
		}
	}
	
	public void takePicture() {
		TakePictureTask picture = new TakePictureTask();
		picture.execute();
	}
	
	private class TakePictureTask extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if(isFirstClick) {
				camera.startPreview();
				Toast.makeText(PictureCameraActivity.this, "Slowly pan camera until perimeter is yellow", Toast.LENGTH_SHORT).show();
				isFirstClick = false;
			}
			else {
				Intent intent = new Intent(PictureCameraActivity.this, SoundRecordActivity.class);
				
				intent.putExtra("first file", Uri.fromFile(firstPic))
						.putExtra("second file", Uri.fromFile(secondPic))
						.putExtra("identifier", "picture");

				startActivityForResult(new Intent(PictureCameraActivity.this, SoundRecordActivity.class), CameraSurfaceActivity.MEDIA_TYPE_AUDIO);
				isFirstClick = true;
			}
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			camera.takePicture(null, null, callBack);
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
	}
	
	@Override
	public void onBackPressed() {
		if (!isFirstClick) {
			finish();
			startActivity(new Intent(this, VideoCameraActivity.class).putExtra("isVideo", true));
		} else {
			isFirstClick = true;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log("in onActivityresult");
		
		if(resultCode == RESULT_OK) {
			if(requestCode == CameraSurfaceActivity.MEDIA_TYPE_AUDIO) {
			}
		}
		else {
			log("result cancelled");
			isFirstClick = true;
		}
		
	}
	
	private static void log(String data) {
		Log.d("PictureCameraActivity", data);
	}
}
