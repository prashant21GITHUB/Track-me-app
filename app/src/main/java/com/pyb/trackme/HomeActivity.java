package com.pyb.trackme;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pyb.trackme.db.TrackDetailsDB;
import com.pyb.trackme.services.ILocationSharingService;
import com.pyb.trackme.services.TrackMeService;
import com.pyb.trackme.socket.IAckListener;
import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.socket.IEventListener;
import com.pyb.trackme.socket.SocketManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cz.msebera.android.httpclient.Header;


public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String loggedInName;
    private String loggedInMobile;
    private DrawerLayout mDrawerLayout;
    private ListView sharingListView;
    private ListView trackingListView;
    private List<String> sharingContactsList;
    private List<String> trackingContactsList;
    private Switch sharingSwitch;
    private String CHANNEL_ID = "TrackMe_Notification_Channel";
    private ImageView liveSharingImage;
    private SocketManager socketManager;
    private String LOGIN_PREF_NAME;
    private ILocationSharingService locationSharingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        readLoginAndTrackingDetailsFromIntent(getIntent());
        initializeLocationSharingSwitch();
        trackingContactsList = sharingContactsList = Collections.EMPTY_LIST;
        intializeDrawerLayout(toolbar);
        initializeMap();
        createNotificationChannel();
        socketManager = SocketManager.getInstance();
        socketManager.connect(new IConnectionListener() {
            @Override
            public void onConnect() {
                socketManager.sendEventMessage("connectedMobile", loggedInMobile);
                socketManager.onEvent("publisherAvailable", new IEventListener() {
                    @Override
                    public void onEvent(String event, Object[] args) {
                        String mobile = (String) args[0];
                        subscribeToContact(mobile);
                        if(!trackingContactsList.contains(mobile)) {
                            trackingContactsList.add(mobile);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((ArrayAdapter) trackingListView.getAdapter()).notifyDataSetChanged();
                                    Toast.makeText(HomeActivity.this, mobile + " has just started sharing location !!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
                socketManager.onEvent("publisherNotAvailable", (event, args) -> {
                    String mobile = (String) args[0];
                    HomeActivity.this.runOnUiThread(() -> {
                        updateNotAvailableStatusOnMap(mobile);
                    });

                });

            }

            @Override
            public void onDisconnect() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(HomeActivity.this, "You are not online !!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        bindService(new Intent(getApplicationContext(), TrackMeService.class), locationSharingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void readLoginAndTrackingDetailsFromIntent(Intent intent) {
        loggedInMobile = intent.getStringExtra("mobile");
        loggedInName = intent.getStringExtra("name");
    }


    @Override
    public void onResume(){
        super.onResume();
        if(TrackMeService.isRunning()) {
            sharingSwitch.setChecked(true);
            liveSharingImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        trackingContactsList = new ArrayList<>(TrackDetailsDB.db().getContactsToTrackLocation());
        sharingContactsList = new ArrayList<>(TrackDetailsDB.db().getContactsToShareLocation());
        ((ArrayAdapter) trackingListView.getAdapter()).notifyDataSetChanged();
        ((ArrayAdapter) sharingListView.getAdapter()).notifyDataSetChanged();
    }

    private void initializeMap() {
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    private void intializeDrawerLayout(Toolbar toolbar) {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ((TextView)mDrawerLayout.findViewById(R.id.drawer_header)).setText(loggedInName);
        sharingListView =  findViewById(R.id.sharing_list_view);
        trackingListView = findViewById(R.id.tracking_list_view);
        sharingListView.setAdapter(new NavListViewAdapter(this, sharingContactsList));
        trackingListView.setAdapter(new NavListViewAdapter(this, trackingContactsList));
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
    }

    private ServiceConnection locationSharingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationSharingService = (ILocationSharingService) service;
            sharingSwitch.setEnabled(true);
            try {
                if(locationSharingService.isLocationSharingOn()) {
                    sharingSwitch.setChecked(true);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationSharingService = null;
            sharingSwitch.setEnabled(false);
        }
    };

    private void initializeLocationSharingSwitch() {
        sharingSwitch = findViewById(R.id.sharing_switch);
        sharingSwitch.setEnabled(false);
        liveSharingImage = findViewById(R.id.live_sharing_image);

        sharingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        checkLocationPermission();
                        sharingSwitch.setChecked(false);
                        return;
                    }
                    if(!isLocationServiceOn() || !isConnectedToInternet()) {
                        sharingSwitch.setChecked(false);
                        return;
                    }
                    if(sharingContactsList.isEmpty()) {
//                        showAlertDialogToSelectContacts();
                        sharingSwitch.setChecked(false);
                        Toast.makeText(HomeActivity.this, "Select contacts with whom you want to share your location !!", Toast.LENGTH_LONG).show();
                        return;
                    }
//                    sendEventToPublishLocationData();
//                    Intent service = new Intent(getApplicationContext(), TrackMeService.class);
//                    bindService(service, new Se)
//                    service.putExtra("mobile", loggedInMobile);
                    //TODO Check if service needs to be checked whether it is already running or not
//                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//                        startService(service);
//                    } else {
//                        startForegroundService(service);
//                    }
                    if(locationSharingService != null) {
                        try {
                            locationSharingService.startLocationSharing();
                            liveSharingImage.setVisibility(View.VISIBLE);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                } else {
//                    stopService(new Intent(getApplicationContext(), TrackMeService.class));
                    if(locationSharingService != null) {
                        try {
                            locationSharingService.stopLocationSharing();
                            liveSharingImage.setVisibility(View.GONE);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
//                    socketManager.sendEventMessage("stopPublish", loggedInMobile);
//                    if(trackingContactsList.isEmpty()) {
//                        socketManager.disconnect();
//                    }
                }
            }
        });
    }

    private void sendEventToPublishLocationData() {
        if(!sharingContactsList.isEmpty()) {
            JSONArray arr = new JSONArray();
            arr.put(loggedInMobile);
            for (String contact : sharingContactsList) {
                arr.put(contact);
            }
            socketManager.sendEventMessage("startPublish", arr);
        }
    }

    private void showAlertDialogToSelectContacts() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Select contacts to share your location with them")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        openSelectContactsActivity();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isLocationServiceOn() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if(!isConnected) {
            buildNoInternetDialog();
        }
        return isConnected;
    }

    private void buildNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                .setTitle("Check your internet connection")
                .setMessage("Please connect to mobile data or wifi !!")
                .setCancelable(true)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(HomeActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sharingSwitch.setChecked(true);
                    // permission was granted, yay! Do the
                    // location-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void syncTrackingDetailsFromServer() {
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        requestParams.put("mobile", loggedInMobile);
        APIClient.put("user/track/details", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if(response.getBoolean("success")) {
                        JSONArray arr = response.getJSONArray("sharingWith");
                        sharingContactsList.clear();
                        for(int i=0; i < arr.length(); i++) {
                            sharingContactsList.add(arr.getString(i));
                        }
//                        if(!sharingContactsList.isEmpty()) {
//                            sharingContactsLayout.setVisibility(View.VISIBLE);
//                        }
                        arr = response.getJSONArray("tracking");
                        trackingContactsList.clear();
                        for(int i=0; i < arr.length(); i++) {
                            trackingContactsList.add(arr.getString(i));
                        }
//                        if(!trackingContactsList.isEmpty()) {
//                            trackingContactsLayout.setVisibility(View.VISIBLE);
//                        }
                        ((ArrayAdapter) trackingListView.getAdapter()).notifyDataSetChanged();
                        ((ArrayAdapter) sharingListView.getAdapter()).notifyDataSetChanged();
                        if(!trackingContactsList.isEmpty()) {
                            createSocketConnectionForTracking();
                        }
                    } else {
                        Toast.makeText(HomeActivity.this, "Failed to get tracking details from server, please try after sometime !!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(HomeActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast.makeText(HomeActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void createSocketConnectionForTracking() {
        if(socketManager.isConnected()) {
            subscribeToTrackContacts();
        }
    }

    private void subscribeToTrackContacts() {
        for (final String contact : trackingContactsList) {

            subscribeToContact(contact);
        }
    }

    private void subscribeToContact(final String contact) {
        socketManager.sendEventMessage("subscribe", contact, new IAckListener() {
            @Override
            public void onReply(Object[] args) {
                JSONObject data = (JSONObject) args[0];
                try {
                    if ("connected".equals(data.getString("status"))) {
                        socketManager.onEvent(contact, new IEventListener() {
                            @Override
                            public void onEvent(String event, final Object[] args) {
                                HomeActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        JSONObject data = (JSONObject) args[0];
                                        double lat;
                                        double lng;
                                        try {
                                            lat = data.getDouble("lat");
                                            lng = data.getDouble("lng");

                                        } catch (JSONException e) {
                                            return;
                                        }
                                        updateCurrentTrackingPosition(contact, lat, lng);
                                    }
                                });
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private GoogleMap googleMap;
    @Override
    public void onMapReady(GoogleMap map) {
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.
        LatLng india = new LatLng(20.5937, 78.9629);
        googleMap = map;
        googleMap.addMarker(new MarkerOptions().position(india)
                .title("India"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(india));

    }

    private void readLoggedInUserDetails() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String name = preferences.getString("Name", "");
        loggedInName = name;
        loggedInMobile = mobile;
    }

    private void clearSavedLoginDetails() {
        SharedPreferences preferences = getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Mobile", "");
        editor.putString("Name", "");
        editor.commit();
        this.loggedInName = "";
        this.loggedInMobile = "";
    }

//    private void saveLocationServiceRunningStatus(boolean running) {
//        SharedPreferences preferences = getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
//        SharedPreferences.Editor editor = preferences.edit();
//        editor.putBoolean("LocationServiceRunningStatus", running);
//        editor.commit();
//    }

//    private void readLocationServiceRunningStatus() {
//        SharedPreferences preferences = getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
//        isLocationServiceRunning = preferences.getBoolean("LocationServiceRunningStatus", false);
//    }

    private Map<String, LatLng> lastLocationTrackingMap = new HashMap<>();
    private Map<String, Marker> currLocationMarkerMap = new HashMap<>();
    private int[] lineColors = {Color.BLUE, Color.MAGENTA, Color.RED,  Color.YELLOW, Color.GREEN, Color.DKGRAY, Color.LTGRAY};
    private float[] markerIcon = {
            BitmapDescriptorFactory.HUE_BLUE,
            BitmapDescriptorFactory.HUE_MAGENTA,
            BitmapDescriptorFactory.HUE_RED,
            BitmapDescriptorFactory.HUE_YELLOW,
            BitmapDescriptorFactory.HUE_GREEN,
            BitmapDescriptorFactory.HUE_CYAN,
            BitmapDescriptorFactory.HUE_VIOLET
    };
    private Map<String, Pair<Integer, Float>> colorsMap = new HashMap<>();
    private int nextColor = new Random().nextInt((6 - 0) + 1) + 0;

    private void updateCurrentTrackingPosition(String trackingContact, double lat, double lng) {
        LatLng latLng = new LatLng(lat, lng);
        LatLng lastLocation = lastLocationTrackingMap.get(trackingContact);
        Pair<Integer, Float> lineAndIconColorPair = colorsMap.get(trackingContact);
        if(lineAndIconColorPair == null) {
            lineAndIconColorPair = new Pair<>(lineColors[nextColor], markerIcon[nextColor]);
            nextColor ++;
            if(nextColor == lineColors.length) {
                nextColor = 0;
            }
            colorsMap.put(trackingContact, lineAndIconColorPair);
        }
//        if(lastLocation != null) {
//            PatternItem patternItem = new Dot();
//            Polyline line = googleMap.addPolyline(new PolylineOptions()
//                    .add(lastLocation, latLng)
//                    .width(10)
//                    .endCap(new RoundCap())
//                    .geodesic(true)
//                    .pattern(Arrays.asList(patternItem))
//                    .jointType(JointType.ROUND)
//                    .color(lineAndIconColorPair.first));
//        } else {
//            MarkerOptions markerOptions = new MarkerOptions();
//            markerOptions.position(latLng);
//            markerOptions.title(trackingContact + " Start");
//            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
//            googleMap.addMarker(markerOptions);
//        }
//        lastLocationTrackingMap.put(trackingContact, latLng);
        Marker currLocationMarker = currLocationMarkerMap.get(trackingContact);
        if(currLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions()
            .position(latLng)
            .title(trackingContact)
            .snippet("Current location")
                    .draggable(false).anchor(0.5f, 0.7f)
                    .icon(BitmapDescriptorFactory.defaultMarker(lineAndIconColorPair.second));

            currLocationMarker = googleMap.addMarker(markerOptions);
            currLocationMarker.showInfoWindow();
            currLocationMarkerMap.put(trackingContact, currLocationMarker);
        }
        currLocationMarker.setPosition(latLng);
        //move map camera
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
    }

    private void updateNotAvailableStatusOnMap(String mobile) {
        Toast.makeText(HomeActivity.this, mobile + " has stopped sharing location !!", Toast.LENGTH_SHORT).show();
        Marker currLocationMarker = currLocationMarkerMap.get(mobile);
        if(currLocationMarker != null) {
            currLocationMarker.hideInfoWindow();
            currLocationMarker.setSnippet("Not live");
            currLocationMarker.showInfoWindow();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.logout:
                stopService(new Intent(getApplicationContext(), TrackMeService.class));
                socketManager.disconnect();
                clearSavedLoginDetails();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
                return true;
            case R.id.share_location:
                openSelectContactsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void openSelectContactsActivity() {
        Intent i = new Intent(HomeActivity.this, SelectContactsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("name", loggedInName);
        bundle.putString("mobile", loggedInMobile);
        i.putExtras(bundle);
        startActivity(i);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("TrackMe_HomeActivity", "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(locationSharingServiceConnection);
        if(!TrackMeService.isRunning()) {
            socketManager.disconnect();
        }
        googleMap.clear();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TrackMe";
            String description = "TrackMe";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}


