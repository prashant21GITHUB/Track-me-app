package com.pyb.trackme;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class SelectContactsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_CONTACT = 131;
    private EditText inputContact;
    private ListView contactsListView;
    private ImageButton openContactsBtn;
    private ImageButton addBtn;
    private Button submitBtn;
    private List<Pair<String, String>> contactsList;
    private CustomListViewAdapter listViewAdapter;
    private ProgressBar progressBar;
    private String loggedInName;
    private String loggedInMobile;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.select_contacts_activity);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        inputContact = findViewById(R.id.selected_mobile_number);
        contactsListView = findViewById(R.id.contacts_view);
        openContactsBtn = findViewById(R.id.imageButton);
        submitBtn = findViewById(R.id.done_button);
        addBtn = findViewById(R.id.add_button);
        progressBar = findViewById(R.id.select_contact_progressBar);
        contactsList = new ArrayList<>();
        attachListeners();
        listViewAdapter = new CustomListViewAdapter(this, contactsList);
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
                .setMessage("Do not share location with " + contactsList.get(position).first +" ?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        contactsList.remove(position);
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
                shareLocation();

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
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        List<String> contacts = new ArrayList<>(contactsList.size());
        for(Pair<String, String> pair : contactsList) {
            contacts.add(pair.second);
        }
        requestParams.put("mobile", loggedInMobile);
        requestParams.put("contacts", contacts);
        APIClient.put("user/location/share", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if(response.getBoolean("success")) {
                        String[] contacts = new String[contactsList.size()];
                        int i = 0;
                        for(Pair<String, String> pair : contactsList) {
                            contacts[i++] = pair.first;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(SelectContactsActivity.this)
                                .setTitle("Shared location with")
                                .setItems(contacts, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) { }
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
                    } else {
                        Toast.makeText(SelectContactsActivity.this, "Failed to share location, please try after sometime !!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SelectContactsActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SelectContactsActivity.this, "Internal error, please try after sometime !!", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void onAddButtonClick() {
        String number = inputContact.getText().toString();
        if(ValidationUtils.isValidNumber(number)) {
            addContactIfRegistered(number, number);
        } else {
            Toast.makeText(this, "Enter valid mobile number !!", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickContact() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        startActivityForResult(pickContactIntent, REQUEST_CODE_PICK_CONTACT);
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
                    addContactIfRegistered(number, name);
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void addContactIfRegistered(final String number, final String name) {
        progressBar.setVisibility(View.VISIBLE);
        RequestParams requestParams = new RequestParams();
        requestParams.setUseJsonStreamer(true);
        requestParams.put("mobile", number);
        APIClient.put("user/isregistered", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if(response.getBoolean("success")) {
                        contactsList.add(new Pair<>(name, number));
                        listViewAdapter.notifyDataSetChanged();
                        Toast.makeText(SelectContactsActivity.this, "Contact added to the list !!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SelectContactsActivity.this, "Contact is not registered on TrackMe !!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
            }

        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
