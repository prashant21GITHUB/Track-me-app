package com.pyb.trackme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pyb.trackme.utils.ConnectionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class SplashActivity extends AppCompatActivity {
    private ArrayList<String> sharingContactsList;
    private ArrayList<String> trackingContactsList;
    private String LOGIN_PREF_NAME;
    private String loggedInName;
    private String loggedInMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        readLoggedInUserDetails();
        new Handler().postDelayed(new Runnable() {


            @Override
            public void run() {
                // This method will be executed once the timer is over
                if(ConnectionUtils.isConnectedToInternet(SplashActivity.this)) {
                    syncTrackingDetailsFromServer();

                } else {
                    Toast.makeText(SplashActivity.this, "Internet connection is not available !!", Toast.LENGTH_SHORT).show();
                }

            }
        }, 5000);
    }

    private void syncTrackingDetailsFromServer() {
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        requestParams.put("mobile", loggedInMobile);
        APIClient.put("user/track/details", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                sharingContactsList = new ArrayList<>();
                trackingContactsList = new ArrayList<>();
                try {
                    if(response.getBoolean("success")) {
                        JSONArray arr = response.getJSONArray("sharingWith");
                        for(int i=0; i < arr.length(); i++) {
                            sharingContactsList.add(arr.getString(i));
                        }
//                        if(!sharingContactsList.isEmpty()) {
//                            sharingContactsLayout.setVisibility(View.VISIBLE);
//                        }
                        arr = response.getJSONArray("tracking");
                        for(int i=0; i < arr.length(); i++) {
                            trackingContactsList.add(arr.getString(i));
                        }
                    }
                    Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                    i.putStringArrayListExtra("sharingContactsList", sharingContactsList);
                    i.putStringArrayListExtra("trackingContactsList", trackingContactsList);
                    i.putExtra("name", loggedInName);
                    i.putExtra("mobile", loggedInMobile);
                    startActivity(i);
                    finish();
                } catch (JSONException e) {
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(SplashActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
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
