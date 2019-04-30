package com.pyb.trackme.socket;

import android.os.RemoteException;

public interface IConnectionListener {

    void onConnect() throws RemoteException;

    void onDisconnect();
}
