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
package org.opendatakit.hope.activities.settings;

import org.opendatakit.hope.R;

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
    
    public static final String TABLE_ID_INTENT_KEY = "tableId";
    
    private long tableId;
    private EditText listFormatEt;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tableId = getIntent().getLongExtra(TABLE_ID_INTENT_KEY, -1);
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
    }
}
