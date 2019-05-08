package com.pyb.trackme.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.utils.ConnectionUtils;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private IConnectionListener connectionListener;

    public NetworkChangeReceiver(IConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if(ConnectionUtils.isConnectedToInternet(context)) {
            connectionListener.onConnect();
        } else {
            connectionListener.onDisconnect();
        }
    }
}
