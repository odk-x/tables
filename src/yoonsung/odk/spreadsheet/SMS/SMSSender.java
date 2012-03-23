package yoonsung.odk.spreadsheet.SMS;

import android.telephony.SmsManager;

public class SMSSender {
    
    private static final int MAX_MSG_LENGTH = 140;
    
	public void sendSMS(String destPhNum, String content) {
		SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(destPhNum, null, content, null, null);
	}
	
	public void sendSMSWithCutoff(String destPhNum, String content) {
	    if (content.length() > MAX_MSG_LENGTH) {
	        content = content.substring(0, MAX_MSG_LENGTH - 3) + "...";
	    }
	    sendSMS(destPhNum, content);
	}
}
