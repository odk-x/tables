/*
 * Copyright (C) 2014 University of Washington
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
