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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Properties;
import java.util.Set;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AccountCredentials implements Interceptor {

    private EncDecString EncDec;

    //                                 12345678901234567890123 (max 23 char)
    private static final String TAG = "AccountCredentials";

    private Context mContext;
    //
    private String accountSid = "";
    private String authToken;
    private String credentials;
    //
    private String twilioPhoneNumber;
    private String toPhoneNumber;
    private int localTimeOffset;
    //
    // ---------------------------------------------------------------------------------------------
    private SharedPreferences sharedPreferences;

    public AccountCredentials(Context context) {
        this.mContext = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        EncDec = new EncDecString();
        getAccountSid();
        this.authToken = getAccountToken();
        this.credentials = Credentials.basic(accountSid, authToken);
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }

    private String getDecrypted(String attributeName) {
        String encValue = sharedPreferences.getString(attributeName, "");
        String decyrptedValue = "";
        try {
            decyrptedValue = EncDec.decryptBase64String(encValue);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return decyrptedValue;
    }
    private void setEncrypted(String attributeName, String theValue) {
        this.authToken = theValue;
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        try {
            prefEditor.putString(attributeName, EncDec.encryptBase64String(theValue));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        prefEditor.apply();
        prefEditor.commit();
        return;
    }
    // ---------------------------------------------------------------------------------------------
    // Encrypt the account token when stored on the phone.
    // Decrypt for use in the application.

    public String getAccountSid() {
        this.accountSid = getDecrypted("account_sid"); // sharedPreferences.getString("account_sid", "");
        return this.accountSid;
    }
    public void setAccountSid(String aParam) {
        setEncrypted("account_sid", aParam);
        this.accountSid = aParam;
    }
    public boolean existAccountSid() {
        if (accountSid.isEmpty()) {
            return false;
        }
        return true;
    }

    public String getAccountToken() {
        this.authToken = getDecrypted("auth_token");
        return this.authToken;
    }
    public void setAccountToken(String aParam) {
        setEncrypted("auth_token", aParam);
        this.authToken = aParam;
    }

    // ----------------------------------------------------
    // Needs to be set, in the Settings panel. Or calculate difference from GMT to local time.
    public void setLocalTimeOffset(String aParam) {
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        prefEditor.putString("local_time_offset", aParam);
        prefEditor.apply();
        prefEditor.commit();
    }
    public int getLocalTimeOffset() {
        return Integer.parseInt( sharedPreferences.getString("local_time_offset", "") );
    }
    public double getLocalTimeOffsetDouble() {
        return Double.parseDouble( sharedPreferences.getString("local_time_offset", "") );
    }
    public String getLocalTimeOffsetString() {
        return sharedPreferences.getString("local_time_offset", "");
    }

    // ---------------------------------------------------------------------------------------------
    // Application information maintained between panels and between app stopping and starting.

    public void setTwilioPhoneNumber(String aParam) {
        setEncrypted("twilio_phone_number", aParam);
    }
    public String getTwilioPhoneNumber() {
        return getDecrypted("twilio_phone_number");
    }

    public void setToPhoneNumber(String aParam) {
        setEncrypted("to_phone_number", aParam);
    }
    public String getToPhoneNumber() {
        return getDecrypted("to_phone_number");
    }

    public void setAccPhoneNumberList(String aParam) {
        setEncrypted("account_phone_number_list", aParam);
    }
    public String getAccPhoneNumberList() {
        return getDecrypted("account_phone_number_list");
    }

    public void setSendToList(String aParam) {
        setEncrypted("send_to_list", aParam);
    }
    public String getSendToList() {
        return getDecrypted("send_to_list");
    }
    // ----------------------------------------------------
    // Future use when improving preformance.
    private String accNumberList = "";
    public void setAccNumberList(String aParam) {
        setEncrypted("account_phone_number_list", aParam);
    }
    public String getAccNumberList() {
        return getDecrypted("account_phone_number_list");
    }

    // ---------------------------------------------------------------------------------------------
}
