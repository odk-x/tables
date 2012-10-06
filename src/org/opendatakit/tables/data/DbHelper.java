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
package org.opendatakit.tables.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A helper class for the database.
 * 
 * @author hkworden@gmail.com
 */
public class DbHelper extends SQLiteOpenHelper {
    
    private static final String DB_FILE_NAME = "/sdcard/odk/tables/db.sql";
    private static final int DB_VERSION = 1;
    
    private static DbHelper dbh = null;
    
    private DbHelper(Context context) {
        super(context, DB_FILE_NAME, null, DB_VERSION);
    }
    
    public static DbHelper getDbHelper(Context context) {
        if (dbh == null) {
            dbh = new DbHelper(context);
        }
        return dbh;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ColumnProperties.getTableCreateSql());
        db.execSQL(KeyValueStoreManager.getDefaultTableCreateSql());
        db.execSQL(KeyValueStoreManager.getActiveTableCreateSql());
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
