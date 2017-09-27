package com.tfp.tfpsms;

import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AccountCredentials implements Interceptor {

    //                                 12345678901234567890123 (max 23 char)
    private static final String TAG = "AccountCredentials";

    private Context mContext;
    private String accountSid;
    private String authToken;
    private String phoneNumber;
    private String credentials;
    // Twilio Authy Application entry API Key:
    private String appApiKey;

    public AccountCredentials(Context context) {
        this.mContext = context;
        try {
            InputStream open = context.getAssets().open("twilio.properties");
            Properties properties = new Properties();
            properties.load(open);

            accountSid = properties.getProperty("twilio.account.sid");
            authToken = properties.getProperty("twilio.auth.token");
            phoneNumber = properties.getProperty("twilio.phone.number");
            appApiKey = properties.getProperty("authy.app.api.key");

            this.credentials = Credentials.basic(accountSid, authToken);
        } catch (IOException e) {
            Log.e(TAG, "Failed to open twilio.properties");
            throw new RuntimeException("Failed to open twilio.properties");
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }

    public String getAccountSid() {
        return accountSid;
    }

    public String getPhoneNumber() {
        return phoneNumber;
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

    public String getAppApiKey() {
        return appApiKey;
    }

}
