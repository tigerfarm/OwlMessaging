package com.tfp.tfpsms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SendSmsActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private TwSms twilioSms;
    private String twilioNumber;

    private Button sendButton, setButton;
    private EditText sendToPhoneNumber;
    private EditText textMessage;

    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MessagesArrayAdapter messagesArrayAdapter;

    // For testing
    private TextView textString;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sendsms);

        // To return to MainActivity
        setupActionBar();

        // Send message form objects:
        setButton = (Button) findViewById(R.id.setButton);
        setButton.setOnClickListener(this);
        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
        sendToPhoneNumber = (EditText)findViewById(R.id.sendToPhoneNumber);
        textMessage = (EditText)findViewById(R.id.textMessage);
        textString = (TextView) findViewById(R.id.textString);

        accountCredentials = new AccountCredentials(this);
        twilioSms = new TwSms(accountCredentials);
        twilioNumber = accountCredentials.getTwilioPhoneNumber();
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

        populateMessageList();
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
        String toPhoneNumber = sendToPhoneNumber.getText().toString();
        switch (view.getId()) {
            case R.id.sendButton:
                try {
                    accountCredentials.setToPhoneNumber(toPhoneNumber);
                    // properties.setProperty("phone.number", toPhoneNumber);
                    //
                    String theTextMessage = textMessage.getText().toString();
                    twilioSms.setSmsSend(toPhoneNumber, twilioNumber, theTextMessage);
                    sendSms();
                    // wait(1000);
                    populateMessageList();
                    // Intent intent = new Intent(this, MainActivity.class);
                    // startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.setButton:
                try {
                    accountCredentials.setToPhoneNumber(toPhoneNumber);
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
                call.cancel();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                SendSmsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // textScrollBox.setText(responseStatus(myResponse));
                        populateMessageList();
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    private void populateMessageList() {

        // twilioSms.setSmsRequestOnlyTo(twilioNumber);
        twilioSms.setSmsRequestLogs(twilioNumber, sendToPhoneNumber.getText().toString());

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(twilioSms.getRequestUrl())
                .build();
        // textString.setText("+ getRequestUrl: " + twilioSms.getRequestUrl());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                Snackbar.make(swipeRefreshLayout, "Failed to retrieve messages", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseContent = response.body().string();
                final JSONObject responseJson;
                try {
                    responseJson = new JSONObject(responseContent);
                } catch (JSONException e) {
                    Snackbar.make(swipeRefreshLayout, "Failed to parse JSON response", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (response.code() == 200) {
                    SendSmsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println(responseContent);
                            try {
                                JSONArray messages = responseJson.getJSONArray("messages");
                                messagesArrayAdapter.clear();
                                textString.setText("");
                                int im = 0;
                                for (int i = 0; i < messages.length(); i++) {
                                    // messagesArrayAdapter.insert(messages.getJSONObject(i), i);
                                    if ( !messages.getJSONObject(i).getString("status").equalsIgnoreCase("received")) {
                                        // Not if status = received, to remove the case of sending to one of your other account phone numbers.
                                        messagesArrayAdapter.insert(messages.getJSONObject(i), im);
                                        im++;
                                    }
                                }
                                if ( im == 0 ) {
                                    textString.setText("+ No messages.");
                                }
                            } catch (JSONException e) {
                                Snackbar.make(swipeRefreshLayout, "Failed to parse JSON", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Snackbar.make(swipeRefreshLayout, String.format("Received %s status code", response.code()), Snackbar.LENGTH_LONG).show();
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
                labelView.setText(messageJson.getString("to"));
                String theStatus = messageJson.getString("status");
                if (!theStatus.equalsIgnoreCase("delivered")) {
                    labelView.setText(messageJson.getString("to") + " status: " + messageJson.getString("status"));
                }
                hostnameView.setText(messageJson.getString("body"));
                portsView.setText(twilioSms.localDateTime( messageJson.getString("date_sent")));
            } catch (JSONException e) {
                Log.e("MainActivity", "Failed to parse JSON", e);
                System.out.println(e);
            }

            return view;
        }
    }

}