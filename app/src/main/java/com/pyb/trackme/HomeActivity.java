package com.pyb.trackme;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import com.pyb.trackme.db.TrackDetailsDB;
import com.pyb.trackme.restclient.RestClient;
import com.pyb.trackme.restclient.ServiceResponse;
import com.pyb.trackme.restclient.ShareLocationRequest;
import com.pyb.trackme.restclient.TrackingServiceClient;
import com.pyb.trackme.services.ILocationSharingService;
import com.pyb.trackme.services.LocationService;
import com.pyb.trackme.services.TrackMeService;
import com.pyb.trackme.socket.IAckListener;
import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.socket.IEventListener;
import com.pyb.trackme.socket.SocketManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback, IRemoveContactButtonClickListener {

    private static final int REQUEST_CODE_PICK_CONTACT = 131;

    private String loggedInName;
    private String loggedInMobile;
    private DrawerLayout mDrawerLayout;
    private ListView sharingListView;
    private ListView trackingListView;
    private List<String> sharingContactsList;
    private List<Pair<String, Boolean>> trackingContactsList;
    private Switch sharingSwitch;
    private String CHANNEL_ID = "TrackMe_Notification_Channel";
    private SocketManager socketManager;
    private String LOGIN_PREF_NAME;
    private ILocationSharingService locationSharingService;
    private ArrayAdapter<String> sharingListViewAdapter;
    private ArrayAdapter<Pair<String, Boolean>> trackingListViewAdapter;
    private ImageButton addContactBtn;
    private ProgressBar progressBar;
    private boolean locationSharingStatus;
//    private IListViewItemViewProvider trackingListViewHolderProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        readLoggedInUserDetailsAndLocationSharingStatus();
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        readLoginDetailsFromPref(getIntent());
        initializeLocationSharingSwitch();
        sharingContactsList = new ArrayList<>();
        trackingContactsList = new ArrayList<>();
        for(String contact : TrackDetailsDB.db().getContactsToTrackLocation()) {
            trackingContactsList.add(new Pair<>(contact, false));
        }
        sharingListViewAdapter = new SharingContactListViewAdapter(this, sharingContactsList, this);
        trackingListViewAdapter = new TrackingContactsListViewAdapter(this, trackingContactsList);
//        trackingListViewHolderProvider = (IListViewItemViewProvider) trackingListViewAdapter;
        intializeDrawerLayout(toolbar);
        initializeAddContactBtn();
        initializeMap();
        createNotificationChannel();
        initializeSocket();
//        bindService(new Intent(getApplicationContext(), TrackMeService.class), locationSharingServiceConnection, Context.BIND_AUTO_CREATE);
        progressBar = findViewById(R.id.progressBarHomeActivity);
    }

    private void initializeSocket() {
        socketManager = SocketManager.getInstance();
        socketManager.onEvent("publisherAvailable", new IEventListener() {
            @Override
            public void onEvent(String event, Object[] args) {
                String mobile = (String) args[0];
                int position = getPositionInTrackingList(mobile);
                if(position != -1) {
                    trackingContactsList.remove(position);
                } else {
                    TrackDetailsDB.db().addContactToTrackLocation(mobile);
                }
                trackingContactsList.add(new Pair(mobile, true));
                HomeActivity.this.runOnUiThread(() -> {
                    trackingListViewAdapter.notifyDataSetChanged();
                });
                subscribeToContact(mobile);

            }
        });
        socketManager.onEvent("publisherNotAvailable", (event, args) -> {
            String mobile = (String) args[0];
            int position = getPositionInTrackingList(mobile);
            trackingContactsList.remove(position);
            trackingContactsList.add(new Pair<>(mobile, false));
            HomeActivity.this.runOnUiThread(() -> {
                trackingListViewAdapter.notifyDataSetChanged();
                updateNotAvailableStatusOnMap(mobile);
            });

        });
        socketManager.connect(new IConnectionListener() {
            @Override
            public void onConnect() throws RemoteException {
                socketManager.sendEventMessage("connectedMobile", loggedInMobile);
                subscribeToTrackContacts();
            }

            @Override
            public void onDisconnect() {

            }
        });
    }

    private void initializeAddContactBtn() {
        addContactBtn = findViewById(R.id.add_contact_btn);
        addContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
                pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
                startActivityForResult(pickContactIntent, REQUEST_CODE_PICK_CONTACT);
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
//        if(TrackMeService.isRunning()) {
//            sharingSwitch.setChecked(true);
//        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(loggedInMobile.isEmpty()) {
            finish();
        }
