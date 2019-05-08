package com.pyb.trackme.socket;

import android.os.RemoteException;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.pyb.trackme.restclient.ServiceURL.BASE_URL;

public class SocketManager {

    private static SocketManager INSTANCE;
    private Socket mSocket;
    private int socketUsers;

    private SocketManager() {
        try {
            mSocket = IO.socket(BASE_URL);
        } catch (URISyntaxException e) {
            Log.e("SocketManager", "Failed to connect to server: " + e);
        }
    }

    public static SocketManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new SocketManager();
        }
        return INSTANCE;
    }

    public void connect(final IConnectionListener connectionListener) {
        if(!mSocket.connected()) {
            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("ok", "ok");
                    connectionListener.onConnect();
                }
            });
            mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("ok", "ok");
                    connectionListener.onDisconnect();
                }
            });
            mSocket.connect();
        } else {
            connectionListener.onConnect();
        }
        socketUsers++;
    }

    public void disconnect() {
        if(mSocket.connected()) {
            if(socketUsers == 1) {
                mSocket.disconnect();
                mSocket.off();
            }
            socketUsers--;
        }
    }

    public void onEvent(final String event, final IEventListener listener) {
        Emitter.Listener eventListener = new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                listener.onEvent(event, args);
            }
        };
        mSocket.on(event, eventListener);
    }

    public void sendEventMessage(String event, String message) {
        mSocket.emit(event, message);
    }

    public void sendEventMessage(String event, String message, final IAckListener listener) {
        mSocket.emit(event, message, new Ack() {
            @Override
            public void call(Object... args) {
                listener.onReply(args);
            }
        });
    }


    public void sendEventMessage(String event, JSONObject jsonObject) {
        mSocket.emit(event, jsonObject);
    }

    public void offEvent(String event) {
        if(mSocket.hasListeners(event)) {
            List<Emitter.Listener> listeners = mSocket.listeners(event);
            for (Emitter.Listener listener : listeners) {
                mSocket.off(event, listener);
            }
        } else {
            mSocket.off(event);
        }
    }

    public boolean isConnected() {
        return mSocket.connected();
    }

    public void sendEventMessage(String event, JSONArray jsonArray) {
        mSocket.emit(event, jsonArray);
    }
}