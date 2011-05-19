package yoonsung.odk.spreadsheet.SMS;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.SecurityTables;
import yoonsung.odk.spreadsheet.Database.TableList;
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
    	
	private SecurityTables st;
	
	private String tableName;
	private String tableID;
	
	@Override
    public void onReceive(Context context, Intent intent) {
		
		// Service
        NotificationManager nm =(NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        nm.cancel(R.string.app_name);
    	
        // Data-in
        Bundle bundle = intent.getExtras();        
        
        // Header Data
        String phoneNum = getSMSFrom(bundle);
        
        // Split Message Received
        String msg = getSMSBody(bundle);
        try {
            msg = (new MsgHandler()).translateMessage(msg);
        } catch(InvalidQueryException e) {
            e.printStackTrace();
        }
        String[] splt = msg.split(" ");
        
        Log.e("Here", "SMS REceived:" + msg);
        
        // Is this message for ODK Tables?
        if (splt.length < 1 || !splt[0].startsWith("@")) {
        	Log.e("start error", "does not start with @");
        	// Handle Error
        	return;
        } 
        
        // Is there password field in the table
        boolean hasPassword = false;
        String password = null;
        if (splt.length >2 && splt[1].startsWith("#")) {
        	hasPassword = true;
        	password = splt[1].substring(1);
        	Log.e("password", password);
        }
        
        // Is table exist?
        TableList tl = new TableList();
        if (!tl.isTableExist(splt[0].substring(1))) {
        	Log.e("table not esit", "not exisint");
        	// Handle Error
        	return;
        }

        Log.e("passed", "SMS passed");
        
        // Make a notice
        makeToastNotice(context, bundle);
        
        // Table Name
        this.tableName = splt[0].substring(1);
        this.tableID = Integer.toString(tl.getTableID(tableName));
        
        Log.e("tableID", tableID);
        
        // Security
        this.st = new SecurityTables(tableID);
        
        String srcPN = getSMSFrom(bundle);
        String timestamp = getSMSTimestamp(bundle);
        if(!tl.isSecurityTable(tableID)) {
	        // Security table cannot be accessed remotely 
        	if(splt[1].startsWith("+") || (hasPassword && splt[2].startsWith("+"))) {
	        	// Additions
	        	if (st.isWriteSecurityTableExist()) {
	        		// Write security table exists
	        		// Additionally validate password
	        		String writeSecTableID = st.getWriteTableID();
	        		Log.e("Validation",  "" + st.validatePasswordForPhoneNum(writeSecTableID, phoneNum, password));
	        		if (st.isPhoneNumInWriteTable(phoneNum)
	        				&& st.validatePasswordForPhoneNum(writeSecTableID, phoneNum, password)) {
	        			handleAddition(msg, srcPN, timestamp, hasPassword);
	        			Log.e("SMSReceiver", "Handling Addtion:" + phoneNum + ":" + password);
	        		}
	        	} else {
	        		// Write security table does not exist
	        		Log.e("SMSReceiver", "Handling Addition");
	        		handleAddition(msg, srcPN, timestamp, hasPassword);
	        	}
	        } else {
	        	// Querying
	        	if (st.isReadSecurityTableExist()) {
	        		// Read security table exists
	        		// Additionally validate password
	        		String readSecTableID = st.getReadTableID();
	        		if (st.isPhoneNumInReadTable(phoneNum)
	        				&& st.validatePasswordForPhoneNum(readSecTableID, phoneNum, password)) {
	        			Log.e("SMSReceiver", "Handling Query");
	        			handleQuery(msg, srcPN, timestamp);
	        		}
	        	} else {
	        		// Read security table does not exist
	        		Log.e("SMSReceiver", "Handling Query");
	        		handleQuery(msg, srcPN, timestamp);
	        	}
	        }
        }
    }
	
	private void handleAddition(String msg, String srcPN, String timestamp,
	        boolean hasPassword) {
        // Parse
        SMSConverter ps = new SMSConverter(tableID);
        Map<String, String> alldata;
        try {
			alldata = ps.parseSMS(msg, hasPassword);
		} catch (InvalidQueryException e) {
		    e.printStackTrace();
			Log.d("sra", "err:" + e.getMessage());
			return;
		}
       
        // Filter SMS-IN columns
		Map<String, String> data = new HashMap<String, String>();
        ColumnProperty cp = new ColumnProperty(tableID);
        for (String key : alldata.keySet()) {
        	if (cp.getSMSIN(key)) {
        		data.put(key, alldata.get(key));
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
        
        Log.d("sra", "data:" + data.toString());
        // Something to update
        if (data.size() > 0) {
        	// Add to DB
        	addNewData(data, srcPN, timestamp);
        }
	}
	
	private void handleQuery(String msg, String srcPN, String timestamp) {
		SMSConverter ps = new SMSConverter(tableID);
		String resp;
		try {
			resp = ps.getQueryResponse(msg);
		} catch (InvalidQueryException e) {
			resp = "invalid query: " + e.getMessage();
		}
		if(resp.length() > 160) {
			resp = resp.substring(0, 160);
		}
		SMSSender sender = new SMSSender();
		Log.d("sending", resp);
		sender.sendSMS(srcPN, resp);
	}
    
	private void addNewData(Map<String, String> data, String phoneNumberIn, String timeStamp) {
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
		DataTable dataManager = new DataTable(tableID);
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