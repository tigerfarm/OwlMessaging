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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TwSms URL_REQUEST = new TwSms();

    // https://developer.android.com/reference/android/widget/TextView.html
    TextView textString;
    TextView textScrollBox;
    Button asynchronousGet, synchronousGet, asynchronousPOST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        asynchronousGet = (Button) findViewById(R.id.asynchronousGet);
        synchronousGet = (Button) findViewById(R.id.synchronousGet);
        asynchronousPOST = (Button) findViewById(R.id.asynchronousPost);

        asynchronousGet.setOnClickListener(this);
        synchronousGet.setOnClickListener(this);
        asynchronousPOST.setOnClickListener(this);

        textString = (TextView) findViewById(R.id.textString);

        textScrollBox = (TextView) findViewById(R.id.scrollBox);
        textScrollBox.setMovementMethod(new ScrollingMovementMethod());
        // textScrollBox.setText(aString);
    }
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onClick(View view) {
        String requestUrl;
        switch (view.getId()) {
            case R.id.asynchronousGet:
                try {
                    String phoneNumTo = "+16504837603";
                    textString.setText("+ SMS send message to: " + phoneNumTo);
                    URL_REQUEST.setSmsSend(phoneNumTo,"Hello from Android app");
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
                    String phoneNumber = "+16504837603";
                    textString.setText("+ Get messages sent to: "+ phoneNumber);

                    URL_REQUEST.setSmsRequestTo(phoneNumber);
                    getMessageList();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.asynchronousPost:
                try {
                    String phoneNumber = "+16504837603";
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
    void getMessagesToDelete() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AccountCredentials())
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
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String aMessageId = getDeleteMessageList(myResponse);
                        try {
                            deleteOneMessage( aMessageId );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        textScrollBox.setText("+ Messages deleted = 1");
                    }
                });
            }
        });
    }

    String getDeleteMessageList(String jsonList) {
        String theList = "";

        // Check if there is no messages.
        String mtMessages = "\"messages\": []";
        if (jsonList.indexOf(mtMessages, 0)>0) {
            return theList;
        }

        // Message SID:
        // "sid": "SM57be9436e08a43d2bcec786fba8c9424",
        String theSid = "\"sid\":";
        String endValue = "\",";

        int si = jsonList.indexOf(theSid, 0);
        int ei = 0;
        // while (si > 0) {
            ei = jsonList.indexOf(endValue, si);
            if (si > 0) {
                ei = jsonList.indexOf(endValue, si);
                theList = theList + jsonList.substring(si + theSid.length() + 2, ei);
            }
            si = jsonList.indexOf(theSid, ei);
        // }

        return theList;
    }

    void deleteOneMessage(final String aMessageId ) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AccountCredentials())
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
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            textScrollBox.setText("+ deleteOneMessage " + myResponse);
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
                .addInterceptor(new AccountCredentials())
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
                MainActivity.this.runOnUiThread(new Runnable() {
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
            theList = theList + jsonList.substring(si + theDateSent.length() + 2 + 5, ei - 6);

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
                .addInterceptor(new AccountCredentials())
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
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textScrollBox.setText(myResponse);
                    }
                });
            }
        });
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
                MainActivity.this.runOnUiThread(new Runnable() {
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
                MainActivity.this.runOnUiThread(new Runnable() {
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
