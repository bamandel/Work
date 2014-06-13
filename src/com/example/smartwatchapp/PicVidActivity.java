package com.example.smartwatchapp;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.os.Build;

public class PicVidActivity extends Activity {
	private static Class<?> nextClass = null;
	
	private static TextView picBefore, vidAfter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pic_vid);
		
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pic_vid, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_pic_vid, container, false);
			
			picBefore = (TextView) rootView.findViewById(R.id.tvPicBefore);
			vidAfter = (TextView) rootView.findViewById(R.id.tvVidAfter);
			
			picBefore.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(picBefore.getText().equals(getString(R.string.pic_question))) {
						log("Pic Button Pressed");
						picBefore.setText(getString(R.string.before_question));
						vidAfter.setText(getString(R.string.after_question));
						nextClass = PictureCameraActivity.class;
					}
					else {
						log("Before Button Pressed");
						startActivity(new Intent(getActivity(), nextClass)
							.putExtra("before after", "before"));
					}
				}
			});
			
			vidAfter.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(vidAfter.getText().equals(getString(R.string.vid_question))) {
						log("Vid Button Pressed");
						picBefore.setText(getString(R.string.before_question));
						vidAfter.setText(getString(R.string.after_question));
						nextClass = VideoCameraActivity.class;
					}
					else {
						log("After Button Pressed");
						startActivity(new Intent(getActivity(), nextClass)
							.putExtra("before after", "after"));
					}
				}
			});
			
			return rootView;
		}
	}

	@Override
	public void onBackPressed() {
		if(picBefore.getText().equals("Before?") || vidAfter.getText().equals("After")) {
			log("Fake back pressed");
			picBefore.setText(getString(R.string.pic_question));
			vidAfter.setText(getString(R.string.vid_question));
		}
		else {
			log("Real back Pressed");
			super.onBackPressed();
		}
	}
	
	private static void log(String input) {
		Log.d("PicVidActivity", input);
	}

}
