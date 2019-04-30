package com.pyb.trackme;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pyb.trackme.db.TrackDetailsDB;
import com.pyb.trackme.restclient.LoginServiceClient;
import com.pyb.trackme.restclient.MobileRequest;
import com.pyb.trackme.restclient.RestClient;
import com.pyb.trackme.restclient.ServiceResponse;
import com.pyb.trackme.restclient.ShareLocationRequest;
import com.pyb.trackme.restclient.TrackingServiceClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cz.msebera.android.httpclient.Header;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectContactsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_CONTACT = 131;
    private String LOGIN_PREF_NAME;
    private EditText inputContact;
    private ListView contactsListView;
    private ImageButton openContactsBtn;
    private ImageButton addBtn;
    private Button submitBtn;
    private List<Pair<String, String>> nameAndMobilePairList;
    private CustomListViewAdapter listViewAdapter;
    private ProgressBar progressBar;
    private String loggedInName;
    private String loggedInMobile;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.select_contacts_activity);
        LOGIN_PREF_NAME = getApplicationInfo().packageName +"_Login";
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        inputContact = findViewById(R.id.selected_mobile_number);
        contactsListView = findViewById(R.id.contacts_view);
        openContactsBtn = findViewById(R.id.imageButton);
        submitBtn = findViewById(R.id.done_button);
        addBtn = findViewById(R.id.add_button);
        progressBar = findViewById(R.id.select_contact_progressBar);
        nameAndMobilePairList = new ArrayList<>();
        initializeSavedContactListFromPref();
        attachListeners();
        listViewAdapter = new CustomListViewAdapter(this, nameAndMobilePairList);
        contactsListView.setAdapter(listViewAdapter);
        contactsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                showAlertDialog(position);
                return true;
            }
        });
        setLoggedInUserDetails(getIntent().getExtras());
    }

    private void setLoggedInUserDetails(Bundle bundle) {
        loggedInName = bundle.getString("name", "");
        loggedInMobile = bundle.getString("mobile", "");
    }

    private void showAlertDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SelectContactsActivity.this)
                .setTitle("Confirm")
                .setMessage("Do not share location with " + nameAndMobilePairList.get(position).first +" ?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        nameAndMobilePairList.remove(position);
                        listViewAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void attachListeners() {
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddButtonClick();
            }
        });
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnectedToInternet()) {
                    shareLocation();
                } else {
                    buildNoInternetDialog();
                }
            }
        });
        openContactsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickContact();
            }
        });
    }

    private void shareLocation() {
        final List<String> contactsList = new ArrayList<>(nameAndMobilePairList.size());
        for(Pair<String, String> pair : nameAndMobilePairList) {
            contactsList.add(pair.second);
        }
        TrackingServiceClient client = RestClient.getTrackingServiceClient();
        Call<ServiceResponse> call = client.addContactsForSharingLocation(new ShareLocationRequest(loggedInMobile, contactsList));
        call.enqueue(new Callback<ServiceResponse>() {
            @Override
            public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                if(response.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);
                    ServiceResponse serviceResponse = response.body();
                    if (serviceResponse.isSuccess()) {
                        String[] contacts = new String[nameAndMobilePairList.size()];
                        int i = 0;
                        for (Pair<String, String> pair : nameAndMobilePairList) {
                            contacts[i++] = pair.first;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(SelectContactsActivity.this)
                                .setTitle("Shared location with")
                                .setItems(contacts, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .setCancelable(true)
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        finish();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                        saveContactListInPref();
//                        TrackDetailsDB.db().clear();
                        TrackDetailsDB.db().addContactsToShareLocation(contactsList);
                    } else {
                        Toast.makeText(SelectContactsActivity.this, "Failed to share location, please try after sometime !!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SelectContactsActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ServiceResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SelectContactsActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void onAddButtonClick() {
        String number = inputContact.getText().toString();
        if(ValidationUtils.isValidNumber(number)) {
            if(isConnectedToInternet()) {
                addContactIfRegistered(number, number);
            } else {

            }
        } else {
            Toast.makeText(this, "Enter valid mobile number !!", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickContact() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        startActivityForResult(pickContactIntent, REQUEST_CODE_PICK_CONTACT);
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if(!isConnected) {
            buildNoInternetDialog();
        }
        return isConnected;
    }

    private void buildNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SelectContactsActivity.this)
                .setTitle("Check your internet connection")
                .setMessage("Please connect to mobile data or wifi !!")
                .setCancelable(true)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode)
        {
            case (REQUEST_CODE_PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK)
                {
                    Uri contactUri = data.getData();
                    // We only need the NUMBER column, because there will be only one row in the result
                    String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER};

                    // Perform the query on the contact to get the NUMBER column
                    // We don't need a selection or sort order (there's only one result for the given URI)
                    // CAUTION: The query() method should be called from a separate thread to avoid blocking
                    // your app's UI thread. (For simplicity of the sample, this code doesn't do that.)
                    // Consider using CursorLoader to perform the query.
                    Cursor cursor = getContentResolver()
                            .query(contactUri, projection, null, null, null);
                    cursor.moveToFirst();

                    // Retrieve the phone number from the NUMBER column
                    int columnName = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int columnNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String name = cursor.getString(columnName);
                    String number = cursor.getString(columnNumber);
                    number = number.replace("-","");
                    number = number.replace(" ","");
                    if(number.length() > 10) {
                        number = number.substring(number.length() - 10);
                    }
                    if(isConnectedToInternet()) {
                        addContactIfRegistered(number, name);
                    } else {
                        Toast.makeText(SelectContactsActivity.this, "You are not online !!", Toast.LENGTH_SHORT).show();
                    }
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void addContactIfRegistered(String number, String name) {
        LoginServiceClient client = RestClient.getLoginServiceClient();
        Call<ServiceResponse> call = client.isUserRegistered(new MobileRequest(number));
        call.enqueue(new Callback<ServiceResponse>() {
            @Override
            public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                if(response.isSuccessful()) {
                progressBar.setVisibility(View.GONE);
                ServiceResponse serviceResponse = response.body();
                    if(serviceResponse.isSuccess()) {
                        nameAndMobilePairList.add(new Pair<>(name, number));
                        listViewAdapter.notifyDataSetChanged();
                        Toast.makeText(SelectContactsActivity.this, "Contact added to the list !!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SelectContactsActivity.this, "Contact is not registered on TrackMe !!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SelectContactsActivity.this, "Internal error, " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ServiceResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void saveContactListInPref() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> contactWithNamesSet = new LinkedHashSet<>();
        for(Pair<String, String> pair : nameAndMobilePairList) {
            contactWithNamesSet.add(pair.first + ":" + pair.second);
        }
        editor.putStringSet("SavedContactList", contactWithNamesSet);
        editor.commit();
    }

    private void initializeSavedContactListFromPref() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        Set<String> contactWithNamesSet = preferences.getStringSet("SavedContactList", Collections.EMPTY_SET);
        for(String contact : contactWithNamesSet) {
            String details[] = contact.split(":");
            String name = details[0];
            String mobile = details[1];
            nameAndMobilePairList.add(new Pair<>(name, mobile));
        }
    }

    private void readLoggedInUserDetails() {
        SharedPreferences preferences = this.getSharedPreferences(LOGIN_PREF_NAME, MODE_PRIVATE);
        String mobile = preferences.getString("Mobile", "");
        String name = preferences.getString("Name", "");
        loggedInName = name;
        loggedInMobile = mobile;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
