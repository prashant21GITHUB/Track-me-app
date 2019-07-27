package com.pyb.trackme.activities;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
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
import com.pyb.trackme.R;
import com.pyb.trackme.TrackMeApplication;
import com.pyb.trackme.adapter.GroupInfo;
import com.pyb.trackme.adapter.GroupsExpandableListViewAdapter;
import com.pyb.trackme.adapter.IOnTrackingContactFocusListener;
import com.pyb.trackme.adapter.SharingExpandableListViewAdapter;
import com.pyb.trackme.adapter.TrackingExpandableListViewAdapter;
import com.pyb.trackme.cache.TrackDetailsDB;
import com.pyb.trackme.receiver.LocationServiceChangeReceiver;
import com.pyb.trackme.receiver.NetworkChangeReceiver;
import com.pyb.trackme.restclient.AddRemoveContactRequest;
import com.pyb.trackme.restclient.LoginServiceClient;
import com.pyb.trackme.restclient.MobileRequest;
import com.pyb.trackme.restclient.RestClient;
import com.pyb.trackme.restclient.ServiceResponse;
import com.pyb.trackme.restclient.TrackingDetailsResponse;
import com.pyb.trackme.restclient.TrackingServiceClient;
import com.pyb.trackme.selectMultipleContacts.contact.Contact;
import com.pyb.trackme.selectMultipleContacts.contact.ContactDescription;
import com.pyb.trackme.selectMultipleContacts.contact.ContactSortOrder;
import com.pyb.trackme.selectMultipleContacts.core.ContactPickerActivity;
import com.pyb.trackme.selectMultipleContacts.picture.ContactPictureType;
import com.pyb.trackme.services.LocationService;
import com.pyb.trackme.socket.IAckListener;
import com.pyb.trackme.socket.IConnectionListener;
import com.pyb.trackme.socket.IEventListener;
import com.pyb.trackme.socket.ISocketConnectionListener;
import com.pyb.trackme.socket.SocketManager;
import com.pyb.trackme.utils.ConnectionUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.pyb.trackme.fcm.MessageAction.STARTED_SHARING;
import static com.pyb.trackme.fcm.MessageAction.TRACKING_REQUEST;
import static com.pyb.trackme.selectMultipleContacts.core.ContactPickerActivity.GROUP_NAME;
import static com.pyb.trackme.selectMultipleContacts.core.ContactPickerActivity.RESULT_CONTACT_DATA;


