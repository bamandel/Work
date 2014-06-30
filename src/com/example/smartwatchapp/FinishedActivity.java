package com.example.smartwatchapp;

import java.io.File;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class FinishedActivity extends Activity implements OnClickListener{
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
	
	private static BluetoothAdapter bluetoothAdapter = null;
	private BluetoothService btService = null;
	
	private static Uri firstFilePath = null;
	private static Uri secondFilePath = null;
	private static Uri dataFilePath = null;
	private static int identifier = -1;
	
	private static final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case BluetoothService.MESSAGE_STATE_CHANGE:
				switch(msg.arg1) {
				case BluetoothService.STATE_CONNECTED:
					Log.d("Main activity", "Connected");
					break;
				case BluetoothService.STATE_CONNECTING:
					Log.d("MainActivity", "Connecting");
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:
					Log.d("MainActivity", "Not Connected");
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
				//Toast.makeText(getApplicationContext(),msg.getData().getString(BluetoothService.TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_finished);
		
		identifier = getIntent().getIntExtra(IDENTIFIER, -1);
		
		//All paths will contain a file
		firstFilePath = getIntent().getParcelableExtra(FIRST_FILE);
		if(firstFilePath == null) {
			setResult(RESULT_OK, new Intent().putExtra("response", "yes"));
			finish();
			return;
		}
		
		//only video will need adiitional data
		if(identifier == MEDIA_TYPE_VIDEO) {
			dataFilePath = getIntent().getParcelableExtra(DATA_FILE);
		}
		
		//only picture will need additional data AND another picture file
		if(identifier == MEDIA_TYPE_IMAGE) {
			secondFilePath = getIntent().getParcelableExtra(SECOND_FILE);
			dataFilePath = getIntent().getParcelableExtra(DATA_FILE);
		}
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_finished, container, false);
			
			TextView yes = (TextView) rootView.findViewById(R.id.tvYes);
			TextView no = (TextView) rootView.findViewById(R.id.tvNo);
			yes.setOnClickListener((OnClickListener) getActivity());
			no.setOnClickListener((OnClickListener) getActivity());
			
			return rootView;
		}
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.tvYes) {
			log("yes pressed");
			setResult(RESULT_OK, new Intent().putExtra("response", "yes"));
			
			if(btService.getState() != BluetoothService.STATE_CONNECTED) {
				log("Not Connected");
				Toast.makeText(this, "Storing data locally", Toast.LENGTH_SHORT).show();
				//Have data already stored so only toast shows
			} else {
				if (firstFilePath != null) {
					//Send files then delete them
					switch (identifier) {
					case MEDIA_TYPE_IMAGE:
						sendData(new File(secondFilePath.getPath()));
						deleteFile(secondFilePath.getLastPathSegment());
					case MEDIA_TYPE_VIDEO:
						sendData(new File(dataFilePath.getPath()));
						deleteFile(dataFilePath.getLastPathSegment());
					}
					sendData(new File(firstFilePath.getPath()));
					deleteFile(firstFilePath.getLastPathSegment());
				}
			}
			
			finish();
		} else if (v.getId() == R.id.tvNo) {
			log("no Pressed");
			setResult(RESULT_OK, new Intent().putExtra("response", "no"));
			finish();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if(!bluetoothAdapter.isEnabled()) {
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
		} else {
			if(btService == null) {
				btService = new BluetoothService(handler);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(btService != null) {
			if(btService.getState() == BluetoothService.STATE_NONE) {
				btService.start();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(btService != null) {
			btService.stop();
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		log("back pressed");
		setResult(RESULT_OK, new Intent().putExtra("response", "no"));
		
	}
	
	private void sendData(File data) {
		if(btService.getState() != BluetoothService.STATE_CONNECTED) {
			log("Not Connected");
			return;
		}
		btService.write(data);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_ENABLE_BT:
			if(resultCode == RESULT_OK) {
				btService = new BluetoothService(handler);
			} else {
				Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
				finish();
			}
		}
		
	}

	private static void log(String data) {
		Log.d("FinishedActivity", data);
	}
}
