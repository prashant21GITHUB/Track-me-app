package com.pyb.trackme.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pyb.trackme.R;
import com.pyb.trackme.cache.AppConstants;
import com.pyb.trackme.restclient.LoginRequest;
import com.pyb.trackme.restclient.LoginResponse;
import com.pyb.trackme.restclient.LoginServiceClient;
import com.pyb.trackme.restclient.RestClient;
import com.pyb.trackme.utils.ConnectionUtils;
import com.pyb.trackme.utils.ValidationUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationActivity extends AppCompatActivity {

    private Button submitButton;
    private EditText mobileNumber;
    private EditText password;
    private EditText fullName;
    private TextView loginLink;
    private ProgressBar progressBar;
    private final static int EXIT_CODE = 111;
    private String LOGIN_PREF_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        submitButton = findViewById(R.id.reg_btn);
        mobileNumber = findViewById(R.id.mobile_reg);
        password = findViewById(R.id.reg_pwd);
        progressBar = findViewById(R.id.progressBarReg);
        loginLink = findViewById(R.id.lnkLogin);
        fullName = findViewById(R.id.name_reg);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClick();
            }
        });
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void onSubmitButtonClick() {
        if(!ConnectionUtils.isConnectedToInternet(this)) {
            Toast.makeText(this, "Check your internet connection !!", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = fullName.getText().toString();
        if(!ValidationUtils.isValidName(name)) {
            mobileNumber.requestFocus();
            Toast.makeText(getApplicationContext(), "Enter valid name !!", Toast.LENGTH_SHORT).show();
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

        registerUser(name, mobile, pwd);

    }

    private void registerUser(final String name, final String mobileNumber, final String password) {
        LoginServiceClient client = RestClient.getLoginServiceClient();
        Call<LoginResponse> call = client.register(new LoginRequest(mobileNumber, password, name));
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                if(response.isSuccessful()) {
                    LoginResponse loginResponse = response.body();
                    if(loginResponse.isSuccess()) {
                        saveUserLoginDetails(name, mobileNumber);
                        Intent intent = new Intent(RegistrationActivity.this, SplashActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("name", name);
                        bundle.putString("mobile", mobileNumber);
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }  else {
                    Toast.makeText(RegistrationActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserLoginDetails(String name, String mobileNumber) {
        SharedPreferences preferences = getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(AppConstants.NAME_PREF, name);
        editor.putString(AppConstants.MOBILE_PREF, mobileNumber);
        editor.commit();
    }

}
