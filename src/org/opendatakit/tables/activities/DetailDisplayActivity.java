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

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.utils.TableFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.views.webkits.CustomTableView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


/**
 * The activity that displays information for a detail view.
 * @author most was unknown
 * @author sudar.sam@gmail.com
 *
 */
public class DetailDisplayActivity extends SherlockActivity
        implements DisplayActivity {

  private static final String TAG = "DetaiDisplayActivity";

  // These are strings necessary for the key value store
  public static final String KVS_PARTITION = "DetailDisplayActivity";
  public static final String KVS_ASPECT_DEFAULT = "default";
  /** The key storing the default file name for the detail view. */
  public static final String KEY_FILENAME = "filename";

    public static final String INTENT_KEY_ROW_ID = "rowId";
    /** Intent key to specify a filename other than that saved in the kvs. Does
     * not have to be initialized if the caller intends the value in the kvs to
     * be used.
     */
    public static final String INTENT_KEY_FILENAME = "filename";

    private String mAppName;
    /** The id of the row that is being displayed in this detail view. */
    private String mRowId;
    /** The table id to which the row belongs. */
    private String mTableId;
    private Controller c;
    private CustomTableView mCustomTableView = null;
    private UserTable mTable = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
        if ( mAppName == null ) {
          mAppName = TableFileUtils.getDefaultAppName();
        }
        setTitle("");
        mRowId = getIntent().getStringExtra(INTENT_KEY_ROW_ID);
        mTableId = getIntent().getStringExtra(Controller.INTENT_KEY_TABLE_ID);
        if (mRowId == null) {
          Log.e(TAG, "no row id was specified");
        }
        if (mTableId == null) {
          Log.e(TAG, "no table id was specified");
        }
        c = new Controller(this, this, getIntent().getExtras(), savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      c.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
        Intent data) {
      if (c.handleActivityReturn(requestCode, resultCode, data)) {
        // If we're here, we also need to update the displayed data, which
        // may have been changed.
        displayView();
      } else {
        super.onActivityResult(requestCode, resultCode, data);
      }
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
        displayView();
    }

    /**
     * Return the default detail view file name that has been set for the table
     * represented by the given {@link TableProperties} object. May return null
     * if no default detail view has been set.
     * @param tableProperties
     * @return
     */
    public static String getDefaultDetailFileName(
        TableProperties tableProperties) {
      // Then we need to recover the file name.
      KeyValueStoreHelper detailActivityKVS =
          tableProperties.getKeyValueStoreHelper(
              DetailDisplayActivity.KVS_PARTITION);
      String recoveredFilename =
          detailActivityKVS.getString(DetailDisplayActivity.KEY_FILENAME);
      return recoveredFilename;
    }

    @Override
    public void init() {
        // change the info bar text IF necessary
        c.setDetailViewInfoBarText();
        // See if the caller included a filename that should be used. Will be
        // null if not found, so we can just pass it right along into the view.

        TableProperties tableProperties =
            TableProperties.getTablePropertiesForTable(this, mAppName, mTableId);
        DbTable dbTable = DbTable.getDbTable(tableProperties);
        mTable = dbTable.getTableForSingleRow(mRowId);
        if (mTable.getNumberOfRows() > 1) {
          Log.e(TAG, "a detail display activity is displaying more than a" +
          		" single row for tableid: " + mTableId + " and row id: " +
              mRowId);
        }
        // Now we have to get the file name we're going to be displaying.
        // There are two options--the default file, set in TableProperties,
        // and a custom file that has been passed in.
        String filename =
            getIntent().getStringExtra(INTENT_KEY_FILENAME);
        if (filename == null) {
          // Then we need to recover the file name.
          String recoveredFilename = getDefaultDetailFileName(tableProperties);
          if (recoveredFilename == null) {
            // Then no default file has been set.
            Log.i(TAG, "no detail view has been set for tableId: " + mTableId);
          }
          // If we recovered it, good, otherwise we leave as null.
          filename = recoveredFilename;
        }
        mCustomTableView = CustomTableView.get(this, mAppName, mTable, filename);
    }

    private void displayView() {
      mCustomTableView.display();
      c.setDisplayView(mCustomTableView);
      setContentView(c.getContainerView());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        c.buildOptionsMenu(menu, false);
        // We want to be able to edit a row from the detail view. Rather than
        // add a new button, we're just going to hook onto the add row
        // button, change the png.
        MenuItem addRow = menu.getItem(Controller.MENU_ITEM_ID_ADD_ROW_BUTTON);
        addRow.setIcon(R.drawable.content_edit);
        addRow.setEnabled(true);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
      // If they've selected the edit button, we need to handle it here.
      // Otherwise, we let controller handle it.
      if (item.getItemId() == Controller.MENU_ITEM_ID_ADD_ROW_BUTTON) {
        // get the row num.
        int rowNum = mTable.getRowNumFromId(mRowId);
        // handle the case that it wasn't found, and do nothing
        if (rowNum == -1) {
          Toast.makeText(this.getApplicationContext(),
        		  getString(R.string.error_row_not_found), Toast.LENGTH_SHORT).show();
          return true;
        }
        c.editRow(mTable, rowNum);
        return true;
      }
        return c.handleMenuItemSelection(item);
    }
}
