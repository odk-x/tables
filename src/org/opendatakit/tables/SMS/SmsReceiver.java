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
package org.opendatakit.tables.SMS;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.TableProperties;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
 
public class SmsReceiver extends BroadcastReceiver {
	
    private TableProperties tp;
	
	@Override
    public void onReceive(Context context, Intent intent) {
	    MsgHandler mh = new MsgHandler(new DataManager(
	            DbHelper.getDbHelper(context)), new SMSSender());
	    String body = getSMSBody(intent.getExtras());
	    mh.handleMessage(body, getSMSFrom(intent.getExtras()));
	    
		
		// Service
        NotificationManager nm =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.string.app_name);
    	
        // Data-in
        Bundle bundle = intent.getExtras();        
        
        // Header Data
        String phoneNum = getSMSFrom(bundle);
    }
    
    private String getSMSFrom(Bundle bundle) {
    	SmsMessage[] msgs = null;
        String str = "";            
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            for (int i=0; i<msgs.length; i++){
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
                str += msgs[i].getOriginatingAddress();
            }
        }             	
        return str;
    }

    private String getSMSTimestamp(Bundle bundle) {
    	SmsMessage[] msgs = null;
        String str = "";            
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            for (int i=0; i<msgs.length; i++){
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
                str += msgs[i].getTimestampMillis();
            }
        }
  
        // Epoch timestamp -> MM/dd/yyyy HH:mm:ss
        long epoch = new Long(str);
        Date datetime = new Date(epoch);  
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
       
        return df.format(datetime);
    }
    
    private String getSMSBody(Bundle bundle) {
    	SmsMessage[] msgs = null;
        String body = "";            
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            for (int i=0; i<msgs.length; i++){
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
                body += msgs[i].getMessageBody().toString();
                body += "\n";        
            }
        } 
        return body;
    }
    
}