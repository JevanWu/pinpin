package com.teamjx.bluetooth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.teamjx.pinpin.GameActivity;
import com.teamjx.pinpin.R;




import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that setup bluetooth connection
 */
public class BluetoothSetupActivity extends Activity {

	// Debugging
	private static final String TAG = "BluetoothGameSetup";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Layout Views

	private Button mStartGameButton;
	private Button mConnectButton;
	private Button mDiscoverableButton;
	private Button mExitButton;
	// Name of the connected device
	private String mConnectedDeviceName = null;


	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private static MessageHub mHub = null;

	// private ServerSocket mServer;
	// private Socket mClient;
	// private int ListenPort = 8080;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		setContentView(R.layout.main);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mHub == null)
				setup();
		}
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mHub != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mHub.getState() == MessageHub.STATE_NONE) {
				// Start the Bluetooth chat services
				mHub.start();
			}
		}
	}

	private void setup() {
		Log.d(TAG, "setup()");

		mConnectButton = (Button) findViewById(R.id.button_connect);
		mConnectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				Intent deviceList = new Intent(v.getContext(),
						DeviceListActivity.class);
				startActivityForResult(deviceList,
						REQUEST_CONNECT_DEVICE_SECURE);
			}
		});

		mDiscoverableButton = (Button) findViewById(R.id.button_discoverable);
		mDiscoverableButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				// Ensure this device is discoverable by others
				ensureDiscoverable();
			}
		});

		// Initialise the send button with a listener that for click events
		mStartGameButton = (Button) findViewById(R.id.button_start_game);
		mStartGameButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (GameConstants.DEMO == true) {

					Toast.makeText(
							getApplicationContext(),
							"Warning: Demostration mod activated, no connection established!",
							Toast.LENGTH_SHORT).show();
					Intent gameIntent = new Intent(v.getContext(),
							GameActivity.class);
					startActivity(gameIntent);
				} else {
					if (mHub.getState() != MessageHub.STATE_CONNECTED) {

						Toast.makeText(getApplicationContext(),
								"Please connect to Bluetooth Device first!",
								Toast.LENGTH_SHORT).show();

					} else {
						Intent gameIntent = new Intent(v.getContext(),
								GameActivity.class);
						startActivity(gameIntent);
					}
				}
			}
		});
		mExitButton = (Button) findViewById(R.id.button_exit);
		mExitButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				finish();
			}
		});
		// Initialise the BluetoothChatService to perform bluetooth connections
		mHub = MessageHub.getMessageHub();
		mHub.setContext(this);
		mHub.setSetupHandler(mHandler);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mHub != null) {
			mHub.stop();
			mHub = null;
		}
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	// /**
	// * Sends a message.
	// *
	// * @param message
	// * A string of text to send.
	// */
	// public void sendMessage(String message) {
	// // Check that we're actually connected before trying anything
	// if (mService.getState() != BluetoothService.STATE_CONNECTED) {
	// Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
	// .show();
	// return;
	// }
	//
	// // Check that there's actually something to send
	// if (message.length() > 0) {
	// // Get the message bytes and tell the BluetoothChatService to write
	// byte[] send = message.getBytes();
	// mService.write(send);
	//
	// }
	//
	// }

	@TargetApi(Build.VERSION_CODES.HONEYCOMB) private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB) private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	// The Handler that gets information back from the BluetoothService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case MessageHub.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to,
							mConnectedDeviceName));

					break;
				case MessageHub.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case MessageHub.STATE_LISTEN:
				case MessageHub.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);

				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);

				// new Thread(new StoCThread(mClient, readMessage)).start();

				mHub.forwardMessage(
						Integer.parseInt(readMessage.substring(0, 2)),
						readMessage);

				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;

		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setup();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();

			}

		}

	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mHub.connect(device);
	}


	// TCP connection
	// private class TCPAcceptThread implements Runnable {
	//
	// @Override
	// public void run() {
	// try {
	// mServer = new ServerSocket(ListenPort);
	// while (true) {
	// mClient = mServer.accept();
	//
	// new Thread(new CtoSThread(mClient)).start();
	// }
	// } catch (IOException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }
	//
	// }
	//
	// }
	//
	// private class CtoSThread implements Runnable {
	// private Socket socket = null;
	// private InputStream is = null;
	// private BufferedReader bris = null;
	// private boolean flag = true;
	//
	// public CtoSThread(Socket socket) {
	// super();
	// this.socket = socket;
	// try {
	// is = socket.getInputStream();
	// bris = new BufferedReader(new InputStreamReader(is));
	// } catch (IOException e) {
	// flag = false;
	// e.printStackTrace();
	// }
	//
	// }
	//
	// private void ReceiveMsg() {
	//
	// String line = null;
	// try {
	// while ((line = bris.readLine()) != null) {
	//
	// sendMessage(line);
	// }
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// }
	//
	// @Override
	// public void run() {
	// while (flag) {
	// ReceiveMsg();
	// }
	//
	// }
	//
	// }
	//
	// private class StoCThread implements Runnable {
	// private Socket socket = null;
	// private OutputStream os = null;
	// private String message = null;
	//
	//
	// public StoCThread(Socket socket, String message) {
	// super();
	// this.socket = socket;
	// this.message = message;
	// }
	//
	// private void SendMsg() {
	// try {
	// os = socket.getOutputStream();
	// BufferedWriter bwos = new BufferedWriter(
	// new OutputStreamWriter(os));
	// bwos.write(message+"\n");
	// bwos.flush();
	//
	// } catch (IOException e) {
	//
	// e.printStackTrace();
	// }
	//
	// }
	//
	// @Override
	// public void run() {
	//
	// SendMsg();
	//
	//
	// }
	//
	// }
}
