package com.pyb.trackme.services;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import com.pyb.trackme.R;
import com.pyb.trackme.TrackMeApplication;
import com.pyb.trackme.activities.HomeActivity;
import com.pyb.trackme.cache.AppConstants;
import com.pyb.trackme.cache.TrackDetailsDB;
import com.pyb.trackme.receiver.LocationServiceChangeReceiver;
import com.pyb.trackme.receiver.NetworkChangeReceiver;
import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.socket.IEventListener;
import com.pyb.trackme.socket.ISocketConnectionListener;
import com.pyb.trackme.socket.SocketManager;
import com.pyb.trackme.utils.ConnectionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

public class LocationService extends Service {

    private final long DELAY_IN_MILLIS = 10000L;
//    private PowerManager.WakeLock wakeLock;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private String loggedInMobile;
    private SocketManager socketManager;
    private String loggedInName;
    private int ONGOING_NOTIFICATION_ID = 1343;
    private int STOPPED_SHARING_NOTIFICATION_ID = 1353;
    private String NOTIFICATION_CHANNEL_ID = "TrackMe_Notification_Channel";
    private final String TAG = "TrackMe_LocationService";
    private String LOGIN_PREF_NAME;
    private ISocketConnectionListener socketConnectionListener;
    private NetworkChangeReceiver networkChangeReceiver;
    private LocationServiceChangeReceiver locationServiceChangeReceiver;
    private Handler handler;
    private TrackDetailsDB db;
    private boolean activeSubscribersAvailable;
    private boolean sendLocationUpdate;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        socketManager = ((TrackMeApplication)getApplication()).getSocketManager();
        initializeSocketEventListeners();
        db = TrackDetailsDB.db();
        showForegroundNotification();

//        PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);
//        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TrackMe_Lock");
//        if (!wakeLock.isHeld()) {
//            wakeLock.acquire();
//        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // periodic interval
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setSmallestDisplacement(1f);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        handler = new Handler();

        locationServiceChangeReceiver = new LocationServiceChangeReceiver(new IConnectionListener() {
            private final Runnable onConnectRunner = () -> {
                if(ConnectionUtils.isLocationServiceOn(LocationService.this)) {
                    cancelSharingStoppedNotification();
//                        showForegroundNotification();
                    if(activeSubscribersAvailable) {
                        resumeSendingLocationUpdates();
                    }
                }
            };

            private final Runnable onDisconnectRunner = () -> {
                if(!ConnectionUtils.isLocationServiceOn(LocationService.this)) {
                    if(ConnectionUtils.isConnectedToInternet(LocationService.this)) {
                        socketManager.sendEventMessage("notLive", loggedInMobile);
                    }
                    stopSendingLocationUpdates();
//                        stopForegroundNotification();
                    showSharingStoppedNotification();
                }
            };

            @Override
            public void onConnect() {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(onConnectRunner, DELAY_IN_MILLIS);
            }

            @Override
            public void onDisconnect() {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(onDisconnectRunner, DELAY_IN_MILLIS);
            }
        });

        networkChangeReceiver = new NetworkChangeReceiver(new IConnectionListener() {
            private final Runnable onConnectRunner = () -> {
                if (ConnectionUtils.isConnectedToInternet(LocationService.this)) {
                    cancelSharingStoppedNotification();
//                        showForegroundNotification();
                    if(activeSubscribersAvailable) {
                        resumeSendingLocationUpdates();
                    }
                }
            };

            private final Runnable onDisconnectRunner = () -> {
                if (!ConnectionUtils.isConnectedToInternet(LocationService.this)) {
                    stopSendingLocationUpdates();
//                        stopForegroundNotification();
                    showSharingStoppedNotification();
                }
            };

            @Override
            public void onConnect() {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(onConnectRunner, DELAY_IN_MILLIS);
            }

            @Override
            public void onDisconnect() {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(onDisconnectRunner, DELAY_IN_MILLIS);
            }
        });

