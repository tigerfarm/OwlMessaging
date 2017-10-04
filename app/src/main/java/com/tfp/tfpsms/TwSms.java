package com.tfp.tfpsms;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

    public void setSmsRequestLogs(String fromPhoneNumber, String toPhoneNumber) {
        requestUrl = setSmsRequest + "?From="+fromPhoneNumber + "&To="+toPhoneNumber;
    }
    public void setSmsRequestLogsTo(String phoneNumber) {
        requestUrl = setSmsRequest + "?To="+phoneNumber;
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

    public void setAccPhoneNumbers() {
        // https://api.twilio.com/2010-04-01/Accounts/your_account_SID/IncomingPhoneNumbers.json
        requestUrl =  String.format("https://api.twilio.com/2010-04-01/Accounts/%s/IncomingPhoneNumbers.json", accountCredentials.getAccountSid());
    }

    public String getLookup(String thePhoneNumber) {
        // http://lookups.twilio.com/v1/PhoneNumbers/+12093539979?Type=carrier
        return "https://lookups.twilio.com/v1/PhoneNumbers/+" + thePhoneNumber + "?Type=carrier";
    }

    // ---------------------------------------------------------------------------------------------
    String localDateTime(String theGmtDate) {
        //                                                        "27 Sep 2017 00:32:47"
        SimpleDateFormat readDateFormatter = new SimpleDateFormat("dd MMM yyyy hh:mm:ss");
        Date gmtDate = new Date();
        try {
            //  012345678901234567890123456789
            //  123456                   123456
            // :Tue, 26 Sep 2017 00:49:31 +0000:
            gmtDate = readDateFormatter.parse(theGmtDate.substring(5, 25));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(gmtDate);
        cal.add(Calendar.HOUR, accountCredentials.getLocalTimeOffset()); // from GMT to PST

        SimpleDateFormat writeDateformatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
        return writeDateformatter.format(cal.getTime());
    }

    // ---------------------------------------------------------------------------------------------
    // Twilio Account Security API Requests (Sample API for testing: POST without credentials)

    private String default_seconds_to_expire = "120";

    public void setPushAuthentication( String authyId, String AskForApproval ) throws Exception {
        setPushAuthentication(authyId, AskForApproval, default_seconds_to_expire);
    }
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

    // ---------------------------------------------------------------------------------------------
    // GET Hello World (for testing)
    // TwilioSms.setUrlHello();
    // textString.setText("+ GET Hello World text file: "+TwilioSms.getRequestUrl());
    // getRequest();
    private String urlHello = "http://tigerfarmpress.com/hello.txt";
    public void setUrlHello() throws Exception {
        requestUrl = urlHello;
    }


}
