package com.tfp.tfpsms;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AccSecActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private TwSms twilioAccSec;

    private Button theButton;
    private EditText editMessage;
    private TextView textString, msgString;
    private TextView textScrollBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accsec);
        //
        // Top bar with: return to MainActivity.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        accountCredentials = new AccountCredentials(this);
        twilioAccSec = new TwSms(accountCredentials);
        //
        theButton = (Button) findViewById(R.id.theButton);
        theButton.setOnClickListener(this);
        //
        editMessage = (EditText)findViewById(R.id.editMessage);
        //
        textString = (TextView) findViewById(R.id.textString);
        msgString = (TextView) findViewById(R.id.msgString);
        textScrollBox = (TextView) findViewById(R.id.scrollBox);
        textScrollBox.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.theButton:
                String themessage = editMessage.getText().toString();

                try {
                    twilioAccSec.setPhoneVerificationSend("sms", "1", "2223331234");
                    textString.setText("+ POST Phone Verification: "+twilioAccSec.getRequestUrl());
                    postRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                /*
                textString.setText("+ textString, encrypted :" + encString.trim() +":");
                msgString.setText("+ msgString, decrypted :" + decString +":");
                */

                textScrollBox.setText("+ textScrollBox");
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    void postRequest() throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(twilioAccSec.getRequestUrl())
                .post(twilioAccSec.getPostParams())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                AccSecActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textScrollBox.setText(myResponse);
                    }
                });
            }
        });
    }

}
