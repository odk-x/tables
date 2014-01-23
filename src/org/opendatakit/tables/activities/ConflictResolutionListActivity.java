package org.opendatakit.tables.activities;

import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.data.ConflictTable;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.utils.TableFileUtils;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;

/**
 * An activity for presenting a list of all the rows in conflict.
 * @author sudar.sam@gmail.com
 *
 */
public class ConflictResolutionListActivity extends SherlockListActivity {

  private static final String TAG =
      ConflictResolutionListActivity.class.getSimpleName();

  private ConflictTable mConflictTable;
  private ArrayAdapter<String> mAdapter;

  @Override
  protected void onResume() {
    super.onResume();
    // Do this in on resume so that if we resolve a row it will be refreshed
    // when we come back.
    String tableId =
        getIntent().getStringExtra(Controller.INTENT_KEY_TABLE_ID);
    DbHelper dbHelper = DbHelper.getDbHelper(this, TableFileUtils.ODK_TABLES_APP_NAME);
    TableProperties tableProperties =
        TableProperties.getTablePropertiesForTable(dbHelper, tableId,
            KeyValueStore.Type.ACTIVE);
    DbTable dbTable = DbTable.getDbTable(dbHelper, tableProperties);
    this.mConflictTable = dbTable.getConflictTable();
    this.mAdapter = new ArrayAdapter<String>(
        getSupportActionBar().getThemedContext(),
        android.R.layout.simple_list_item_1);
    for (int i = 0; i < this.mConflictTable.getLocalTable().getNumberOfRows(); i++) {
      String localRowId = this.mConflictTable.getLocalTable()
          .getMetadataByElementKey(i, DataTableColumns.ID);
      String serverRowId = this.mConflictTable.getServerTable()
          .getMetadataByElementKey(i, DataTableColumns.ID);
      if (!localRowId.equals(serverRowId)) {
        Log.e(TAG, "row ids at same index are not the same! this is an " +
            "error.");
      }
      this.mAdapter.add(localRowId);
    }
    this.setListAdapter(mAdapter);
  }


  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Log.e(TAG, "[onListItemClick] clicked position: " + position);
    Intent i = new Intent(this, ConflictResolutionRowActivity.class);
    i.putExtra(Controller.INTENT_KEY_TABLE_ID,
        mConflictTable.getLocalTable().getTableProperties().getTableId());
    String rowId = this.mConflictTable.getLocalTable().getRowId(position);
    i.putExtra(ConflictResolutionRowActivity.INTENT_KEY_ROW_ID, rowId);
    this.startActivity(i);
  }

}
