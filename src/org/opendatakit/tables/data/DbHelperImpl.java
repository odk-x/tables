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

import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * A helper class for the database.
 *
 * @author hkworden@gmail.com
 * @author sudar.sam@gmail.com
 */
public class DbHelperImpl {

    private static DbHelperImpl dbh = null;

    private String appName;
    private DataModelDatabaseHelper mDbHelper;

    private DbHelperImpl(Context context, String appName) {
      this.appName = appName;
      this.mDbHelper = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
    }

    public String getAppName() {
      return appName;
    }

    public SQLiteDatabase getReadableDatabase() {
      return mDbHelper.getReadableDatabase();
    }

    public SQLiteDatabase getWritableDatabase() {
      return mDbHelper.getWritableDatabase();
    }

    public static DbHelperImpl getDbHelper(Context context, String appName) {
        if (dbh == null) {
            dbh = new DbHelperImpl(context, appName);
        }
        return dbh;
    }
}
