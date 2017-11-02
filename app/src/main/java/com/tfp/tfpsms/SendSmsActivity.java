package com.tfp.tfpsms;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
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
import java.util.Collections;
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
    private String jsonAccPhoneNumbers = "";
    private Spinner twilioNumberSpinner;

    private Menu theMenu;
    private Spinner sendToSpinner;

    private Button sendButton, setButton;
    private EditText sendToPhoneNumber;
    private EditText textMessage;

    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MessagesArrayAdapter messagesArrayAdapter;

    private boolean networkOkay = true;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sendsms);

        // Top bar with: return to MainActivity.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // -----------------------
        // Send message form objects:
        setButton = (Button) findViewById(R.id.setButton);
        setButton.setOnClickListener(this);
        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
        sendToPhoneNumber = (EditText)findViewById(R.id.sendToPhoneNumber);
        textMessage = (EditText)findViewById(R.id.textMessage);

        accountCredentials = new AccountCredentials(this);
        twilioSms = new TwSms(accountCredentials);

        // -----------------------
        // Set sendToSpinner
        // https://developer.android.com/guide/topics/ui/controls/spinner.html
        // spinnerArray[0] = "1231231234";
        // spinnerArray[1] = "1333231234";
        // spinnerArray[2] = "18182103863";
        List<String> listItems = getSendToList( accountCredentials.getSendToList() );
        String[] spinnerArray = new String[ listItems.size() ];
        listItems.toArray( spinnerArray );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.send_to_spinner_item, Arrays.asList(spinnerArray));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sendToSpinner = (Spinner)findViewById(R.id.sendToSpinner);
        sendToSpinner.setAdapter(adapter);
        int thePosition = adapter.getPosition( accountCredentials.getToPhoneNumber() );
        if (thePosition >= 0) {
            sendToSpinner.setSelection( thePosition );
        } else {
            sendToPhoneNumber.setText(accountCredentials.getToPhoneNumber());
        }

        // -----------------------
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

        theMenu = menu;

        // Adds 3-dot option menu in the action bar.
        getMenuInflater().inflate(R.menu.menu_sendsms, menu);

        // Top bar list of account phone numbers:
        jsonAccPhoneNumbers = accountCredentials.getAccPhoneNumberList();
        if (loadAccPhoneNumberSpinner(theMenu, jsonAccPhoneNumbers) > 0) {
            populateMessageList();
        }
        return true;
    }

    @Override
    public void onClick(View view) {

        if (!networkOkay) {
            messagesArrayAdapter.clear();
            String theMessage = "{ \"from\": \"Network test. \", \"body\": \"- Network connection failed. Please wait and try refresh.\", \"date_sent\": \"\" }";
            // String theMessage = "{ \"from\": \"+ messageText 01\", \"body\": \"+ messageText 02\", \"date_sent\": \"+ messageText 03\" }";
            JSONObject jsonMessage = null;
            try {
                jsonMessage = new JSONObject(theMessage);
            } catch (JSONException en) {
                en.printStackTrace();
            }
            messagesArrayAdapter.add(jsonMessage);
            return;
        }

        // Either the editText Phone Number or the spinner number.
        String theFormPhoneNumber = sendToPhoneNumber.getText().toString();
        if ( theFormPhoneNumber.trim().equalsIgnoreCase("") ) {
            theFormPhoneNumber = sendToSpinner.getSelectedItem().toString();
        }
        accountCredentials.setToPhoneNumber(theFormPhoneNumber);

        // Set the Application Twilio Phone Number.
        String twilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        accountCredentials.setTwilioPhoneNumber(twilioNumber);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Either the editText Phone Number or the spinner number.
        String theFormPhoneNumber = sendToPhoneNumber.getText().toString();
        if ( theFormPhoneNumber.trim().equalsIgnoreCase("") ) {
            theFormPhoneNumber = sendToSpinner.getSelectedItem().toString();
        }
        accountCredentials.setToPhoneNumber(theFormPhoneNumber);
        final String theFormPhoneNumberForDelete = theFormPhoneNumber;

        // Set the Application Twilio Phone Number.
        final String twilioNumber = twilioNumberSpinner.getSelectedItem().toString();
        accountCredentials.setTwilioPhoneNumber( twilioNumber );

        // -----------------------------------------------------

        // Note, this automatically adds a back-arrow to parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            // -----------------------------------------------------
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to delete this conversation between:\n" + twilioNumber + " and " + theFormPhoneNumberForDelete + "?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Snackbar.make(swipeRefreshLayout, "+ Delete confirmed, please wait.", Snackbar.LENGTH_LONG).show();
                            try {
                                // textString.setText("+ Remove messages to  : " + theFormPhoneNumber);
                                twilioSms.setSmsRequestLogs(twilioNumber, theFormPhoneNumberForDelete);
                                getMessagesToDelete();
                                // msgString.setText( "+ Remove messages from: "+ theFormPhoneNumber);
                                twilioSms.setSmsRequestLogs(theFormPhoneNumberForDelete, twilioNumber);
                                getMessagesToDelete();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Snackbar.make(swipeRefreshLayout, "+ Delete cancelled.", Snackbar.LENGTH_LONG).show();
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            // Snackbar.make(swipeRefreshLayout, "+ After dialog.", Snackbar.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------------------------------
    private List<String> getSendToList(String jsonList) {
        List<String> listItems = new ArrayList<String>();
        // Check if there is no messages.
        String mtMessages = "\"messages\": []";
        if (jsonList.indexOf(mtMessages, 0)>0) {
            return listItems;
        }
        // Phone Numbers:
        // 0123456789012345678901234567890123456789
        // +1231231234:+1231231234:+1231231234:
        String theStart = "+";
        String theEnd = ":";
        int si = 0;
        int ei = 0;
        while (si >= 0) {
            ei = jsonList.indexOf(theEnd, si);
            if (si > 0) {
                ei = jsonList.indexOf(theEnd, si);
                listItems.add( jsonList.substring(si + 1, ei) );
            }
            si = jsonList.indexOf(theStart, ei);
        }
        return listItems;
    }

    // ---------------------------------------------------------------------------------------------
    void sendSms() throws Exception {
        networkOkay = false;
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
                networkOkay = true;
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
                Snackbar.make(swipeRefreshLayout, "- Error: failed to delete messages.", Snackbar.LENGTH_LONG).show();
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
                Snackbar.make(swipeRefreshLayout, "- Error: failed to delete message.", Snackbar.LENGTH_LONG).show();
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
    private int loadAccPhoneNumberSpinner(Menu menu, String jsonList) {
        Snackbar.make(swipeRefreshLayout, "+ Loading messages ...", Snackbar.LENGTH_LONG).show();

        int numPhoneNumbers = 0;
        final JSONObject responseJson;
        try {
            responseJson = new JSONObject(jsonList);
        } catch (JSONException e) {
            Snackbar.make(swipeRefreshLayout, "- Error: Failed to parse JSON response", Snackbar.LENGTH_LONG).show();
            return numPhoneNumbers;
        }
        // Top bar spinner list of account phone numbers.
        MenuItem item = menu.findItem(spinner);
        twilioNumberSpinner = (Spinner) item.getActionView();
        List<String> spinnerList = new ArrayList<String>();
        try {
            JSONArray jList = responseJson.getJSONArray("incoming_phone_numbers");
            for (numPhoneNumbers = 0; numPhoneNumbers < jList.length(); numPhoneNumbers++) {
                String accPhoneNumber = jList.getJSONObject(numPhoneNumbers).getString("phone_number");
                spinnerList.add( accPhoneNumber );
            }
        } catch (JSONException e) {
            Snackbar.make(swipeRefreshLayout, "- Failed to parse JSON", Snackbar.LENGTH_LONG).show();
            return numPhoneNumbers;
        }
        String[] spinnerArray = new String[ spinnerList.size() ];
        spinnerList.toArray( spinnerArray );
        Arrays.sort(spinnerArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList(spinnerArray));
        //
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        twilioNumberSpinner.setAdapter(adapter);
        twilioNumberSpinner.setSelection( adapter.getPosition(accountCredentials.getTwilioPhoneNumber()) );

        return numPhoneNumbers;
    }

    // ---------------------------------------------------------------------------------------------
    private void populateMessageList() {
        networkOkay = false;
        Snackbar.make(swipeRefreshLayout, "+ Loading messages...", Snackbar.LENGTH_LONG).show();

        // Either the field Phone Number or the spinner number.
        String theFormPhoneNumber = sendToPhoneNumber.getText().toString();
        if ( theFormPhoneNumber.trim().equalsIgnoreCase("") ) {
            theFormPhoneNumber = sendToSpinner.getSelectedItem().toString();
        }
        final String finalFormPhoneNumber = theFormPhoneNumber;
        // accountCredentials.setToPhoneNumber(theFormPhoneNumber);

        final String selectedTwilioNumber = twilioNumberSpinner.getSelectedItem().toString();

        // textString.setText("+ Messages from " + selectedTwilioNumber + " to " + formPhoneNumber);
        twilioSms.setSmsRequestLogs(selectedTwilioNumber, theFormPhoneNumber);
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
                networkOkay = true;
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
                            // System.out.println("+ stringFromTwilioNumberToFormNumber: " + stringFromTwilioNumberToFormNumber);
                            populateMessageListView(stringFromTwilioNumberToFormNumber, selectedTwilioNumber, finalFormPhoneNumber);
                        }
                    });
                } else {
                    Snackbar.make(swipeRefreshLayout, String.format("- Received %s status code", response.code()), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void populateMessageListView(final String stringFromTwilioNumberToFormNumber, final String selectedTwilioNumber, String formPhoneNumber) {

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
                if (response.code() == 200) {
                    SendSmsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messagesArrayAdapter.clear();
                            System.out.println("+ stringFromFormNumberToTwilioNumber: " + stringFromFormNumberToTwilioNumber);
                            try {
                                List<String> messageList;
                                messageList = printMessageLog("received", stringFromTwilioNumberToFormNumber);
                                messageList.addAll(printMessageLog("delivered", stringFromFormNumberToTwilioNumber));
                                Collections.sort(messageList);
                                String sortedJson = "{\"messages\": [";
                                for (int index = (messageList.size() - 1); index > 0; index--) {
                                    sortedJson = sortedJson + " {" + messageList.get(index) + "},";
                                }
                                sortedJson = sortedJson + " {" + messageList.get(0) + "}";
                                sortedJson = sortedJson + " ] }";
                                final JSONObject jsonSortedMessages = new JSONObject(sortedJson);
                                JSONArray conversationMessages = jsonSortedMessages.getJSONArray("messages");
                                int im = 0;
                                int i = 0;
                                for (i = 0; i < conversationMessages.length(); i++) {
                                    messagesArrayAdapter.insert(conversationMessages.getJSONObject(i), i);
                                    im++;
                                }
                                if ( im == 0 ) {
                                    // Not working
                                    Snackbar.make(swipeRefreshLayout, getString(R.string.NoMessages), Snackbar.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON", Snackbar.LENGTH_LONG).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    Snackbar.make(swipeRefreshLayout, String.format("- Error: received %s status code", response.code()), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private List printMessageLog(String ignoreType, String theResponse) throws Exception {
        List<String> messageList = new ArrayList<>();
        String messageString;
        JSONObject responseJson;
        JSONArray theJsonArray;
        try {
            responseJson = new JSONObject(theResponse);
            theJsonArray = responseJson.getJSONArray("messages");
        } catch (JSONException e) {
            Snackbar.make(swipeRefreshLayout, "- Error: failed to parse JSON response", Snackbar.LENGTH_LONG).show();
            messageList.add("");
            return messageList;
        }
        try {
            for (int i = 0; i < theJsonArray.length(); i++) {
                if (!theJsonArray.getJSONObject(i).getString("status").equalsIgnoreCase(ignoreType)) {
                    messageString
                            = "\"date_sort\": \"" + sortDateTime(theJsonArray.getJSONObject(i).getString("date_sent")) + "\""
                            + ", \"from\": \"" + theJsonArray.getJSONObject(i).getString("from") + "\""
                            + ", \"to\": \"" + theJsonArray.getJSONObject(i).getString("to") + "\""
                            + ", \"date_sent\": \"" + theJsonArray.getJSONObject(i).getString("date_sent") + "\""
                            + ", \"status\": \"" + theJsonArray.getJSONObject(i).getString("status") + "\""
                            + ", \"body\": \"" + unescapeJavaString(theJsonArray.getJSONObject(i).getString("body")) + "\"";
                    messageList.add(messageString);
                    // System.out.println("+ " + messageString);
                }
            }
        } catch (JSONException e) {
            System.out.println("-- Failed to parse JSON response.");
        }
        return messageList;
    }

    private String sortDateTime(String theGmtDate) {
        String theSortDateTime = "19800101:00:00:00"; // error default.
        //                                                        "26 Sep 2017 00:49:31"
        SimpleDateFormat readDateFormatter = new SimpleDateFormat("dd MMM yyyy hh:mm:ss");
        // :Tue, 26 Sep 2017 00:49:31 +0000:
        //  012345678901234567890123456789
        int numDateStart = 5;
        int numDateEnd = 25;
        if (theGmtDate.length() < numDateEnd) {
            return theSortDateTime;                     // return the error default value.
        }
        Date gmtDate;
        try {
            gmtDate = readDateFormatter.parse(theGmtDate.substring(numDateStart, numDateEnd));
        } catch (ParseException ex) {
            return theSortDateTime;                     // return the error default value.
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(gmtDate);
        SimpleDateFormat writeDateformatter = new SimpleDateFormat("yyyyMMdd:HH:mm:ss");
        return writeDateformatter.format(cal.getTime());
    }

    public String unescapeJavaString(String st) {
        String theString = st.replace("\n", "\\n").replace("\r", "\\r");
        return theString;
    }

    private class MessagesArrayAdapter extends ArrayAdapter<JSONObject> {
        public MessagesArrayAdapter(@NonNull Context context, @LayoutRes int resource) {
            super(context, resource);
        }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            View view = getLayoutInflater().inflate(R.layout.list_item_message, parent, false);
            TextView row01 = (TextView) view.findViewById(R.id.row01);
            TextView row02 =(TextView) view.findViewById(R.id.row02);
            TextView row03 =(TextView) view.findViewById(R.id.row03);

            JSONObject messageJson = getItem(position);
            try {
                String messageTwilioNumber = twilioNumberSpinner.getSelectedItem().toString();
                String thePhoneNumber = messageJson.getString("from");
                if (messageTwilioNumber.equalsIgnoreCase(thePhoneNumber)) {
                    view.setBackgroundResource( R.color.listBackgroundIncoming );
                    row01.setGravity(Gravity.RIGHT);
                    row02.setGravity(Gravity.RIGHT);
                    row03.setGravity(Gravity.RIGHT);
                }
                row01.setText("From: " + messageJson.getString("from") + " To: " + messageJson.getString("to"));
                String theStatus = messageJson.getString("status");
                if (!theStatus.equalsIgnoreCase("delivered") && !theStatus.equalsIgnoreCase("received")) {
                    row01.setText( row01.getText() + ", status: " + messageJson.getString("status"));
                }
                row02.setText(messageJson.getString("body"));
                row03.setText(twilioSms.localDateTime( messageJson.getString("date_sent")));
            } catch (JSONException e) {
                Log.e("SendSmsActivity", "- Error: failed to parse JSON", e);
                System.out.println(e);
            }
            return view;
        }
    }

    // ---------------------------------------------------------------------------------------------
}