package com.pyb.trackme;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private Button submitButton;
    private EditText mobileNumber;
    private EditText name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        submitButton = findViewById(R.id.goButton);
        mobileNumber = findViewById(R.id.mobileNumber);
        name = findViewById(R.id.name);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClick();
            }
        });
    }

    private void onSubmitButtonClick() {
        loginUser();
//        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
//        startActivity(intent);
    }

    private void loginUser() {
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        requestParams.put("name", "prashant");
        requestParams.put("mobile", "6756453423");
        APIClient.post("user/register", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject instead of expected JSONArray
                try {
                    Toast.makeText(getApplicationContext(), "Json response: "+ response.get("message"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                Toast.makeText(getApplicationContext(), "Json response array : " + timeline.toString(), Toast.LENGTH_SHORT).show();
            }
        });

    }


}
