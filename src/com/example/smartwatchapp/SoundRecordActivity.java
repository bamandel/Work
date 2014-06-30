package com.example.smartwatchapp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SoundRecordActivity extends Activity implements OnClickListener{
	
	private static ImageView record;
	private static TextView finish;
	private static ProgressBar progressBar;
	
	private boolean isClicked = false;
	
	private int progressStatus = 0;
	private static long time = 0;
	
	private Handler handler;
	
	private File audio;
	
	private MediaRecorder recorder = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sound_record);
		
		handler = new Handler();
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
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
			View rootView = inflater.inflate(R.layout.fragment_sound_record, container, false);
			
			record = (ImageView) rootView.findViewById(R.id.ivRecord);
			record.setOnClickListener((OnClickListener) getActivity());
			
			finish = (TextView) rootView.findViewById(R.id.tvFinish);
			finish.setOnClickListener((OnClickListener) getActivity());
			
			progressBar = (ProgressBar) rootView.findViewById(R.id.pbTimer);
			
			return rootView;
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.ivRecord: {
			time = System.currentTimeMillis();
			new Thread(new Runnable() {
				@Override
				public void run() {
					while(progressStatus < 100 && isClicked) {
						progressStatus = startProgressBar();
						handler.post(new Runnable() {
							@Override
							public void run() {
								progressBar.setProgress(progressStatus);
							}
						});
						if(progressStatus >= 100) {
							SoundRecordActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									record.setImageResource(R.drawable.record);
									record(false);
									isClicked = false;
								}
							});
							break;
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
			record(isClicked);
			break;
		}
		case R.id.tvFinish: {
			//Write data here
			audio = getOutputMediaFile(FinishedActivity.MEDIA_TYPE_AUDIO);
			
			Intent intent = new Intent(this, FinishedActivity.class);
			intent.putExtra("identifier", "audio");
			if(audio != null)
				intent.putExtra("audio file", Uri.fromFile(audio));
			setResult(RESULT_OK, intent );
			finish();
			return;
		}
		}
	}
	
	/*
	private void record() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.example.smartwatchapp");
		
		if (SpeechRecognizer.isRecognitionAvailable(this)) {
			recognizer = SpeechRecognizer.createSpeechRecognizer(this);
			listener = new RecognitionListener() {

				@Override
				public void onResults(Bundle results) {
					ArrayList<String> voiceResults = results
							.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

					float[] confidenceResults = results
							.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

					int temp = 0;
					float past = 0;
					for (int i = 0; i < confidenceResults.length; i++) {
						if (confidenceResults[i] > past) {
							past = confidenceResults[i];
							temp = i;
						}
					}

					log("Top result is: " + temp);

					if (voiceResults == null) {
						log("No results");
					} else {
						log("found voice results");
						//Store them in a file
					}
				}

				@Override
				public void onReadyForSpeech(Bundle params) {
				}

				@Override
				public void onBeginningOfSpeech() {
				}

				@Override
				public void onRmsChanged(float rmsdB) {
				}

				@Override
				public void onBufferReceived(byte[] buffer) {
				}

				@Override
				public void onEndOfSpeech() {
				}

				@Override
				public void onError(int error) {
				}

				@Override
				public void onPartialResults(Bundle partialResults) {
				}

				@Override
				public void onEvent(int eventType, Bundle params) {
				}
			};

			recognizer.setRecognitionListener(listener);

			if (!isClicked) {
				recognizer.startListening(intent);
				record.setImageResource(R.drawable.record_pressed);
				isClicked = true;
			} else {
				recognizer.stopListening();
				record.setImageResource(R.drawable.record);
				isClicked = false;
			} 
		} else {
			log("No Speech Recognizer available");
		}
	}
*/
	
	private void record(boolean start) {
		if(start && !isClicked) {
			record.setImageResource(R.drawable.record_pressed);
			isClicked = true;
			startRecording();
		} else {
			record.setImageResource(R.drawable.record);
			isClicked = false;
			stopRecording();
		}
	}
	
	private void startRecording() {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		
		audio = getOutputMediaFile(CameraSurfaceActivity.MEDIA_TYPE_AUDIO);
		recorder.setOutputFile(audio.toString());
		
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		
		try {
			recorder.prepare();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		recorder.start();
	}
	
	private void stopRecording() {
		recorder.stop();
		recorder.release();
		recorder = null;
	}
	
	private int startProgressBar() {
		long curTime = System.currentTimeMillis();
		log("Increment");
		return (int) ((curTime - time) / 100);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if(recorder != null) {
			recorder.release();
			recorder = null;
		}
	}
	
	private File getOutputMediaFile(int type) {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		
		if (type == FinishedActivity.MEDIA_TYPE_AUDIO) {
			log("saving audio file");
			try {
				return File.createTempFile(File.separator + "_AUD_" + timeStamp, ".wav");
			} catch (IOException e) {
				log("Audio saving IOException thrown");
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	private static void log(String data) {
		Log.d("SoundRecordActivity", data);
	}
	
}
