package com.example.smartwatchapp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SoundRecordActivity extends Activity implements OnClickListener{
	
	private static ImageView record;
	private static TextView next;
	private static EditText sentence;
	private static ProgressBar progressBar;
	
	private boolean isClicked = false;
	
	private int progressStatus = 0;
	private static long time = 0;
	
	private Handler handler;
	
	private SpeechRecognizer recognizer;
	private RecognitionListener listener;
	
	private String phrase = "";
	
	private String beforeAfter;
	
	private File audio;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sound_record);
		
		handler = new Handler();
		beforeAfter = getIntent().getStringExtra("before after");
		
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
			
			next = (TextView) rootView.findViewById(R.id.tvNext);
			next.setOnClickListener((OnClickListener) getActivity());
			
			sentence = (EditText) rootView.findViewById(R.id.etPhrase);
			
			progressBar = (ProgressBar) rootView.findViewById(R.id.pbTimer);
			
			return rootView;
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.ivRecord: {
			record();
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
									recognizer.stopListening();
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
			break;
		}
		case R.id.tvNext: {
			startActivityForResult(new Intent(this, FinishedActivity.class)
				.putExtra("audio file", Uri.fromFile(audio))
				.putExtra("identifier", "audio")
				, FinishedActivity.MEDIA_TYPE_AUDIO);
			break;
		}
		}
	}
	
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
						phrase = voiceResults.get(temp);
						sentence.setText(phrase);
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
	
	private int startProgressBar() {
		long curTime = System.currentTimeMillis();
		
		return (int) ((curTime - time) / 100);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	private File getOutputMediaFile(int type) {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		
		if (type == FinishedActivity.MEDIA_TYPE_AUDIO) {
			log("saving audio file");
			try {
				return File.createTempFile(File.separator + beforeAfter + "_AUD_" + timeStamp, ".wav");
			} catch (IOException e) {
				log("Audio saving IOException thrown");
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == RESULT_OK && data != null) {
			if (requestCode == FinishedActivity.MEDIA_TYPE_AUDIO) {
				if (data.getStringExtra("response").equals("yes")) {
					// Store data and finish
					log("rsult OK Yes entered");

					/*
					 * Use Bluetooth here to send mediaFile to Phone
					 */
					
					audio = getOutputMediaFile(FinishedActivity.MEDIA_TYPE_AUDIO);
					
					startActivity(new Intent(this, PicVidActivity.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				}
				if (data.getStringExtra("response").equals("no")) {
					log("result OK No entered");
				}
			}
		}
		else {
			log("result cancelled");
		}
	}
	
	private static void log(String data) {
		Log.d("SoundRecordActivity", data);
	}
	
}
