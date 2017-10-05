package com.tfp.tfpsms;
/*
    Steps to add OkHTTP:
    Into build.gradle (Module: app), add:
        dependencies { ... compile 'com.squareup.okhttp3:okhttp:3.4.1' ... }

    Add access (uses-permission) into AndroidManifest.xml:
    <manifest ...>
        <uses-permission android:name="android.permission.INTERNET"/>
        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <application ...
 */

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.tfp.tfpsms.R.id.spinner;

public class DebugActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private TwSms twilioSms;
    private Spinner twilioNumberSpinner;

    // https://developer.android.com/reference/android/widget/TextView.html
    // https://developer.android.com/reference/android/widget/RelativeLayout.LayoutParams.html
    private TextView textString, msgString;
    private TextView textScrollBox;
    private Button listPhoneNumbers, buttonGet, buttonDelete;
    private Button accPhoneNumbers;

    private Button buttonSend;
    private EditText formPhoneNumber;
    private EditText textMessage;

    private EncDecString doEncDecString;
    private Button buttonEncDec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        // -----------------------------------------------------------------------------------------
        listPhoneNumbers = (Button) findViewById(R.id.listPhoneNumbers);
        listPhoneNumbers.setOnClickListener(this);

        buttonGet = (Button) findViewById(R.id.buttonGet);
        buttonGet.setOnClickListener(this);

        buttonDelete = (Button) findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(this);

        accPhoneNumbers = (Button) findViewById(R.id.accPhoneNumbers);
        accPhoneNumbers.setOnClickListener(this);
        //
        // Send message form objects:
        buttonSend = (Button) findViewById(R.id.buttonSend);
        buttonSend.setOnClickListener(this);
        formPhoneNumber = (EditText)findViewById(R.id.formPhoneNumber);
        textMessage = (EditText)findViewById(R.id.textMessage);
        //
        // Encrypt - decrypt:
        doEncDecString = new EncDecString();
        buttonEncDec = (Button) findViewById(R.id.buttonEncDec);
        buttonEncDec.setOnClickListener(this);

        textString = (TextView) findViewById(R.id.textString);
        msgString = (TextView) findViewById(R.id.msgString);

        textScrollBox = (TextView) findViewById(R.id.scrollBox);
        textScrollBox.setMovementMethod(new ScrollingMovementMethod());

        // -----------------------------------------------------------------------------------------
        accountCredentials = new AccountCredentials(this);
        twilioSms = new TwSms(accountCredentials);
        formPhoneNumber.setText( accountCredentials.getToPhoneNumber() );

        // Top bar with: return to MainActivity.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Adds 3-dot option menu in the action bar.
        getMenuInflater().inflate(R.menu.menu_debug, menu);

        // Top bar list of account phone numbers:
        loadSpinnerAccPhoneNumbers(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Note, this automatically back-arrow to parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_encode) {
            textString.setText("+ 3-dot menu selected: action_encode");
            return true;
        } else if (id == R.id.action_getfrom) {
            textString.setText("+ 3-dot menu selected: action_getfrom");
            return true;
        } else if (id == R.id.action_getto) {
            textString.setText("+ 3-dot menu selected: action_getto");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public void onClick(View view) {
        String twilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        String theFormPhoneNumber = formPhoneNumber.getText().toString();
        switch (view.getId()) {
            case R.id.buttonSend:
                try {
                    String theTextMessage = textMessage.getText().toString();
                    textString.setText("+ Send message to: " + theFormPhoneNumber);
                    twilioSms.setSmsSend(theFormPhoneNumber, twilioNumber, theTextMessage);
                    sendSms();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.listPhoneNumbers:
                try {
                    textString.setText("+ Messages From: "+ twilioNumber);
                    msgString.setText("+ Messages to  : "+ theFormPhoneNumber);
                    twilioSms.setSmsRequestLogs(twilioNumber, theFormPhoneNumber);
                    getMessageList();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.buttonGet:
                try {
                    textString.setText("+ Messages From: "+ theFormPhoneNumber);
                    msgString.setText("+ Messages to  : "+ twilioNumber);
                    twilioSms.setSmsRequestLogs(theFormPhoneNumber, twilioNumber);
                    getMessageList();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.buttonDelete:
                try {
                    textScrollBox.setText("+ Remove messages exchanged with: " + theFormPhoneNumber);
                    //
                    textString.setText("+ Remove messages to  : " + theFormPhoneNumber);
                    twilioSms.setSmsRequestLogs(twilioNumber, theFormPhoneNumber);
                    getMessagesToDelete();
                    //
                    msgString.setText( "+ Remove messages from: "+ theFormPhoneNumber);
                    twilioSms.setSmsRequestLogs(theFormPhoneNumber, twilioNumber);
                    getMessagesToDelete();
                    //
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.accPhoneNumbers:
                try {
                    textString.setText("+ Account Phone Number List");
                    twilioSms.setAccPhoneNumbers();
                    // msgString.setText("+ Request: " + twilioSms.getRequestUrl());
                    getAccPhoneNumbers();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.buttonEncDec:
                try {
                    String themessage = textMessage.getText().toString();
                    String encString = "abc";
                    String decString = "def";
                    try {
                        encString = doEncDecString.encryptBase64String(themessage);
                        decString = doEncDecString.decryptBase64String(encString);
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (InvalidAlgorithmParameterException e) {
                        e.printStackTrace();
                    }
                    textString.setText("+ textString, encrypted and decrypted.");
                    // msgString.setText("+ msgString, decrypted :" + decString +":");
                    textScrollBox.setText(
                            "+ textString \n:" + themessage +":\n\n"
                                    + "+ textString, encrypted \n:" + encString.trim() +":\n\n"
                                    + "+ textString, decrypted \n:" + decString +":\n"
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    private void getMessagesToDelete() throws Exception {

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
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                DebugActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> messageList = getDeleteMessageList(myResponse);
                        int aCounter = 0;
                        try {
                            String aMessageId;
                            for (Iterator<String> iter = messageList.iterator(); iter.hasNext();) {
                                aMessageId = iter.next();
                                deleteOneMessage( aMessageId );
                                aCounter++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        textScrollBox.setText( textScrollBox.getText() + "\n+ Messages deleted = " + aCounter);
                    }
                });
            }
        });
    }

    private List<String> getDeleteMessageList(String jsonList) {
        List<String> listSids = new ArrayList<String>();
        // Check if there is no messages.
        String mtMessages = "\"messages\": []";
        if (jsonList.indexOf(mtMessages, 0)>0) {
            return listSids;
        }
        // Message SID:
        // "sid": "SM57be9436e08a43d2bcec786fba8c9424",
        String theSid = "\"sid\":";
        String endValue = "\",";
        int si = jsonList.indexOf(theSid, 0);
        int ei = 0;
        while (si > 0) {
            ei = jsonList.indexOf(endValue, si);
            if (si > 0) {
                ei = jsonList.indexOf(endValue, si);
                String aSid = jsonList.substring(si + theSid.length() + 2, ei);
                listSids.add(aSid);
            }
            si = jsonList.indexOf(theSid, ei);
        }
        return listSids;
    }

    private void deleteOneMessage(final String aMessageId ) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(twilioSms.rmSmsMessages(aMessageId))
                .delete()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                DebugActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // textScrollBox.setText("+ deleteOneMessage " + myResponse);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    private void getMessageList() throws Exception {

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
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                DebugActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textScrollBox.setText( printMessageLog(myResponse) );
                    }
                });
            }
        });
    }

    private String printMessageLog(String jsonResponse) {
        JSONObject responseJson;
        JSONArray theJsonArray;
        try {
            responseJson = new JSONObject(jsonResponse);
            theJsonArray = responseJson.getJSONArray("messages");
        } catch (JSONException e) {
            System.out.println("-- Failed to parse JSON response.");
            return "-- Failed to parse JSON response.";
        }
        String theResponse = "++ Message list: from, to, status, date, message" + "\n";
        try {
            int theLength = theJsonArray.length();
            if ( theLength == 0 ) {
                return "+ No messages.";
            }
            for (int i = 0; i < theLength; i++) {
                theJsonArray.getJSONObject(i).getString("from");
                int counter = i + 1;
                theResponse = theResponse
                        + counter
                        + " " + theJsonArray.getJSONObject(i).getString("from")
                        + ", " + theJsonArray.getJSONObject(i).getString("to")
                        + ", " + theJsonArray.getJSONObject(i).getString("status")
                        + ", " + twilioSms.localDateTime( theJsonArray.getJSONObject(i).getString("date_updated") )
                        + ", " + theJsonArray.getJSONObject(i).getString("body")
                        + "\n";
            }
        } catch (JSONException e) {
            System.out.println("-- Failed to parse JSON response.");
            return "-- Failed to parse JSON response items.";
        }
        return theResponse;
    }

    // ---------------------------------------------------------------------------------------------
    private void sendSms() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(twilioSms.getRequestUrl())
                .post(twilioSms.getPostParams())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                DebugActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textScrollBox.setText(responseStatus(myResponse));
                    }
                });
            }
        });
    }

    private String responseStatus(String jsonList) {
        String theStatusResult = "+ Response status: ";

        // List the date sent and the message body:
        // "body": "Hello from Android app",
        // "date_sent": "Mon, 25 Sep 2017 19:55:18 +0000",
        String theDateUpdated = "\"date_updated\":";
        String theBody = "\"body\":";
        String theStatus = "\"status\":";
        String endValue = "\",";

        int si = jsonList.indexOf(theDateUpdated, 0);
        int ei = 0;
        if (si > 0) {
            ei = jsonList.indexOf(endValue, si);
            //  123456                   123456
            // :Tue, 26 Sep 2017 00:49:31 +0000:
            theStatusResult = theStatusResult + localDateTime( jsonList.substring(si + theDateUpdated.length() + 2 + 5, ei - 6) );
        }
        si = jsonList.indexOf(theStatus, 0);
        if (si > 0) {
            ei = jsonList.indexOf(endValue, si);
            theStatusResult = theStatusResult + ", " + jsonList.substring(si + theStatus.length() + 2, ei) + "\n";
        }
        si = jsonList.indexOf(theBody, 0);
        if (si > 0) {
            ei = jsonList.indexOf(endValue, si);
            theStatusResult = theStatusResult + "+ Message: " + jsonList.substring(si + theStatus.length(), ei) + "\n";
        }

        return theStatusResult;
    }

    private String localDateTime(String theGmtDate) {
        //                                                        "27 Sep 2017 00:32:47"
        SimpleDateFormat readDateFormatter = new SimpleDateFormat("dd MMM yyyy hh:mm:ss");
        Date gmtDate = new Date();
        try {
            gmtDate = readDateFormatter.parse(theGmtDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(gmtDate);
        cal.add(Calendar.HOUR, -7); // from GMT to PST

        SimpleDateFormat writeDateformatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
        return writeDateformatter.format(cal.getTime());
    }

    // ---------------------------------------------------------------------------------------------
    private void getAccPhoneNumbers() throws Exception {
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
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String jsonResponse = response.body().string();
                DebugActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textScrollBox.setText(accPhoneNumberPrintList(jsonResponse));
                    }
                });
            }
        });
    }

    private String accPhoneNumberPrintList(String jsonList) {
        String aPrintList = "";
        final JSONObject responseJson;
        try {
            responseJson = new JSONObject(jsonList);
        } catch (JSONException e) {
            // Snackbar.make(swipeRefreshLayout, "Failed to parse JSON response", Snackbar.LENGTH_LONG).show();
            return aPrintList;
        }
        try {
            JSONArray jList = responseJson.getJSONArray("incoming_phone_numbers");
            for (int i = 0; i < jList.length(); i++) {
                aPrintList = aPrintList
                        + jList.getJSONObject(i).getString("phone_number")
                        + " : "
                        + jList.getJSONObject(i).getString("friendly_name")
                        + "\n";
            }
        } catch (JSONException e) {
            // Snackbar.make(swipeRefreshLayout, "Failed to parse JSON", Snackbar.LENGTH_LONG).show();
            return aPrintList;
        }
        return aPrintList;
    }

    // ---------------------------------------------------------------------------------------------
    private void loadSpinnerAccPhoneNumbers(final Menu menu) {
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
                call.cancel();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String jsonResponse = response.body().string();
                DebugActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textScrollBox.setText("+ Add to the spinner:\n" + accPhoneNumberPrintList(menu, jsonResponse));
                    }
                });
            }
        });
    }

    private String accPhoneNumberPrintList(Menu menu, String jsonList) {
        String aPrintList = "";
        final JSONObject responseJson;
        try {
            responseJson = new JSONObject(jsonList);
        } catch (JSONException e) {
            // Snackbar.make(swipeRefreshLayout, "Failed to parse JSON response", Snackbar.LENGTH_LONG).show();
            return aPrintList;
        }

        // Top bar spinner list of account phone numbers.
        MenuItem item = menu.findItem(spinner);
        twilioNumberSpinner = (Spinner) item.getActionView();
        List<String> spinnerList = new ArrayList<String>();
        try {
            JSONArray jList = responseJson.getJSONArray("incoming_phone_numbers");
            for (int i = 0; i < jList.length(); i++) {
                String accPhoneNumber = jList.getJSONObject(i).getString("phone_number");
                spinnerList.add( accPhoneNumber );
                aPrintList = aPrintList
                        + accPhoneNumber
                        + " : "
                        + jList.getJSONObject(i).getString("friendly_name")
                        + "\n";
            }
        } catch (JSONException e) {
            // Snackbar.make(swipeRefreshLayout, "Failed to parse JSON", Snackbar.LENGTH_LONG).show();
            return aPrintList;
        }
        String[] spinnerArray = new String[ spinnerList.size() ];
        spinnerList.toArray( spinnerArray );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList(spinnerArray));
        //
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        twilioNumberSpinner.setAdapter(adapter);
        twilioNumberSpinner.setSelection( adapter.getPosition("+"+accountCredentials.getTwilioPhoneNumber()) );

        return aPrintList;
    }

    // ---------------------------------------------------------------------------------------------
}
