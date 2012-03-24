package yoonsung.odk.spreadsheet.SMS;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.util.SecurityUtil;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
 
public class SmsReceiver extends BroadcastReceiver {
	
    private TableProperties tp;
	
	@Override
    public void onReceive(Context context, Intent intent) {
	    MsgHandler mh = new MsgHandler(new DataManager(
	            DbHelper.getDbHelper(context)), new SMSSender());
	    String body = getSMSBody(intent.getExtras());
	    mh.handleMessage(body, getSMSFrom(intent.getExtras()));
	    
		
		// Service
        NotificationManager nm =(NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
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