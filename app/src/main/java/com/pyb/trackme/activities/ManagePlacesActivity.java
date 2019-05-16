package com.pyb.trackme.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.libraries.places.api.Places;
import com.pyb.trackme.R;

public class ManagePlacesActivity extends AppCompatActivity {

    private Button searchLocationBtn;
    int PLACE_PICKER_REQUEST = 1;
    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.manage_places_activity);
        searchLocationBtn = findViewById(R.id.manage_places_btn);
        searchLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

            }
        });
    }
}
