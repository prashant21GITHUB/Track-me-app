package com.pyb.trackme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private Button submitButton;
    private EditText mobileNumber;
    private EditText userName;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        submitButton = findViewById(R.id.goButton);
        mobileNumber = findViewById(R.id.mobileNumber);
        progressBar = findViewById(R.id.progressBar);
        userName = findViewById(R.id.name);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClick();
            }
        });
    }

    private void onSubmitButtonClick() {
        String mobile = mobileNumber.getText().toString();
        if(!isValidNumber(mobile)) {
            mobileNumber.requestFocus();
            Toast.makeText(getApplicationContext(), "Enter valid 10 digit mobile number !!", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = userName.getText().toString();
        if(!isValidName(name)) {
            userName.requestFocus();
            Toast.makeText(getApplicationContext(), "Enter valid name !!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        loginUser(name, mobile);

    }

    private boolean isValidNumber(String mobile) {
        return mobile != null && mobile.matches("[6-9]{1}[0-9]{9}");
    }

    private boolean isValidName(String name) {
        return name != null && name.matches("(?i)[a-z][a-z0-9_]*");
    }

    private void loginUser(String name, String mobileNumber) {
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        requestParams.put("name", name);
        requestParams.put("mobile", mobileNumber);
        APIClient.post("user/register", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    Toast.makeText(getApplicationContext(), "Json response: "+ response.get("message"), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Json response array : " + timeline.toString(), Toast.LENGTH_SHORT).show();

            }

            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                progressBar.setVisibility(View.GONE);
            }

            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                progressBar.setVisibility(View.GONE);
            }
        });

    }


}
