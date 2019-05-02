package com.pyb.trackme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pyb.trackme.db.AppConstants;
import com.pyb.trackme.restclient.LoginRequest;
import com.pyb.trackme.restclient.LoginResponse;
import com.pyb.trackme.restclient.LoginServiceClient;
import com.pyb.trackme.restclient.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        isUserLoggedIn = checkIfUserAlreadyLoggedIn();
        if(!isUserLoggedIn) {
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
//                    startActivityForResult(intent, EXIT_CODE);
                    startActivity(intent);
                }
            });
        } else {
            Intent intent = new Intent(this, SplashActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("name", loggedInName);
            bundle.putString("mobile", loggedInMobile);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if(isUserLoggedIn) {
            finish();  //TODO review the logic to destroy login activity
        }
    }
    private void onSubmitButtonClick() {
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
                progressBar.setVisibility(View.GONE);
                if(response.isSuccessful()) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess()) {
                        saveUserLoginDetails(mobile, loginResponse.getName());
                        isUserLoggedIn = true;
                        Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("name", loginResponse.getName());
                        bundle.putString("mobile", mobile);
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
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
                Toast.makeText(getApplicationContext(), "Failed to login !!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkIfUserAlreadyLoggedIn() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString(AppConstants.MOBILE_PREF, "");
        String name = preferences.getString(AppConstants.NAME_PREF, "");
        loggedInName = name;
        loggedInMobile = mobile;
        return !mobile.isEmpty() && !name.isEmpty();
    }

    private void saveUserLoginDetails(String mobileNumber, String name) {
        SharedPreferences preferences = getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(AppConstants.MOBILE_PREF, mobileNumber);
        editor.putString(AppConstants.NAME_PREF, name);
        editor.commit();
    }



}
