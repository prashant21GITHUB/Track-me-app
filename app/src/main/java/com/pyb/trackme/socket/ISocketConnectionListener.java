package com.pyb.trackme.socket;

public interface ISocketConnectionListener {

    void onConnect(boolean alreadyConnected);

    void onDisconnect();
}
