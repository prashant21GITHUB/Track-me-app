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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

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

    private void loginUser(final String mobileNumber, final String password) {
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        requestParams.put("mobile", mobileNumber);
        requestParams.put("password", password);
        APIClient.put("user/login", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if(response.getBoolean("success")) {
                        saveUserLoginDetails(mobileNumber, response.getString("name"));
                        isUserLoggedIn = true;
                        Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("name", response.getString("name"));
                        bundle.putString("mobile", response.getString("mobile"));
                        intent.putExtras(bundle);
                        startActivity(intent);
//                        startActivityForResult(intent, EXIT_CODE);
                    } else {
                        Toast.makeText(getApplicationContext(), response.getString("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Json response array : " + timeline.toString(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
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
