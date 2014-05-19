package org.opendatakit.tables.activities;

import java.util.Set;
import java.util.TreeSet;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.ListActivity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * An activity for presenting a list of all the rows in conflict.
 * @author sudar.sam@gmail.com
 *
 */
public class CheckpointResolutionListActivity extends ListActivity {

  private static final String TAG =
      CheckpointResolutionListActivity.class.getSimpleName();

  private UserTable mTable;
  private ArrayAdapter<String> mAdapter;

  @Override
  protected void onResume() {
    super.onResume();
    // Do this in on resume so that if we resolve a row it will be refreshed
    // when we come back.
    String appName = getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    String tableId =
        getIntent().getStringExtra(Constants.IntentKeys.TABLE_ID);
    TableProperties tableProperties =
        TableProperties.getTablePropertiesForTable(this, appName, tableId);
    DbTable dbTable = DbTable.getDbTable(tableProperties);
    this.mTable = dbTable.rawSqlQuery(null, null, null, null, null, null);
    this.mAdapter = new ArrayAdapter<String>(
        getActionBar().getThemedContext(),
        android.R.layout.simple_list_item_1);
    Set<String> rowIds = new TreeSet<String>();
    for (int i = 0; i < this.mTable.getNumberOfRows(); i++) {
      Row row = this.mTable.getRowAtIndex(i);
      String type = row.getDataOrMetadataByElementKey(DataTableColumns.SAVEPOINT_TYPE);
      if ( type == null || type.length() == 0 ) {
        String rowId = row.getDataOrMetadataByElementKey(DataTableColumns.ID);
        rowIds.add(rowId);
      }
    }
    this.mAdapter.addAll(rowIds);
    this.setListAdapter(mAdapter);
  }


  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    String rowId = mAdapter.getItem(position);
    Log.e(TAG, "[onListItemClick] clicked position: " + position + " rowId: " + rowId);
    Intent i = new Intent(this, ConflictResolutionRowActivity.class);
    i.putExtra(Constants.IntentKeys.APP_NAME,
        mTable.getTableProperties().getAppName());
    i.putExtra(Constants.IntentKeys.TABLE_ID,
        mTable.getTableProperties().getTableId());
    i.putExtra(CheckpointResolutionRowActivity.INTENT_KEY_ROW_ID, rowId);
    this.startActivity(i);
  }

}
