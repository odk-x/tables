package yoonsung.odk.spreadsheet.SMS;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.SpreadSheet;
import yoonsung.odk.spreadsheet.Database.Data;
import android.app.ActivityManager;
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
    
	@Override
    public void onReceive(Context context, Intent intent) {
    	// Service
        NotificationManager nm =(NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        nm.cancel(R.string.app_name);
    	
        // Data-in
        Bundle bundle = intent.getExtras();        
        
        // Make a notice
        makeToastNotice(context, bundle);
        
        // Parse
        SMSConverter ps = new SMSConverter();
        HashMap<String, String> data = ps.parseSMS(getSMSBody(bundle));
       
        // Something to update
        if (data.size() > 0) {
        	// Add to DB
        	addNewData(data, getSMSFrom(bundle), getSMSTimestamp(bundle));
        	
        	// Refresh screen if needed
	        refreshSpreadSheetScreen(context);
        }
    }
    
	private void addNewData(HashMap<String, String> data, String phoneNumberIn, String timeStamp) {
		// Prepare content values
		ContentValues cv = new ContentValues();
		for (String key : data.keySet()) {
			cv.put(key, data.get(key).trim());
		}
		
		// Add a new row
		Data dataManager = new Data();
		try {
			dataManager.addRow(cv, phoneNumberIn, timeStamp);
		} catch (Exception e) {
			Log.e("SMSReiver", "Unable to add new data from SMS");
		}
	}
	
	private void refreshSpreadSheetScreen(Context context) {
		// Check if the application is on the top level task
        ActivityManager  am = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        String topActClass = am.getRunningTasks(1).get(0).topActivity.getClassName();
        
        // Refresh the screen with new data if on the top level
        if (topActClass.equals("yoonsung.odk.spreadsheet.Activity.SpreadSheet")) {   
	        Intent startActivity = new Intent();
	        startActivity.setClass(context, SpreadSheet.class);
	        startActivity.setAction(SpreadSheet.class.getName());
	        startActivity.setFlags(
	        Intent.FLAG_ACTIVITY_NEW_TASK
	        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
	        );
	        context.startActivity(startActivity);
        }
	}
	
    private void makeToastNotice(Context context, Bundle bundle) {
    	SmsMessage[] msgs = null;
        String str = "";            
        if (bundle != null)
        {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            for (int i=0; i<msgs.length; i++){
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
                str += "SMS from " + msgs[i].getOriginatingAddress();                     
                str += " :";
                str += msgs[i].getMessageBody().toString();
                str += "\n";        
            }
            //---display the new SMS message---
            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
        }             	
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