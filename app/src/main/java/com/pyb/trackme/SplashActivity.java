package com.pyb.trackme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pyb.trackme.db.TrackDetailsDB;
import com.pyb.trackme.restclient.MobileRequest;
import com.pyb.trackme.restclient.RestClient;
import com.pyb.trackme.restclient.TrackingDetailsResponse;
import com.pyb.trackme.restclient.TrackingServiceClient;
import com.pyb.trackme.services.TrackMeService;
import com.pyb.trackme.utils.ConnectionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {
    private ArrayList<String> sharingContactsList;
    private ArrayList<String> trackingContactsList;
    private String LOGIN_PREF_NAME;
    private String loggedInName;
    private String loggedInMobile;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        readLoggedInUserDetails();
        getTrackingDetails();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(new Intent(getApplicationContext(), TrackMeService.class));
        } else {
            startForegroundService(new Intent(getApplicationContext(), TrackMeService.class));
        }
        swipeRefreshLayout = findViewById(R.id.splashPullToRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getTrackingDetails();
            }
        });
        new Handler().postDelayed(new Runnable() {


            @Override
            public void run() {
                // This method will be executed once the timer is over
                if(sharingContactsList != null && trackingContactsList != null) {
                    Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                    i.putExtra("name", loggedInName);
                    i.putExtra("mobile", loggedInMobile);
                    startActivity(i);
                    finish();
                } else {
                    findViewById(R.id.progressBar3).setVisibility(View.GONE);
//                    Toast.makeText(SplashActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
                }
            }
        }, 3000);
    }

    private void getTrackingDetails() {
        if(ConnectionUtils.isConnectedToInternet(SplashActivity.this)) {
            syncTrackingDetailsFromServer();
        } else {
            Toast.makeText(SplashActivity.this, "Internet connection is not available !!", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncTrackingDetailsFromServer() {
        TrackingServiceClient client = RestClient.getTrackingServiceClient();
        Call<TrackingDetailsResponse> call = client.getTrackingDetails(new MobileRequest(loggedInMobile));
        call.enqueue(new Callback<TrackingDetailsResponse>() {
            @Override
            public void onResponse(Call<TrackingDetailsResponse> call, Response<TrackingDetailsResponse> response) {
                sharingContactsList = new ArrayList<>();
                trackingContactsList = new ArrayList<>();
                if(response.isSuccessful()) {
                    TrackingDetailsResponse trackingDetailsResponse = response.body();
                    if(trackingDetailsResponse.isSuccess()) {
                        sharingContactsList = trackingDetailsResponse.getSharingWith();
                        trackingContactsList  = trackingDetailsResponse.getTracking();
                        TrackDetailsDB.db().clear();
                        TrackDetailsDB.db().addContactsToShareLocation(sharingContactsList);
                        TrackDetailsDB.db().addContactsToTrackLocation(trackingContactsList);
                    }
                } else {
                    Toast.makeText(SplashActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<TrackingDetailsResponse> call, Throwable t) {
                Toast.makeText(SplashActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void readLoggedInUserDetails() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String name = preferences.getString("Name", "");
        loggedInName = name;
        loggedInMobile = mobile;
    }
}
