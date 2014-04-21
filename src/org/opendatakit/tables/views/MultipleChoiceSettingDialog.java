/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.views;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.TableProperties;

import android.app.Dialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
    private TableProperties tp;
    private ColumnProperties cp;
    private LinearLayout layout;
    private ArrayList<String> optionValues;
    private List<EditText> optionFields;

    public MultipleChoiceSettingDialog(Context context, TableProperties tp, ColumnProperties cp) {
        super(context);
        this.context = context;
        this.tp = tp;
        this.cp = cp;
        setTitle(context.getString(R.string.multiple_choice_options));
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
        for (String option : cp.getDisplayChoicesList()) {
            optionValues.add(option);
        }
        init();
    }

    private void init() {
        layout.removeAllViews();
        optionFields.clear();
        TableLayout optionList = new TableLayout(context);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
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
        optionList.setColumnStretchable(0, true);
        layout.addView(optionList);
        Button addButton = new Button(context);
        addButton.setText(context.getString(R.string.add_choice));
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateValueList();
                optionValues.add("");
                init();
            }
        });
        Button saveButton = new Button(context);
        saveButton.setText(context.getString(R.string.save));
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateValueList();
                SQLiteDatabase db =  tp.getWritableDatabase();
                try {
                  db.beginTransaction();
                  cp.setDisplayChoicesList(db, optionValues);
                  db.setTransactionSuccessful();
                } finally {
                  db.endTransaction();
                  db.close();
                }
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
