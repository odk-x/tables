package com.yoonsung.spreadsheetsms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsMessage;
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
        Parser ps = new Parser();
        HashMap<String, String> map = ps.parseMapColValue(getSMSBody(bundle));
       
        if (map.size() > 0) {
        	// Add to DB
        	addRow(map);
        
	        // Check if the application is on the top level task
	        ActivityManager  am = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
	        String topActClass = am.getRunningTasks(1).get(0).topActivity.getClassName();
	        
	        // Refresh the screen with new data if on the top level
	        if (topActClass.equals("com.yoonsung.spreadsheetsms.SpreadSheetSMS")) {   
		        Intent startActivity = new Intent();
		        startActivity.setClass(context, SpreadSheetSMS.class);
		        startActivity.setAction(SpreadSheetSMS.class.getName());
		        startActivity.setFlags(
		        Intent.FLAG_ACTIVITY_NEW_TASK
		        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
		        );
		        context.startActivity(startActivity);
	        }
        }
    }
    
    public void makeToastNotice(Context context, Bundle bundle) {
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
    
    // Add the inserting entry received from SMS.
    private void addRow(HashMap<String, String> map) {
    	// Connect DB
		SQLiteDatabase db = new DBIO().getConn();
		// Prepare insert statement
		String sql = "INSERT INTO data";
		// Add columns to be affected
		int i = 0;
		for (String key : new HashSet<String>(map.keySet())) {
			if (i == 0) {
				sql += "(" + "`" + key +"`, ";
			} else if (i == map.size() -1) {
				sql += "`" + key +"`) ";
			} else {
				sql += "`" + key +"`, ";
			}
			i++;
		}
		// Add corresponding values
		int j = 0;
		for (String key : new HashSet<String>(map.keySet())) {
			if (j == 0) {
				sql += "VALUES('" + map.get(key) +"', ";
			} else if (j == map.size() - 1) {
				sql += "'" + map.get(key) +"') ";
			} else {
				sql += "'" + map.get(key) +"', ";
			}
			j++;
		}
		db.execSQL(sql);
    }
}