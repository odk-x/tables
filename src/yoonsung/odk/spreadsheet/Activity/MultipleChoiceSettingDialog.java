package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
import java.util.List;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

/**
 * A dialog for editing the multiple-choice options for a column.
 * 
 * @author hkworden
 */
public class MultipleChoiceSettingDialog extends Dialog {
    
    private Context context;
    private ColumnProperties cp;
    private LinearLayout layout;
    private List<String> optionValues;
    private List<EditText> optionFields;
    
    public MultipleChoiceSettingDialog(Context context, ColumnProperties cp) {
        super(context);
        this.context = context;
        this.cp = cp;
        layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout);
        optionValues = new ArrayList<String>();
        optionFields = new ArrayList<EditText>();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        optionValues.clear();
        for (String option : cp.getMultipleChoiceOptions()) {
            optionValues.add(option);
        }
        init();
    }
    
    private void init() {
        layout.removeAllViews();
        optionFields.clear();
        TableLayout optionList = new TableLayout(context);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        optionList.setLayoutParams(tlp);
        for (int i = 0; i < optionValues.size(); i++) {
            final int index = i;
            EditText field = new EditText(context);
            field.setText(optionValues.get(i));
            optionFields.add(field);
            TableRow row = new TableRow(context);
            row.addView(field);
            Button deleteButton = new Button(context);
            deleteButton.setText("X");
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateValueList();
                    optionValues.remove(index);
                    init();
                }
            });
            row.addView(deleteButton);
            optionList.addView(row);
        }
        layout.addView(optionList);
        Button addButton = new Button(context);
        addButton.setText("Add Choice");
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateValueList();
                optionValues.add("");
                init();
            }
        });
        Button saveButton = new Button(context);
        saveButton.setText("Save");
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateValueList();
                cp.setMultipleChoiceOptions(
                        optionValues.toArray(new String[0]));
                dismiss();
            }
        });
        LinearLayout buttonWrapper = new LinearLayout(context);
        buttonWrapper.addView(addButton);
        buttonWrapper.addView(saveButton);
        layout.addView(buttonWrapper);
    }
    
    private void updateValueList() {
        for (int i = 0; i < optionFields.size(); i++) {
            EditText field = optionFields.get(i);
            optionValues.set(i, field.getText().toString());
        }
    }
}
