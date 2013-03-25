/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.DataStructure;

import java.io.File;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.util.TableFileUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class is being cleaned up and refactored, but the refactoring is not
 * yet complete. I (sudar.sam) am putting my name on it just because I've
 * changed it, but I did not write it.
 * @author sudar.sam@gmail.com
 *
 */
public class DisplayPrefsDBManager extends SQLiteOpenHelper {

  public static final String DB_NAME = "colors";

  public static final String TABLE_ID_COL = "tableId";
  public static final String COL_NAME_COL = "colName";
  public static final String COMP_COL = "comp";
  public static final String VAL_COL = "val";
  public static final String FOREGROUND_COL = "foreground";
  public static final String BACKGROUND_COL = "background";
  public static final String ID_COL = "id";

  private static DisplayPrefsDBManager singleton = null;

    private DisplayPrefsDBManager(Context context) {
        super(context, ODKFileUtils.getMetadataFolder(TableFileUtils.ODK_TABLES_APP_NAME) + File.separator + "display_prefs.db", null, 1);
    }

    public static DisplayPrefsDBManager getManager(Context context) {
      if (singleton == null) {
        singleton = new DisplayPrefsDBManager(context);
      }
      return singleton;
    }

    public static String[] getColumns() {
      return new String[] {
          TABLE_ID_COL,
          COL_NAME_COL,
          COMP_COL,
          VAL_COL,
          FOREGROUND_COL,
          BACKGROUND_COL,
          ID_COL
      };
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String colorSql;
        colorSql = "CREATE TABLE " + DB_NAME + " (" +
                ID_COL + " TEXT PRIMARY KEY," +
                TABLE_ID_COL + " TEXT," +
                COL_NAME_COL + " TEXT," +
                COMP_COL + " TEXT," +
                VAL_COL + " TEXT," +
                FOREGROUND_COL + " TEXT," +
                BACKGROUND_COL + " TEXT" +
                ");";
        db.execSQL(colorSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int v1, int v2) {
        // TODO Auto-generated method stub
    }

}