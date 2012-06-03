package com.r2src.dyad;

import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.bump.api.BumpAPIIntents;
import com.bump.api.IBumpAPI;

public abstract class BumpBonder extends Bonder {
	private IBumpAPI api;
	private byte[] myPart = new byte[SECRET_LENGTH];
	private static final int SECRET_LENGTH = 32;

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		// TODO: handle disconnects, not-matched, etc.
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			try {
				if (action.equals(BumpAPIIntents.CONNECTED)) {
					api.enableBumping();
				} else if (action.equals(BumpAPIIntents.MATCHED)) {
					api.confirm(intent.getLongExtra("proposedChannelID", 0),
							true);
				} else if (action.equals(BumpAPIIntents.CHANNEL_CONFIRMED)) {
					new Random().nextBytes(myPart);
					api.send(intent.getLongExtra("channelID", 0), myPart);
				} else if (action.equals(BumpAPIIntents.DATA_RECEIVED)) {
					byte[] otherPart = intent.getByteArrayExtra("data");
					setSharedSecret(combine(myPart, otherPart));
				}
			} catch (RemoteException e) {
			}
		}

		
	};

	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			api = IBumpAPI.Stub.asInterface(binder);
			try {
				api.configure(getBumpApiKey(), getBumpUserName());
			} catch (RemoteException e) {
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	@Override
	public void onResume() {
		super.onResume();

		bindService(new Intent(IBumpAPI.class.getName()), connection,
				Context.BIND_AUTO_CREATE);

		IntentFilter filter = new IntentFilter();
		filter.addAction(BumpAPIIntents.CHANNEL_CONFIRMED);
		filter.addAction(BumpAPIIntents.DATA_RECEIVED);
		filter.addAction(BumpAPIIntents.NOT_MATCHED);
		filter.addAction(BumpAPIIntents.MATCHED);
		filter.addAction(BumpAPIIntents.CONNECTED);
		registerReceiver(receiver, filter);
	}

	@Override
	public void onPause() {
		super.onPause();
		unbindService(connection);
		unregisterReceiver(receiver);
	}
	
	// XORs the two byte arrays. They should be of equal length.
	private byte[] combine(byte[] myPart, byte[] otherPart) {
		if(myPart.length != otherPart.length) throw new IllegalArgumentException("arrays should be of equal length");
		
		byte[] result = new byte[myPart.length];
		for (int i=0; i<myPart.length; i++)
			result[i] = (byte) (myPart[i] ^ otherPart[i]);
		return result;
	}

	public abstract String getBumpApiKey();

	public abstract String getBumpUserName();

}
