/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.sms;

import android.telephony.SmsManager;
import android.util.Log;

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
	    Log.d("SMSS", "sending to " + destPhNum + ":" + content);
	    sendSMS(destPhNum, content);
	}
}
