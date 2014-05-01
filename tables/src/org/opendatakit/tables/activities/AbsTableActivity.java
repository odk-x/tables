package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.utils.Constants;

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
  
  /** 
   * The {@link TableProperties} of the table for which you're displaying
   * preferences.
   */
  protected TableProperties mTableProperties;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String tableId = retrieveTableIdFromIntent();
    if (tableId == null) {
      Log.e(TAG, "[onCreate] table id was not present in Intent.");
      throw new IllegalArgumentException(
          "A table id was not passed to a table activity");
    }
    TableProperties retrievedProperties = 
        retrieveTablePropertiesForId(tableId);
    if (retrievedProperties == null) {
      Log.e(TAG, "TableProperties not found for id: " + tableId);
      throw new IllegalArgumentException(
          "did not find TableProperties for table id: " + tableId);
    }
    this.mTableProperties = retrievedProperties;
  }
  
  /**
   * Retrieve the table id from the intent. Returns null if not present.
   * @return
   */
  String retrieveTableIdFromIntent() {
    return this.getIntent().getStringExtra(Constants.IntentKeys.TABLE_ID);
  }
  
  /**
   * Retrieves the table properties for the given tableId from the database.
   * @param tableId the id of the table
   * @return
   */
  TableProperties retrieveTablePropertiesForId(String tableId) {
    TableProperties result = TableProperties.getTablePropertiesForTable(
        this,
        this.getAppName(),
        tableId);
    return result;
  }
  
  /**
   * Returns the {@link TableProperties} that this activity is displaying.
   * @return
   */
  public TableProperties getTableProperties() {
    return this.mTableProperties;
  }

}