        Log.d(TAG, "Service Created");
    }

    private void initializeSocketEventListeners() {
        socketManager.onEvent("stopSendingLocation", new IEventListener() {
            @Override
            public void onEvent(String event, Object[] args) {
                activeSubscribersAvailable = false;
                stopSendingLocationUpdates();
                saveSendLocationUpdateFlag(false);
            }
        });
        socketManager.onEvent("startSendingLocation", new IEventListener() {
            @Override
            public void onEvent(String event, Object[] args) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        activeSubscribersAvailable = true;
                        saveSendLocationUpdateFlag(true);
                        resumeSendingLocationUpdates();
                    }
                });
            }
        });
    }

    private void showForegroundNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), HomeActivity.class);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.setAction(NOTIFICATION_CHANNEL_ID);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_noti)
                .setContentIntent(pendingIntent)
                .setContentTitle("Sharing live location !!")
                .setContentText("Click here to stop sharing")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(ONGOING_NOTIFICATION_ID, builder.build());
        } else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(ONGOING_NOTIFICATION_ID, builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        readDetailsFromPref();
        Log.d(TAG, "Service Started");
        if(!socketConnected) {
            connectToServer();
        } else {
            sendEventToPublishLocationData();
            if(sendLocationUpdate) {
                resumeSendingLocationUpdates();
            }
        }
        registerNetworkChangeReceiver();
        registerLocationServiceChangeReceiver();
        return START_STICKY;
    }

    private void registerLocationServiceChangeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.PROVIDERS_CHANGED");
        registerReceiver(locationServiceChangeReceiver, filter);
    }

    private void registerNetworkChangeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
//        filter.addAction(getPackageName() + "android.net.wifi.WIFI_STATE_CHANGED");  ///TODO check if this intent filter is needed ?
        registerReceiver(networkChangeReceiver, filter);
    }

    private boolean socketConnected;
    private void connectToServer() {
        socketConnectionListener = new ISocketConnectionListener() {
            @Override
            public void onConnect(boolean alreadyConnected) {
                socketConnected = true;
                if(!alreadyConnected) {
                    socketManager.sendEventMessage("connectedMobile", loggedInMobile);
                }
                sendEventToPublishLocationData();
                if(sendLocationUpdate) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resumeSendingLocationUpdates();
                        }
                    });
                }
            }

            @Override
            public void onDisconnect() {
                socketConnected = false;
            }
        };
        socketManager.connect(socketConnectionListener);
    }

    private void resumeSendingLocationUpdates() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            Log.d(TAG, "Resume sending location updates");;
        } else {
            Log.e(TAG, "ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permissions not granted");
        }
    }

    private void sendEventToPublishLocationData() {
        Collection<String> sharingContactsList = db.getContactsToShareLocation();
        if(!sharingContactsList.isEmpty()) {
            JSONArray arr = new JSONArray();
            arr.put(loggedInMobile);
            for (String contact : sharingContactsList) {
                if(db.getSharingStatus(contact)) {
                    arr.put(contact);
                }
            }
            socketManager.sendEventMessage("startPublish", arr);
        }
    }

    private void readDetailsFromPref() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString(AppConstants.MOBILE_PREF, "");
        String name = preferences.getString(AppConstants.NAME_PREF, "");
        sendLocationUpdate  = preferences.getBoolean(AppConstants.SEND_LOCATION_UPDATE, false);
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
//        if(wakeLock.isHeld()) {
//            wakeLock.release();
//        }
        //token passed as null, so that it can remove all runners
        socketManager.sendEventMessage("stopPublish", loggedInMobile);
        handler.removeCallbacksAndMessages(null);
        stopSendingLocationUpdates();
        socketManager.softDisconnect(socketConnectionListener);
        stopForegroundNotification();
        unregisterReceiver(networkChangeReceiver);
        unregisterReceiver(locationServiceChangeReceiver);
        Log.d("TrackMe_LocationService", "Service destroyed");
        super.onDestroy();
    }

    private void stopForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        } else {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(ONGOING_NOTIFICATION_ID);
        }
    }

    private void stopSendingLocationUpdates() {
        Log.d("TrackMe_LocationService", "Stop sending location updates");
        if(mFusedLocationClient != null) {
            mFusedLocationClient.flushLocations();
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private void showSharingStoppedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_noti)
                .setContentTitle("Stopped sharing location !!")
                .setContentText("Check internet or location settings")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(STOPPED_SHARING_NOTIFICATION_ID, builder.build());
    }

    private void cancelSharingStoppedNotification() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(STOPPED_SHARING_NOTIFICATION_ID);
    }

    private void saveSendLocationUpdateFlag(boolean flag) {
        SharedPreferences preferences = getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(AppConstants.SEND_LOCATION_UPDATE, flag);
        editor.commit();
    }
}
