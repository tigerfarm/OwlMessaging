package com.tfp.tfpsms;

import android.widget.TextView;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class TwSms {

    // ---------------------------------------------------------------------------------------------
    // Twilio SMS API Requests

    private AccountCredentials TWILIO_ACCOUNT = new AccountCredentials();
    private final String TwilioPhoneNumber = TWILIO_ACCOUNT.getTwilioPhoneNumber();

    AccountCredentials Acc = new AccountCredentials();
    private final String ACCOUNT_SID = Acc.getAccountSid();
    private String setSmsRequest = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";
    // SMS Send: https://www.twilio.com/docs/api/messaging/send-messages
    // SMS Messages: https://www.twilio.com/docs/api/messaging/message

    private RequestBody postParams;
    public RequestBody getPostParams() throws Exception {
        return postParams;
    }

    private String requestUrl;
    public String getRequestUrl() throws Exception {
        return requestUrl;
    }

    public void setSmsRequest() throws Exception {
        requestUrl = setSmsRequest;
    }

    public void setSmsRequestFrom(String phoneNumber) throws Exception {
        requestUrl = setSmsRequest + "?From="+phoneNumber + "&To="+TwilioPhoneNumber;
    }
    public void setSmsRequestTo(String phoneNumber) throws Exception {
        requestUrl = setSmsRequest + "?To="+phoneNumber + "&From="+TwilioPhoneNumber;
    }

    public void setSmsSend( String phoneNumTo, String theMessage ) throws Exception {
        postParams = new FormBody.Builder()
                .add("From", TwilioPhoneNumber)
                .add("To", phoneNumTo)
                .add("Body", theMessage)
                .build();
        requestUrl = setSmsRequest;
    }

    public String rmSmsMessages(String messageSid) throws Exception {
        // https://api.twilio.com/2010-04-01/Accounts/your_account_SID/Messages/SM1cacf80fb168403da49512ee7aa3ca16.json'
        String deleteUrl = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages/" + messageSid + ".json";
        return deleteUrl;
    }

    // ---------------------------------------------------------------------------------------------
    // Not used in the SMS version
    // ---------------------------------------------------------------------------------------------
    // Twilio Account Security API Requests (Sample API for testing: POST without credentials)
    private final String API_KEY = Acc.getAppApiKey();

    private String seconds_to_expire = "120";
    public void setPushAuthentication( String AuthyId, String AskForApproval, String seconds_to_expire ) throws Exception {
        postParams = new FormBody.Builder()
                .add("message", AskForApproval)
                .add("seconds_to_expire", seconds_to_expire)
                .build();
        requestUrl = "https://api.authy.com/onetouch/json/users/"+AuthyId+"/approval_requests?api_key=" + API_KEY;
    }

    private String PhoneVerSend = "https://api.authy.com/protected/json/phones/verification/start" + "?api_key=" + API_KEY;
    public void setPhoneVerificationSend( String param1, String param2, String param3 ) throws Exception {
        postParams = new FormBody.Builder()
                .add("via", param1)
                .add("country_code", param2)
                .add("phone_number", param3)
                .build();
        requestUrl = PhoneVerSend;
    }
                    /*
                    URL_REQUEST.setPhoneVerificationSend("sms", "1", "2223331234");
                    textString.setText("+ POST Phone Verification: "+URL_REQUEST.getRequestUrl());
                    postRequest();
                    */
                    /*
                    String AuthyId = "12312312";   // Mine
                    String AskForApproval = "Lunch at 1pm, the usual place.";
                    String seconds_to_expire = "120";
                    URL_REQUEST.setPushAuthentication(AuthyId, AskForApproval, seconds_to_expire);
                    textString.setText("+ Approval Request: " + AskForApproval);
                    postRequest();
                    */

    // ---------------------------------------------------------------------------------------------
    // GET Hello World (for testing)
    private String urlHello = "http://tigerfarmpress.com/hello.txt";
    public void setUrlHello() throws Exception {
        requestUrl = urlHello;
    }


}
