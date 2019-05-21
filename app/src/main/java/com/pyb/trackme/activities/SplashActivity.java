package com.pyb.trackme.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pyb.trackme.R;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(getApplicationContext());
//        new Handler().post(() -> {
//            String id = FirebaseInstanceId.getInstance().getId();
//            Log.d("TrackMe_SplashActivity", "Token: " + id);
//        });

        setContentView(R.layout.splash_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        readLoggedInUserDetails();
        getTrackingDetails();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                i.putExtra("name", loggedInName);
                i.putExtra("mobile", loggedInMobile);
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

    private void readLoggedInUserDetails() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String name = preferences.getString("Name", "");
        loggedInName = name;
        loggedInMobile = mobile;
    }
}
