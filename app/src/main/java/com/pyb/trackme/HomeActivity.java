package com.pyb.trackme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private SearchView searchView;
    private ListView searchContactListView;
    private List<String> contactList;

    public static final int RequestPermissionCode  = 1 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        searchView = findViewById(R.id.search_contact);
        searchContactListView = findViewById(R.id.search_contact_list);
        contactList = new ArrayList<>();
        searchView.requestFocus();
        searchView.setEnabled(false);

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetContactsIntoArrayList();

                ArrayAdapter arrayAdapter = new ArrayAdapter<String>(
                        HomeActivity.this,
                        R.layout.contact_items_listview,
                        R.id.textView, contactList
                );

                searchContactListView.setAdapter(arrayAdapter);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        EnableRuntimePermission();
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

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                HomeActivity.this,
                Manifest.permission.READ_CONTACTS))
        {

            Toast.makeText(HomeActivity.this,"CONTACTS permission allows us to Access contacts", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(HomeActivity.this,new String[]{
                    Manifest.permission.READ_CONTACTS}, RequestPermissionCode);

        }
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

                }
                break;
        }
    }

}
