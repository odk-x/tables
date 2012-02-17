package yoonsung.odk.spreadsheet.Activity.settings;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.data.Preferences;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

/**
 * An activity for setting display options.
 * 
 * @author hkworden
 */
public class MainDisplaySettings extends Activity {
    
    public static final String TABLE_ID_INTENT_KEY = "tableId";
    
    private static final String[] spinnerTexts = { "Table", "List", "Line Graph" };
    
    private Preferences prefs;
    private String tableId;
    private Spinner viewTypeSpinner;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Preferences(this);
        tableId = getIntent().getStringExtra(TABLE_ID_INTENT_KEY);
        setContentView(R.layout.settings_maindisplay);
        viewTypeSpinner = (Spinner) findViewById(
                R.id.settings_maindisplay_viewtype);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, spinnerTexts);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        viewTypeSpinner.setAdapter(adapter);
        viewTypeSpinner.setSelection(prefs.getPreferredViewType(tableId));
        Button saveButton = (Button) findViewById(
                R.id.settings_maindisplay_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                finish();
            }
        });
        Button cancelButton = (Button) findViewById(
                R.id.settings_maindisplay_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void saveSettings() {
        prefs.setPreferredViewType(tableId,
                viewTypeSpinner.getSelectedItemPosition());
    }
}