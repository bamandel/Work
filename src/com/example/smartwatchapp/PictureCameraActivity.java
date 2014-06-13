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
		log("Taking Picture");
		takePicture();
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
				startActivityForResult(new Intent(PictureCameraActivity.this, FinishedActivity.class)
					.putExtra("first file", Uri.fromFile(firstPic))
					.putExtra("second file", Uri.fromFile(secondPic))
					.putExtra("identifier", "picture")
					, FinishedActivity.MEDIA_TYPE_IMAGE);
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
		super.onBackPressed();
		isFirstClick = true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log("in onActivityresult");
		
		if(resultCode == RESULT_OK && data != null) {
			if(requestCode == FinishedActivity.MEDIA_TYPE_IMAGE) {
				if(data.getStringExtra("response").equals("yes")) {
					log("response was yes");
					
					startActivity(new Intent(this, SoundRecordActivity.class)
						.putExtra("before after", beforeAfter));
					
				}
				if(data.getStringExtra("response").equals("no")) {
					log("response was no");
					isFirstClick = true;
				}
			}
		}
		else {
			log("result cancelled");
		}
		
	}
	
	private static void log(String data) {
		Log.d("PictureCameraActivity", data);
	}
}
