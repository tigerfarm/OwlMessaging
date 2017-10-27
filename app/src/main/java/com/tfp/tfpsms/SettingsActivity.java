package com.tfp.tfpsms;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private Spinner spinnerGmtOffset;
    private String[] spinnerValuesGmtOffset;

    private Button updateButton;
    private EditText accountSid, accountToken;
    private TextView showResults;

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // To return to MainActivity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);

        // -----------------------
        // Send message form objects:
        updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(this);
        accountSid = (EditText)findViewById(R.id.accountSid);
        accountToken = (EditText)findViewById(R.id.accountToken);
        showResults = (TextView)findViewById(R.id.showResults);

        // showResults.setText("+ Settings started.");
        // Snackbar.make(swipeRefreshLayout, "+ Settings started.", Snackbar.LENGTH_LONG).show();

        accountCredentials = new AccountCredentials(this);
        accountSid.setText(accountCredentials.getAccountSid());
        accountToken.setText(accountCredentials.getAccountToken());

        // -----------------------
        // Set spinnerGmtOffset

        spinnerValuesGmtOffset = getResources().getStringArray(R.array.gmt_offset_values); // Arrary Values
        ArrayAdapter<String> adapterValues = new ArrayAdapter<>(this, R.layout.gmt_offset_spinner_item, Arrays.asList(spinnerValuesGmtOffset));

        // For testing:
        // String[] spinnerLabels = new String[ 3 ];
        // spinnerLabels[0] = "1";
        // spinnerLabels[1] = "2";
        // spinnerLabels[2] = "3";
        //
        String[] spinnerLabels = getResources().getStringArray(R.array.gmt_offset_labels);
        //
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.gmt_offset_spinner_item, Arrays.asList(spinnerLabels));
        //
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGmtOffset = (Spinner)findViewById(R.id.spinnerGmtOffset);
        spinnerGmtOffset.setAdapter(adapter);

        // accountCredentials.getLocalTimeOffsetString();   // "-2" or "-7: PT: California"
        String theLabel = accountCredentials.getLocalTimeOffsetString();
        int is = theLabel.indexOf(":");
        if (is > 0) {
            theLabel = theLabel.substring(0, is+1);
        }
        int thePosition = adapterValues.getPosition( theLabel );
        // showResults.setText("+ theLabel :" + theLabel + ": " + thePosition );
        if (thePosition >= 0) {
            spinnerGmtOffset.setSelection( thePosition );
        } else {
            spinnerGmtOffset.setSelection( 0 ); // default initialization
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Adds 3-dot option menu in the action bar.
        // getMenuInflater().inflate(R.menu.menu_sendsms, menu);

        // loadSpinnerAccPhoneNumbers();

        return true;
    }

    @Override
    public void onClick(View view) {

        // String accountSid = accountSid.getText();
        Snackbar.make(swipeRefreshLayout, "+ Update clicked.", Snackbar.LENGTH_LONG).show();

        switch (view.getId()) {
            case R.id.updateButton:
                try {
                    accountCredentials.setAccountSid( accountSid.getText().toString() );
                    accountCredentials.setAccountToken( accountToken.getText().toString() );

                    // int thePosition = spinnerGmtOffset.getSelectedItemPosition();
                    String theValue = spinnerValuesGmtOffset[spinnerGmtOffset.getSelectedItemPosition()];
                    accountCredentials.setLocalTimeOffset(theValue);

                    // showResults.setText("+ spinnerGmtOffset : " + theValue );
                    Snackbar.make(swipeRefreshLayout, "+ Settings updated.", Snackbar.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    }

}