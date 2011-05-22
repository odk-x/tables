package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
import java.util.List;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs.ColColorRule;
import yoonsung.odk.spreadsheet.lib.ColorPickerDialog;

/**
 * The dialog for managing display preferences for a column.
 */
public class DisplayPrefsDialog extends Dialog {
    
    private TableActivity ta;
    private DisplayPrefs dp;
    private String colName;
    private List<ColColorRule> colRules;
    private List<EditText> ruleInputFields;
    int lastFocusedRow;
    
    DisplayPrefsDialog(TableActivity ta, DisplayPrefs dp, String colName) {
        super(ta);
        this.ta = ta;
        this.dp = dp;
        this.colName = colName;
        setTitle("Conditional Colors: " + colName);
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
        LinearLayout ll = new LinearLayout(ta);
        ll.setOrientation(LinearLayout.VERTICAL);
        TableLayout tl = new TableLayout(ta);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tl.setLayoutParams(tlp);
        for(int i=0; i<colRules.size(); i++) {
            TableRow row = getEditRow(i);
            tl.addView(row);
        }
        ll.addView(tl);
        Button addRuleButton = new Button(ta);
        addRuleButton.setText("Add Rule");
        addRuleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLastRowVal();
                dp.addRule(colName, ' ', "", Color.BLACK);
                refreshView();
            }
        });
        ll.addView(addRuleButton);
        Button closeButton = new Button(ta);
        closeButton.setText("OK");
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLastRowVal();
                ta.refreshDisplay();
                dismiss();
            }
        });
        tl.setColumnStretchable(1, true);
        ll.addView(closeButton);
        setContentView(ll);
    }
    
    private TableRow getEditRow(final int index) {
        final ColColorRule rule = colRules.get(index);
        TableRow row = new TableRow(ta);
        // preparing delete button
        TextView deleteButton = new TextView(ta);
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
        EditText input = new EditText(ta);
        input.setText((rule.compType + rule.val).trim());
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
        Button colorPickButton = new Button(ta);
        colorPickButton.setText("  ");
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
                ColorPickerDialog cpd = new ColorPickerDialog(ta, ccl,
                        rule.color);
                cpd.show();
            }
        });
        row.addView(colorPickButton);
        return row;
    }
    
    private void updateLastRowVal() {
        if(lastFocusedRow < 0) { return; }
        EditText inputEt = ruleInputFields.get(lastFocusedRow);
        String input = inputEt.getText().toString().trim();
        if(input.equals("")) { return; }
        ColColorRule rule = colRules.get(lastFocusedRow);
        rule.compType = input.charAt(0);
        rule.val = input.substring(1).trim();
        dp.updateRule(rule);
    }
    
}
