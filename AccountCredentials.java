package com.tfp.tfpsms;

import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AccountCredentials implements Interceptor {

    //                                 12345678901234567890123 (max 23 char)
    private static final String TAG = "ERR:AccountCredentials";
    // ---------------------------------------------------------------------------------------------
    // Account SID and Token
    private final String AUTH_TOKEN = "your_account_token";
    private final String AccountSid = "your_account_SID";
    public String getAccountSid() {
        return AccountSid;
    }

    // Twilio account phone number for sending and receiving messages:
    private final String TwilioPhoneNumber = "+12223331234";
    public String getTwilioPhoneNumber() {
        return TwilioPhoneNumber;
    }

    // ---------------------------------------------------------------------------------------------
    private String credentials;
    public AccountCredentials() {
        this.credentials = Credentials.basic(AccountSid, AUTH_TOKEN);
    }

    public AccountCredentials(String user, String password) {
        this.credentials = Credentials.basic(user, password);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }

    // ---------------------------------------------------------------------------------------------
    public static String readFile(final String theReadFilename) {
        // Need Storage access.
        // Write permission implies read permission, so you don't need both.
        // <manifest ...> ... <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/> ... </manifest>
        // <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
        //
        // FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
        //
        // Problem with the following is that "context" is not defined.
        // Context context;
        // context.getApplicationInfo().dataDir
        // context.getFilesDir();
        // File path = context.getExternalFilesDir(null);  // external storage (SD card)
        // File path = context.getFilesDir();   // internal storage
        // File file = new File(path, "my-file-name.txt");
        // File file = new File(getExternalFilesDir(null), "DemoFile.jpg");

        String theFileText = "";
        try {
            File readFile = new File(theReadFilename);
            if ( !readFile.exists() ) {
                Log.d(TAG, "--- ERROR, theReadFilename not exist.");
                return theFileText;
            }
            DataInputStream pin = new DataInputStream(new FileInputStream(readFile));
            String theLine = pin.readLine();
            int lineNum=0;
            while (theLine != null) {
                theFileText = theFileText + pin.readLine();
            }
            pin.close();
        } catch (IOException e) {
            // Logger.logError(TAG, e);
            Log.d(TAG, "writeStringAsFile");
        }
        return theFileText;
    }

    // ---------------------------------------------------------------------------------------------
    public static void writeFile(final String theWriteFilename, final String fileText) {
        try {
            File writeFile = new File(theWriteFilename);
            FileOutputStream fout = new FileOutputStream(writeFile);
            PrintStream pout = new PrintStream(fout);
            pout.println(fileText);
            pout.close();
        } catch (IOException e) {
            Log.d(TAG, "writeStringAsFile");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // NOT used in the SMS version.
    // ---------------------------------------------------------------------------------------------
    // Twilio Authy Application entry API Key:
    private final String AppApiKey = "your_app_key";
    public String getAppApiKey() {
        return AppApiKey;
    }

}
