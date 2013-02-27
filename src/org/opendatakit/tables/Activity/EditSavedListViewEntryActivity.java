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

import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;

import android.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * This class is the point of interaction by which list views are added and 
 * managed. 
 * 
 * @author sudar.sam@gmail.com
 *
 */
/*
 * All that I can think we need to have at this point is a name of the list
 * view and a filename associated with that list view. Perhaps we'll eventually
 * also want to set the type to be either collection view or regular? Unsure...
 * It should do checking for things like duplicate names, limit values and
 * things so that we won't be able to inject into the underlying SQL db, etc.
 */
public class EditSavedListViewEntryActivity extends PreferenceActivity {
  
  private static final String TAG = 
      EditSavedListViewEntryActivity.class.getName();
  
  /**
   * Intent key for the table id.
   */
  public static final String INTENT_KEY_TABLE_ID = "tableId";
  
  /**
   * Return code for having added a file name for the list view.
   */
  private static final int RETURN_CODE_NEW_FILE = 0;
  
  // The table id to which this list view belongs.
  private String tableId;
  // The table properties object for the table to which it belongs.
  private TableProperties tp;
  private DbHelper dbh;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.dbh = DbHelper.getDbHelper(this);
    this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId, 
        KeyValueStore.Type.ACTIVE);
    addPreferencesFromResource(
        org.opendatakit.tables.R.xml.preference_listview_entry);
  }
  
  public void init() {
    // PreferenceManager pm = getPreferenceManager();
  }

}
