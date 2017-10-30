package com.tfp.tfpsms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.tfp.tfpsms.R.id.spinner;

public class MainActivity extends AppCompatActivity {

    private AccountCredentials accountCredentials;
    private TwSms twilioSms;
    private String jsonAccPhoneNumbers = "";

    private ListView listView;
    private MessagesArrayAdapter messagesArrayAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Menu theMenu;
    private Spinner twilioNumberSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // startActivity(new Intent(this, SettingsActivity.class));
        accountCredentials = new AccountCredentials(this);
        if ( !accountCredentials.existAccountSid() ) {
            // if the Twilio account info hasn't been entered, go to Settings.
            // Snackbar.make(swipeRefreshLayout, "+ Enter your correct Twilio account information into Settings.", Snackbar.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
        }
        twilioSms = new TwSms(accountCredentials);

        // Snackbar.make(swipeRefreshLayout, "+ getAccountSid: " + accountCredentials.getAccountSid(), Snackbar.LENGTH_LONG).show();
        // Snackbar.make(swipeRefreshLayout, "+ ." + accountCredentials.getAccountSid(), Snackbar.LENGTH_LONG).show();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Use when working on specific panel.
        // startActivity(new Intent(this, SettingsActivity.class));

        messagesArrayAdapter = new MessagesArrayAdapter(this, android.R.layout.simple_list_item_1);
        listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(messagesArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                // final String item = (String) parent.getItemAtPosition(position);
                int itemPosition = position;
                String itemValue = (String) listView.getItemAtPosition(position);
                Snackbar.make(swipeRefreshLayout, "+ Position: " + itemPosition + " : " +itemValue, Snackbar.LENGTH_LONG).show();
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

        theMenu = menu;

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

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

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

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
        if (id == R.id.menu_refresh) {
            loadSpinnerAccPhoneNumbers();
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
            return true;
        }
        if (selectedTwilioNumber.isEmpty()) {
            Snackbar.make(swipeRefreshLayout, "- No account phone numbers.", Snackbar.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sendsms) {
            Intent intent = new Intent(this, SendSmsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_lookup) {
            Intent intent = new Intent(this, LookupActivity.class);
            startActivity(intent);
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
    private void loadSpinnerAccPhoneNumbers() {
        Snackbar.make(swipeRefreshLayout, "+ Loading account phone numbers...", Snackbar.LENGTH_LONG).show();
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
        Snackbar.make(swipeRefreshLayout, "+ Loading messages...", Snackbar.LENGTH_LONG).show();

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
                                List<String> senderList = new ArrayList<>();    // To store a list of sender phone numbers
                                for (int i = 0; i < messages.length(); i++) {
                                    if (messages.getJSONObject(i).getString("status").equalsIgnoreCase("received")) {
                                        messagesArrayAdapter.insert(messages.getJSONObject(i), im);
                                        im++;
                                        senderList.add(messages.getJSONObject(i).getString("from"));
                                    }
                                }
                                if ( im == 0 ) {
                                    Snackbar.make(swipeRefreshLayout, getString(R.string.NoMessages), Snackbar.LENGTH_LONG).show();
                                }
                                // --------------------------
                                // Sort, minimize and store the list, for use in the Send SMS panel.
                                Collections.sort(senderList);
                                String theSenderList = ":";
                                String anItem = "";
                                for (int index = 0; index < senderList.size(); index++) {
                                    String thisItem = senderList.get(index);
                                    if (!anItem.equalsIgnoreCase(thisItem)) {
                                        theSenderList = theSenderList + thisItem + ":";
                                    }
                                    anItem = thisItem;
                                }
                                // System.out.println("+ theSenderList: " + theSenderList);
                                accountCredentials.setSendToList(theSenderList);
                                // --------------------------
                            } catch (JSONException e) {
                                Snackbar.make(swipeRefreshLayout, "-- Error: failed to parse JSON response: populateMessageList", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    // Snackbar.make(swipeRefreshLayout, String.format("- Received %s status code", response.code()), Snackbar.LENGTH_LONG).show();
                    Snackbar.make(swipeRefreshLayout, "+ Logging into your Twilio account failed(" + response.code() + ").\n++ Go to Settings.", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private class MessagesArrayAdapter extends ArrayAdapter<JSONObject> {
        public MessagesArrayAdapter(@NonNull Context context, @LayoutRes int resource) {
            super(context, resource);
        }
        /* Example of how to load a messages manually:
        messagesArrayAdapter.clear();
        String theMessage = "{ \"from\": \"+ messageText 01\", \"body\": \"+ messageText 02\", \"date_sent\": \"+ messageText 03\" }";
        JSONObject jsonMessage = null;
        try {
            jsonMessage = new JSONObject(theMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        messagesArrayAdapter.add(jsonMessage);
        */

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = getLayoutInflater().inflate(R.layout.list_item_message, parent, false);

            TextView row01 = (TextView) view.findViewById(R.id.row01);
            TextView row02 = (TextView) view.findViewById(R.id.row02);
            TextView row03 = (TextView) view.findViewById(R.id.row03);

            JSONObject messageJson = getItem(position);
            try {
                row01.setText(
                        "+ From: " + messageJson.getString("from")
                        // + " to " + messageJson.getString("to")
                );
                row02.setText(messageJson.getString("body"));
                row03.setText(twilioSms.localDateTime(messageJson.getString("date_sent")));
            } catch (JSONException e) {
                Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON response: MessagesArrayAdapter", Snackbar.LENGTH_LONG).show();
                System.out.println(e);
            }
            return view;
        }
    }

}
