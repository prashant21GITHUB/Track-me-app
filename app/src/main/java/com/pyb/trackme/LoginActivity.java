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
    private final static int EXIT_CODE = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
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
                    startActivityForResult(intent, EXIT_CODE);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isUserLoggedIn) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivityForResult(intent, EXIT_CODE);
        }
    }

    private void onSubmitButtonClick() {
        String mobile = mobileNumber.getText().toString();
        if(!isValidNumber(mobile)) {
            mobileNumber.requestFocus();
            Toast.makeText(getApplicationContext(), "Enter valid 10 digit mobile number !!", Toast.LENGTH_SHORT).show();
            return;
        }

        String pwd = this.password.getText().toString();
        if(!isValidPassword(pwd)) {
            this.password.requestFocus();
            Toast.makeText(getApplicationContext(), "Password can't be blank !!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        loginUser(mobile, pwd);

    }

    private boolean isValidNumber(String mobile) {
        return mobile != null && mobile.matches("[6-9]{1}[0-9]{9}");
    }

    private boolean isValidPassword(String pwd) {
        return pwd != null && !pwd.isEmpty();
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
                        saveUserLoginDetails(mobileNumber, password);
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivityForResult(intent, EXIT_CODE);
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
        SharedPreferences preferences = this.getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String pwd = preferences.getString("Password", "");
        return !mobile.isEmpty() && !pwd.isEmpty();
    }

    private void saveUserLoginDetails(String mobileNumber, String password) {
        SharedPreferences preferences = getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Mobile", mobileNumber);
        editor.putString("Password", password);
        editor.commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        setResult(EXIT_CODE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setResult(EXIT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == EXIT_CODE){
            finish();
        }
    }


}
