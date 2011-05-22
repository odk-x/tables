package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
import java.util.List;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs.ColColorRule;
import yoonsung.odk.spreadsheet.lib.ColorPickerDialog;

/**
 * The dialog for managing display preferences for a column.
 */
public class DisplayPrefsDialog extends Dialog {
    
    private Context context;
    private DisplayPrefs dp;
    private String colName;
    private List<ColColorRule> colRules;
    private List<EditText> ruleInputFields;
    int lastFocusedRow;
    
    DisplayPrefsDialog(Context context, DisplayPrefs dp, String colName) {
        super(context);
        this.context = context;
        this.dp = dp;
        this.colName = colName;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshView();
    }
    
    private void refreshView() {
        lastFocusedRow = -1;
        colRules = dp.getColorRulesForCol(colName);
        ruleInputFields = new ArrayList<EditText>();
        TableLayout tl = new TableLayout(context);
        for(int i=0; i<colRules.size(); i++) {
            TableRow row = getEditRow(i);
            tl.addView(row);
        }
        setContentView(tl);
    }
    
    private TableRow getEditRow(final int index) {
        final ColColorRule rule = colRules.get(index);
        TableRow row = new TableRow(context);
        // preparing delete button
        Button deleteButton = new Button(context);
        deleteButton.setText("X");
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLastRowVal();
                dp.deleteRule(rule);
                refreshView();
            }
        });
        row.addView(deleteButton);
        // preparing the text field
        EditText input = new EditText(context);
        input.setText(rule.compType + rule.val);
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    lastFocusedRow = index;
                    return;
                } else {
                    updateLastRowVal();
                }
            }
        });
        ruleInputFields.add(input);
        row.addView(input);
        // preparing the color picker button
        Button colorPickButton = new Button(context);
        colorPickButton.setText("V");
        colorPickButton.setBackgroundColor(rule.color);
        colorPickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLastRowVal();
                ColorPickerDialog.OnColorChangedListener ccl =
                        new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void colorChanged(int color) {
                        rule.color = color;
                        dp.updateRule(rule);
                        refreshView();
                    }
                };
                ColorPickerDialog cpd = new ColorPickerDialog(context, ccl,
                        rule.color);
                cpd.show();
            }
        });
        row.addView(colorPickButton);
        return row;
    }
    
    private void updateLastRowVal() {
        if(lastFocusedRow < 0) { return; }
        String input = ruleInputFields.get(lastFocusedRow).getText().toString();
        ColColorRule rule = colRules.get(lastFocusedRow);
        rule.compType = input.charAt(0);
        rule.val = input.substring(1).trim();
        dp.updateRule(rule);
    }
    
}
