package com.pyb.trackme;

import android.app.Application;

import com.pyb.trackme.socket.SocketManager;

public class TrackMeApplication extends Application {

    private SocketManager socketManager;

    @Override
    public void onCreate() {
        super.onCreate();
        socketManager = SocketManager.getInstance();
    }

    public SocketManager getSocketManager() {
        if(socketManager == null) {
            SocketManager.getInstance();
        }
        return socketManager;
    }
}
