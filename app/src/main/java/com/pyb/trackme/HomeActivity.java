package com.pyb.trackme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.msebera.android.httpclient.Header;


public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

//    private ListView trackListView;
    private List<String> coordinatesList;
    private String loggedInName;
    private String loggedInMobile;
    private TextView greetingsView;
    private EditText mobileNumberToTrack;
    private Button trackBtn;
    private DrawerLayout mDrawerLayout;
    private ListView sharingListView;
    private ListView trackingListView;
    private ArrayAdapter mStringAdaptor;
    private String[] mStringOfPlanets = {"one", "two"};
    private List<String> sharingContactsList;
    private List<String> trackingContactsList;
    private LinearLayout sharingContactsLayout;
    private LinearLayout trackingContactsLayout;
    private Switch sharingSwitch;

    private Socket mSocket;
    private Emitter.Listener onSocketConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("ok", "ok");
            onServerConnection();
        }
    };

    private void onServerConnection() {
        mSocket.emit("connectedMobile", loggedInMobile);
        runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(HomeActivity.this, "Connected to server !!", Toast.LENGTH_SHORT).show();
//                trackBtn.setEnabled(true);
            }
        });


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setLoggedInUserDetails(getIntent().getExtras());
        sharingContactsList = new ArrayList<>();
        trackingContactsList = new ArrayList<>();
        getTrackingDetailsFromServer();
//        trackBtn = findViewById(R.id.track_btn);
        greetingsView = findViewById(R.id.greetings);
        mobileNumberToTrack = findViewById(R.id.mobile_number_track);
//        trackListView = findViewById(R.id.track_list_view);
        coordinatesList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(
                HomeActivity.this,
                R.layout.contact_items_listview,
                R.id.textView, coordinatesList
        );
//        trackListView.setAdapter(arrayAdapter);

        sharingSwitch = findViewById(R.id.sharing_switch);
        addListenersToSharingSwitch();
        greetingsView.setText("Hello " + loggedInName);
//        trackBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                onTrackBtnClick();
//            }
//        });
        try {
//            trackBtn.setEnabled(false);
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

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ((TextView)mDrawerLayout.findViewById(R.id.drawer_header)).setText(loggedInName);
        sharingContactsLayout = mDrawerLayout.findViewById(R.id.sharing_contacts_layout);
        trackingContactsLayout = mDrawerLayout.findViewById(R.id.tracking_contacts_layout);
        sharingListView = (ListView) findViewById(R.id.sharing_list_view);
        trackingListView = findViewById(R.id.tracking_list_view);
        // init adaptor
        mStringAdaptor = new ArrayAdapter<String>(this, R.layout.drawer_list_item, mStringOfPlanets);
        sharingListView.setAdapter(new NavListViewAdapter(this, sharingContactsList));
        trackingListView.setAdapter(new NavListViewAdapter(this, trackingContactsList));
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
    }

    private void addListenersToSharingSwitch() {
    }

    private void getTrackingDetailsFromServer() {
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        requestParams.put("mobile", loggedInMobile);
        APIClient.put("user/track/details", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if(response.getBoolean("success")) {
                        JSONArray arr = response.getJSONArray("sharingWith");
                        for(int i=0; i < arr.length(); i++) {
                            sharingContactsList.add(arr.getString(i));
                        }
                        if(sharingContactsList.isEmpty()) {
                            sharingContactsLayout.setVisibility(View.GONE);
                        }
                        arr = response.getJSONArray("tracking");
                        for(int i=0; i < arr.length(); i++) {
                            trackingContactsList.add(arr.getString(i));
                        }
                        if(trackingContactsList.isEmpty()) {
                            trackingContactsLayout.setVisibility(View.GONE);
                        }
                        ((ArrayAdapter) trackingListView.getAdapter()).notifyDataSetChanged();
                        ((ArrayAdapter) sharingListView.getAdapter()).notifyDataSetChanged();
                    } else {
                        Toast.makeText(HomeActivity.this, "Failed to share location, please try after sometime !!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
//                progressBar.setVisibility(View.GONE);
                Toast.makeText(HomeActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
//                progressBar.setVisibility(View.GONE);
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
                    new UpdateCurrentTrackingPosition(lat, lng).invoke();
                }
            });
        }
    };

    private void setLoggedInUserDetails(Bundle bundle) {
        loggedInName = bundle.getString("name", "");
        loggedInMobile = bundle.getString("mobile", "");
    }

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


    private void clearSavedLoginDetails() {
        SharedPreferences preferences = getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Mobile", "");
        editor.putString("Name", "");
        editor.commit();
        this.loggedInName = "";
        this.loggedInMobile = "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        googleMap.clear();
        if(mSocket.connected()) {
            String mobileNo = mobileNumberToTrack.getText().toString();
            mSocket.disconnect();
            mSocket.off(mobileNo, onNewMessage);
        }
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

    private class ExecuteAsynTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... mobile) {
            setLoggedInUserDetails(HomeActivity.this.getIntent().getExtras());

            return null;
        }

        protected void onProgressUpdate(Void... progress) {
//            setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Void result) {
        }

    }
}