//        trackingContactsList.clear();

//        trackingContactsList.addAll(TrackDetailsDB.db().getContactsToTrackLocation());
        sharingContactsList.clear();
        sharingContactsList.addAll(TrackDetailsDB.db().getContactsToShareLocation());
        sharingListViewAdapter.notifyDataSetChanged();
        trackingListViewAdapter.notifyDataSetChanged();
//        subscribeToTrackContacts();
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
        sharingListView.setAdapter(sharingListViewAdapter);
        trackingListView.setAdapter(trackingListViewAdapter);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
    }

    private void showDialogToRemoveContact(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                .setTitle("Confirm")
                .setMessage("Do not share location with " + sharingContactsList.get(position) +" ?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteContactFromSharingList(position);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void deleteContactFromSharingList(int position) {
        TrackingServiceClient client = RestClient.getTrackingServiceClient();
        client.deleteContactFromSharingLocationList(new ShareLocationRequest(loggedInMobile, sharingContactsList.get(position), ""))
                .enqueue(new Callback<ServiceResponse>() {
                    @Override
                    public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                        if(response.isSuccessful()) {
                            ServiceResponse serviceResponse = response.body();
                            if(serviceResponse.isSuccess()) {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("contactToRemove", sharingContactsList.get(position));
                                    jsonObject.put("publisher", loggedInMobile);
                                    socketManager.sendEventMessage("removeContact", jsonObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                TrackDetailsDB.db().deleteContactFromSharingList(sharingContactsList.get(position));
                                sharingContactsList.remove(position);
                                sharingListViewAdapter.notifyDataSetChanged();
                                Toast.makeText(HomeActivity.this, "Contact removed from sharing list", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(HomeActivity.this, "Unable to remove: " + serviceResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ServiceResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "Internal error, " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
        if(locationSharingStatus) {
            sharingSwitch.setChecked(true);
        }
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
                    Intent service = new Intent(getApplicationContext(), LocationService.class);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        startService(service);
                    } else {
                        startForegroundService(service);
                    }
                } else {
                    Intent service = new Intent(getApplicationContext(), LocationService.class);
                    stopService(service);
                }
            }
        });
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
//                    sharingSwitch.setChecked(true);
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


    private void subscribeToTrackContacts() {
        for (final Pair<String, Boolean> contactLiveStatusPair : trackingContactsList) {
            subscribeToContact(contactLiveStatusPair.first);
        }
    }

    private int getPositionInTrackingList(String contact) {
        int position = 0;
        boolean found = false;
        for(Pair<String, Boolean> numberWithStatusPair : trackingContactsList) {
            if(contact.equals(numberWithStatusPair.first)) {
                found = true;
                break;
            } else {
                position++;
            }
        }
        return found ? position : -1;
    }

    private void subscribeToContact(final String contact) {
        socketManager.sendEventMessage("subscribe", contact, new IAckListener() {
            @Override
            public void onReply(Object[] args) {
                final JSONObject data = (JSONObject) args[0];
                try {
                    int position = getPositionInTrackingList(contact);
                    trackingContactsList.remove(position);
                    if ("connected".equals(data.getString("status"))) {
                        trackingContactsList.add(new Pair<>(contact, true));
                        HomeActivity.this.runOnUiThread(() -> {
                            try {
                                if(data.get("lastLocation") != null) {
                                    JSONObject lastLocationObj = null;

                                    lastLocationObj = data.getJSONObject("lastLocation");
                                    updateCurrentTrackingPosition(contact, lastLocationObj.getDouble("lat"),
                                            lastLocationObj.getDouble("lng"), "Last location");

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            trackingListViewAdapter.notifyDataSetChanged();

                        });

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
                                        updateCurrentTrackingPosition(contact, lat, lng, "Current location");
                                    }
                                });
                            }
                        });
                    } else {
                        trackingContactsList.add(new Pair<>(contact, false));
                        HomeActivity.this.runOnUiThread(() -> {
                            trackingListViewAdapter.notifyDataSetChanged();

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

    private void readLoggedInUserDetailsAndLocationSharingStatus() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String name = preferences.getString("Name", "");
        locationSharingStatus = preferences.getBoolean("locationSharingStatus", false);
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

    private void updateCurrentTrackingPosition(String trackingContact, double lat, double lng, String snippet) {
        LatLng latLng = new LatLng(lat, lng);
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
                    .draggable(false).anchor(0.5f, 0.7f)
                    .icon(BitmapDescriptorFactory.defaultMarker(lineAndIconColorPair.second));

            currLocationMarker = googleMap.addMarker(markerOptions);
            currLocationMarker.showInfoWindow();
            currLocationMarkerMap.put(trackingContact, currLocationMarker);
        }
        if(!snippet.equals(currLocationMarker.getSnippet())) {
            currLocationMarker.hideInfoWindow();
            currLocationMarker.setSnippet(snippet);
            currLocationMarker.showInfoWindow();
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
                showLogoutDialog();
                return true;
//            case R.id.share_location:
//                openSelectContactsActivity();
//                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                .setTitle("Logout")
                .setMessage("Are you sure to logout ?")
                .setCancelable(true)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       return;
                    }
                })
                .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            if(locationSharingService != null) {
                                locationSharingService.stopLocationSharing();
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        stopService(new Intent(getApplicationContext(), LocationService.class));
                        socketManager.disconnect();
                        clearSavedLoginDetails();
                        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
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
        socketManager.disconnect();
        unbindService(locationSharingServiceConnection);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode)
        {
            case (REQUEST_CODE_PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK)
                {
                    Uri contactUri = data.getData();
                    // We only need the NUMBER column, because there will be only one row in the result
                    String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER};

                    // Perform the query on the contact to get the NUMBER column
                    // We don't need a selection or sort order (there's only one result for the given URI)
                    // CAUTION: The query() method should be called from a separate thread to avoid blocking
                    // your app's UI thread. (For simplicity of the sample, this code doesn't do that.)
                    // Consider using CursorLoader to perform the query.
                    Cursor cursor = getContentResolver()
                            .query(contactUri, projection, null, null, null);
                    cursor.moveToFirst();

                    // Retrieve the phone number from the NUMBER column
                    int columnName = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int columnNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String name = cursor.getString(columnName);
                    String number = cursor.getString(columnNumber);
                    number = number.replace("-","");
                    number = number.replace(" ","");
                    if(number.length() > 10) {
                        number = number.substring(number.length() - 10);
                    }
                    if(isConnectedToInternet()) {
                        if(sharingContactsList.contains(number)) {
                            Toast.makeText(HomeActivity.this, "Already added in contact list", Toast.LENGTH_SHORT).show();
                        } else {
                            addContactIfRegistered(number, name);
                        }
                    } else {
                        Toast.makeText(HomeActivity.this, "You are not online !!", Toast.LENGTH_SHORT).show();
                    }
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void addContactIfRegistered(String number, String name) {
        TrackingServiceClient client = RestClient.getTrackingServiceClient();
        Call<ServiceResponse> call = client.addContactForSharingLocation(new ShareLocationRequest(loggedInMobile, number, name));
        call.enqueue(new Callback<ServiceResponse>() {
            @Override
            public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                if(response.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);
                    ServiceResponse serviceResponse = response.body();
                    if(serviceResponse.isSuccess()) {
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("contactToAdd", number);
                            jsonObject.put("publisher", loggedInMobile);
                            socketManager.sendEventMessage("addContact", jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        sharingContactsList.add(number);
                        sharingListViewAdapter.notifyDataSetChanged();
                        TrackDetailsDB.db().addContactToShareLocation(number);
                        Toast.makeText(HomeActivity.this, "Contact added to the list !!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(HomeActivity.this, serviceResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ServiceResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onClick(int position) {
        showDialogToRemoveContact(position);
    }
}


