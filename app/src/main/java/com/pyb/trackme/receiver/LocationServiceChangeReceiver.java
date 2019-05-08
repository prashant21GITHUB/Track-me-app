package com.pyb.trackme.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import com.pyb.trackme.socket.IConnectionListener;

public class LocationServiceChangeReceiver extends BroadcastReceiver {

    private IConnectionListener connectionListener;

    public LocationServiceChangeReceiver(IConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(isLocationServiceOn(context)) {
            connectionListener.onConnect();
        } else{
            connectionListener.onDisconnect();
        }
    }

    public boolean isLocationServiceOn(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false;
        }
        return true;
    }
}
