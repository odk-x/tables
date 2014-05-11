package org.opendatakit.tables.utils;

import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.KeyValueHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.tables.views.SpreadsheetView;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class PreferenceUtil {
  
  private static final String TAG = PreferenceUtil.class.getSimpleName();
    
  /**
   * Save viewType to be the default view type for the table represented by
   * tableProperties.
   * @param context
   * @param tableProperties
   * @param viewType
   */
  public static void setDefaultViewType(
      Context context,
      TableProperties tableProperties,
      TableViewType viewType) {
    SQLiteDatabase db = tableProperties.getWritableDatabase();
    try {
      db.beginTransaction();
      tableProperties.setDefaultViewType(db, viewType);
      db.setTransactionSuccessful();
    } catch ( Exception e ) {
      e.printStackTrace();
      Log.e(TAG, "Unable to change default view type: " + e.toString());
      Toast.makeText(
          context,
          "Unable to change default view type",
          Toast.LENGTH_LONG).show();
    } finally {
      db.endTransaction();
      db.close();
    }
  }
  
  /**
   * Get the width thast has been set for the column. If none has been set,
   * returns {@link SpreadsheetView#DEFAULT_COL_WIDTH}.
   * @param tableProperties
   * @param elementKey
   * @return
   */
  public static int getColumnWidth(
      TableProperties tableProperties,
      String elementKey) {
    KeyValueStoreHelper kvsh =
        tableProperties.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
    KeyValueHelper aspectHelper = kvsh.getAspectHelper(elementKey);
    Integer result = aspectHelper.getInteger(
        LocalKeyValueStoreConstants.Spreadsheet.KEY_COLUMN_WIDTH);
    if (result == null) {
      result = SpreadsheetView.DEFAULT_COL_WIDTH;
    }
    return result;
  }
  
  public static void setColumnWidth(
      TableProperties tableProperties,
      String elementKey,
      int newColumnWith) {
    KeyValueStoreHelper kvsh =
        tableProperties.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
    KeyValueHelper aspectHelper = kvsh.getAspectHelper(elementKey);
    aspectHelper.setInteger(
        LocalKeyValueStoreConstants.Spreadsheet.KEY_COLUMN_WIDTH,
        newColumnWith);
  }

}
