package org.opendatakit.tables.utils;

import java.util.ArrayList;

import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ColumnDefinition;
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
  
  public ArrayList<Column> getUserDefinedColumns(
      SQLiteDatabase db, String tableId) {
    return ODKDatabaseUtils.getUserDefinedColumns(db, tableId);
  }
  
  public void insertDataIntoExistingDBTableWithId(
      SQLiteDatabase database,
      String tableId,
      ArrayList<ColumnDefinition> orderedColumns,
      ContentValues contentValues,
      String uuid) {
    ODKDatabaseUtils.insertDataIntoExistingDBTableWithId(
        database,
        tableId,
        orderedColumns,
        contentValues,
        uuid);
  }
  
  public void updateDataInExistingDBTableWithId(
      SQLiteDatabase database,
      String tableId,
      ArrayList<ColumnDefinition> orderedColumns,
      ContentValues contentValues,
      String uuid) {
    ODKDatabaseUtils.updateDataInExistingDBTableWithId(
        database, 
        tableId, 
        orderedColumns,
        contentValues, 
        uuid);
  }

}
