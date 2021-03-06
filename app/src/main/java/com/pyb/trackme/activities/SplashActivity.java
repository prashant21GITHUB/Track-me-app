package com.pyb.trackme.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.pyb.trackme.R;
import com.pyb.trackme.cache.AppConstants;
import com.pyb.trackme.cache.TrackDetailsDB;
import com.pyb.trackme.restclient.MobileRequest;
import com.pyb.trackme.restclient.RestClient;
import com.pyb.trackme.restclient.TrackingDetailsResponse;
import com.pyb.trackme.restclient.TrackingServiceClient;
import com.pyb.trackme.utils.ConnectionUtils;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {
    private ArrayList<String> sharingContactsList;
    private ArrayList<String> trackingContactsList;
    private String LOGIN_PREF_NAME;
    private String loggedInName;
    private String loggedInMobile;
    private String fcmToken;
    private final String TAG = "TrackMe_SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        readDetailsFromPref();
        if(!checkIfUserAlreadyLoggedIn()) {
            Intent i = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
            return;
        }
        getTrackingDetails();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                startActivity(i);
                finish();
            }
        }, 3000);
    }

    private void getTrackingDetails() {
        boolean isDataPresentInPref = TrackDetailsDB.db().readDataFromPref(getApplicationContext());
        if(isDataPresentInPref) {
            return;
        }
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
                        TrackDetailsDB.db().addContactsToShareLocation(sharingContactsList);
                        TrackDetailsDB.db().addContactsToTrackLocation(trackingContactsList);
                    }
                } else {
                    Toast.makeText(SplashActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TrackingDetailsResponse> call, Throwable t) {
                Toast.makeText(SplashActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void readDetailsFromPref() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString(AppConstants.MOBILE_PREF, "");
        String name = preferences.getString(AppConstants.NAME_PREF, "");
        String token = preferences.getString(AppConstants.FCM_TOKEN, "");
        loggedInName = name;
        loggedInMobile = mobile;
        fcmToken = token;
    }

    private boolean checkIfUserAlreadyLoggedIn() {
        return !loggedInMobile.isEmpty() && !loggedInName.isEmpty();
    }
}
