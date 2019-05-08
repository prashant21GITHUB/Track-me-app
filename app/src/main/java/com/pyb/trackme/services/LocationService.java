package com.pyb.trackme.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pyb.trackme.HomeActivity;
import com.pyb.trackme.R;
import com.pyb.trackme.TrackMeApplication;
import com.pyb.trackme.db.TrackDetailsDB;
import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.socket.SocketManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LocationService extends Service {

    private PowerManager.WakeLock wakeLock;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private String loggedInMobile;
    private SocketManager socketManager;
    private String loggedInName;
    private int ONGOING_NOTIFICATION_ID = 1343;
    private String NOTIFICATION_CHANNEL_ID = "TrackMe_Notification_Channel";
    private final String TAG = "TrackMe_LocationService";
    private String LOGIN_PREF_NAME;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        socketManager = ((TrackMeApplication)getApplication()).getSocketManager();
        Intent notificationIntent = new Intent(getApplicationContext(), HomeActivity.class);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_noti)
                .setContentIntent(pendingIntent)
                .setContentTitle("Sharing live location !!")
                .setContentText("Click here to stop sharing")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(ONGOING_NOTIFICATION_ID, builder.build());
        } else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(ONGOING_NOTIFICATION_ID, builder.build());
        }

        PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TrackMe_Lock");
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // periodic interval
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setSmallestDisplacement(1f);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        Log.d(TAG, "Service Created");
    }

    private boolean alreadyConnectedToServer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        readLoggedInUserDetailsAndSharingLocationStatus();
        Log.d(TAG, "Service Started");
        if(!socketConnected) {
            connectToServer();
        }
        return START_STICKY;
    }

    private boolean socketConnected;
    private void connectToServer() {
        socketManager.connect(new IConnectionListener() {
            @Override
            public void onConnect() {
                socketConnected = true;
                sendEventToPublishLocationData();
                socketManager.sendEventMessage("connectedMobile", loggedInMobile);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        resumeSendingLocationUpdates();
                    }
                });
            }

            @Override
            public void onDisconnect() {
                socketConnected = false;
            }
        });
    }

    private void resumeSendingLocationUpdates() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            Log.i(TAG, "resumeSendingLocationUpdates");
        } else {
            Log.e(TAG, "ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permissions not granted");
        }
    }

    private void sendEventToPublishLocationData() {
        Collection<String> sharingContactsList = TrackDetailsDB.db().getContactsToShareLocation();
        if(!sharingContactsList.isEmpty()) {
            JSONArray arr = new JSONArray();
            arr.put(loggedInMobile);
            for (String contact : sharingContactsList) {
                arr.put(contact);
            }
            socketManager.sendEventMessage("startPublish", arr);
        }
    }

    private void readLoggedInUserDetailsAndSharingLocationStatus() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String name = preferences.getString("Name", "");
        loggedInName = name;
        loggedInMobile = mobile;
    }

    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.d(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("mobile", loggedInMobile);
                    jsonObject.put("lat", lat);
                    jsonObject.put("lng", lng);
                    if(socketManager.isConnected()) {
                        socketManager.sendEventMessage("publish", jsonObject);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    };

    @Override
    public void onDestroy() {
        if(wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopSendingLocationUpdates();
        socketManager.disconnect();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        } else {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(ONGOING_NOTIFICATION_ID);
        }
        Log.d("TrackMe_LocationService", "Service destroyed");
        super.onDestroy();
    }

    private void stopSendingLocationUpdates() {
        socketManager.sendEventMessage("stopPublish", loggedInMobile);
        mFusedLocationClient.flushLocations();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
}