package org.opendatakit.tables.utils;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;

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

}
