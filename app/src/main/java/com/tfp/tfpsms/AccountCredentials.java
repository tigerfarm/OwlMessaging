package com.tfp.tfpsms;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Set;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AccountCredentials implements Interceptor {

    //                                 12345678901234567890123 (max 23 char)
    private static final String TAG = "AccountCredentials";

    private Context mContext;
    private String accountSid = "";
    private String authToken;
    private String credentials;
    //
    private String twilioPhoneNumber;
    private String toPhoneNumber;
    private int localTimeOffset;
    //
    // Twilio Authy Application entry API Key:
    private String appApiKey = "Vye28fVRU1Bo85BISTENwp1klE5a7tir";  // Owl Publishing
    private SharedPreferences sharedPreferences;

    public AccountCredentials(Context context) {
        this.mContext = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //
        this.accountSid = sharedPreferences.getString("account_sid", "");
        this.authToken = sharedPreferences.getString("auth_token", "");
        this.credentials = Credentials.basic(accountSid, authToken);
        //
        this.twilioPhoneNumber = sharedPreferences.getString("twilio_phone_number", "");
        //
        this.toPhoneNumber = sharedPreferences.getString("to_phone_number", "");
        this.localTimeOffset = Integer.parseInt( sharedPreferences.getString("local_time_offset", "-7") );    // Default: -7 is San Francisco time
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }

    public boolean existAccountSid() {
        if (accountSid.isEmpty()) {
            return false;
        }
        return true;
    }

    public String getAccountSid() {
        return accountSid;
    }

    public void setTwilioPhoneNumber(String aParam) {
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        prefEditor.putString("twilio_phone_number", aParam);
        prefEditor.apply();
        prefEditor.commit();
    }
    public String getTwilioPhoneNumber() {
        return twilioPhoneNumber;
    }

    public void setToPhoneNumber(String aParam) {
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        prefEditor.putString("to_phone_number", aParam);
        prefEditor.apply();
        prefEditor.commit();
    }
    public String getToPhoneNumber() {
        return toPhoneNumber;
    }

    // Needs to be set, in the Settings panel. Or calculate difference from GMT to local time.
    public void setLocalTimeOffset(String aParam) {
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        prefEditor.putString("local_time_offset", aParam);
        prefEditor.apply();
        prefEditor.commit();
    }
    public int getLocalTimeOffset() {
        return Integer.parseInt( sharedPreferences.getString("local_time_offset", "-7") );
    }

    // ---------------------------------------------------------------------------------------------
    // NOT used in the SMS version.
    // ---------------------------------------------------------------------------------------------

    public String getAppApiKey() {
        return appApiKey;
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

}
