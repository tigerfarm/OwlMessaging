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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private TwSms twilioSms;
    private Spinner gmtOffsetSpinner;

    private Button updateButton;
    private EditText accountSid, accountToken;
    private TextView showResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // To return to MainActivity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // -----------------------
        // Send message form objects:
        updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(this);
        accountSid = (EditText)findViewById(R.id.accountSid);
        accountToken = (EditText)findViewById(R.id.accountToken);
        showResults = (TextView)findViewById(R.id.showResults);

        // showResults.setText("+ Settings started.");

        accountCredentials = new AccountCredentials(this);
        accountSid.setText(accountCredentials.getAccountSid());
        accountToken.setText(accountCredentials.getAccountTokenDecrypted());
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Adds 3-dot option menu in the action bar.
        getMenuInflater().inflate(R.menu.menu_sendsms, menu);

        // loadSpinnerAccPhoneNumbers();

        return true;
    }

    @Override
    public void onClick(View view) {

        // String accountSid = accountSid.getText();

        switch (view.getId()) {
            case R.id.updateButton:
                try {
                    accountCredentials.setAccountSid( accountSid.getText().toString() );
                    accountCredentials.setAccountTokenEncrypted( accountToken.getText().toString() );
                    showResults.setText("+ Settings updated." );
                    /*
                    showResults.setText("+ Get the data: "
                            + "\n+ getAccountSid: " + accountCredentials.getAccountSid()
                            + "\n+ getAccountToken: " + accountCredentials.getAccountToken()
                            + "\n+ getAccountTokenDecrypted: " + accountCredentials.getAccountTokenDecrypted()
                    );
                    */
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    }

}