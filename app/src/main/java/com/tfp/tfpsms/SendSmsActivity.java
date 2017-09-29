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
import android.util.Log;
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
    private TwSms URL_REQUEST;
    private TwSms twilioSms;

    private String twilioNumber;
    private String phoneNumber;
    Properties properties = new Properties();


    private Button sendButton, setButton;
    private EditText sendToPhoneNumber;
    private EditText textMessage;

    private SwipeRefreshLayout swipeRefreshLayout;
    private MessagesArrayAdapter messagesArrayAdapter;
    private ListView listView;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sendsms);

        // Send message form objects:
        setButton = (Button) findViewById(R.id.setButton);
        sendButton = (Button) findViewById(R.id.sendButton);
        sendToPhoneNumber = (EditText)findViewById(R.id.sendToPhoneNumber);
        textMessage = (EditText)findViewById(R.id.textMessage);

        accountCredentials = new AccountCredentials(this);
        URL_REQUEST = new TwSms(accountCredentials);
        twilioSms = new TwSms(accountCredentials);

        try {
            InputStream open = getAssets().open("twilio.properties");
            // Properties properties = new Properties();
            properties.load(open);
            twilioNumber = properties.getProperty("twilio.phone.number");
            phoneNumber = properties.getProperty("phone.number");
        } catch (IOException e) {
            Log.e("DebugActivity", "Failed to open twilio.properties");
            throw new RuntimeException("Failed to open twilio.properties");
        }

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
        sendButton.setOnClickListener(this);
        setButton.setOnClickListener(this);
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sendButton:
                try {
                    String theSendToPhoneNumber = sendToPhoneNumber.getText().toString();
                    String theTextMessage = textMessage.getText().toString();
                    // textString.setText("+ Send message to: " + theSendToPhoneNumber);
                    URL_REQUEST.setSmsSend(theSendToPhoneNumber, twilioNumber, theTextMessage);
                    sendSms();
                    populateMessageList();
                    // Intent intent = new Intent(this, MainActivity.class);
                    // startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.setButton:
                try {
                    phoneNumber = sendToPhoneNumber.getText().toString();
                    properties.setProperty("phone.number", phoneNumber);
                    populateMessageList();
                    // textString.setText("+ Phone Number Exchange List");
                    // textScrollBox.setText("+ List not available, yet.");
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

        twilioSms.setSmsRequestTo(phoneNumber, twilioNumber);

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
                                for (int i = 0; i < messages.length(); i++) {
                                    messagesArrayAdapter.insert(messages.getJSONObject(i), i);
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
                hostnameView.setText(messageJson.getString("body"));
                portsView.setText(messageJson.getString("date_sent"));
            } catch (JSONException e) {
                Log.e("MainActivity", "Failed to parse JSON", e);
                System.out.println(e);
            }

            return view;
        }
    }

    // ---------------------------------------------------------------------------------------------
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


}
