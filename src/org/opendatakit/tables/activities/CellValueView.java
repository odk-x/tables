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
package org.opendatakit.tables.activities;

import org.opendatakit.tables.data.ColumnProperties;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;


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
        
        private CellEditView(Context context) {
            super(context);
        }
        
        public abstract String getValue();
    }
    
    private static class MultipleChoiceEditView extends CellEditView {
        
        private final Spinner spinner;
        
        public MultipleChoiceEditView(Context context, ColumnProperties cp,
                String value) {
            super(context);
            String[] opts = cp.getMultipleChoiceOptions();
            int selection = -1;
            for (int i = 0; i < opts.length; i++) {
                if (opts[i].equals(value)) {
                    selection = i;
                    break;
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_spinner_item,
                    cp.getMultipleChoiceOptions());
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            spinner = new Spinner(context);
            spinner.setAdapter(adapter);
            if (selection != -1) {
                spinner.setSelection(selection);
            }
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
