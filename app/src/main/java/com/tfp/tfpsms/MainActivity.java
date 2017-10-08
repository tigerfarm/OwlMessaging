package com.tfp.tfpsms;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.tfp.tfpsms.R.id.spinner;
import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private AccountCredentials accountCredentials;
    private TwSms twilioSms;

    private ListView listView;
    private MessagesArrayAdapter messagesArrayAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner twilioNumberSpinner;

    // Display process and error messages
    private TextView textString, msgString, textScrollBox;
    private TextView listString;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accountCredentials = new AccountCredentials(this);
        if ( !accountCredentials.existAccountSid() ) {
            // if the Twilio account info hasn't been entered, go to Settings.
            startActivity(new Intent(this, SettingsActivity.class));
        }
        twilioSms = new TwSms(accountCredentials);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textString = (TextView) findViewById(R.id.textString);
        msgString = (TextView) findViewById(R.id.msgString);
        textScrollBox = (TextView) findViewById(R.id.textScrollBox);
        listString = (TextView) findViewById(R.id.listString);

        listString.setText("+ Initiate list.");
        messagesArrayAdapter = new MessagesArrayAdapter(this, android.R.layout.simple_list_item_1);
        listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(messagesArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                int itemPosition = position;
                String itemValue = (String) listView.getItemAtPosition(position);
                listString.setText("+ Position: "+itemPosition+" : " +itemValue);
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

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Top bar list of account phone numbers:
        loadSpinnerAccPhoneNumbers(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Set the Appication Twilio Phone Number before going to another panel.
        String selectedTwilioNumber = "";
        try {
            selectedTwilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        }
        catch (NullPointerException e) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (selectedTwilioNumber.isEmpty()) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        // Set the Twilio phone number for the next panel to use.
        String twilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        accountCredentials.setTwilioPhoneNumber( twilioNumber );

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_dev) {
            Intent intent = new Intent(this, DevActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_sendsms) {
            Intent intent = new Intent(this, SendSmsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_lookup) {
            Intent intent = new Intent(this, LookupActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_accsec) {
            Intent intent = new Intent(this, AccSecActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_refresh) {
            populateMessageList();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        accPhoneNumberPrintList(menu, jsonResponse);
                    }
                });
            }
        });
    }

    private void accPhoneNumberPrintList(Menu menu, String jsonList) {
        final JSONObject responseJson;
        try {
            responseJson = new JSONObject(jsonList);
        } catch (JSONException e) {
            Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON response: accPhoneNumberPrintList", Snackbar.LENGTH_LONG).show();
            return;
        }

        // Top bar spinner list of account phone numbers.
        MenuItem item = menu.findItem(spinner);
        twilioNumberSpinner = (Spinner) item.getActionView();
        List<String> spinnerList = new ArrayList<String>();
        int i = 0;
        try {
            JSONArray jList = responseJson.getJSONArray("incoming_phone_numbers");
            for (i = 0; i < jList.length(); i++) {
                String accPhoneNumber = jList.getJSONObject(i).getString("phone_number");
                spinnerList.add( accPhoneNumber );
            }
        } catch (JSONException e) {
            textString.setText("++ Check and set: Twilio Account Settings.");
            Snackbar.make(swipeRefreshLayout,
                    "-- Error: failed to parse JSON response: accPhoneNumberPrintList",
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        if (i==0) {
            textString.setText("--- Your Twilio Account does not have an SMS capiable phone number.");
            msgString.setText("++ Go to your Twilio and select a SMS capiable phone number.");
            return;
        }
        String[] spinnerArray = new String[ spinnerList.size() ];
        spinnerList.toArray( spinnerArray );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList(spinnerArray));
        //
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        twilioNumberSpinner.setAdapter(adapter);
        //
        // Set the application Twilio account phone number.
        String twilioPhoneNumber = accountCredentials.getTwilioPhoneNumber();
        int positionTwilioPhoneNumber = adapter.getPosition(twilioPhoneNumber);
        if ( positionTwilioPhoneNumber < 0) {
            positionTwilioPhoneNumber = 0;
        }
        twilioNumberSpinner.setSelection( positionTwilioPhoneNumber );
        accountCredentials.setTwilioPhoneNumber( twilioNumberSpinner.getSelectedItem().toString() );

        populateMessageList();
    }

    // ---------------------------------------------------------------------------------------------
    private void populateMessageList() {

        textString.setText("");
        msgString.setText("");
        textScrollBox.setText("");

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
                                messagesArrayAdapter.clear();
                                int im = 0;
                                for (int i = 0; i < messages.length(); i++) {
                                    if (messages.getJSONObject(i).getString("status").equalsIgnoreCase("received")) {
                                        messagesArrayAdapter.insert(messages.getJSONObject(i), im);
                                        im++;
                                    }
                                }
                                if ( im == 0 ) {
                                    textString.setText("+ No messages.");
                                }
                            } catch (JSONException e) {
                                Snackbar.make(swipeRefreshLayout, "-- Error: failed to parse JSON response: populateMessageList", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Snackbar.make(swipeRefreshLayout, String.format("Received %s status code", response.code()), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    // Variation on this:
    // http://theopentutorials.com/tutorials/android/listview/android-custom-listview-with-image-and-text-using-baseadapter/
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
                labelView.setText(
                        "+ From: " + messageJson.getString("from")
                        // + " to " + messageJson.getString("to")
                );
                hostnameView.setText(messageJson.getString("body"));
                portsView.setText(twilioSms.localDateTime(messageJson.getString("date_sent")));
            } catch (JSONException e) {
                Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON response: MessagesArrayAdapter", Snackbar.LENGTH_LONG).show();
                System.out.println(e);
            }
            return view;
        }
    }

    // ---------------------------------------------------------------------------------------------
}
