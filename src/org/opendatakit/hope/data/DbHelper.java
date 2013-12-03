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
package org.opendatakit.hope.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * A helper class for the database.
 *
 * @author hkworden@gmail.com
 * @author sudar.sam@gmail.com
 */
public class DbHelper {

    private DbHelperImpl impl;

    private static DbHelper dbh = null;

    private DbHelper(Context context) {
        impl = DbHelperImpl.getDbHelper(context);
    }

    public static DbHelper getDbHelper(Context context) {
        if (dbh == null) {
            dbh = new DbHelper(context);
        }
        return dbh;
    }

    public SQLiteDatabase getReadableDatabase() {
      return impl.getReadableDatabase();
    }

    public SQLiteDatabase getWritableDatabase() {
      return impl.getWritableDatabase();
    }
}
