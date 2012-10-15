package org.opendatakit.tables.DataStructure;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/*
 * SS: refactored and changing all the colNames
 * to public static final.
 */
public class DisplayPrefsDBManager extends SQLiteOpenHelper {
    
    //private static final String DB_LOC = "display_prefs.sql";
  // trying to see if i can find it.
  private static final String DB_LOC = 
      "/sdcard/odk/tables/display_prefs.sql";
  
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
        super(context, DB_LOC, null, 1);
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
                ID_COL + " INTEGER PRIMARY KEY," +
                TABLE_ID_COL + " TEXT," +
                COL_NAME_COL + " colname TEXT," +
                COMP_COL + " comp TEXT," +
                VAL_COL + " val TEXT," +
                FOREGROUND_COL + " foreground TEXT," +
                BACKGROUND_COL + " background TEXT" +
                ");";
        db.execSQL(colorSql);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int v1, int v2) {
        // TODO Auto-generated method stub
    }
    
}