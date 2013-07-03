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
package org.opendatakit.tables.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.MessageShortcut;

import android.app.Dialog;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class ShortcutsManager extends ListActivity {
    
    private List<MessageShortcut> dataList;
    
    /**
     * Called when the activity is first created.
     * @param savedInstanceState the data most recently saved if the activity
     * is being re-initialized; otherwise null
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataList = getShortcutInfo();
        init();
    }
    
    private void init() {
        List<Map<String, String>> adapterList =
            new ArrayList<Map<String, String>>();
        for(MessageShortcut ms : dataList) {
            Map<String, String> map = new HashMap<String, String>();
            adapterList.add(map);
            map.put("name", "@" + ms.getName());
            map.put("desc", ms.getInput());
        }
        String[] from = {"name", "desc"};
        int[] to = {R.id.sc_item_name, R.id.sc_item_desc};
        SimpleAdapter adapter = new SimpleAdapter(this, adapterList,
                R.layout.shortcut_list_item, from, to);
        setContentView(R.layout.shortcut_list);
        setListAdapter(adapter);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        (new ShortcutEditDialog(dataList.get(position))).show();
    }
    
    private List<MessageShortcut> getShortcutInfo() {
        List<MessageShortcut> l = new ArrayList<MessageShortcut>();
        l.add(new MessageShortcut("fish", "price of %type% at %loc%",
                "@fish ?price =type %type% =location %loc%"));
        l.add(new MessageShortcut("forecast", "weather at %loc%",
                "@weather ?forecast =location %loc%"));
        return l;
    }
    
    private class ShortcutEditDialog extends Dialog {
        
        ShortcutEditDialog(final MessageShortcut ms) {
            super(ShortcutsManager.this);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.shortcut_edit_dialog);
            // preparing text fields
            final TextView nameET =
                (TextView) findViewById(R.id.scedit_dialog_name);
            final TextView inputET =
                (TextView) findViewById(R.id.scedit_dialog_input);
            final TextView outputET =
                (TextView) findViewById(R.id.scedit_dialog_output);
            nameET.setText(ms.getName());
            inputET.setText(ms.getInput());
            outputET.setText(ms.getOutput());
            // preparing buttons
            Button saveButton =
                (Button) findViewById(R.id.scedit_dialog_savebutton);
            Button cancelButton =
                (Button) findViewById(R.id.scedit_dialog_cancelbutton);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ms.setName(nameET.getText().toString());
                    ms.setInput(inputET.getText().toString());
                    ms.setOutput(outputET.getText().toString());
                    init();
                    dismiss();
                }
            });
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
        
    }
    
}