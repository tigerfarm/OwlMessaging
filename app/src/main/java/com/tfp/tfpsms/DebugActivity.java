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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DebugActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private TwSms URL_REQUEST;

    // https://developer.android.com/reference/android/widget/TextView.html
    // https://developer.android.com/reference/android/widget/RelativeLayout.LayoutParams.html
    private TextView textString, msgString;
    private TextView textScrollBox;
    private Button listPhoneNumbers, asynchronousGet, synchronousGet, asynchronousPOST;
    private Button accPhoneNumbers;

    private Button sendButton;
    private EditText sendToPhoneNumber;
    private EditText textMessage;

    private String twilioNumber;
    private String phoneNumber;

    private EncDecString doEncDecString;
    private Button buttonEncDec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        // To return to MainActivity
        setupActionBar();

        accPhoneNumbers = (Button) findViewById(R.id.accPhoneNumbers);
        listPhoneNumbers = (Button) findViewById(R.id.listPhoneNumbers);
        asynchronousGet = (Button) findViewById(R.id.asynchronousGet);
        synchronousGet = (Button) findViewById(R.id.synchronousGet);
        asynchronousPOST = (Button) findViewById(R.id.asynchronousPost);

        // Send message form objects:
        sendButton = (Button) findViewById(R.id.sendButton);
        sendToPhoneNumber = (EditText)findViewById(R.id.sendToPhoneNumber);
        textMessage = (EditText)findViewById(R.id.textMessage);
        //
        // Encrypt - decrypt:
        doEncDecString = new EncDecString();
        buttonEncDec = (Button) findViewById(R.id.buttonEncDec);
        buttonEncDec.setOnClickListener(this);

        sendButton.setOnClickListener(this);
        accPhoneNumbers.setOnClickListener(this);
        listPhoneNumbers.setOnClickListener(this);
        asynchronousGet.setOnClickListener(this);
        synchronousGet.setOnClickListener(this);
        asynchronousPOST.setOnClickListener(this);

        textString = (TextView) findViewById(R.id.textString);
        msgString = (TextView) findViewById(R.id.msgString);

        textScrollBox = (TextView) findViewById(R.id.scrollBox);
        textScrollBox.setMovementMethod(new ScrollingMovementMethod());
        // textScrollBox.setText(aString);

        accountCredentials = new AccountCredentials(this);
        URL_REQUEST = new TwSms(accountCredentials);

        twilioNumber = accountCredentials.getTwilioPhoneNumber();
        phoneNumber = accountCredentials.getToPhoneNumber();
    }

    // ---------------------------------------------------------------------------------------------
    // Show the Back arrow (Up button) in the action bar.
    // When clicked, to the MainActivity.
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
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
            case R.id.sendButton:
                try {
                    String theSendToPhoneNumber = sendToPhoneNumber.getText().toString();
                    String theTextMessage = textMessage.getText().toString();
                    textString.setText("+ Send message to: " + theSendToPhoneNumber);
                    URL_REQUEST.setSmsSend(phoneNumber, twilioNumber, theTextMessage);
                    sendSms();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.accPhoneNumbers:
                try {
                    textString.setText("+ Account Phone Number List");
                    URL_REQUEST.setAccPhoneNumbers();
                    // msgString.setText("+ Request: " + URL_REQUEST.getRequestUrl());
                    getAccPhoneNumbers();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.listPhoneNumbers:
                try {
                    textString.setText("+ Phone Number Exchange List");
                    textScrollBox.setText("+ List not available, yet.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.asynchronousGet:
                try {
                    textString.setText("+ SMS send message to: " + phoneNumber);
                    URL_REQUEST.setSmsSend(phoneNumber, twilioNumber, "Hello from Android app");
                    sendSms();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.synchronousGet:
                try {
                    // URL_REQUEST.setUrlHello();
                    // textString.setText("+ GET Hello World text file: "+URL_REQUEST.getRequestUrl());
                    // getRequest();
                    textString.setText("+ Get messages sent to: "+ phoneNumber);
                    URL_REQUEST.setSmsRequestTo(phoneNumber, twilioNumber);
                    getMessageList();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.asynchronousPost:
                try {
                    textString.setText("+ Remove messages to: " + phoneNumber);
                    getMessagesToDelete();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    void getAccPhoneNumbers() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(URL_REQUEST.getRequestUrl())
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

    String accPhoneNumberPrintList(String jsonList) {
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
    void getMessagesToDelete() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(URL_REQUEST.getRequestUrl())
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
                        textScrollBox.setText("+ Messages deleted = " + aCounter);
                    }
                });
            }
        });
    }

    List<String> getDeleteMessageList(String jsonList) {

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

    void deleteOneMessage(final String aMessageId ) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(URL_REQUEST.rmSmsMessages(aMessageId))
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
    void getMessageList() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(URL_REQUEST.getRequestUrl())
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
                        textScrollBox.setText(listMessages(myResponse));
                    }
                });
            }
        });
    }

    String listMessages(String jsonList) {
        String theList = "+ No messages.";

        // Check if there is no messages.
        String mtMessages = "\"messages\": []";
        if (jsonList.indexOf(mtMessages, 0)>0) {
            return theList;
        }
        theList = "";

        // List the date sent and the message body:
        // "body": "Hello from Android app",
        // "date_sent": "Mon, 25 Sep 2017 19:55:18 +0000",
        String theDateSent = "\"date_sent\":";
        String theBody = "\"body\":";
        String endValue = "\",";

        int si = jsonList.indexOf(theDateSent, 0);
        int ei = 0;
        while (si > 0) {
            ei = jsonList.indexOf(endValue, si);
            //  123456                   123456
            // :Tue, 26 Sep 2017 00:49:31 +0000:
            theList = theList + localDateTime( jsonList.substring(si + theDateSent.length() + 2 + 5, ei - 6) );

            si = jsonList.indexOf(theBody, ei);
            if (si > 0) {
                ei = jsonList.indexOf(endValue, si);
                theList = theList + ", " + jsonList.substring(si + theBody.length() + 2, ei) + "\n";
            }
            si = jsonList.indexOf(theDateSent, ei);
        }

        return theList;
    }

    // ---------------------------------------------------------------------------------------------
    void sendSms() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(URL_REQUEST.getRequestUrl())
                .post(URL_REQUEST.getPostParams())
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

    String responseStatus(String jsonList) {
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

    String localDateTime(String theGmtDate) {
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
    // Not used.
    // ---------------------------------------------------------------------------------------------
    void getRequest() throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL_REQUEST.getRequestUrl())
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
                        textScrollBox.setText(myResponse);
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    void postRequest() throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL_REQUEST.getRequestUrl())
                .post(URL_REQUEST.getPostParams())
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
                        textScrollBox.setText(myResponse);
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
}
