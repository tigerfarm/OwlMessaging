package com.tfp.tfpsms;

public class printJson {

    public void pretty(String jsonData) {
        int si;
        int ei;
        int startBracket;
        int endBracket;
        int aComma;
        int aBlank;
        String tabString = "   ";
        int tabStringLength = tabString.length();
        String indentString = "";

        // System.out.println("++ Print: " + jsonData);
        si = jsonData.indexOf("{", 0);
        ei = jsonData.indexOf("}", 0);
        if (si < 0 || ei < 0) {
            System.out.println("-- Error: responce is Not JSON data.");
            System.out.println("+ Responce text: " + jsonData + "...");
            return;
        }
        int jsonDataLength = jsonData.length();
        while (si < jsonDataLength) {
            // {"caller_name": null, "country_code": "US", "phone_number": "+18182103863", "national_format": "(818) 210-3863", "carrier": {"mobile_country_code": null, "mobile_network_code": null, "name": "Twilio", "type": "voip", "error_code": null}, "add_ons": null, "url": "https://lookups.twilio.com/v1/PhoneNumbers/+18182103863?Type=carrier"}
            startBracket = jsonData.indexOf("{", si);
            endBracket = jsonData.indexOf("}", si);
            aComma = jsonData.indexOf(",", si);
            aBlank = jsonData.indexOf(" ", si);
            // System.out.println(indentString + "si="+si+", startBracket="+startBracket+", endBracket="+endBracket+", aComma="+aComma);
            if (si == aBlank) {
                // Case: first char is a blank " ".
                // Skips by.
                si = si + 1;
            } else if (si == aComma) {
                // Case: }, "add_ons": null,
                // Skips by.
                si = si + 1;
            } else if (si == startBracket) {
                // Case: {"caller_name": null, "country_code": "US"}
                // Prints: {
                System.out.println(indentString + "{");
                indentString = indentString + tabString;
                si = si + 1;
            } else if (si == endBracket) {
                // Need a case to handle: "current_price": "0.090"}, {"prefixes": ["1808"],
                // Case: }, "add_ons": null,
                // Prints: },
                // Case: }
                // Prints: }
                // Stacy: Need a case to handle: {\"sub\": {\"media\": \"ab.json\"}} --- last printed :"sub": {"media": "ab.json":
                indentString = indentString.substring(0, indentString.length() - tabStringLength);
                if (aComma == si + 1) {
                    System.out.println(indentString + "},");
                    si = aComma + 1;
                } else {
                    System.out.println(indentString + "}");
                    si = endBracket + 1;
                }
                if (indentString.length() > tabStringLength) {
                    indentString = indentString.substring(0, indentString.length() - tabStringLength);
                }
            } else if ((startBracket > 0) && (startBracket < endBracket) && (startBracket < aComma)) {
                // Case: "carrier": {"mobile_country_code": null
                // si=112, startBracket=124, endBracket=181, aComma=152
                // Prints: "carrier":
                System.out.println(indentString + jsonData.substring(si, startBracket));
                si = startBracket;
                indentString = indentString + tabString;
            } else if ((aComma < 0) || (endBracket < aComma)) {
                // Case: "country_code": "US"}
                // Case: "country_code": "US"}, "add_ons": null,
                // Prints: "country_code": "US"
                System.out.println(indentString + jsonData.substring(si, endBracket));
                si = endBracket;
            } else if (endBracket > aComma) {
                // Case: "caller_name": null, "country_code": "US"}
                // Prints: "caller_name": null,
                System.out.println(indentString + jsonData.substring(si, aComma) + ",");
                si = aComma + 1;
            } else {
                System.out.println(indentString + "- error, case not accounted for.");
            }
        }
    }

}
