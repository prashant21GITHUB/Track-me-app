package com.pyb.trackme;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pyb.trackme.services.LocationService;
import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.socket.SocketManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.msebera.android.httpclient.Header;


public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private List<String> coordinatesList;
    private String loggedInName;
    private String loggedInMobile;
    private DrawerLayout mDrawerLayout;
    private ListView sharingListView;
    private ListView trackingListView;
    private List<String> sharingContactsList;
    private List<String> trackingContactsList;
    private LinearLayout sharingContactsLayout;
    private LinearLayout trackingContactsLayout;
    private Switch sharingSwitch;
    private final SocketManager socketManager = SocketManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setLoggedInUserDetails(getIntent().getExtras());
        initializeLocationSharingSwitch();
        intializeDrawerLayout(toolbar);
        initializeMap();
        connectToServer();
    }

    private void initializeMap() {
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }


    private void intializeDrawerLayout(Toolbar toolbar) {
        sharingContactsList = new ArrayList<>();
        trackingContactsList = new ArrayList<>();
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ((TextView)mDrawerLayout.findViewById(R.id.drawer_header)).setText(loggedInName);
        sharingContactsLayout = mDrawerLayout.findViewById(R.id.sharing_contacts_layout);
        trackingContactsLayout = mDrawerLayout.findViewById(R.id.tracking_contacts_layout);
        sharingListView =  findViewById(R.id.sharing_list_view);
        trackingListView = findViewById(R.id.tracking_list_view);
        sharingListView.setAdapter(new NavListViewAdapter(this, sharingContactsList));
        trackingListView.setAdapter(new NavListViewAdapter(this, trackingContactsList));
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
    }

    private void initializeLocationSharingSwitch() {
        sharingSwitch = findViewById(R.id.sharing_switch);
        final ImageView liveSharingImage = findViewById(R.id.live_sharing_image);
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
                    startService(new Intent(getApplicationContext(), LocationService.class));
                    liveSharingImage.setVisibility(View.VISIBLE);
                } else {
                    stopService(new Intent(getApplicationContext(), LocationService.class));
                    liveSharingImage.setVisibility(View.GONE);
                }
            }
        });
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
                        if(!sharingContactsList.isEmpty()) {
                            sharingContactsLayout.setVisibility(View.VISIBLE);
                        }
                        arr = response.getJSONArray("tracking");
                        trackingContactsList.clear();
                        for(int i=0; i < arr.length(); i++) {
                            trackingContactsList.add(arr.getString(i));
                        }
                        if(!trackingContactsList.isEmpty()) {
                            trackingContactsLayout.setVisibility(View.VISIBLE);
                        }
                        ((ArrayAdapter) trackingListView.getAdapter()).notifyDataSetChanged();
                        ((ArrayAdapter) sharingListView.getAdapter()).notifyDataSetChanged();
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

    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if(mCurrLocationMarker == null) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("Current Position");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    mCurrLocationMarker = googleMap.addMarker(markerOptions);
                }
                mCurrLocationMarker.setPosition(latLng);
                //move map camera
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
            }
        }
    };

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ProgressBar progressBar;
    private final int ACCESS_FINE_LOCATION_PERMISSION = 1;
    private final int INTERNET_PERMISSION = 2;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private Marker mCurrLocationMarker;
    private Location mLastLocation;

    private LatLng lastLocation;

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
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
                    updateCurrentTrackingPosition(lat, lng);
                }
            });
        }
    };

    private void setLoggedInUserDetails(Bundle bundle) {
        loggedInName = bundle.getString("name", "");
        loggedInMobile = bundle.getString("mobile", "");
    }




    private void clearSavedLoginDetails() {
        SharedPreferences preferences = getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Mobile", "");
        editor.putString("Name", "");
        editor.commit();
        this.loggedInName = "";
        this.loggedInMobile = "";
    }

    private void updateCurrentTrackingPosition(double lat, double lng) {
        LatLng latLng = new LatLng(lat, lng);
        if(lastLocation != null) {
            PatternItem patternItem = new Dot();
            Polyline line = googleMap.addPolyline(new PolylineOptions()
                    .add(lastLocation, latLng)
                    .width(10)
                    .endCap(new RoundCap())
                    .geodesic(true)
                    .pattern(Arrays.asList(patternItem))
                    .jointType(JointType.ROUND)
                    .color(Color.MAGENTA));
        } else {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Start Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            googleMap.addMarker(markerOptions);
        }
        lastLocation = latLng;
        if(mCurrLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            mCurrLocationMarker = googleMap.addMarker(markerOptions);
        }
        mCurrLocationMarker.setPosition(latLng);
        //move map camera
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
    }

    private void connectToServer() {
        socketManager.connect(new IConnectionListener() {
            @Override
            public void onConnect() {
                socketManager.sendEventMessage("connectedMobile", loggedInMobile);
                runOnUiThread(new Runnable() {
                    public void run() {
                        syncTrackingDetailsFromServer();
                        Toast.makeText(HomeActivity.this, "Connected to server !!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDisconnect() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(HomeActivity.this, "You are not connected anymore !!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
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
                clearSavedLoginDetails();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
                return true;
            case R.id.share_location:
                Intent i = new Intent(HomeActivity.this, SelectContactsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("name", loggedInName);
                bundle.putString("mobile", loggedInMobile);
                i.putExtras(bundle);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        googleMap.clear();
        socketManager.disconnect();
    }
}


