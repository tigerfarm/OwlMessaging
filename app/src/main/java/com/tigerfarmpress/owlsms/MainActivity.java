package com.tigerfarmpress.owlsms;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.tigerfarmpress.owlsms.R.id.spinner;

// To add OkHttp, add into app/src/build.gradle Module:
// dependencies { ... compile 'com.squareup.okhttp3:okhttp:3.4.1' ... }

public class MainActivity extends AppCompatActivity {

    private AccountCredentials accountCredentials;
    private TwSms twilioSms;
    private String jsonAccPhoneNumbers = "";

    private SwipeRefreshLayout swipeRefreshLayout;
    private Menu theMenu;
    private Spinner twilioNumberSpinner;

    private boolean networkOkay = true;

    // For contacts and recent messages
    private EditText formPhoneNumber;
    private static TextView labelContactName;
    private ListView listView, listViewMsg;
    private ArrayList<String> ContactNamesNumbers, ContactNumbers, ContactNames, ContactNumbersNames;
    private ArrayList<String> StoreMessages;
    private ArrayAdapter<String> arrayAdapterContacts;
    private ArrayAdapter<String> arrayAdapterMsg;
    private Cursor cursor ;
    private String name, phonenumber ;

    private FloatingActionButton callActionFab, callActionRefresh, callActionSmsList, callActionContacts;

