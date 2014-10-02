package org.opendatakit.tables.activities;

import java.util.ArrayList;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

/**
 * This class is the base for any Activity that will display information about
 * a particular table. Callers must pass in a table id in the bundle with the
 * key {@link Constants.IntentKeys#TABLE_ID}.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsTableActivity extends AbsBaseActivity {
  
  private static final String TAG = 
      AbsTableActivity.class.getSimpleName();
  
  private String mTableId;
  private ArrayList<ColumnDefinition> mColumnDefinitions;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mTableId = retrieveTableIdFromIntent();
    if (mTableId == null) {
      Log.e(TAG, "[onCreate] table id was not present in Intent.");
      throw new IllegalStateException(
          "A table id was not passed to a table activity");
    }
    
    Log.e(TAG, "[onCreate] building mColumnDefinitions.");
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(
          Tables.getInstance().getApplicationContext(), getAppName());
      mColumnDefinitions = TableUtil.get().getColumnDefinitions(db, getTableId());
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
  }
  
  /**
   * Retrieve the table id from the intent. Returns null if not present.
   * @return
   */
  String retrieveTableIdFromIntent() {
    return this.getIntent().getStringExtra(Constants.IntentKeys.TABLE_ID);
  }

  public String getTableId() {
    return this.mTableId;
  }
  
  public ArrayList<ColumnDefinition> getColumnDefinitions() {
    return this.mColumnDefinitions;
  }
}
