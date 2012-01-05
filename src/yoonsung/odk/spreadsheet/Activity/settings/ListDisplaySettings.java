package yoonsung.odk.spreadsheet.Activity.settings;

import yoonsung.odk.spreadsheet.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * An activity for setting list display options.
 * 
 * @author hkworden
 */
public class ListDisplaySettings extends Activity {
    
    private EditText listFormatEt;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_listdisplay);
        listFormatEt = (EditText) findViewById(
                R.id.settings_listdisplay_listformat);
        Button saveButton = (Button) findViewById(
                R.id.settings_listdisplay_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                finish();
            }
        });
        Button cancelButton = (Button) findViewById(
                R.id.settings_listdisplay_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void saveSettings() {
        String listFormat = listFormatEt.getText().toString();
    }
}
