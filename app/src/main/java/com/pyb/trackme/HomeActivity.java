package com.pyb.trackme;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private SearchView searchView;
    private ListView searchContactListView;
    private List<String> contactList;
    private String loggedInName;
    private String loggedInMobile;
    private TextView greetingsView;
    private EditText mobileNumberToTrack;
    private Button trackBtn;

    public static final int RequestPermissionCode  = 1 ;
    public static final int EXIT_CODE = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLoggedInUserDetails(getIntent().getExtras());
        setContentView(R.layout.home_activity);
        trackBtn = findViewById(R.id.track_btn);
        greetingsView = findViewById(R.id.greetings);
        mobileNumberToTrack = findViewById(R.id.mobile_number_track);

        greetingsView.setText("Hello " + loggedInName);
        trackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTrackBtnClick();
            }
        });

//        searchView = findViewById(R.id.search_contact);
//        searchContactListView = findViewById(R.id.search_contact_list);
//        contactList = new ArrayList<>();
//        searchView.requestFocus();
//        searchView.setEnabled(false);

//        searchView.setOnSearchClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                GetContactsIntoArrayList();
//
//                ArrayAdapter arrayAdapter = new ArrayAdapter<String>(
//                        HomeActivity.this,
//                        R.layout.contact_items_listview,
//                        R.id.textView, contactList
//                );
//
//                searchContactListView.setAdapter(arrayAdapter);
//            }
//        });
    }

    private void onTrackBtnClick() {
        String mobileNo = mobileNumberToTrack.getText().toString();
        if(!ValidationUtils.isValidNumber(mobileNo)) {
            Toast.makeText(this, "Enter valid mobile number !!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Request sent !!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoggedInUserDetails(Bundle bundle) {
        loggedInName = bundle.getString("name", "");
        loggedInMobile = bundle.getString("mobile", "");
    }

    @Override
    protected void onResume() {
        super.onResume();
//        EnableRuntimePermission();
    }

    public void GetContactsIntoArrayList(){

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);
        String name, number;
        while (cursor.moveToNext()) {

            name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            contactList.add(name + " "  + ":" + " " + number);
        }

        cursor.close();

    }

    public void EnableRuntimePermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, RequestPermissionCode);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }

//        if (ActivityCompat.shouldShowRequestPermissionRationale(
//                HomeActivity.this,
//                Manifest.permission.READ_CONTACTS))
//        {
//
//            Toast.makeText(HomeActivity.this,"CONTACTS permission allows us to Access contacts", Toast.LENGTH_LONG).show();
//
//        } else {
//
//            ActivityCompat.requestPermissions(HomeActivity.this,new String[]{
//                    Manifest.permission.READ_CONTACTS}, RequestPermissionCode);
//
//        }
    }

    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {

        switch (RC) {

            case RequestPermissionCode:

                if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {
                    searchView.setEnabled(true);

                    Toast.makeText(HomeActivity.this,"Permission Granted, Now your application can access CONTACTS.", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(HomeActivity.this,"Permission Canceled, Now your application cannot access CONTACTS.", Toast.LENGTH_LONG).show();
                    this.finish();

                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_activity_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.logout:
                clearSavedLoginDetails();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivityForResult(intent, EXIT_CODE);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void clearSavedLoginDetails() {
        SharedPreferences preferences = getSharedPreferences(getApplicationInfo().packageName +"_Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Mobile", "");
        editor.putString("Password", "");
        editor.commit();
        this.loggedInName = "";
        this.loggedInMobile = "";
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


