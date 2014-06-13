package com.example.smartwatchapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class FinishedActivity extends Activity implements OnClickListener{
	public static final String URI = "uri";
	public static final String DESTINATION = "destination";
	public static final String DIRECTION = "direction";
	public static final String TIMESTAMP = "timestamp";
	
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
	
	public static final int STATE_NONE = 0;
	public static final int STATE_LISTEN = 1;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED = 3;
	
	private static final String NAME = "BluetoothService";
	private String deviceAddress = "00:00:00:00:00:00";
	private String deviceName = "";
	private static final UUID MY_UUID =
	//		UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  /*Serial Port*/
	//      UUID.fromString("00001203-0000-1000-8000-00805F9B34FB");  /*File Transfer*/
			UUID.fromString("c3f10dc0-677b-11e3-949a-0800200c9a66");  /*Samsung Gear according to Peng*/
	
	private static BluetoothDevice mDevice = null;
	
	public static final int DIRECTION_OUTBOUND = 0;
	public static final int DIRECTION_INBOUND = 1;
	
	public static final int REQUEST_CONNECT_DEVICE = 1;
	public static final int REQUEST_ENABLE_BT = 2;
	
	private static BluetoothAdapter bluetoothAdapter = null;
	
	/*
	private static AcceptThread acceptThread = null;
	private static ConnectedThread connectedThread = null;;
*/
	
	private static Uri firstFilePath = null;
	private static Uri secondFilePath = null;
	private static Uri dataFilePath = null;
	private static int identifier = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_finished);
		
		identifier = getIntent().getIntExtra(IDENTIFIER, -1);
		
		//All paths will contain a file
		firstFilePath = getIntent().getParcelableExtra(FIRST_FILE);
		
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
			/*
			log("Finding Paired devices");
			Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
			if(pairedDevices.size() > 0) {
				for(BluetoothDevice device : pairedDevices) {
					mDevice = device;
				}
			}
			
			deviceName = mDevice.getName();
			deviceAddress = mDevice.getAddress();
			
			log("Creating new Threads");
			acceptThread = new AcceptThread();
			acceptThread.start();
			
			log("Writing data");
			switch(identifier) {
			case MEDIA_TYPE_IMAGE:
				sendData(secondFilePath.toString());
			case MEDIA_TYPE_VIDEO:
				sendData(dataFilePath.toString());
			}
			
			sendData(firstFilePath.toString());
			*/

			finish();
		}

		if (v.getId() == R.id.tvNo) {
			log("no Pressed");
			setResult(RESULT_OK, new Intent().putExtra("response", "no"));
			finish();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if(!bluetoothAdapter.isEnabled()) {
			startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		/*if(connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}
		if(acceptThread != null) {
			acceptThread.cancel();
			acceptThread = null;
		}*/
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		log("back pressed");
		setResult(RESULT_OK, new Intent().putExtra("response", "no"));
		
	}
	
	/*private void sendData(String data) {
		if(data.length() > 0 && connectedThread != null) {
			byte[] send = data.getBytes();
			connectedThread.write(send);
			log("data sent");
		}
	}
	
	private class AcceptThread extends Thread {
		private final BluetoothServerSocket serverSocket;
		
		public AcceptThread() {
			BluetoothServerSocket temp = null;
			try {
				temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				log("Error while listening");
				e.printStackTrace();
			}
			serverSocket = temp;
		}
		
		public void run() {
			BluetoothSocket socket = null;
			while(true) {
				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					log("Server not accepted");
					e.printStackTrace();
					break;
				}
				
				if(socket != null) {
					manageConnectedSocket(socket);
					try {
						serverSocket.close();
					} catch (IOException e) {
						log("Server not closed");
						e.printStackTrace();
					}
					break;
				}
			}
		}
		
		public void cancel() {
			try {
				serverSocket.close();
			} catch (IOException e) {
				log("AcceptThread cancel failed");
				e.printStackTrace();
			}
		}
		
	}
	
	private void manageConnectedSocket(BluetoothSocket socket) {
		log("Managing Connected Socket");
		connectedThread = new ConnectedThread(socket);
		connectedThread.start();
	}
	
	private class ConnectedThread extends Thread {
		private final BluetoothSocket socket;
		private final InputStream input;
		private final OutputStream output;
		
		public ConnectedThread(BluetoothSocket newSocket) {
			socket = newSocket;
			InputStream tempIn = null;
			OutputStream tempOut = null;
			try {
				tempIn = socket.getInputStream();
				tempOut = socket.getOutputStream();
			} catch (IOException e) {
				log("error getting streams");
				e.printStackTrace();
			}
			
			input = tempIn;
			output = tempOut;
		}
		
		@Override
		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;
			
			while(true) {
				try {
					bytes = input.read(buffer);
				} catch (IOException e) {
					log("read faield");
					e.printStackTrace();
					break;
				}
			}
		}
		
		public void write(byte[] bytes) {
			try {
				output.write(bytes);
			} catch (IOException e) {
				log("Connected write failed");
				e.printStackTrace();
			}
		}
		
		public void cancel() {
			try {
				socket.close();
			} catch(IOException e) {
				e.printStackTrace();
				log("connectedThread cnacel failed");
			}
		}
		
	}*/
	
	private static void log(String data) {
		Log.d("FinishedActivity", data);
	}
}
