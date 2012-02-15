package yoonsung.odk.spreadsheet.data;

import java.util.Map;

import yoonsung.odk.spreadsheet.data.TableProperties.State;
import yoonsung.odk.spreadsheet.data.TableProperties.Transactioning;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and modifying a user table.
 * 
 * @author hkworden@gmail.com (Hilary Worden)
 */
public class DbTable {

	public static final String DB_ROW_ID = "id";
	public static final String DB_SRC_PHONE_NUMBER = "srcPhoneNum";
	public static final String DB_LAST_MODIFIED_TIME = "lastModTime";
	public static final String DB_SYNC_ID = "syncId";
	public static final String DB_SYNC_TAG = "syncTag";
	public static final String DB_STATE = "state";
	public static final String DB_TRANSACTIONING = "transactioning";

	private final DbHelper dbh;
	private final TableProperties tp;

	public static DbTable getDbTable(DbHelper dbh, long tableId) {
		return new DbTable(dbh, tableId);
	}

	private DbTable(DbHelper dbh, long tableId) {
		this.dbh = dbh;
		this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId);
	}

	static void createDbTable(SQLiteDatabase db, TableProperties tp) {
		db.execSQL("CREATE TABLE " + tp.getDbTableName() + "(" + DB_ROW_ID
				+ " INTEGER PRIMARY KEY" + ", " + DB_SRC_PHONE_NUMBER + " TEXT"
				+ ", " + DB_LAST_MODIFIED_TIME + " TEXT NOT NULL" + ", "
				+ DB_SYNC_ID + " TEXT" + ", " + DB_SYNC_TAG + " TEXT" + ", "
				+ DB_STATE + " INT NOT NULL" + ", " + DB_TRANSACTIONING
				+ " INT NOT NULL" + ")");
	}

	/**
	 * @return a raw table of all the data in the table
	 */
	public Table getRaw() {
		return getRaw(null, null, null, null);
	}

	/**
	 * Gets a table of raw data.
	 * 
	 * @param columns
	 *            the columns to select (if null, all columns will be selected)
	 * @param selectionKeys
	 *            the column names for the WHERE clause (can be null)
	 * @param selectionArgs
	 *            the selection arguments (can be null)
	 * @param orderBy
	 *            the column to order by (can be null)
	 * @return a Table of the requested data
	 */
	public Table getRaw(String[] columns, String[] selectionKeys,
			String[] selectionArgs, String orderBy) {
		if (columns == null) {
			ColumnProperties[] cps = tp.getColumns();
			columns = new String[cps.length + 6];
			columns[0] = DB_SRC_PHONE_NUMBER;
			columns[1] = DB_LAST_MODIFIED_TIME;
			columns[2] = DB_SYNC_ID;
			columns[3] = DB_SYNC_TAG;
			columns[4] = DB_STATE;
			columns[5] = DB_TRANSACTIONING;
			for (int i = 0; i < cps.length; i++) {
				columns[i + 6] = cps[i].getColumnDbName();
			}
		}
		return dataQuery(columns, buildSelectionSql(selectionKeys),
				selectionArgs, orderBy);
	}

	public UserTable getUserTable(String[] selectionKeys,
			String[] selectionArgs, String orderBy) {
		String selection = buildSelectionSql(selectionKeys);
		Table table = dataQuery(tp.getColumnOrder(), selection, selectionArgs,
				orderBy);
		String[] footer = footerQuery(tp.getColumnOrder(), selection,
				selectionArgs);
		return new UserTable(table.getRowIds(), tp.getColumnOrder(),
				table.getData(), footer);
	}

	public UserTable getUserOverview(String[] primes, String[] selectionKeys,
			String[] selectionArgs, String orderBy) {
		if (primes.length == 0) {
			return getUserTable(selectionKeys, selectionArgs, orderBy);
		}
		String selection = buildSelectionSql(selectionKeys);

		StringBuilder allSelectList = new StringBuilder("y." + DB_ROW_ID
				+ " AS " + DB_ROW_ID);
		for (String col : tp.getColumnOrder()) {
			allSelectList.append(", y." + col + " AS " + col);
		}

		StringBuilder xList = new StringBuilder();
		StringBuilder yList = new StringBuilder();
		for (String prime : primes) {
			xList.append(prime + ", ");
			yList.append(", " + prime);
		}
		if (orderBy == null) {
			xList.delete(xList.length() - 2, xList.length());
		} else {
			xList.append(orderBy);
		}
		yList.delete(0, 2);
		StringBuilder xSelect = new StringBuilder();
		xSelect.append("SELECT MAX(" + DB_ROW_ID + ") as id");
		if (orderBy != null) {
			xSelect.append(", " + xList.toString());
		}
		xSelect.append(" FROM " + tp.getDbTableName());
		xSelect.append(" GROUP BY " + xList.toString());

		StringBuilder idSelect;
		if (orderBy == null) {
			idSelect = xSelect;
		} else {
			StringBuilder joinBuilder = new StringBuilder();
			joinBuilder.append("y.s" + orderBy + " = x." + orderBy);
			for (String prime : primes) {
				joinBuilder.append(" AND x." + prime + " = y." + prime);
			}
			idSelect = new StringBuilder();
			idSelect.append("SELECT x." + DB_ROW_ID + " FROM (");
			idSelect.append(xSelect.toString());
			idSelect.append(") x JOIN (");
			idSelect.append("SELECT MAX(" + orderBy + ") AS s" + orderBy);
			idSelect.append(", " + yList.toString());
			idSelect.append(" FROM " + tp.getDbTableName());
			idSelect.append(" GROUP BY " + yList.toString());
			idSelect.append(") y ON " + joinBuilder.toString());
		}

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT " + allSelectList.toString() + " FROM (");
		sqlBuilder.append(idSelect.toString());
		sqlBuilder.append(") x JOIN " + tp.getDbTableName() + " y");
		sqlBuilder.append(" ON x." + DB_ROW_ID + " = y." + DB_ROW_ID);

		SQLiteDatabase db = dbh.getReadableDatabase();
		Cursor c = db.rawQuery(sqlBuilder.toString(), selectionArgs);
		Table table = buildTable(c, tp.getColumnOrder());
		c.close();
		db.close();
		String[] footer = footerQuery(tp.getColumnOrder(), selection,
				selectionArgs);
		return new UserTable(table.getRowIds(), tp.getColumnOrder(),
				table.getData(), footer);
	}

	/**
	 * Queries the table with the given options and returns a Table.
	 */
	private Table dataQuery(String[] columns, String selection,
			String[] selectionArgs, String orderBy) {
		String[] colArr = new String[columns.length + 1];
		colArr[0] = DB_ROW_ID;
		for (int i = 0; i < columns.length; i++) {
			colArr[i + 1] = columns[i];
		}
		SQLiteDatabase db = dbh.getReadableDatabase();
		Cursor c = db.query(tp.getDbTableName(), colArr, selection,
				selectionArgs, null, null, orderBy);
		Table table = buildTable(c, columns);
		c.close();
		db.close();
		return table;
	}

	/**
	 * Builds a Table with the data from the given cursor. The cursor, but not
	 * the columns array, must include the row ID column.
	 */
	private Table buildTable(Cursor c, String[] columns) {
		int[] colIndices = new int[columns.length];
		int rowCount = c.getCount();
		int[] rowIds = new int[rowCount];
		String[][] data = new String[rowCount][columns.length];
		int rowIdIndex = c.getColumnIndexOrThrow(DB_ROW_ID);
		for (int i = 0; i < columns.length; i++) {
			colIndices[i] = c.getColumnIndexOrThrow(columns[i]);
		}
		c.moveToFirst();
		for (int i = 0; i < rowCount; i++) {
			rowIds[i] = c.getInt(rowIdIndex);
			for (int j = 0; j < columns.length; j++) {
				data[i][j] = c.getString(colIndices[j]);
			}
			c.moveToNext();
		}
		return new Table(rowIds, columns, data);
	}

	private String[] footerQuery(String[] columns, String selection,
			String[] selectionArgs) {
		ColumnProperties[] cps = new ColumnProperties[columns.length];
		StringBuilder sqlBuilder = new StringBuilder("SELECT");
		for (int i = 0; i < columns.length; i++) {
			String colDbName = columns[i];
			cps[i] = tp.getColumnByDbName(colDbName);
			int mode = cps[i].getFooterMode();
			switch (mode) {
			case ColumnProperties.FooterMode.COUNT:
				sqlBuilder.append(", COUNT(" + colDbName + ") AS " + colDbName);
				break;
			case ColumnProperties.FooterMode.MAXIMUM:
				sqlBuilder.append(", MAX(" + colDbName + ") AS " + colDbName);
				break;
			case ColumnProperties.FooterMode.MEAN:
				sqlBuilder.append(", COUNT(" + colDbName + ") AS count"
						+ colDbName);
				sqlBuilder
						.append(", SUM(" + colDbName + ") AS sum" + colDbName);
				break;
			case ColumnProperties.FooterMode.MINIMUM:
				sqlBuilder.append(", MIN(" + colDbName + ") AS " + colDbName);
				break;
			case ColumnProperties.FooterMode.SUM:
				sqlBuilder.append(", SUM(" + colDbName + ") AS " + colDbName);
				break;
			}
		}
		if (sqlBuilder.length() == 6) {
			return new String[columns.length];
		}
		sqlBuilder.delete(6, 7);
		sqlBuilder.append(" FROM " + tp.getDbTableName());
		if ((selection != null) && (selection.length() != 0)) {
			sqlBuilder.append(" WHERE " + selection);
		}
		String[] footer = new String[columns.length];
		SQLiteDatabase db = dbh.getReadableDatabase();
		Cursor c = db.rawQuery(sqlBuilder.toString(), selectionArgs);
		c.moveToFirst();
		for (int i = 0; i < columns.length; i++) {
			if (cps[i].getFooterMode() == ColumnProperties.FooterMode.MEAN) {
				int sIndex = c.getColumnIndexOrThrow("sum" + columns[i]);
				int cIndex = c.getColumnIndexOrThrow("count" + columns[i]);
				double sum = c.getInt(sIndex);
				int count = c.getInt(cIndex);
				footer[i] = String.valueOf(sum / count);
			} else if (cps[i].getFooterMode() != ColumnProperties.FooterMode.NONE) {
				int index = c.getColumnIndexOrThrow(columns[i]);
				footer[i] = c.getString(index);
			}
		}
		c.close();
		db.close();
		return footer;
	}

	/**
	 * Adds a row to the table with the given values, no source phone number,
	 * and the current time as the last modification time.
	 */
	public void addRow(Map<String, String> values) {
		addRow(values, DataUtil.getNowInDbFormat(), null);
	}

	/**
	 * Adds a row to the table.
	 */
	public void addRow(Map<String, String> values, String lastModTime,
			String srcPhone) {
		if (lastModTime == null) {
			lastModTime = DataUtil.getNowInDbFormat();
		}
		ContentValues cv = new ContentValues();
		for (String column : values.keySet()) {
			cv.put(column, values.get(column));
		}
		cv.put(DB_LAST_MODIFIED_TIME, lastModTime);
		cv.put(DB_SRC_PHONE_NUMBER, srcPhone);
		cv.put(DB_STATE, State.INSERTING);
		cv.put(DB_TRANSACTIONING, Transactioning.FALSE);
		SQLiteDatabase db = dbh.getWritableDatabase();
		Log.d("DBT", "insert, id=" + db.insert(tp.getDbTableName(), null, cv));
		db.close();
	}

	/**
	 * Updates a row in the table with the given values, no source phone number,
	 * and the current time as the last modification time.
	 */
	public void updateRow(int rowId, Map<String, String> values) {
		updateRow(rowId, values, null, DataUtil.getNowInDbFormat());
	}

	/**
	 * Updates a row in the table.
	 */
	public void updateRow(int rowId, Map<String, String> values,
			String srcPhone, String lastModTime) {
		ContentValues cv = new ContentValues();
		for (String column : values.keySet()) {
			cv.put(column, values.get(column));
		}
		cv.put(DB_STATE, State.UPDATING);
		String[] whereArgs = { String.valueOf(rowId),
				String.valueOf(State.REST),
				String.valueOf(Transactioning.FALSE) };
		String rowIdEquals = DB_ROW_ID + " = ?";
		String stateEquals = DB_STATE + " = ?";
		String transactioningEquals = DB_TRANSACTIONING + " = ?";
		SQLiteDatabase db = dbh.getWritableDatabase();
		int numUpdated = db.update(tp.getDbTableName(), cv, rowIdEquals
				+ " AND " + stateEquals + " AND " + transactioningEquals,
				whereArgs);
		if (numUpdated == 0) {
			// row is not in REST state, so keep it in the state it is in
			cv.remove(DB_STATE);
			String[] newWhereArgs = { String.valueOf(rowId),
					String.valueOf(Transactioning.FALSE) };
			numUpdated = db.update(tp.getDbTableName(), cv, rowIdEquals
					+ " AND " + transactioningEquals, newWhereArgs);
			assert numUpdated == 1;
		}
		db.close();
	}

	/**
	 * Changes a row's state to {@link State#DELETING}. It will actually be
	 * deleted the next time the SyncAdapter runs.
	 */
	public void deleteRow(int rowId) {
		ContentValues cv = new ContentValues();
		cv.put(DB_STATE, State.DELETING);
		String[] whereArgs = { String.valueOf(rowId), String.valueOf(Transactioning.FALSE) };
		String rowIdEquals = DB_ROW_ID + " = ?";
		String transactioningEquals = DB_TRANSACTIONING + " = ?";
		SQLiteDatabase db = dbh.getWritableDatabase();
		int numUpdated = db.update(tp.getDbTableName(), cv, rowIdEquals + " AND " + transactioningEquals, whereArgs);
		assert numUpdated == 1;
		db.close();
	}

	/**
	 * Deletes the given row from the table.
	 */
	public void deleteRowActual(int rowId) {
		String[] whereArgs = { String.valueOf(rowId) };
		SQLiteDatabase db = dbh.getWritableDatabase();
		db.delete(tp.getDbTableName(), DB_ROW_ID + " = ?", whereArgs);
		db.close();
	}

	private String buildSelectionSql(String[] selectionKeys) {
		if ((selectionKeys == null) || (selectionKeys.length == 0)) {
			return null;
		}
		StringBuilder selBuilder = new StringBuilder();
		for (String key : selectionKeys) {
			selBuilder.append(" AND " + key + " = ?");
		}
		if (selBuilder.length() > 0) {
			selBuilder.delete(0, 5);
		}
		return selBuilder.toString();
	}
}
