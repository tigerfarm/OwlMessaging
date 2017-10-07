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
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

    private ListView listView;
    private MessagesArrayAdapter messagesArrayAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner twilioNumberSpinner;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        accountCredentials = new AccountCredentials(this);
        twilioSms = new TwSms(accountCredentials);

        messagesArrayAdapter = new MessagesArrayAdapter(this, android.R.layout.simple_list_item_1);
        listView = (ListView) findViewById(R.id.list_view);
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Top bar list of account phone numbers:
        loadSpinnerAccPhoneNumbers(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Set the Appication Twilio Phone Number before going to another panel.
        accountCredentials.setTwilioPhoneNumber( twilioNumberSpinner.getSelectedItem().toString() );

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_debug) {
            Intent intent = new Intent(this, DebugActivity.class);
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
    private void populateMessageList() {

        // Set the Application Twilio Phone Number.
        String selectedTwilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        accountCredentials.setTwilioPhoneNumber(selectedTwilioNumber);

        // Get the recent message logs for the Twilio Phone Number.
        twilioSms.setSmsRequestLogsTo(selectedTwilioNumber);

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
                labelView.setText("+ From: " + messageJson.getString("from") + " to " + messageJson.getString("to"));
                hostnameView.setText(messageJson.getString("body"));
                portsView.setText(twilioSms.localDateTime(messageJson.getString("date_sent")));
            } catch (JSONException e) {
                Log.e("MainActivity", "Failed to parse JSON", e);
                System.out.println(e);
            }

            return view;
        }
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
                        String theResponse = accPhoneNumberPrintList(menu, jsonResponse);
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
            Snackbar.make(swipeRefreshLayout, "Failed to parse JSON", Snackbar.LENGTH_LONG).show();
            return aPrintList;
        }
        String[] spinnerArray = new String[ spinnerList.size() ];
        spinnerList.toArray( spinnerArray );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList(spinnerArray));
        //
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        twilioNumberSpinner.setAdapter(adapter);
        String twilioPhoneNumber = accountCredentials.getTwilioPhoneNumber();
        if (twilioPhoneNumber.startsWith("+")) {
            twilioNumberSpinner.setSelection( adapter.getPosition(twilioPhoneNumber) );
        } else {
            // Temporary case while switching over from data entered phone number in Settings.
            twilioNumberSpinner.setSelection( adapter.getPosition("+" + twilioPhoneNumber) );
            accountCredentials.setTwilioPhoneNumber(twilioPhoneNumber);
        }

        populateMessageList();

        return aPrintList;
    }

   // ---------------------------------------------------------------------------------------------
}
