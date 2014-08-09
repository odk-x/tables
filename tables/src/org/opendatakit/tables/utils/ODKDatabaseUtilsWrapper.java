package org.opendatakit.tables.utils;

import org.opendatakit.common.android.utilities.ODKDatabaseUtils;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * An extremely thin wrapper around {@link ODKDatabaseUtils}. It wraps the
 * static calls with non-static calls, allowing mocking to occur with just
 * Mockito.
 * @author sudar.sam@gmail.com
 *
 */
/*
 * NB: As additional calls need to be made via this object, they should be
 * added. I'm not adding them all now because I don't need all of them.
 */
public class ODKDatabaseUtilsWrapper {
  
  public void writeDataIntoExistingDBTableWithId(
      SQLiteDatabase database,
      String tableName,
      ContentValues contentValues,
      String uuid) {
    ODKDatabaseUtils.writeDataIntoExistingDBTableWithId(
        database,
        tableName,
        contentValues,
        uuid);
  }
  
  public void updateDataInExistingDBTableWithId(
      SQLiteDatabase database,
      String tableName,
      ContentValues contentValues,
      String uuid) {
    ODKDatabaseUtils.updateDataInExistingDBTableWithId(
        database, 
        tableName, 
        contentValues, 
        uuid);
  }

}
