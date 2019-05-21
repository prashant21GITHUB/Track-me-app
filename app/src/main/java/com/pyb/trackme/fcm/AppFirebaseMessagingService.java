package com.pyb.trackme.fcm;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = "TrackMe_PushNotiService";

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.i(TAG,s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
    }

}
