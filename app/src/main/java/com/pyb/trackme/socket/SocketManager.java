package com.pyb.trackme.socket;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.List;

public class SocketManager {

    private static SocketManager INSTANCE = new SocketManager();
    private Socket mSocket;

    private SocketManager() {
        try {
            mSocket = IO.socket("http://127.0.0.1:3000/");
        } catch (URISyntaxException e) {
            Log.e("SocketManager", "Failed to connect to server: " + e);
        }
    }

    public static SocketManager getInstance() {
        return INSTANCE;
    }

    public void connect(final IConnectionListener connectionListener) {
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
    }

    public void disconnect() {
        if(mSocket.connected()) {
            mSocket.disconnect();
            mSocket.off();
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

}
