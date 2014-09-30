package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.Iterator;

import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.database.DatabaseConstants;
import org.opendatakit.common.android.provider.TableDefinitionsColumns;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * The base Activity for all ODK Tables activities. Performs basic
 * functionality like retrieving the app name from an intent that all classes
 * should be doing.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsBaseActivity extends Activity {

  protected String mAppName;
  protected String mActionTableId = null;
  
  Bundle mCheckpointTables = new Bundle();
  Bundle mConflictTables = new Bundle();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mAppName = retrieveAppNameFromIntent();
    if ( savedInstanceState != null ) {
      if ( savedInstanceState.containsKey(Constants.IntentKeys.ACTION_TABLE_ID) ) {
        mActionTableId = savedInstanceState.getString(Constants.IntentKeys.ACTION_TABLE_ID);
        if ( mActionTableId != null && mActionTableId.length() == 0 ) {
          mActionTableId = null;
        }
      }
      
      if ( savedInstanceState.containsKey(Constants.IntentKeys.CHECKPOINT_TABLES) ) {
        mCheckpointTables = savedInstanceState.getBundle(Constants.IntentKeys.CHECKPOINT_TABLES);
      }

      if ( savedInstanceState.containsKey(Constants.IntentKeys.CONFLICT_TABLES) ) {
        mConflictTables = savedInstanceState.getBundle(Constants.IntentKeys.CONFLICT_TABLES);
      }
    }
  }
  
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    
    if ( mActionTableId != null && mActionTableId.length() != 0 ) {
      outState.putString(Constants.IntentKeys.ACTION_TABLE_ID, mActionTableId);
    }
    if ( mCheckpointTables != null && !mCheckpointTables.isEmpty() ) {
      outState.putBundle(Constants.IntentKeys.CHECKPOINT_TABLES, mCheckpointTables);
    }
    if ( mConflictTables != null && !mConflictTables.isEmpty() ) {
      outState.putBundle(Constants.IntentKeys.CONFLICT_TABLES, mConflictTables);
    }
  }

  public String getActionTableId() {
    return mActionTableId;
  }
  
  public void setActionTableId(String tableId) {
    mActionTableId = tableId;
  }
  
  public void scanAllTables() {
    long now = System.currentTimeMillis();
    Log.i(this.getClass().getSimpleName(), "scanAllTables -- searching for conflicts and checkpoints ");
    
    SQLiteDatabase db = null;
    Cursor c = null;

    StringBuilder b = new StringBuilder();
    b.append("SELECT ").append(TableDefinitionsColumns.TABLE_ID).append(" FROM \"")
     .append(DatabaseConstants.TABLE_DEFS_TABLE_NAME).append("\"");

    ArrayList<String> tableIds = new ArrayList<String>();
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(this, getAppName());
      try {
        c = db.rawQuery(b.toString(), null);
        int idxId = c.getColumnIndex(TableDefinitionsColumns.TABLE_ID);
        if ( c.moveToFirst() ) {
          do {
            tableIds.add(ODKDatabaseUtils.get().getIndexAsString(c, idxId));
          } while ( c.moveToNext() );
        }
        c.close();
      } finally {
        if ( c != null && !c.isClosed() ) {
          c.close();
        }
      }
      
      Bundle checkpointTables = new Bundle();
      Bundle conflictTables = new Bundle();
      
      for ( String tableId : tableIds ) {
        b.setLength(0);
        b.append("SELECT SUM(case when _savepoint_type is null then 1 else 0 end) as checkpoints,")
         .append("SUM(case when _conflict_type is not null then 1 else 0 end) as conflicts from \"")
         .append(tableId).append("\"");
        
        try {
          c = db.rawQuery(b.toString(), null);
          int idxCheckpoints = c.getColumnIndex("checkpoints");
          int idxConflicts = c.getColumnIndex("conflicts");
          c.moveToFirst();
          Integer checkpoints = ODKDatabaseUtils.get().getIndexAsType(c, Integer.class, idxCheckpoints);
          Integer conflicts = ODKDatabaseUtils.get().getIndexAsType(c, Integer.class, idxConflicts);
          c.close();
          
          if ( checkpoints != null && checkpoints != 0 ) {
            checkpointTables.putString(tableId, tableId);
          }
          if ( conflicts != null && conflicts != 0 ) {
            conflictTables.putString(tableId, tableId);
          }
        } finally {
          if ( c != null && !c.isClosed() ) {
            c.close();
          }
        }
      }
      mCheckpointTables = checkpointTables;
      mConflictTables = conflictTables;
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    
    long elapsed = System.currentTimeMillis() - now;
    Log.i(this.getClass().getSimpleName(), "scanAllTables -- full table scan completed: " + Long.toString(elapsed) + " ms");
  }
  
  @Override
  protected void onPostResume() {
    super.onPostResume();
    // Hijack the app here, after all screens have been resumed,
    // to ensure that all checkpoints and conflicts have been
    // resolved. If they haven't, we branch to the resolution
    // activity.
    
    if ( ( mCheckpointTables == null || mCheckpointTables.isEmpty() ) &&
         ( mConflictTables == null || mConflictTables.isEmpty() ) ) {
      scanAllTables();
    }
    if ( (mCheckpointTables != null) && !mCheckpointTables.isEmpty() ) {
      Iterator<String> iterator = mCheckpointTables.keySet().iterator();
      String tableId = iterator.next();
      mCheckpointTables.remove(tableId);

      Intent i;
      i = new Intent();
      i.setComponent(new ComponentName(Constants.ExternalIntentStrings.SYNC_PACKAGE_NAME,
              Constants.ExternalIntentStrings.SYNC_CHECKPOINT_ACTIVITY_COMPONENT_NAME));
      i.setAction(Intent.ACTION_EDIT);
      i.putExtra(Constants.IntentKeys.APP_NAME,
          getAppName());
      i.putExtra(
          Constants.IntentKeys.TABLE_ID,
          tableId);
      try {
        this.startActivityForResult(i, Constants.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER);
      } catch ( ActivityNotFoundException e ) {
        Toast.makeText(this, getString(R.string.activity_not_found, 
            Constants.ExternalIntentStrings.SYNC_CHECKPOINT_ACTIVITY_COMPONENT_NAME), Toast.LENGTH_LONG).show();
      }
    }
    if ( (mConflictTables != null) && !mConflictTables.isEmpty() ) {
      Iterator<String> iterator = mConflictTables.keySet().iterator();
      String tableId = iterator.next();
      mConflictTables.remove(tableId);

      Intent i;
      i = new Intent();
      i.setComponent(new ComponentName(Constants.ExternalIntentStrings.SYNC_PACKAGE_NAME,
          Constants.ExternalIntentStrings.SYNC_CONFLICT_ACTIVITY_COMPONENT_NAME));
      i.setAction(Intent.ACTION_EDIT);
      i.putExtra(Constants.IntentKeys.APP_NAME,
          getAppName());
      i.putExtra(
          Constants.IntentKeys.TABLE_ID,
          tableId);
      try {
        this.startActivityForResult(i, Constants.RequestCodes.LAUNCH_CONFLICT_RESOLVER);
      } catch ( ActivityNotFoundException e ) {
        Toast.makeText(this, getString(R.string.activity_not_found, 
            Constants.ExternalIntentStrings.SYNC_CONFLICT_ACTIVITY_COMPONENT_NAME), Toast.LENGTH_LONG).show();
      }
    }

  }
  
  /**
   * Gets the app name from the Intent. If it is not present it returns a
   * default app name.
   * @return
   */
  String retrieveAppNameFromIntent() {
    String result = 
        this.getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if (result == null) {
      result = TableFileUtils.getDefaultAppName();
    }
    return result;
  }
  
  /**
   * Get the app name that has been set for this activity.
   * @return
   */
  public String getAppName() {
    return this.mAppName;
  }
  
  /**
   * All Intents in the app expect an app name. This method returns an Intent
   * that can be expected to play nice with other activities. The class is not
   * set.
   * @return
   */
  public Intent createNewIntentWithAppName() {
    Intent intent = new Intent();
    intent.putExtra(
        Constants.IntentKeys.APP_NAME,
        getAppName());
    return intent;
  }

}