    private static final int SNACKBAR_DURATION = 4000;
    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 1;
    private static final int INTERNET_PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (accountCredentials.getShowContacts()) {
                    LoadContacts();
                } else {
                    populateMessageList();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        // startActivity(new Intent(this, SettingsActivity.class));
        accountCredentials = new AccountCredentials(this);
        if (!accountCredentials.existAccountSid()) {
            // if the Twilio account info hasn't been entered, go to Settings.
            // Snackbar.make(swipeRefreshLayout, "+ Enter your correct Twilio account information into Settings.", Snackbar.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
        }
        twilioSms = new TwSms(accountCredentials);

        // Snackbar.make(swipeRefreshLayout, "+ getAccountSid: " + accountCredentials.getAccountSid(), Snackbar.LENGTH_LONG).show();
        // Snackbar.make(swipeRefreshLayout, "+ ." + accountCredentials.getAccountSid(), Snackbar.LENGTH_LONG).show();

        // Use when working on specific panel.
        // startActivity(new Intent(this, SettingsActivity.class));

        // ---------------------------------------------------------------------------------------------
        labelContactName = (TextView)findViewById(R.id.labelContactName);
        formPhoneNumber = (EditText)findViewById(R.id.formPhoneNumber);
        labelContactName.setText(accountCredentials.getToContactName());
        formPhoneNumber.setText(accountCredentials.getToPhoneNumber());
        callActionFab = (FloatingActionButton) findViewById(R.id.call_action_fab);
        callActionFab.setOnClickListener(callActionFabClickListener());
        callActionSmsList = (FloatingActionButton) findViewById(R.id.action_smslist_fab);
        callActionSmsList.setOnClickListener(callActionSmsListClickListener());
        callActionRefresh = (FloatingActionButton) findViewById(R.id.action_refresh);
        callActionRefresh.setOnClickListener(callActionRefreshClickListener());
        callActionContacts = (FloatingActionButton) findViewById(R.id.action_contactlist_fab);
        callActionContacts.setOnClickListener(callActionContactsClickListener());

        // Recent message list structures
        StoreMessages = new ArrayList<String>();

        // ---------------------------------------------------------------------------------------------
        // Load Contacts
        ContactNamesNumbers = new ArrayList<String>();
        ContactNumbersNames = new ArrayList<String>();
        ContactNames = new ArrayList<String>();
        ContactNumbers = new ArrayList<String>();
        listView = (ListView)findViewById(R.id.listview1);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // https://stackoverflow.com/questions/20032270/why-my-android-setonitemclicklistener-doesnt-work
            // This may fix: https://stackoverflow.com/questions/14332409/custom-listview-is-not-responding-to-the-click-event/14333069#14333069
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;
                String itemValue = (String) listView.getItemAtPosition(position);
                if (accountCredentials.getShowContacts()) {
                    String theValue = itemValue.substring(itemValue.lastIndexOf("+")+2, itemValue.length());
                    formPhoneNumber.setText( itemValue.substring(itemValue.lastIndexOf("+"), itemValue.length()) );
                    labelContactName.setText(itemValue.substring(0, itemValue.lastIndexOf("+")-1));
                    return;
                }
                // Value is a phone number: From: +12223331234
                // Value is a name:         From: Stacy David
                String theValue = itemValue.substring(itemValue.lastIndexOf(":")+2, itemValue.length());
                if (theValue.startsWith("+")) {
                    // if the value is a phone number.
                    formPhoneNumber.setText( theValue );
                    labelContactName.setText("");
                    return;
                }
                // if the value is a name.
                labelContactName.setText(theValue);
                int i = ContactNames.indexOf(theValue);
                if (i>=0) {
                    String theNumber = ContactNamesNumbers.get(i);
                    formPhoneNumber.setText(theNumber.substring(theNumber.lastIndexOf(" ")+1, theNumber.length()));
                } else {
                    formPhoneNumber.setText("");
                }
            }
        });

    }

    // ---------------------------------------------------------------------------------------------
    // Stacy, step 1: Check for permission:
    // Documentation: https://developer.android.com/training/permissions/requesting.html

    private boolean checkPermissionForContacts() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
    }
    private boolean checkPermissionForWriteStorage() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    private boolean checkPermissionForInternet() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
    }

    // ----------------------------------
    // Stacy, step 2: Request permission.

    private void requestPermissionForContacts() {
        // Snackbar.make(swipeRefreshLayout, "+ requestPermissionForContacts", SNACKBAR_DURATION).show();
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
            Snackbar.make(swipeRefreshLayout, "Read contacts permissions: please allow in your application settings.", SNACKBAR_DURATION).show();
        } else {
            ActivityCompat.requestPermissions( this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_REQUEST_CODE);
        }
    }
    private void requestPermissionForInternet() {
        // Snackbar.make(swipeRefreshLayout, "+ requestPermissionForInternet", SNACKBAR_DURATION).show();
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET)) {
            Snackbar.make(swipeRefreshLayout, "Internet access permissions: please allow in your application settings.", SNACKBAR_DURATION).show();
        } else {
            ActivityCompat.requestPermissions( this, new String[]{Manifest.permission.INTERNET}, INTERNET_PERMISSION_REQUEST_CODE);
        }
    }

    // ----------------------------------
    // Stacy, step 3: Act on the user giving or denying, the permission.

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // Contacts permissions
        if (requestCode == CONTACTS_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(swipeRefreshLayout, "+ Permission Canceled, your application cannot access CONTACTS.", SNACKBAR_DURATION).show();
            } else {
                // Snackbar.make(coordinatorLayout, "+ Permission Granted, Now your application can access CONTACTS.", Snackbar.LENGTH_LONG).show();
            }
        }
        // Internet permissions
        if (requestCode == INTERNET_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(swipeRefreshLayout, "+ Permission Canceled, your application cannot access the INTERNET.", SNACKBAR_DURATION).show();
            } else {
                // Snackbar.make(coordinatorLayout, "+ Permission Granted, Now your application can access CONTACTS.", Snackbar.LENGTH_LONG).show();
            }
        }

    }


    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        theMenu = menu;

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (!checkPermissionForContacts()) {
            Snackbar.make(swipeRefreshLayout, "+ Request Permission For Contacts.", Snackbar.LENGTH_LONG).show();
            requestPermissionForContacts();
            return false;
        }
        LoadContacts();

        // Top bar list of account phone numbers:
        jsonAccPhoneNumbers = accountCredentials.getAccPhoneNumberList();
        if (jsonAccPhoneNumbers.isEmpty()) {
            // Reload the account phone numbers from Twilio.
            loadSpinnerAccPhoneNumbers();
            // When starting/re-starting, clear this value:
            accountCredentials.setToPhoneNumber( "" );
        } else {
            if (loadAccPhoneNumberSpinner(theMenu, jsonAccPhoneNumbers) > 0) {
                populateMessageList();
            }
        }

        if (accountCredentials.getShowContacts()) {
            // Snackbar.make(swipeRefreshLayout, "+ Show contacts.", Snackbar.LENGTH_LONG).show();
            showContacts();
        } else {
            // Snackbar.make(swipeRefreshLayout, "+ Show SMS messages.", Snackbar.LENGTH_LONG).show();
            showSmsList();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            doRefresh();
            return true;
        }

        if (!checkBeforeLeaving("")) {
            // Something wrong, stay on this panel.
            return true;
        }
        if (id == R.id.action_lookup) {
            doLookup();
            return true;
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------------------------------
    public boolean checkBeforeLeaving(String type) {

        if (!networkOkay) {
            Snackbar.make(swipeRefreshLayout, "- Network connect failed. Please wait and try reloading.", Snackbar.LENGTH_LONG).show();
            return false;
        }

        // -----------------------------------------
        // This covers the case of no network access, and then when there is access,
        // the user can reload/refresh the data.
        String selectedTwilioNumber = "";
        try {
            selectedTwilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        }
        catch (NullPointerException e) {
            Snackbar.make(swipeRefreshLayout, "+ No account phone numbers listed.", Snackbar.LENGTH_LONG).show();
        }
        if (!selectedTwilioNumber.isEmpty()) {
            // Set the Twilio phone number for the next panel to use.
            String twilioNumber = twilioNumberSpinner.getSelectedItem().toString();
            accountCredentials.setTwilioPhoneNumber( twilioNumber );
        }
        if (type.equalsIgnoreCase("refresh")) {
            return true;
        }

        // -----------------------------------------
        // Set the Appication Twilio Phone Number before going to another panel.
        try {
            selectedTwilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        }
        catch (NullPointerException e) {
            Snackbar.make(swipeRefreshLayout, "- No account phone numbers.", Snackbar.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return false;
        }
        if (selectedTwilioNumber.isEmpty()) {
            Snackbar.make(swipeRefreshLayout, "- No account phone numbers.", Snackbar.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return false;
        }

        String callPhoneNumber = formPhoneNumber.getText().toString();
        String callContactName = labelContactName.getText().toString();
        if (!callPhoneNumber.isEmpty()) {
            accountCredentials.setToPhoneNumber(callPhoneNumber);
            accountCredentials.setToContactName(callContactName);
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------
    private View.OnClickListener callActionFabClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkBeforeLeaving("")) {
                    // Something wrong, stay on this panel.
                    return;
                }
                if (!formPhoneNumber.getText().toString().isEmpty()) {
                    // hide keyboard
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(formPhoneNumber.getWindowToken(), 0);
                    //
                    String callPhoneNumber = formPhoneNumber.getText().toString();
                    String callContactName = labelContactName.getText().toString();
                    if (!callPhoneNumber.isEmpty()) {
                        accountCredentials.setToPhoneNumber(callPhoneNumber);
                        accountCredentials.setToContactName(callContactName);
                        Intent intent = new Intent(MainActivity.this, SendSmsActivity.class);
                        startActivity(intent);
                    }
                }
            }
        };
    }
    private View.OnClickListener callActionRefreshClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRefresh();
            }
        };
    }
    private View.OnClickListener callActionSmsListClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSmsList();
            }
        };
    }
    private View.OnClickListener callActionContactsClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContacts();
            }
        };
    }
    public void doRefresh() {
        checkBeforeLeaving("refresh");
        Snackbar.make(swipeRefreshLayout, "+ Refresh information...", Snackbar.LENGTH_LONG).show();
        loadSpinnerAccPhoneNumbers();
        LoadContacts();
        if (accountCredentials.getShowContacts()) {
            listView.setAdapter(arrayAdapterContacts);
        }
    }
    public void showSmsList() {
        callActionSmsList.hide();
        accountCredentials.setShowContacts(false);
        listView.setAdapter(arrayAdapterMsg);
        callActionContacts.show();
    }
    public void showContacts() {
        callActionContacts.hide();
        accountCredentials.setShowContacts(true);
        listView.setAdapter(arrayAdapterContacts);
        callActionSmsList.show();
    }

    // ---------------------------------------------------------------------------------------------

    public void LoadContacts(){
        ContactNamesNumbers.clear();
        ContactNumbersNames.clear();
        ContactNumbers.clear();
        ContactNames.clear();
        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);
        while (cursor.moveToNext()) {
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            phonenumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
            String theType = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));
            //
            int phoneType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
            String typeLabel;
            int typeMobile = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
            switch (phoneType)
            {
                /*
                case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                    typeLabel = "Home";
                    break;
                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                    typeLabel = "Work";
                    break;
                    */
                case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                    typeLabel = "Mobile";
                    break;
                case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
                    // For custom label, example: Work office or Work mobile
                    typeLabel = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
                    if (typeLabel.toLowerCase().contains("mobile")) {
                        phoneType = typeMobile;
                    }
                    break;
                default:
                    typeLabel = "";
            }
            //
            if ((theType == null || theType.equalsIgnoreCase("com.google")) && phoneType == typeMobile && phonenumber != null) {
                // null is the value for the emulator.
                // Don't add WhatsApp contacts ("com.whatsapp") because it duplicates the phone number.
                ContactNamesNumbers.add(name + " " + phonenumber);
                ContactNames.add(name);
                ContactNumbersNames.add(phonenumber + ":" + name);
                ContactNumbers.add(phonenumber);
            }
        }
        cursor.close();

        Collections.sort(ContactNamesNumbers);
        Collections.sort(ContactNames);
        arrayAdapterContacts = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, ContactNamesNumbers);
        //
        Collections.sort(ContactNumbersNames);
        Collections.sort(ContactNumbers);
    }

    // ---------------------------------------------------------------------------------------------
    private void loadSpinnerAccPhoneNumbers() {

        // Before the first OkHttpClient call.
        if (!checkPermissionForInternet()) {
            Snackbar.make(swipeRefreshLayout, "+ Request Permission For Internet.", Snackbar.LENGTH_LONG).show();
            requestPermissionForInternet();
            return;
        }

        networkOkay = false;
        // Snackbar.make(swipeRefreshLayout, "+ Loading account phone numbers...", Snackbar.LENGTH_LONG).show();
        twilioSms.setAccPhoneNumbers();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(twilioSms.getRequestUrl())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Snackbar.make(swipeRefreshLayout, "- Error: Network failure, try again.", Snackbar.LENGTH_LONG).show();
                call.cancel();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                networkOkay = true;
                final String jsonResponse = response.body().string();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ( jsonResponse.contains("\"code\": 20003") || jsonResponse.contains("\"status\": 404") ) {
                            Snackbar.make(swipeRefreshLayout, "+ Logging into your Twilio account failed. Go to Settings.", Snackbar.LENGTH_LONG).show();
                        } else if (loadAccPhoneNumberSpinner(theMenu, jsonResponse) > 0) {
                            jsonAccPhoneNumbers = jsonResponse;
                            accountCredentials.setAccPhoneNumberList(jsonResponse);
                            populateMessageList();
                        }
                    }
                });
            }
        });
    }

    private int loadAccPhoneNumberSpinner(Menu menu, String jsonList) {
        int numSmsPhoneNumbers = 0;
        final JSONObject responseJson;
        try {
            responseJson = new JSONObject(jsonList);
        } catch (JSONException e) {
            Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON response: accPhoneNumberPrintList", Snackbar.LENGTH_LONG).show();
            return numSmsPhoneNumbers;
        }
        // Top bar spinner list of account phone numbers.
        MenuItem item = menu.findItem(spinner);
        twilioNumberSpinner = (Spinner) item.getActionView();
        List<String> spinnerList = new ArrayList<String>();
        try {
            JSONArray jList = responseJson.getJSONArray("incoming_phone_numbers");
            for (int numPhoneNumbers = 0; numPhoneNumbers < jList.length(); numPhoneNumbers++) {
                String accPhoneNumber = jList.getJSONObject(numPhoneNumbers).getString("phone_number");
                if ( jList.getJSONObject(numPhoneNumbers).getJSONObject("capabilities").getBoolean("sms") ) {
                    // Only SMS capable phone numbers
                    spinnerList.add( accPhoneNumber );
                    numSmsPhoneNumbers ++;
                    // Note Hong Kong and UK numbers can only send/receive local SMS
                }
            }
        } catch (JSONException e) {
            // textString.setText("++ Check and set: Twilio Account Settings.");
            Snackbar.make(swipeRefreshLayout,
                    "-- Error: failed to parse JSON response: accPhoneNumberPrintList",
                    Snackbar.LENGTH_LONG).show();
            return numSmsPhoneNumbers;
        }
        if (numSmsPhoneNumbers==0) {
            // textString.setText("--- Your Twilio Account does not have an SMS capiable phone number.");
            // msgString.setText("++ Go to your Twilio and select a SMS capiable phone number.");
            Snackbar.make(swipeRefreshLayout, "- Error: No SMS capable account phone numbers: accPhoneNumberPrintList", Snackbar.LENGTH_LONG).show();
            return numSmsPhoneNumbers;
        }
        String[] spinnerArray = new String[ spinnerList.size() ];
        spinnerList.toArray( spinnerArray );
        Arrays.sort(spinnerArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList(spinnerArray));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        twilioNumberSpinner.setAdapter(adapter);
        //
        // Set the application Twilio account phone number.
        String twilioPhoneNumber = accountCredentials.getTwilioPhoneNumber();
        int positionTwilioPhoneNumber = adapter.getPosition(twilioPhoneNumber);
        if ( positionTwilioPhoneNumber < 0) {
            // if first time or the phone number is not found (example deleted from the account), set to the first phone number.
            positionTwilioPhoneNumber = 0;
            accountCredentials.setTwilioPhoneNumber( twilioNumberSpinner.getSelectedItem().toString() );
        }
        twilioNumberSpinner.setSelection( positionTwilioPhoneNumber );
        // Snackbar.make(swipeRefreshLayout, "+ positionTwilioPhoneNumber" + positionTwilioPhoneNumber, Snackbar.LENGTH_LONG).show();
        return numSmsPhoneNumbers;
    }

    // ---------------------------------------------------------------------------------------------
    private void populateMessageList() {
        networkOkay = false;
        // Snackbar.make(swipeRefreshLayout, "+ Loading messages...", Snackbar.LENGTH_LONG).show();

        // Set the Application Twilio Phone Number.
        String twilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        accountCredentials.setTwilioPhoneNumber(twilioNumber);
        twilioSms.setSmsRequestLogsTo(twilioNumber);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(twilioSms.getRequestUrl())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                Snackbar.make(swipeRefreshLayout, "- Error: failed to retrieve messages: populateMessageList", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                networkOkay = true;
                final String responseContent = response.body().string();
                final JSONObject responseJson;
                try {
                    responseJson = new JSONObject(responseContent);
                } catch (JSONException e) {
                    Snackbar.make(swipeRefreshLayout,
                            "- Error: failed to parse JSON response: populateMessageList",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (response.code() == 200) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println(responseContent);
                            try {
                                JSONArray messages = responseJson.getJSONArray("messages");
                                StoreMessages.clear();
                                int im = 0;
                                for (int i = 0; i < messages.length(); i++) {
                                    if (messages.getJSONObject(i).getString("status").equalsIgnoreCase("received")) {
                                        String theData = "";
                                        JSONObject messageJson = messages.getJSONObject(i);
                                        try {
                                            theData = messageJson.getString("body")
                                                    + "\n" + twilioSms.localDateTime(messageJson.getString("date_sent")).substring(0,6)
                                                    + ", From: "
                                                    // + messageJson.getString("from") + " "
                                                    + getNameFromNumber(messageJson.getString("from"));
                                            // + " to " + messageJson.getString("to")
                                        } catch (JSONException e) {
                                            Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON response: MessagesArrayAdapter", Snackbar.LENGTH_LONG).show();
                                            System.out.println(e);
                                        }
                                        StoreMessages.add(theData);
                                        im++;
                                    }
                                }
                                if ( im == 0 ) {
                                    Snackbar.make(swipeRefreshLayout, getString(R.string.NoMessages), Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                                arrayAdapterMsg = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, StoreMessages);
                                if (!accountCredentials.getShowContacts()) {
                                    listViewMsg = (ListView)findViewById(R.id.listview1);
                                    listViewMsg.setAdapter(arrayAdapterMsg);
                                }
                            } catch (JSONException e) {
                                Snackbar.make(swipeRefreshLayout, "-- Error: failed to parse JSON response: populateMessageList", Snackbar.LENGTH_LONG).show();
                                return;
                            }
                        }
                    });
                } else {
                    Snackbar.make(swipeRefreshLayout, "+ Logging into your Twilio account failed(" + response.code() + ").\n++ Go to Settings.", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private String getNameFromNumber(String phoneNumber) {
        String theName = phoneNumber;
        int i = ContactNumbers.indexOf(phoneNumber);
        if (i>=0) {
            theName = ContactNumbersNames.get(i);
            theName = theName.substring(theName.indexOf(":")+1, theName.length());
        }
        return theName;
    }

    // ---------------------------------------------------------------------------------------------
    private void doLookup() {
        if (!checkBeforeLeaving("")) {
            return;
        }
        if (!formPhoneNumber.getText().toString().isEmpty()) {
            // hide keyboard
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(formPhoneNumber.getWindowToken(), 0);
            //
            final String callPhoneNumber = formPhoneNumber.getText().toString();
            // String callContactName = labelContactName.getText().toString();
            if (!callPhoneNumber.isEmpty()) {
                // -----------------------------------------------------
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Confirm phone number information lookup for:\n      " + callPhoneNumber)
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                lookupResults( callPhoneNumber );
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Snackbar.make(swipeRefreshLayout, "+ Cancelled.", Snackbar.LENGTH_LONG).show();
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                // -----------------------------------------------------
            }
        }
    }

    private void lookupResults( final String formPhoneNumber ) {

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(twilioSms.getLookup(formPhoneNumber))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                Snackbar.make(swipeRefreshLayout, "- Error: Failed to retrieve messages.", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseContent = response.body().string();

                final JSONObject responseJson;
                try {
                    responseJson = new JSONObject(responseContent);
                } catch (JSONException e) {
                    Snackbar.make(swipeRefreshLayout, "- Error 1: Failed to parse JSON response.", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (response.code() == 200) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println(responseContent);
                            String theData = "";
                            try {
                                theData = "\nCountry code: " + responseJson.getString("country_code")
                                        + "\nLocal format: " + responseJson.getString("national_format")
                                        + "\nCarrier: " + responseJson.getJSONObject("carrier").getString("name")
                                        + "\nType of line: " + responseJson.getJSONObject("carrier").getString("type");
                            } catch (JSONException e) {
                                Snackbar.make(swipeRefreshLayout, "- Error 2: Failed to parse JSON response.", Snackbar.LENGTH_LONG).show();
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("Phone number information for:\n      " + formPhoneNumber + "\n" + theData)
                                    .setCancelable(false)
                                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Snackbar.make(swipeRefreshLayout, "+ Lookup complete", Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });
                } else if (response.code() == 404) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(swipeRefreshLayout, "+ Phone number not found.", Snackbar.LENGTH_LONG).show();
                        }
                    });
                } else {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(swipeRefreshLayout, "- Error: Received %s status code.", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    // ---------------------------------------------------------------------------------------------

}
