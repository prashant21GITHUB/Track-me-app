package com.pyb.trackme.services;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
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
import com.pyb.trackme.db.TrackDetailsDB;
import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.socket.SocketManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

public class TrackMeService extends Service {

    private PowerManager.WakeLock wakeLock;
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private String loggedInMobile;
    private final SocketManager socketManager = SocketManager.getInstance();
    private String loggedInName;
    private int ONGOING_NOTIFICATION_ID = 1343;
    private String NOTIFICATION_CHANNEL_ID = "TrackMe_Notification_Channel";
    private final String TAG = "TrackMe_LocationService";
    private static boolean running;
    private boolean locationSharingStatus;

    private final ILocationSharingService.Stub locationSharingServiceBinder = new ILocationSharingService.Stub() {

        @Override
        public void startLocationSharing() throws RemoteException {
            startLocationSharingService();
        }

        @Override
        public void stopLocationSharing() throws RemoteException {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true); //true will remove notification
            }
            socketManager.sendEventMessage("stopPublish", loggedInMobile);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            locationSharingStatus = false;
            saveLocationSharingStatusInPref(true);
        }

        @Override
        public boolean isLocationSharingOn() {
            return locationSharingStatus;
        }

        @Override
        public void onLogout() {
            loggedInMobile = null;
            loggedInName = null;
        }
    };

    private void startLocationSharingService() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            running = false;
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            sendEventToPublishLocationData();
            locationSharingStatus = true;
            saveLocationSharingStatusInPref(true);
            running = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent notificationIntent = new Intent(getApplicationContext(), HomeActivity.class);
                PendingIntent pendingIntent =
                        PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.add_icon)
                        .setContentIntent(pendingIntent)
                        .setContentTitle("Sharing live location")
                        .setContentText("Sharing live location with users")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                startForeground(ONGOING_NOTIFICATION_ID, builder.build());
            }

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
    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return locationSharingServiceBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();
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
        if(loggedInMobile == null || loggedInMobile.isEmpty()) {
            readLoggedInDetailsAndLocationSharingStatusFromPreferences();
        }


        Log.d(TAG, "Service Created");
    }

    private void connectToServer() {
        socketManager.connect(new IConnectionListener() {
            @Override
            public void onConnect() {
                socketManager.sendEventMessage("connectedMobile", loggedInMobile);
                if(locationSharingStatus) {
                    Looper.prepare();
                    new Handler().post(() -> startLocationSharingService());
                }
            }

            @Override
            public void onDisconnect() {
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "Service Started");
        if(socketManager.isConnected()) {
            if(locationSharingStatus) {
                startLocationSharingService();
            }
        } else {
           connectToServer();
        }
        return START_STICKY;
    }


    private void readLoggedInDetailsAndLocationSharingStatusFromPreferences() {
        SharedPreferences preferences = this.getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String name = preferences.getString("Name", "");
        loggedInName = name;
        loggedInMobile = mobile;
        locationSharingStatus = preferences.getBoolean("locationSharingStatus", false);
    }

    private void saveLocationSharingStatusInPref(boolean status) {
        SharedPreferences preferences = this.getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("locationSharingStatus", status);
        editor.commit();
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
        // TODO Auto-generated method stub
        if(wakeLock.isHeld()) {
            wakeLock.release();
        }
//        socketManager.disconnect();
//        mFusedLocationClient.flushLocations();
//        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        running = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        }
        Log.d("TrackMe_LocationService", "Service destroyed");
        super.onDestroy();
    }


    public static boolean isRunning() {
        return running;
    }
}
