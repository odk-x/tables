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
import java.util.Map;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.ODKDataUtils;
import org.opendatakit.tables.R;

import android.app.Dialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
    private String appName;
    private String tableId;
    private ColumnDefinition cd;
    private LinearLayout layout;
    private ArrayList<Map<String,Object>> optionValues;
    private List<EditText> optionFields;

    public MultipleChoiceSettingDialog(Context context, String appName, String tableId, ColumnDefinition cd) {
        super(context);
        this.context = context;
        this.appName = appName;
        this.tableId = tableId;
        this.cd = cd;
        setTitle(context.getString(R.string.multiple_choice_options));
        layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout);
        optionValues = new ArrayList<Map<String,Object>>();
        optionFields = new ArrayList<EditText>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        optionValues.clear();
        ArrayList<Map<String,Object>> choices;
        
        SQLiteDatabase db = null;
        try {
          db = DatabaseFactory.get().getDatabase(context, appName);
          choices = (ArrayList<Map<String, Object>>) 
              ColumnUtil.get().getDisplayChoicesList(db, tableId, cd.getElementKey());
        } finally {
          if ( db != null ) {
            db.close();
          }
        }

        for (Map<String,Object> option : choices) {
            optionValues.add(option);
        }
        init();
    }

    @SuppressWarnings("unchecked")
    private String getLocalizedString(Map<String,Object> option) {
      Object displayObj = option.get("display");
      if ( displayObj != null ) {
        Object textObj = ((Map<String,Object>) displayObj).get("text");
        if ( textObj != null ) {
          if ( textObj instanceof String ) {
            return (String) textObj;
          }
          return ODKDataUtils.getLocalizedDisplayName((Map<String,Object>)textObj);
        }
      }
      return "";
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
            field.setText(getLocalizedString(optionValues.get(i)));
            optionFields.add(field);
            TableRow row = new TableRow(context);
            row.addView(field);
//            Button deleteButton = new Button(context);
//            deleteButton.setText("X");
//            deleteButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    updateValueList();
//                    optionValues.remove(index);
//                    init();
//                }
//            });
//            row.addView(deleteButton);
            optionList.addView(row);
        }
        optionList.setColumnStretchable(0, true);
        layout.addView(optionList);
//        Button addButton = new Button(context);
//        addButton.setText(context.getString(R.string.add_choice));
//        addButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                updateValueList();
//                optionValues.add("");
//                init();
//            }
//        });
//        Button saveButton = new Button(context);
//        saveButton.setText(context.getString(R.string.save));
//        saveButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                updateValueList();
//                SQLiteDatabase db = null;
//                try {
//                  db = DatabaseFactory.get().getDatabase(context, appName);
//                  db.beginTransaction();
//                  ColumnUtil.get().setDisplayChoicesList(db, tableId, cd, optionValues);
//                  db.setTransactionSuccessful();
//                } finally {
//                  if ( db != null ) {
//                    db.endTransaction();
//                    db.close();
//                  }
//                }
//                dismiss();
//            }
//        });
//        LinearLayout buttonWrapper = new LinearLayout(context);
//        buttonWrapper.addView(addButton);
//        buttonWrapper.addView(saveButton);
//        layout.addView(buttonWrapper);
    }

//    private void updateValueList() {
//        for (int i = 0; i < optionFields.size(); i++) {
//            EditText field = optionFields.get(i);
//            optionValues.set(i, field.getText().toString());
//        }
//    }
}
