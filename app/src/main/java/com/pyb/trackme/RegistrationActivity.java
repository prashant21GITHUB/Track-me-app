package com.pyb.trackme;

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

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class RegistrationActivity extends AppCompatActivity {

    private Button submitButton;
    private EditText mobileNumber;
    private EditText password;
    private EditText fullName;
    private TextView loginLink;
    private ProgressBar progressBar;
    private final static int EXIT_CODE = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_activity);
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
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivityForResult(intent, EXIT_CODE);
            }
        });
    }

    private void onSubmitButtonClick() {
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
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        requestParams.put("name", name);
        requestParams.put("mobile", mobileNumber);
        requestParams.put("password", password);
        APIClient.post("user/register", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if(response.getBoolean("success")) {
                        saveUserLoginDetails(mobileNumber, password);
                        Intent intent = new Intent(RegistrationActivity.this, HomeActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("name", name);
                        bundle.putString("mobile", mobileNumber);
                        intent.putExtras(bundle);
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
