package com.tfp.tfpsms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import static android.os.SystemClock.sleep;
import static com.tfp.tfpsms.R.id.spinner;

public class SendSmsActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private TwSms twilioSms;
    private Spinner twilioNumberSpinner;

    private Button sendButton, setButton;
    private EditText sendToPhoneNumber;
    private EditText textMessage;

    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MessagesArrayAdapter messagesArrayAdapter;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sendsms);

        // Top bar with: return to MainActivity.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Send message form objects:
        setButton = (Button) findViewById(R.id.setButton);
        setButton.setOnClickListener(this);
        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
        sendToPhoneNumber = (EditText)findViewById(R.id.sendToPhoneNumber);
        textMessage = (EditText)findViewById(R.id.textMessage);

        accountCredentials = new AccountCredentials(this);
        twilioSms = new TwSms(accountCredentials);
        sendToPhoneNumber.setText(accountCredentials.getToPhoneNumber());

        listView = (ListView) findViewById(R.id.list_view);
        messagesArrayAdapter = new MessagesArrayAdapter(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(messagesArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
            }
        });
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                populateMessageList();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Adds 3-dot option menu in the action bar.
        getMenuInflater().inflate(R.menu.menu_sendsms, menu);

        // Top bar list of account phone numbers:
        loadSpinnerAccPhoneNumbers(menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Set the Application Twilio Phone Number.
        String twilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        accountCredentials.setTwilioPhoneNumber( twilioNumber );
        String theFormPhoneNumber = sendToPhoneNumber.getText().toString();
        accountCredentials.setToPhoneNumber(theFormPhoneNumber);

        // Note, this automatically adds a back-arrow to parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            try {
                // textString.setText("+ Remove messages to  : " + theFormPhoneNumber);
                twilioSms.setSmsRequestLogs(twilioNumber, theFormPhoneNumber);
                getMessagesToDelete();
                // msgString.setText( "+ Remove messages from: "+ theFormPhoneNumber);
                twilioSms.setSmsRequestLogs(theFormPhoneNumber, twilioNumber);
                getMessagesToDelete();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public void onClick(View view) {

        // Set the Application Twilio Phone Number.
        String twilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        accountCredentials.setTwilioPhoneNumber(twilioNumber);
        String theFormPhoneNumber = sendToPhoneNumber.getText().toString();
        accountCredentials.setToPhoneNumber(theFormPhoneNumber);

        switch (view.getId()) {
            case R.id.sendButton:
                try {
                    twilioSms.setSmsSend(theFormPhoneNumber, twilioNumber, textMessage.getText().toString());
                    sendSms();
                    // wait(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.setButton:
                try {
                    // textString.setText("+ setButton, Send message to: " + toPhoneNumber);
                    populateMessageList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    }

    // ---------------------------------------------------------------------------------------------
    void sendSms() throws Exception {
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
                Snackbar.make(swipeRefreshLayout, "- Error: failed to send message.", Snackbar.LENGTH_LONG).show();
                call.cancel();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                SendSmsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // textScrollBox.setText(responseStatus(myResponse));
                        Snackbar.make(swipeRefreshLayout, getString(R.string.MessagesSent), Snackbar.LENGTH_LONG).setDuration(3000).show();
                        // sleep(1000);
                        populateMessageList();
                        textMessage.setText("");
                    }
                });
            }
        });
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
                SendSmsActivity.this.runOnUiThread(new Runnable() {
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
                        messagesArrayAdapter.clear();
                        // textScrollBox.setText( textScrollBox.getText() + "\n+ Messages deleted = " + aCounter);
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
                SendSmsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // textScrollBox.setText("+ deleteOneMessage " + myResponse);
                            Snackbar.make(swipeRefreshLayout, getString(R.string.MessagesDeleted), Snackbar.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
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
                SendSmsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        accPhoneNumberSpinnerList(menu, jsonResponse);
                    }
                });
            }
        });
    }

    private void accPhoneNumberSpinnerList(Menu menu, String jsonList) {
        final JSONObject responseJson;
        try {
            responseJson = new JSONObject(jsonList);
        } catch (JSONException e) {
            Snackbar.make(swipeRefreshLayout, "- Error: Failed to parse JSON response", Snackbar.LENGTH_LONG).show();
            return;
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
            }
        } catch (JSONException e) {
            Snackbar.make(swipeRefreshLayout, "- Failed to parse JSON", Snackbar.LENGTH_LONG).show();
            return;
        }
        String[] spinnerArray = new String[ spinnerList.size() ];
        spinnerList.toArray( spinnerArray );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList(spinnerArray));
        //
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        twilioNumberSpinner.setAdapter(adapter);
        twilioNumberSpinner.setSelection( adapter.getPosition(accountCredentials.getTwilioPhoneNumber()) );

        populateMessageList();

        return;
    }

    // ---------------------------------------------------------------------------------------------
    private void populateMessageList() {

        final String selectedTwilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        final String formPhoneNumber = sendToPhoneNumber.getText().toString();

        // textString.setText("+ Messages from " + selectedTwilioNumber + " to " + formPhoneNumber);
        twilioSms.setSmsRequestLogs(selectedTwilioNumber, formPhoneNumber);
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
                Snackbar.make(swipeRefreshLayout, "- Error: failed to retrieve messages", Snackbar.LENGTH_LONG).show();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String stringFromTwilioNumberToFormNumber = response.body().string();
                final JSONObject responseJson;
                try {
                    responseJson = new JSONObject(stringFromTwilioNumberToFormNumber);
                } catch (JSONException e) {
                    Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON", Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (response.code() == 200) {
                    SendSmsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("+ stringFromTwilioNumberToFormNumber: " + stringFromTwilioNumberToFormNumber);
                            populateMessageListView(responseJson, selectedTwilioNumber, formPhoneNumber);
                        }
                    });
                } else {
                    Snackbar.make(swipeRefreshLayout, String.format("- Received %s status code", response.code()), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void populateMessageListView(final JSONObject jsonFromTwilioNumberToFormNumber, final String selectedTwilioNumber, String formPhoneNumber) {

        // textString.setText("+ Messages from " + formPhoneNumber + " to " + selectedTwilioNumber);
        twilioSms.setSmsRequestLogs(formPhoneNumber, selectedTwilioNumber );
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
                Snackbar.make(swipeRefreshLayout, "- Error: failed to retrieve messages", Snackbar.LENGTH_LONG).show();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {

                // ---------------------------------------------------------------------------------
                // Messages from formPhoneNumber to selectedTwilioNumber
                final String stringFromFormNumberToTwilioNumber = response.body().string();
                final JSONObject jsonFromFormNumberToTwilioNumber;
                try {
                    jsonFromFormNumberToTwilioNumber = new JSONObject(stringFromFormNumberToTwilioNumber);
                } catch (JSONException e) {
                    Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON response", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (response.code() == 200) {
                    SendSmsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messagesArrayAdapter.clear();

                            System.out.println("+ stringFromFormNumberToTwilioNumber: " + stringFromFormNumberToTwilioNumber);
                            try {
                                int im = 0;

                                // Messages from selectedTwilioNumber to formPhoneNumber
                                JSONArray msgFromTwilioNumberToFormNumber = jsonFromTwilioNumberToFormNumber.getJSONArray("messages");
                                // Messages from formPhoneNumber to selectedTwilioNumber
                                JSONArray msgFromFormNumberToTwilioNumber = jsonFromFormNumberToTwilioNumber.getJSONArray("messages");

                                for (int i = 0; i < msgFromTwilioNumberToFormNumber.length(); i++) {
                                    if ( !msgFromTwilioNumberToFormNumber.getJSONObject(i).getString("status").equalsIgnoreCase("received")) {
                                        // if Twilio account phone number to Twilio account phone number: "received" and "delivered"
                                        // if Twilio phone number to non-Twilio phone number: "delivered"
                                        messagesArrayAdapter.insert(msgFromTwilioNumberToFormNumber.getJSONObject(i), im);
                                        im++;
                                    }
                                }
                                for (int i = 0; i < msgFromFormNumberToTwilioNumber.length(); i++) {
                                    if ( !msgFromFormNumberToTwilioNumber.getJSONObject(i).getString("status").equalsIgnoreCase("delivered")) {
                                        // if Twilio account phone number to Twilio account phone number: "received" and "delivered"
                                        // if non-Twilio phone number to Twilio account phone number: "received"
                                        messagesArrayAdapter.insert(msgFromFormNumberToTwilioNumber.getJSONObject(i), im);
                                        im++;
                                    }
                                }
                                /*
                                // Messages from selectedTwilioNumber to formPhoneNumber
                                JSONArray msgFromTwilioNumberToFormNumber = jsonFromTwilioNumberToFormNumber.getJSONArray("messages");
                                // textString.setText("");
                                for (int i = 0; i < msgFromTwilioNumberToFormNumber.length(); i++) {
                                    if ( !msgFromTwilioNumberToFormNumber.getJSONObject(i).getString("status").equalsIgnoreCase("received")) {
                                        // if Twilio account phone number to Twilio account phone number: "received" and "delivered"
                                        // if Twilio phone number to non-Twilio phone number: "delivered"
                                        messagesArrayAdapter.insert(msgFromTwilioNumberToFormNumber.getJSONObject(i), im);
                                        im++;
                                    }
                                }
                                // Messages from formPhoneNumber to selectedTwilioNumber
                                JSONArray msgFromFormNumberToTwilioNumber = jsonFromFormNumberToTwilioNumber.getJSONArray("messages");
                                for (int i = 0; i < msgFromFormNumberToTwilioNumber.length(); i++) {
                                    if ( !msgFromFormNumberToTwilioNumber.getJSONObject(i).getString("status").equalsIgnoreCase("delivered")) {
                                        // if Twilio account phone number to Twilio account phone number: "received" and "delivered"
                                        // if non-Twilio phone number to Twilio account phone number: "received"
                                        messagesArrayAdapter.insert(msgFromFormNumberToTwilioNumber.getJSONObject(i), im);
                                        im++;
                                    }
                                }
                                */

                                if ( im == 0 ) {
                                    Snackbar.make(swipeRefreshLayout, getString(R.string.NoMessages), Snackbar.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Snackbar.make(swipeRefreshLayout, String.format("- Error: received %s status code", response.code()), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private class MessagesArrayAdapter extends ArrayAdapter<JSONObject> {
        public MessagesArrayAdapter(@NonNull Context context, @LayoutRes int resource) {
            super(context, resource);
        }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = getLayoutInflater().inflate(R.layout.list_item_message, parent, false);

            TextView labelView = (TextView) view.findViewById(R.id.host_row_label);
            TextView hostnameView =(TextView) view.findViewById(R.id.host_row_hostname);
            TextView portsView =(TextView) view.findViewById(R.id.host_row_ports);

            JSONObject messageJson = getItem(position);
            try {
                String messageTwilioNumber = twilioNumberSpinner.getSelectedItem().toString();
                String thePhoneNumber = messageJson.getString("from");
                if (messageTwilioNumber.equalsIgnoreCase(thePhoneNumber)) {
                    view.setBackgroundResource( R.color.listBackgroundIncoming );
                    labelView.setGravity(Gravity.RIGHT);
                    hostnameView.setGravity(Gravity.RIGHT);
                    portsView.setGravity(Gravity.RIGHT);
                }
                labelView.setText("From: " + messageJson.getString("from") + " To: " + messageJson.getString("to"));
                String theStatus = messageJson.getString("status");
                if (!theStatus.equalsIgnoreCase("delivered") && !theStatus.equalsIgnoreCase("received")) {
                    labelView.setText( labelView.getText() + ", status: " + messageJson.getString("status"));
                }
                hostnameView.setText(messageJson.getString("body"));
                portsView.setText(twilioSms.localDateTime( messageJson.getString("date_sent")));
            } catch (JSONException e) {
                Log.e("SendSmsActivity", "- Error: failed to parse JSON", e);
                System.out.println(e);
            }
            return view;
        }
    }

    // ---------------------------------------------------------------------------------------------
}