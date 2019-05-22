package com.pyb.trackme.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.pyb.trackme.R;
import com.pyb.trackme.cache.AppConstants;
import com.pyb.trackme.restclient.AddTokenRequest;
import com.pyb.trackme.restclient.LoginRequest;
import com.pyb.trackme.restclient.LoginResponse;
import com.pyb.trackme.restclient.LoginServiceClient;
import com.pyb.trackme.restclient.RestClient;
import com.pyb.trackme.restclient.ServiceResponse;
import com.pyb.trackme.utils.ConnectionUtils;
import com.pyb.trackme.utils.ValidationUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private Button submitButton;
    private EditText mobileNumber;
    private EditText password;
    private TextView regLink;
    private ProgressBar progressBar;
    private boolean isUserLoggedIn;
    private String loggedInName;
    private String loggedInMobile;
    private String LOGIN_PREF_NAME;
    private String fcmToken;
    private final String TAG = "TrackMe_Login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";

        submitButton = findViewById(R.id.btnLogin);
        mobileNumber = findViewById(R.id.login_mobile);
        password = findViewById(R.id.login_pwd);
        progressBar = findViewById(R.id.progressBarLogin);
        regLink = findViewById(R.id.lnkRegister);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClick();
            }
        });
        regLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!ConnectionUtils.isConnectedToInternet(this)) {
            Toast.makeText(this, "Check your internet connection !!", Toast.LENGTH_SHORT).show();
        }
    }
    private void onSubmitButtonClick() {
        if(!ConnectionUtils.isConnectedToInternet(this)) {
            Toast.makeText(this, "Check your internet connection !!", Toast.LENGTH_SHORT).show();
            return;
        }
        String mobile = mobileNumber.getText().toString();
        if(!ValidationUtils.isValidNumber(mobile)) {
            mobileNumber.requestFocus();
            Toast.makeText(getApplicationContext(), "Enter valid 10 digit mobile number !!", Toast.LENGTH_SHORT).show();
            return;
        }

        String pwd = this.password.getText().toString();
        if(!ValidationUtils.isValidPassword(pwd)) {
            this.password.requestFocus();
            Toast.makeText(getApplicationContext(), "Password can't be blank !!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        loginUser(mobile, pwd);

    }

    private void loginUser(String mobile, String pwd) {
        LoginServiceClient loginServiceClient = RestClient.getLoginServiceClient();
        LoginRequest req = new LoginRequest(mobile, pwd, "");
        Call<LoginResponse> call = loginServiceClient.login(req);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if(response.isSuccessful()) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess()) {
                        loggedInMobile = mobile;
                        loggedInName = loginResponse.getName();
                        sendFCMTokenToServer();
                    } else {
                        Toast.makeText(getApplicationContext(), loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Internal error !!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFCMTokenToServer() {
        FirebaseApp.initializeApp(this);
        FirebaseInstanceId.getInstance().getInstanceId().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Failed to get FCM token: "+ e.getMessage());
            }
        }).addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                Log.d(TAG, instanceIdResult.getToken());
                _sendToken(instanceIdResult.getToken());
                fcmToken = instanceIdResult.getToken();
            }
        });
    }

    private void _sendToken(String token) {
        LoginServiceClient client = RestClient.getLoginServiceClient();
        Call<ServiceResponse> call = client.addFCMToken(new AddTokenRequest(loggedInMobile, token));
        call.enqueue(new Callback<ServiceResponse>() {
            @Override
            public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                progressBar.setVisibility(View.GONE);
                if(response.isSuccessful()) {
                    ServiceResponse serviceResponse = response.body();
                    if(serviceResponse.isSuccess()) {
                        saveDetailsInPref(loggedInMobile, loggedInName);
                        Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ServiceResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void saveDetailsInPref(String mobileNumber, String name) {
        SharedPreferences preferences = getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(AppConstants.MOBILE_PREF, mobileNumber);
        editor.putString(AppConstants.NAME_PREF, name);
        editor.putString(AppConstants.FCM_TOKEN, name);
        editor.commit();
    }



}
