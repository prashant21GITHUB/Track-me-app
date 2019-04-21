package com.pyb.trackme.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pyb.trackme.HomeActivity;
import com.pyb.trackme.R;
import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.socket.SocketManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class LocationService extends Service {

    private PowerManager.WakeLock wakeLock;
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private String loggedInMobile;
    private final SocketManager socketManager = SocketManager.getInstance();
    private String loggedInName;
    private String CHANNEL_DEFAULT_IMPORTANCE = "LocationServiceNotification";
    private int ONGOING_NOTIFICATION_ID = 1343;
    private String CHANNEL_ID = "TrackMe_Notification_Channel";

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(getApplicationContext(), HomeActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.add_icon)
                    .setContentIntent(pendingIntent)
                    .setContentTitle("Sharing live location")
                    .setContentText("Sharing live location with users")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            startForeground(ONGOING_NOTIFICATION_ID, builder.build());
        }
        PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);
        connectToServer();
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TrackMe_Lock");
        if(!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // periodic interval
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setSmallestDisplacement(1f);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
//        IntentFilter filter = new IntentFilter(LOCATION_SERVICE_RESTART_ACTION);
//        registerReceiver(receiver, filter);



        // Toast.makeText(getApplicationContext(), "Service Created",
        // Toast.LENGTH_SHORT).show();

        Log.d("LocationService", "Service Created");

    }

    private void connectToServer() {
        socketManager.connect(new IConnectionListener() {
            @Override
            public void onConnect() {
                socketManager.sendEventMessage("connectedMobile", loggedInMobile);
                socketManager.sendEventMessage("startPublish", loggedInMobile);
            }

            @Override
            public void onDisconnect() {
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        super.onStartCommand(intent, flags, startId);

        Log.d("LocationService", "Service Started");
//        loggedInMobile = intent.getExtras().getString("mobile");
        readLoggedInDetailsFromPreferences();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }

        return START_STICKY;
    }

    private void readLoggedInDetailsFromPreferences() {
            SharedPreferences preferences = this.getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
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
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("mobile", loggedInMobile);
                    jsonObject.put("lat", lat);
                    jsonObject.put("lng", lng);
                    socketManager.sendEventMessage("publish", jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    };




    private LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub

            Log.e("Google", "Location Changed");

            if (location == null)
                return;

            if (isConnectingToInternet(getApplicationContext())) {
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();

                try {
                    Log.e("latitude", location.getLatitude() + "");
                    Log.e("longitude", location.getLongitude() + "");

                    jsonObject.put("latitude", location.getLatitude());
                    jsonObject.put("longitude", location.getLongitude());

                    jsonArray.put(jsonObject);

                    Log.e("request", jsonArray.toString());
//
//                    new LocationWebService().execute(new String[] {
//                            Constants.TRACK_URL, jsonArray.toString() });
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
    };

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if(wakeLock.isHeld()) {
            wakeLock.release();
        }
        socketManager.disconnect();
        mFusedLocationClient.flushLocations();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        }
        Log.d("LocationService", "Service destroyed");
        super.onDestroy();
    }

    public static boolean isConnectingToInternet(Context _context) {
        ConnectivityManager connectivity = (ConnectivityManager) _context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    private final String LOCATION_SERVICE_RESTART_ACTION = "com.pyb.trackme.RestartLocationService";
    private final LocationServiceBroadcastReceiver receiver = new LocationServiceBroadcastReceiver();

    public class LocationServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();
            if(LOCATION_SERVICE_RESTART_ACTION.equals(action)) {
                startService(new Intent(context, LocationService.class));
            }
        }
    }


}
