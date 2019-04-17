package com.pyb.trackme;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
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
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private SearchView searchView;
    private ListView trackListView;
    private List<String> coordinatesList;
    private String loggedInName;
    private String loggedInMobile;
    private TextView greetingsView;
    private EditText mobileNumberToTrack;
    private Button trackBtn;

    public static final int RequestPermissionCode  = 1 ;
    public static final int EXIT_CODE = 111;

    private Socket mSocket;
    private Emitter.Listener onSocketConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("ok", "ok");
            onServerConnection();
        }
    };

    private void onServerConnection() {
        runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(HomeActivity.this, "Connected to server !!", Toast.LENGTH_SHORT).show();
                trackBtn.setEnabled(true);
            }
        });


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLoggedInUserDetails(getIntent().getExtras());
        setContentView(R.layout.home_activity);
        trackBtn = findViewById(R.id.track_btn);
        greetingsView = findViewById(R.id.greetings);
        mobileNumberToTrack = findViewById(R.id.mobile_number_track);
        trackListView = findViewById(R.id.track_list_view);
        coordinatesList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(
                HomeActivity.this,
                R.layout.contact_items_listview,
                R.id.textView, coordinatesList
        );
        trackListView.setAdapter(arrayAdapter);

        greetingsView.setText("Hello " + loggedInName);
        trackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTrackBtnClick();
            }
        });
        try {
            trackBtn.setEnabled(false);
            mSocket = IO.socket("http://127.0.0.1:3000/");
            mSocket.on(Socket.EVENT_CONNECT, onSocketConnect);
            mSocket.connect();
        } catch (URISyntaxException e) {}

        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
//        this.locationListener = new HomeActivityLocationListener();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private GoogleMap googleMap;
    @Override
    public void onMapReady(GoogleMap map) {
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.
        LatLng sydney = new LatLng(-33.852, 151.211);
        googleMap = map;
        googleMap.addMarker(new MarkerOptions().position(sydney)
                .title("Marker in Sydney"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(5000); // two minute interval
//        mLocationRequest.setFastestInterval(120000);
//        mLocationRequest.setSmallestDisplacement(1);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
//
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(this,
//                    Manifest.permission.ACCESS_FINE_LOCATION)
//                    == PackageManager.PERMISSION_GRANTED) {
//                //Location Permission already granted
//                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
//                googleMap.setMyLocationEnabled(true);
//            } else {
//                //Request Location Permission
//                checkLocationPermission();
//            }
//        }
//        else {
//            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
//            googleMap.setMyLocationEnabled(true);
//        }

    }

    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
//            progressBar.setVisibility(View.INVISIBLE);
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
//                if (mCurrLocationMarker != null) {
//                    mCurrLocationMarker.remove();
//                }

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

    private void onTrackBtnClick() {
        String mobileNo = mobileNumberToTrack.getText().toString();
        if(!ValidationUtils.isValidNumber(mobileNo)) {
            Toast.makeText(this, "Enter valid mobile number !!", Toast.LENGTH_SHORT).show();
        } else {
            if(mSocket.connected()) {
                if(!mSocket.hasListeners(mobileNo)) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("mobile", mobileNo);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mSocket.on(mobileNo, onNewMessage);
                    mSocket.emit("subscribe", jsonObject);
                }
                Toast.makeText(this, "Request sent !!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not connected to server !!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ArrayAdapter<String> arrayAdapter;
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

//                    arrayAdapter.addAll(lat, lng);
                    new UpdateCurrentTrackingPosition(lat, lng).invoke();

                    // add the message to view
//                    addMessage(username, message);
                }
            });
        }
    };

    private void setLoggedInUserDetails(Bundle bundle) {
        loggedInName = bundle.getString("name", "");
        loggedInMobile = bundle.getString("mobile", "");
    }

    @Override
    protected void onResume() {
        super.onResume();

//        EnableRuntimePermission();
    }

    public void GetContactsIntoArrayList(){

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);
        String name, number;
        while (cursor.moveToNext()) {

            name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

//            contactList.add(name + " "  + ":" + " " + number);
        }

        cursor.close();

    }

    public void EnableRuntimePermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, RequestPermissionCode);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }

//        if (ActivityCompat.shouldShowRequestPermissionRationale(
//                HomeActivity.this,
//                Manifest.permission.READ_CONTACTS))
//        {
//
//            Toast.makeText(HomeActivity.this,"CONTACTS permission allows us to Access contacts", Toast.LENGTH_LONG).show();
//
//        } else {
//
//            ActivityCompat.requestPermissions(HomeActivity.this,new String[]{
//                    Manifest.permission.READ_CONTACTS}, RequestPermissionCode);
//
//        }
    }

//    @Override
//    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {
//
//        switch (RC) {
//
//            case RequestPermissionCode:
//
//                if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {
//                    searchView.setEnabled(true);
//
//                    Toast.makeText(HomeActivity.this,"Permission Granted, Now your application can access CONTACTS.", Toast.LENGTH_LONG).show();
//
//                } else {
//
//                    Toast.makeText(HomeActivity.this,"Permission Canceled, Now your application cannot access CONTACTS.", Toast.LENGTH_LONG).show();
//                    this.finish();
//
//                }
//                break;
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_activity_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.logout:
                clearSavedLoginDetails();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivityForResult(intent, EXIT_CODE);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void clearSavedLoginDetails() {
        SharedPreferences preferences = getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Mobile", "");
        editor.putString("Password", "");
        editor.commit();
        this.loggedInName = "";
        this.loggedInMobile = "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mSocket.connected()) {
            String mobileNo = mobileNumberToTrack.getText().toString();
            mSocket.disconnect();
            mSocket.off(mobileNo, onNewMessage);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

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

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
//
//                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
//                        googleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        googleMap.clear();
    }

    private class UpdateCurrentTrackingPosition {
        private double lat;
        private double lng;

        public UpdateCurrentTrackingPosition(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public void invoke() {
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
    }
}


