package org.opendatakit.tables.Activity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import org.opendatakit.tables.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
/*
 * Depricated Code. It will be updated in near future.
 * Ignore now.
 * 
 * @Author : YoonSung Hong (hys235@cs.washington.edu)
 */
public class FileManager extends Activity {
	
	public static final String FILE_LOAD_PATH = "File Load Path";
	
	private ListView lv;
	private String current_path;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.file_manager);
        
        // 	CURRENT DIR PATH AND LIST OF FILES IN THE PATH.
        FilenameFilter ff = new ExcelFilter();
        File path = new File("/data/data/com.yoonsung.spreadsheetsms");
        //File path = new File("/sdcard/download");
        current_path = path.getAbsolutePath();
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(path.list(ff)));
        
        // SET CURRENT PATH ON THE TEXT VIEW.
        TextView tv = (TextView)findViewById(R.id.file_manager_path);
        tv.setText(current_path); 
        
        // BIND LIST OF FILES WITH THE LIST VIEW.
        //ListView 
        lv = (ListView)findViewById(R.id.file_manager_list);
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list));
        lv.setTextFilterEnabled(true);
       
        // REGISTER ONITEMCLICK LISTENER
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> a, View v, int position, long id) {
        		// CREATE BUNDLE TO SEND BACK TO SpreadSheetSMS CLASS WITH THE LOAD PATH.
        		String file_load_path = current_path + "/" + lv.getItemAtPosition(position).toString();
        		Bundle bundle = new Bundle();
        		bundle.putString(FileManager.FILE_LOAD_PATH, file_load_path);
        		
        		// USE INTENT TO REPLY TO THE INTENT FROM SpreadSheetSMS CLASS.
        		Intent mIntent = new Intent();
        		mIntent.putExtras(bundle);
        		setResult(RESULT_OK, mIntent);
        		finish();
        	 }
		});    
 	}
}

class ExcelFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return (name.endsWith(".xls"));
    }
}
