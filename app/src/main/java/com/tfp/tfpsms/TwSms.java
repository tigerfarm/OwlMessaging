package com.tfp.tfpsms;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class TwSms {

    // ---------------------------------------------------------------------------------------------
    // Twilio SMS API Requests

    private String setSmsRequest;
    // SMS Send: https://www.twilio.com/docs/api/messaging/send-messages
    // SMS Messages: https://www.twilio.com/docs/api/messaging/message

    private AccountCredentials accountCredentials;

    public TwSms(AccountCredentials accountCredentials) {
        this.accountCredentials = accountCredentials;
        this.setSmsRequest = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountCredentials.getAccountSid());
    }

    private RequestBody postParams;

    public RequestBody getPostParams() throws Exception {
        return postParams;
    }

    private String requestUrl;
    public String getRequestUrl() {
        return requestUrl;
    }

    public void setSmsRequest() {
        requestUrl = setSmsRequest;
    }

    public void setSmsRequestFrom(String phoneNumber, String twilioNumber) {
        requestUrl = setSmsRequest + "?From="+phoneNumber + "&To="+twilioNumber;
    }
    public void setSmsRequestTo(String phoneNumber, String twilioNumber) {
        requestUrl = setSmsRequest + "?To="+phoneNumber + "&From="+twilioNumber;
    }

    public void setSmsSend(String phoneNumTo, String twilioNumber, String theMessage) {
        postParams = new FormBody.Builder()
                .add("From", twilioNumber)
                .add("To", phoneNumTo)
                .add("Body", theMessage)
                .build();
        requestUrl = setSmsRequest;
    }

    public String rmSmsMessages(String messageSid) {
        // https://api.twilio.com/2010-04-01/Accounts/your_account_SID/Messages/SM1cacf80fb168403da49512ee7aa3ca16.json'
        return String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages/%s.json", accountCredentials.getAccountSid(), messageSid);
    }

    // ---------------------------------------------------------------------------------------------
    // Not used in the SMS version
    // ---------------------------------------------------------------------------------------------
    // Twilio Account Security API Requests (Sample API for testing: POST without credentials)

    private String seconds_to_expire = "120";

    public void setPushAuthentication( String AuthyId, String AskForApproval, String seconds_to_expire ) throws Exception {
        postParams = new FormBody.Builder()
                .add("message", AskForApproval)
                .add("seconds_to_expire", seconds_to_expire)
                .build();
        requestUrl = "https://api.authy.com/onetouch/json/users/"+AuthyId+"/approval_requests?api_key=" + accountCredentials.getAppApiKey();
    }

    public void setPhoneVerificationSend( String param1, String param2, String param3 ) throws Exception {
        postParams = new FormBody.Builder()
                .add("via", param1)
                .add("country_code", param2)
                .add("phone_number", param3)
                .build();
        requestUrl = "https://api.authy.com/protected/json/phones/verification/start" + "?api_key=" + accountCredentials.getAppApiKey();
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
