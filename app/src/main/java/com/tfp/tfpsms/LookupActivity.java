package com.tfp.tfpsms;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LookupActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private TwSms URL_REQUEST;

    private Button theButton;
    private EditText sendToPhoneNumber;
    private TextView rowOne, rowTwo, rowThree;
    private TextView showResults;

    private SwipeRefreshLayout swipeRefreshLayout;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lookup);

        // To return to MainActivity
        setupActionBar();

        // Send message form objects:
        theButton = (Button) findViewById(R.id.theButton);
        sendToPhoneNumber = (EditText)findViewById(R.id.sendToPhoneNumber);
        showResults = (TextView)findViewById(R.id.showResults);
        rowOne = (TextView)findViewById(R.id.row01);
        rowTwo = (TextView)findViewById(R.id.row02);
        rowThree = (TextView)findViewById(R.id.row03);

        accountCredentials = new AccountCredentials(this);
        URL_REQUEST = new TwSms(accountCredentials);
        sendToPhoneNumber.setText(accountCredentials.getToPhoneNumber());

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        theButton.setOnClickListener(this);
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
            case R.id.theButton:
                try {
                    // showResults.setText("+ onClick:theButton");
                    displayResults();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    private void displayResults() {

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(accountCredentials)
                .build();
        Request request = new Request.Builder()
                .url(URL_REQUEST.getLookup(sendToPhoneNumber.getText().toString()))
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
                    LookupActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println(responseContent);
                            try {
                                rowOne.setText(
                                        responseJson.getString("country_code") + " : " +
                                                responseJson.getString("national_format")
                                );
                                rowTwo.setText("Carrier: " + responseJson.getJSONObject("carrier").getString("name"));
                                rowThree.setText("Type of line: " + responseJson.getJSONObject("carrier").getString("type"));
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

}