public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback,
        IRemoveContactButtonClickListener, IOnTrackingContactFocusListener, IPerContactSwitchListener {

    private static final int REQUEST_CODE_PICK_SHARE_CONTACT = 131;
    private static final int REQUEST_CODE_PICK_TRACK_CONTACT = 133;
    private static final int SELECT_MULTIPLE_CONTACTS_REQUEST_CODE = 135;
    private String TRACKING_REQUEST_NOTI;
    private String STARTED_SHARING_NOTI;
    private final long DELAY_IN_MILLIS = 5000L;
    private final TrackDetailsDB db = TrackDetailsDB.db();
    private final boolean TEST_MODE = false;
    private final String TAG = "TrackMe_HomeActivity";

    private String loggedInName;
    private String loggedInMobile;
    private DrawerLayout mDrawerLayout;
    private ExpandableListView sharingContactsExpandableListView;
    private ExpandableListView trackingContactsExpandableListView;
    private List<String> sharingContactsList;
    private List<String> trackingContactsList;
    private Switch sharingSwitch;
    private CompoundButton.OnCheckedChangeListener sharingSwitchListener;
    private String NOTIFICATION_CHANNEL_ID = "TrackMe_Notification_Channel";
    private SocketManager socketManager;
    private String LOGIN_PREF_NAME;
    private SharingExpandableListViewAdapter sharingExpandableListViewAdapter;
    private TrackingExpandableListViewAdapter trackingExpandableListViewAdapter;
    private ProgressBar progressBar;
    private boolean locationSharingStatus;
    private TextView connectionAlertTextView;
    private TextView locationAlertTextView;
    private NetworkChangeReceiver networkChangeReceiver;
    private LocationServiceChangeReceiver locationServiceChangeReceiver;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentFocussedContactOnMap;
    private ISocketConnectionListener socketConnectionListener = new ISocketConnectionListener() {
        @Override
        public void onConnect(boolean alreadyConnected) {
            if(!alreadyConnected) {
                socketManager.sendEventMessage("connectedMobile", loggedInMobile);
            }
            subscribeToTrackContacts();
        }

        @Override
        public void onDisconnect() {

        }
    };

    private boolean isActivityRunning;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName + "_Login";
        TRACKING_REQUEST_NOTI = getApplicationInfo().packageName + "_" + TRACKING_REQUEST.name();
        STARTED_SHARING_NOTI = getApplicationInfo().packageName + "_" + STARTED_SHARING.name();
        readLoggedInUserDetailsAndLocationSharingStatus();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        connectionAlertTextView = findViewById(R.id.alert_connection);
        locationAlertTextView = findViewById(R.id.alert_location);
        sharingContactsExpandableListView = findViewById(R.id.expandable_sharing_contacts_view);
        trackingContactsExpandableListView = findViewById(R.id.expandable_tracking_contacts_view);
        swipeRefreshLayout = findViewById(R.id.pullToRefresh);
        attachItemClickListeners();

        socketManager = ((TrackMeApplication) getApplication()).getSocketManager();
        handleIntents();

        initializeLocationSharingSwitch();
        initializeDrawerLayout(toolbar);
        initializeMap();
        initializeSwipeRefreshLayout();
        createNotificationChannel();

        initializeReceiverForNetworkEvents();
        initializeReceiverForLocationServiceEvents();
        progressBar = findViewById(R.id.progressBarHomeActivity);
        handler = new Handler(Looper.getMainLooper());
    }

    private void handleIntents() {
        if (getIntent() != null) {
            boolean isDataPresentInPref = db.readDataFromPref(getApplicationContext());
            if (!isDataPresentInPref) {
                getTrackingDetailsFromServerAndInitiliazeSocket();
            } else {
                initializeSharingAndTrackingContactsList();
                if (ConnectionUtils.isConnectedToInternet(this)) {
                    connectToServer();
                }
            }

            handlePushNotificationsIntents(getIntent());
        } else {
            initializeSharingAndTrackingContactsList();
            if (ConnectionUtils.isConnectedToInternet(this)) {
                connectToServer();
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        handlePushNotificationsIntents(intent);

    }

    private void handlePushNotificationsIntents(Intent intent) {
        if(TRACKING_REQUEST_NOTI.equals(intent.getAction())) {
            String subscriber = intent.getStringExtra("SUBSCRIBER");
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("Add " + subscriber +" in your sharing list")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!sharingContactsList.contains(subscriber)) {
                                //TODO: how to get name of the subscriber
                                addSharingContactIfRegistered(subscriber, subscriber);
                            }
                            mDrawerLayout.openDrawer(Gravity.START, true);
                            trackingContactsExpandableListView.collapseGroup(0);
                            sharingContactsExpandableListView.expandGroup(0);
                        }
                    });
            builder.create().show();

        } else if(STARTED_SHARING_NOTI.equals(intent.getAction())) {
            String publisher = intent.getStringExtra("PUBLISHER");
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("Add " + publisher +" in your tracking list")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!trackingContactsList.contains(publisher)) {
                                //TODO: how to get name of the publisher
                                addTrackingContactIfRegistered(publisher, publisher);
                            }
                            mDrawerLayout.openDrawer(Gravity.START, true);
                            trackingContactsExpandableListView.expandGroup(0);
                            sharingContactsExpandableListView.collapseGroup(0);
                        }
                    });
            builder.create().show();
        }
    }

    private void attachItemClickListeners() {
        trackingContactsExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            //In our case, groupPosition is always 1 as there is 1 group ony
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Toast.makeText(
                        getApplicationContext(),
                        "clicked", Toast.LENGTH_SHORT)
                        .show();
                mDrawerLayout.closeDrawer(Gravity.START, true);
                String contact = trackingContactsList.get(childPosition);
                currentFocussedContactOnMap = contact;
                Marker marker = currLocationMarkerMap.get(contact);
                if (marker != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16));
                    marker.showInfoWindow();
                }
                return true;
            }
        });
    }

    private void initializeSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                getTrackingDetailsFromServerAndInitiliazeSocket();
            }
        });
    }

    private boolean alreadyConnectedToServer;

    private void connectToServer() {
        if (alreadyConnectedToServer) {
            subscribeToTrackContacts();
            return;
        }
        socketManager.onEvent("publisherAvailable", new IEventListener() {
            @Override
            public void onEvent(String event, Object[] args) {
                String mobile = (String) args[0];
                if(db.getTrackingStatus(mobile)) {
                    subscribeToContact(mobile);
                    if(isActivityRunning) {
                        HomeActivity.this.runOnUiThread(() -> {
                            Toast.makeText(HomeActivity.this, mobile + " started sharing his location", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
        socketManager.onEvent("publisherNotAvailable", (event, args) -> {
            String mobile = (String) args[0];
            if(db.getTrackingStatus(mobile)) {
                HomeActivity.this.runOnUiThread(() -> {
                    if(isActivityRunning) {
                        Toast.makeText(HomeActivity.this, mobile + " has stopped sharing location !!", Toast.LENGTH_SHORT).show();
                    }
                    updateSnippetOnMap(mobile, "Not live");
                });
            }
        });
        socketManager.onEvent("notLive", (event, args) -> {
            String mobile = (String) args[0];
            if(db.getTrackingStatus(mobile)) {
                HomeActivity.this.runOnUiThread(() -> {
                    if (isActivityRunning) {
                        Toast.makeText(HomeActivity.this, mobile + " is not live !!", Toast.LENGTH_SHORT).show();
                    }
                    updateSnippetOnMap(mobile, "Not live");
                });
            }
        });
        socketManager.connect(socketConnectionListener);
        alreadyConnectedToServer = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (loggedInMobile.isEmpty()) {
            finish();
        }
    }

    private void initializeMap() {
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    private void initializeDrawerLayout(Toolbar toolbar) {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ((TextView) mDrawerLayout.findViewById(R.id.drawer_header)).setText(loggedInName);
        ImageView addShareContactBtn = findViewById(R.id.add_share_contact_btn);
        addShareContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSelectContactActivity(REQUEST_CODE_PICK_SHARE_CONTACT);
            }
        });
        ImageView addTrackContactBtn = findViewById(R.id.add_track_contact_btn);
        addTrackContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSelectContactActivity(REQUEST_CODE_PICK_TRACK_CONTACT);
            }
        });
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.setDrawerSlideAnimationEnabled(true);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
        ImageView addGrpBtn = mDrawerLayout.findViewById(R.id.add_group_btn);
        addGrpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.READ_CONTACTS)
                            != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(HomeActivity.this,
                                Manifest.permission.READ_CONTACTS)) {

                            new AlertDialog.Builder(HomeActivity.this)
                                    .setTitle("Read Contacts permission needed")
                                    .setMessage("This app needs the Read contacts permission, please accept to add a group")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            //Prompt the user once explanation has been shown
                                            ActivityCompat.requestPermissions(HomeActivity.this,
                                                    new String[]{Manifest.permission.READ_CONTACTS},
                                                    MY_PERMISSIONS_READ_CONTACTS);
                                        }
                                    })
                                    .create()
                                    .show();


                        } else {
                            // No explanation needed, we can request the permission.
                            ActivityCompat.requestPermissions(HomeActivity.this,
                                    new String[]{Manifest.permission.READ_CONTACTS},
                                    MY_PERMISSIONS_READ_CONTACTS);
                        }
                        return;
                    }
                }
                Intent intent = new Intent(HomeActivity.this, ContactPickerActivity.class)
