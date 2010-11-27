package yoonsung.odk.spreadsheet.SMS;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.Data;
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
        
        String msg = getSMSBody(bundle);
        String[] splt = msg.split(" ");
        if(splt[1].startsWith("+")) {
            handleAddition(bundle);
        } else if(splt[1].startsWith("?")) {
        	handleQuery(bundle);
        }
    }
	
	private void handleAddition(Bundle bundle) {
        // Parse
        SMSConverter ps = new SMSConverter();
        HashMap<String, String> data = ps.parseSMS(getSMSBody(bundle));
       
        // Filter SMS-IN columns
        ColumnProperty cp = new ColumnProperty();
        for (String key : data.keySet()) {
        	if (!cp.getSMSIN(key)) {
        		data.remove(key);
        	}
        }
           
        // Convert abbreviations to normal names
        for (String abrv : data.keySet()) {
        	String fullName = cp.getNameByAbrv(abrv);
        	if (fullName != null) {
        		String value = data.get(abrv);
        		data.remove(abrv);
        		data.put(fullName, value);
        	}
        }
        
        // Something to update
        if (data.size() > 0) {
        	// Add to DB
        	addNewData(data, getSMSFrom(bundle), getSMSTimestamp(bundle));
        }
	}
	
	private void handleQuery(Bundle bundle) {
		SMSConverter ps = new SMSConverter();
		String resp;
		try {
			resp = ps.getQueryResponse(getSMSBody(bundle));
		} catch (InvalidQueryException e) {
			resp = "invalid query: " + e.getMessage();
		}
		SMSSender sender = new SMSSender();
		Log.d("sending", resp);
		sender.sendSMS(getSMSFrom(bundle), resp);
	}
    
	private void addNewData(HashMap<String, String> data, String phoneNumberIn, String timeStamp) {
		// Prepare content values
		ContentValues cv = new ContentValues();
		for (String key : data.keySet()) {
			String val = data.get(key).trim();
			if (isNumeric(val)) 
				cv.put(key, Integer.parseInt(val));
			else
				cv.put(key, val);
		}
		
		// Add a new row
		Data dataManager = new Data();
		try {
			dataManager.addRow(cv, phoneNumberIn, timeStamp);
		} catch (Exception e) {
			Log.e("SMSReiver", "Unable to add new data from SMS");
		}
	}
	
	private boolean isNumeric(String aStringValue) {
		Pattern pattern = Pattern.compile( "\\d+" );

		Matcher matcher = pattern.matcher(aStringValue);
		return matcher.matches();
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