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

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.views.webkits.CustomDetailView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


public class DetailDisplayActivity extends SherlockActivity
        implements DisplayActivity {

  private static final String TAG = "DetaiDisplayActivity";

    public static final String INTENT_KEY_ROW_ID = "rowId";
    /** Intent key to specify a filename other than that saved in the kvs. Does
     * not have to be initialized if the caller intends the value in the kvs to
     * be used.
     */
    public static final String INTENT_KEY_FILENAME = "filename";

    private String rowId;
    private Controller c;
    private CustomDetailView view;
    private UserTable table = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
        rowId = getIntent().getStringExtra(INTENT_KEY_ROW_ID);
        c = new Controller(this, this, getIntent().getExtras());
        init();
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
        displayView();
        c.setDisplayView(view);
        setContentView(c.getContainerView());
    }

    @Override
    public void init() {
        // change the info bar text IF necessary
        c.setDetailViewInfoBarText();
        // See if the caller included a filename that should be used. Will be
        // null if not found, so we can just pass it right along into the view.
        String intentFilename =
            getIntent().getStringExtra(INTENT_KEY_FILENAME);
        view = new CustomDetailView(this, c.getTableProperties(),
            intentFilename, c);
        displayView();
        c.setDisplayView(view);
        setContentView(c.getContainerView());
    }

    private void displayView() {
      TableProperties tp = c.getTableProperties();
        Query query = new Query(DbHelper.getDbHelper(this), KeyValueStore.Type.ACTIVE, tp);
        query.addRowIdConstraint(rowId);
        table = c.getIsOverview() ?
            c.getDbTable().getUserOverviewTable(query) :
              c.getDbTable().getUserTable(query);
        view.display(rowId, table);
    }

    @Override
    public void onSearch() {
        Controller.launchTableActivity(this, c.getTableProperties(),
                c.getSearchText(), c.getIsOverview(), null, null);
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
        int rowNum = table.getRowNumFromId(rowId);
        // handle the case that it wasn't found, and do nothing
        if (rowNum == -1) {
          Toast.makeText(this.getApplicationContext(),
        		  getString(R.string.error_row_not_found), Toast.LENGTH_SHORT).show();
          return true;
        }
        c.editRow(table, rowNum);
//        Log.d(TAG, "clicked add row button");
        return true;
      }
        return c.handleMenuItemSelection(item);
    }
}