//                        .putExtra(ContactPickerActivity.EXTRA_THEME, true ? R.style.Theme_Dark : R.style.Theme_Light)
                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_BADGE_TYPE, ContactPictureType.ROUND.name())
                        .putExtra(ContactPickerActivity.EXTRA_ONLY_CONTACTS_WITH_PHONE, true)
                        .putExtra(ContactPickerActivity.EXTRA_SELECT_CONTACTS_LIMIT, 10)
                        .putExtra(ContactPickerActivity.EXTRA_SHOW_CHECK_ALL, false)
                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION, ContactDescription.PHONE.name())
                        .putExtra("EXTRA_WITH_GROUP_TAB", false)
//                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_SORT_ORDER, ContactSortOrder.FIRST_NAME.name());
                startActivityForResult(intent, SELECT_MULTIPLE_CONTACTS_REQUEST_CODE);
            }
        });
    }

    private void openSelectContactActivity(int REQUEST_CODE_PICK_CONTACT) {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        startActivityForResult(pickContactIntent, REQUEST_CODE_PICK_CONTACT);
    }

    private void initializeSharingAndTrackingContactsList() {
        if(TEST_MODE) {
            sharingContactsList = Arrays.asList("9999999999", "8888888888");
            trackingContactsList = Arrays.asList("7777777777", "6666666666");
        } else {
            sharingContactsList = new ArrayList<>(db.getContactsToShareLocation());
            trackingContactsList = new ArrayList<>(db.getContactsToTrackLocation());
        }
        sharingExpandableListViewAdapter = new SharingExpandableListViewAdapter(this, sharingContactsList, this, this);
        trackingExpandableListViewAdapter = new TrackingExpandableListViewAdapter(this, trackingContactsList, this, this, this);
        sharingContactsExpandableListView.setAdapter(sharingExpandableListViewAdapter);
        trackingContactsExpandableListView.setAdapter(trackingExpandableListViewAdapter);
        sharingContactsExpandableListView.expandGroup(0);
    }

    private void showDialogToRemoveSharingContact(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                .setTitle("Confirm")
                .setMessage("Stop location sharing with " + sharingContactsList.get(position) + " ?")
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

    private void showDialogToRemoveTrackingContact(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                .setTitle("Confirm")
                .setMessage("Stop tracking " + trackingContactsList.get(position) + " ?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteContactFromTrackingList(position);
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
        client.deleteContactFromSharingLocationList(new AddRemoveContactRequest(loggedInMobile, sharingContactsList.get(position), ""))
                .enqueue(new Callback<ServiceResponse>() {
                    @Override
                    public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                        if (response.isSuccessful()) {
                            ServiceResponse serviceResponse = response.body();
                            if (serviceResponse.isSuccess()) {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("contactToRemove", sharingContactsList.get(position));
                                    jsonObject.put("publisher", loggedInMobile);
                                    socketManager.sendEventMessage("removeContact", jsonObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                db.deleteContactFromSharingList(sharingContactsList.get(position));
                                sharingContactsList.remove(position);
                                sharingExpandableListViewAdapter.notifyDataSetChanged();
                                if (sharingContactsList.isEmpty()) {
                                    stopLocationSharingService();
                                    sharingSwitch.setChecked(false);
                                }
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

    private void deleteContactFromTrackingList(int position) {
        TrackingServiceClient client = RestClient.getTrackingServiceClient();
        client.deleteTrackingContact(new AddRemoveContactRequest(loggedInMobile, trackingContactsList.get(position), ""))
                .enqueue(new Callback<ServiceResponse>() {
                    @Override
                    public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                        if (response.isSuccessful()) {

                            ServiceResponse serviceResponse = response.body();
                            if (serviceResponse.isSuccess()) {
                                unsubscribeToContact(trackingContactsList.get(position));
                                db.deleteContactFromTrackingList(trackingContactsList.get(position));
                                trackingContactsList.remove(position);
                                trackingExpandableListViewAdapter.notifyDataSetChanged();
                                Toast.makeText(HomeActivity.this, "Contact removed from tracking list", Toast.LENGTH_SHORT).show();
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

    private void initializeLocationSharingSwitch() {
        sharingSwitch = findViewById(R.id.sharing_switch);
        if (locationSharingStatus) {
            sharingSwitch.setChecked(true);
            startLocationSharingService();
        }
        sharingSwitchListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        checkLocationPermission();
                        sharingSwitch.setChecked(false);
                        return;
                    }
                    if (!ConnectionUtils.isLocationServiceOn(HomeActivity.this)) {
                        sharingSwitch.setChecked(false);
                        buildAlertMessageNoGps();
                        return;
                    }
                    if (!ConnectionUtils.isConnectedToInternet(HomeActivity.this)) {
                        buildNoInternetDialog();
                        sharingSwitch.setChecked(false);
                        return;
                    }
                    if (sharingContactsList.isEmpty() || !db.isSharingOnForAtLeastOneContact()) {
                        sharingSwitch.setChecked(false);
                        Toast.makeText(HomeActivity.this, "Select contacts with whom you want to share your location !!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    startLocationSharingService();
                } else {
                    stopLocationSharingService();
                }
            }
        };
        sharingSwitch.setOnCheckedChangeListener(sharingSwitchListener);
    }

    private void startLocationSharingService() {
        Intent service = new Intent(getApplicationContext(), LocationService.class);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(service);
        } else {
            startForegroundService(service);
        }
        locationSharingStatus = true;
        new Thread(() -> {
            saveLocationSharingStatusInPref(true);
        }).start();

    }

    private void stopLocationSharingService() {
        Intent service = new Intent(getApplicationContext(), LocationService.class);
        stopService(service);
        locationSharingStatus = false;
        new Thread(() -> {
            saveLocationSharingStatusInPref(false);
        }).start();
    }

    private void saveLocationSharingStatusInPref(boolean status) {
        SharedPreferences preferences = this.getSharedPreferences(getApplicationInfo().packageName + "_Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("locationSharingStatus", status);
        editor.commit();
    }

    private void buildAlertMessageNoGps() {
        if (!isActivityRunning) {
            return;
        }
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
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            buildNoInternetDialog();
        }
        return isConnected;
    }

    private void buildNoInternetDialog() {
        if (!isActivityRunning) {
            return;
        }
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
    public static final int MY_PERMISSIONS_READ_CONTACTS = 199;

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
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
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
        for (String contact : trackingContactsList) {
            if(db.getTrackingStatus(contact)) {
                subscribeToContact(contact);
            }
        }
    }

    private void subscribeToContact(final String contact) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("publisher", contact);
            jsonObject.put("subscriber", loggedInMobile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socketManager.sendEventMessage("subscribe", jsonObject, new IAckListener() {
            @Override
            public void onReply(Object[] args) {
                final JSONObject data = (JSONObject) args[0];

                try {
                    showLastLocation(data, contact);
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
                                        updateCurrentTrackingPosition(contact, lat, lng, "Current location");
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

    private void unsubscribeToContact(String contact) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("publisher", contact);
            jsonObject.put("subscriber", loggedInMobile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socketManager.sendEventMessage("unsubscribe", jsonObject, new IAckListener() {
            @Override
            public void onReply(Object[] args) {
                final JSONObject data = (JSONObject) args[0];
                try {
                    if ("success".equals(data.getString("status"))) {
                        HomeActivity.this.runOnUiThread(() -> {
                                    Toast.makeText(HomeActivity.this, "Stopped tracking " + contact, Toast.LENGTH_SHORT).show();
                                    Marker marker = currLocationMarkerMap.get(contact);
                                    if (marker != null) {
                                        marker.hideInfoWindow();
                                        marker.setSnippet("Last location");
                                        marker.showInfoWindow();
                                    }
                                }
                        );
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showLastLocation(JSONObject data, String contact) {
        HomeActivity.this.runOnUiThread(() -> {
            try {
                if (data.get("lastLocation") != null) {
                    JSONObject lastLocationObj = null;

                    lastLocationObj = data.getJSONObject("lastLocation");
                    updateCurrentTrackingPosition(contact, lastLocationObj.getDouble("lat"),
                            lastLocationObj.getDouble("lng"), "Last location");

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            trackingExpandableListViewAdapter.notifyDataSetChanged();

        });
    }

    private GoogleMap googleMap;

    @Override
    public void onMapReady(GoogleMap map) {
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.
        if(map == null) {
            return;
        }
        LatLng india = new LatLng(20.5937, 78.9629);
        googleMap = map;
//        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
//        googleMap.addMarker(new MarkerOptions().position(india)
//                .title("India"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(india));
        googleMap.setTrafficEnabled(true);
        googleMap.setBuildingsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
//        googleMap.setMyLocationEnabled(true);
//         Enable / Disable zooming controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);

//         Enable / Disable my location button
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
//        googleMap.getUiSettings().setAllGesturesEnabled(true);
        // Enable / Disable Rotate gesture
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
//         Enable / Disable zooming functionality
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        // Enable / Disable Compass icon
        googleMap.getUiSettings().setCompassEnabled(true);
    }

    private void readLoggedInUserDetailsAndLocationSharingStatus() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String name = preferences.getString("Name", "");
        locationSharingStatus = preferences.getBoolean("locationSharingStatus", false);
        loggedInName = name;
        loggedInMobile = mobile;
    }

    private void clearAllDetailsFromPref() {
        SharedPreferences preferences = getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Name", "");
        editor.putString("Mobile", "");
        editor.putBoolean("locationSharingStatus", false);
        editor.remove("trackingContactStatus");
        editor.remove("sharingContactStatus");
        editor.remove("trackingContacts");
        editor.remove("sharingContacts");
        editor.commit();
        this.loggedInName = "";
        this.loggedInMobile = "";
        this.locationSharingStatus = false;
    }

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
            currLocationMarkerMap.put(trackingContact, currLocationMarker);
        }
        if(!snippet.equals(currLocationMarker.getSnippet())) {
            currLocationMarker.hideInfoWindow();
            currLocationMarker.setSnippet(snippet);
        }

        currLocationMarker.setPosition(latLng);
        if(currentFocussedContactOnMap == null) {
            currentFocussedContactOnMap = trackingContact;
        }
        //move map camera
        if(trackingContact.equals(currentFocussedContactOnMap)) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            currLocationMarker.showInfoWindow();
        }
    }

    private void updateSnippetOnMap(String mobile, String snippet) {
        Marker currLocationMarker = currLocationMarkerMap.get(mobile);
        if(currLocationMarker != null) {
            currLocationMarker.hideInfoWindow();
            currLocationMarker.setSnippet(snippet);
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
            case R.id.about_menu:
                showAboutAppDialog();
                return true;
            case R.id.manage_places:
                Intent intent = new Intent(this, ManagePlacesActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void showAboutAppDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                .setTitle("About TrackMe")
                .setIcon(R.drawable.about_me)
                .setMessage("You can share your live location to multiple contacts and also track their live locations.")
                .setCancelable(true)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
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
                        logoutFromServer();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void logoutFromServer() {
        progressBar.setVisibility(View.VISIBLE);
        LoginServiceClient client = RestClient.getLoginServiceClient();
        Call<ServiceResponse> call = client.logout(new MobileRequest(loggedInMobile));
        call.enqueue(new Callback<ServiceResponse>() {
            @Override
            public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                progressBar.setVisibility(View.GONE);
                if(response.isSuccessful()) {
                    ServiceResponse serviceResponse = response.body();
                    if(serviceResponse.isSuccess()) {
                        unsubscribeCurrentContactsGettingTracked();
                        stopService(new Intent(getApplicationContext(), LocationService.class));
                        socketManager.hardDisconnect();
                        clearAllDetailsFromPref();
                        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
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
    public void onResume() {
        super.onResume();
        isActivityRunning = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
//        filter.addAction(getPackageName() + "android.net.wifi.WIFI_STATE_CHANGED");  ///TODO check if this intent filter is needed ?
        registerReceiver(networkChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction("android.location.PROVIDERS_CHANGED");
        registerReceiver(locationServiceChangeReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        isActivityRunning = false;
    }

    private void initializeReceiverForNetworkEvents() {
        networkChangeReceiver = new NetworkChangeReceiver(new IConnectionListener() {
            private final Runnable onConnectRunner = () -> {
                if(ConnectionUtils.isConnectedToInternet(HomeActivity.this)) {
                    connectionAlertTextView.setVisibility(View.GONE);
                    if(!alreadyConnectedToServer) {
                        connectToServer();
                    }
                }
            };

            private final Runnable onDisconnectRunner = () -> {
                if(!ConnectionUtils.isConnectedToInternet(HomeActivity.this)) {
                    connectionAlertTextView.setText("Internet not available !!");
                    connectionAlertTextView.setVisibility(View.VISIBLE);
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
    }

    private void initializeReceiverForLocationServiceEvents() {
        locationServiceChangeReceiver = new LocationServiceChangeReceiver(new IConnectionListener() {
            private final Runnable onConnectRunner = () -> {
                if(ConnectionUtils.isLocationServiceOn(HomeActivity.this)) {
                    locationAlertTextView.setVisibility(View.GONE);
                }
            };

            private final Runnable onDisconnectRunner = () -> {
                if(!ConnectionUtils.isLocationServiceOn(HomeActivity.this)) {
                    locationAlertTextView.setText("Turn on location service !!");
                    locationAlertTextView.setVisibility(View.VISIBLE);
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
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("TrackMe_HomeActivity", "onStop");
        unregisterReceiver(networkChangeReceiver);
        unregisterReceiver(locationServiceChangeReceiver);
        db.saveDataInPref(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //token passed as null, so that it can remove all runners
        handler.removeCallbacksAndMessages(null);
        if(socketManager != null) {
            unsubscribeCurrentContactsGettingTracked();
            socketManager.offEvent("publisherAvailable");
            socketManager.offEvent("publisherNotAvailable");
            socketManager.offEvent("notLive");
            socketManager.softDisconnect(socketConnectionListener);
        }
        if(googleMap != null) {
            googleMap.clear();
        }
    }

    private void unsubscribeCurrentContactsGettingTracked() {
        for(String trackingContact : db.getCurrentContactsGettingTracked()) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("publisher", trackingContact);
                jsonObject.put("subscriber", loggedInMobile);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socketManager.sendEventMessage("unsubscribe", jsonObject, new IAckListener() {
                @Override
                public void onReply(Object[] args) {

                }
            });
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TrackMe";
            String description = "TrackMe";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == REQUEST_CODE_PICK_SHARE_CONTACT ||
                    requestCode == REQUEST_CODE_PICK_TRACK_CONTACT) {
                ContactInfo contactInfo = new ContactInfo(data).invoke();
                String name = contactInfo.getName();
                String number = contactInfo.getNumber();
                if(requestCode == REQUEST_CODE_PICK_SHARE_CONTACT) {
                    if (isConnectedToInternet()) {
                        if (sharingContactsList.contains(number)) {
                            Toast.makeText(HomeActivity.this, "Already added in your sharing list", Toast.LENGTH_SHORT).show();
                        } else {
                            addSharingContactIfRegistered(number, name);
                        }
                    } else {
                        Toast.makeText(HomeActivity.this, "You are not online !!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (isConnectedToInternet()) {
                        if (trackingContactsList.contains(number)) {
                            Toast.makeText(HomeActivity.this, "Already added in your tracking list", Toast.LENGTH_SHORT).show();
                        } else {
                            addTrackingContactIfRegistered(number, name);
                        }
                    } else {
                        Toast.makeText(HomeActivity.this, "You are not online !!", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if(requestCode == SELECT_MULTIPLE_CONTACTS_REQUEST_CODE) {
                String groupName = (String) data.getExtras().get(GROUP_NAME);
                List<Contact> contacts = (List<Contact>) data.getExtras().get(RESULT_CONTACT_DATA);
//                GroupsExpandableListViewAdapter adapter
//                        = new GroupsExpandableListViewAdapter(HomeActivity.this, Arrays.asList(new GroupInfo()))
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void addTrackingContactIfRegistered(String number, String name) {
        TrackingServiceClient client = RestClient.getTrackingServiceClient();
        Call<ServiceResponse> call = client.addTrackingContact(new AddRemoveContactRequest(loggedInMobile, number, name));
        call.enqueue(new Callback<ServiceResponse>() {
            @Override
            public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                if(response.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);
                    ServiceResponse serviceResponse = response.body();
                    if(serviceResponse.isSuccess()) {
                        trackingContactsList.add(number);
                        trackingExpandableListViewAdapter.notifyDataSetChanged();
                        db.addContactToTrackLocation(number);
                        db.updateTrackingStatus(number, false);
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

    private void addSharingContactIfRegistered(String number, String name) {
        TrackingServiceClient client = RestClient.getTrackingServiceClient();
        Call<ServiceResponse> call = client.addContactForSharingLocation(new AddRemoveContactRequest(loggedInMobile, number, name));
        call.enqueue(new Callback<ServiceResponse>() {
            @Override
            public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                if(response.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);
                    ServiceResponse serviceResponse = response.body();
                    if(serviceResponse.isSuccess()) {
                        sharingContactsList.add(number);
                        sharingExpandableListViewAdapter.notifyDataSetChanged();
                        db.addContactToShareLocation(number);
                        db.updateSharingStatus(number, false);
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
    public void onRemoveSharingContactButtonClick(int position) {
        showDialogToRemoveSharingContact(position);
    }

    @Override
    public void onRemoveTrackingContactButtonClick(int position) {
        showDialogToRemoveTrackingContact(position);
    }

    private void getTrackingDetailsFromServerAndInitiliazeSocket() {
        if(ConnectionUtils.isConnectedToInternet(this)) {
            syncTrackingDetailsFromServer();
        } else {
            Toast.makeText(this, "Internet connection is not available !!", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void syncTrackingDetailsFromServer() {
        TrackingServiceClient client = RestClient.getTrackingServiceClient();
        Call<TrackingDetailsResponse> call = client.getTrackingDetails(new MobileRequest(loggedInMobile));
        call.enqueue(new Callback<TrackingDetailsResponse>() {
            @Override
            public void onResponse(Call<TrackingDetailsResponse> call, Response<TrackingDetailsResponse> response) {
                ArrayList<String> sharingContacts;
                ArrayList<String> trackingContacts;
                if(response.isSuccessful()) {
                    TrackingDetailsResponse trackingDetailsResponse = response.body();
                    if(trackingDetailsResponse.isSuccess()) {
                        sharingContacts = trackingDetailsResponse.getSharingWith();
                        trackingContacts  = trackingDetailsResponse.getTracking();
//                        db.clear();
                        db.addContactsToShareLocation(sharingContacts);
                        db.addContactsToTrackLocation(trackingContacts);
                        initializeSharingAndTrackingContactsList();
                        connectToServer();
                        subscribeToTrackContacts();
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<TrackingDetailsResponse> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onTrackingContactFocus(int childPosition) {
        mDrawerLayout.closeDrawer(Gravity.START, true);
        String contact = trackingContactsList.get(childPosition);
        currentFocussedContactOnMap = contact;
        Marker marker = currLocationMarkerMap.get(contact);
        if (marker != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16));
            marker.showInfoWindow();
        }
    }

    @Override
    public void onSharingContactSwitchClick(int position, boolean isChecked) {
        String contact_ = sharingContactsList.get(position);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("publisher", loggedInMobile);
            if(isChecked) {
                if(locationSharingStatus) {
                    jsonObject.put("contactToAdd", contact_);
                    socketManager.sendEventMessage("addContact", jsonObject);
                }
                db.updateSharingStatus(contact_, true);
            } else {
                jsonObject.put("contactToRemove", contact_);
                socketManager.sendEventMessage("removeContact", jsonObject);
                db.updateSharingStatus(contact_, false);
                if(!db.isSharingOnForAtLeastOneContact()) {
                    stopLocationSharingService();
                    sharingSwitch.setChecked(false);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTrackingContactSwitchClick(int position, boolean isChecked) {
        String contact_ = trackingContactsList.get(position);
        if(isChecked) {
            db.updateTrackingStatus(contact_, true);
            subscribeToContact(contact_);
        } else {
            db.updateTrackingStatus(contact_, false);
            unsubscribeToContact(contact_);
            updateSnippetOnMap(contact_, "Not tracking");
        }
    }

    private class ContactInfo {
        private Intent data;
        private String name;
        private String number;

        public ContactInfo(Intent data) {
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }

        public ContactInfo invoke() {
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
            name = cursor.getString(columnName);
            number = cursor.getString(columnNumber);
            number = number.replace("-","");
            number = number.replace(" ","");
            if(number.length() > 10) {
                number = number.substring(number.length() - 10);
            }
            return this;
        }
    }
}


