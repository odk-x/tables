package yoonsung.odk.spreadsheet.data;

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
    
    public DbHelper(Context context) {
        super(context, DB_FILE_NAME, null, DB_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TableProperties.getTableCreateSql());
        db.execSQL(ColumnProperties.getTableCreateSql());
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
