package yoonsung.odk.spreadsheet.SMS;

import android.telephony.SmsManager;

public class SMSSender {

	public void sendSMS(String destPhNum, String content) {
		SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(destPhNum, null, content, null, null);
	}
	
}
