package yoonsung.odk.spreadsheet.activities;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import yoonsung.odk.spreadsheet.data.ColumnProperties;


public class CellValueView {
    
    public static CellEditView getCellEditView(Context context,
            ColumnProperties cp, String value) {
        switch (cp.getColumnType()) {
        case ColumnProperties.ColumnType.MC_OPTIONS:
            return new MultipleChoiceEditView(context, cp, value);
        default:
            return new DefaultEditView(context, value);
        }
    }
    
    public static abstract class CellEditView extends LinearLayout {
        
        public CellEditView(Context context) {
            super(context);
        }
        
        public abstract String getValue();
    }
    
    private static class MultipleChoiceEditView extends CellEditView {
        
        private final Spinner spinner;
        
        public MultipleChoiceEditView(Context context, ColumnProperties cp,
                String value) {
            super(context);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_spinner_item,
                    cp.getMultipleChoiceOptions());
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            spinner = new Spinner(context);
            spinner.setAdapter(adapter);
            addView(spinner);
        }
        
        public String getValue() {
            return (String) spinner.getSelectedItem();
        }
    }
    
    private static class DefaultEditView extends CellEditView {
        
        private final EditText editText;
        
        public DefaultEditView(Context context, String value) {
            super(context);
            editText = new EditText(context);
            editText.setText(value);
            addView(editText);
        }
        
        public String getValue() {
            return editText.getText().toString();
        }
    }
}
