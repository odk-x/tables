package yoonsung.odk.spreadsheet.sync.aggregate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.ClientProtocolException;
import org.opendatakit.aggregate.odktables.client.api.SynchronizeAPI;
import org.opendatakit.aggregate.odktables.client.entity.Column;
import org.opendatakit.aggregate.odktables.client.entity.Modification;
import org.opendatakit.aggregate.odktables.client.entity.SynchronizedRow;
import org.opendatakit.aggregate.odktables.client.exception.AggregateInternalErrorException;
import org.opendatakit.aggregate.odktables.client.exception.ColumnDoesNotExistException;
import org.opendatakit.aggregate.odktables.client.exception.OutOfSynchException;
import org.opendatakit.aggregate.odktables.client.exception.PermissionDeniedException;
import org.opendatakit.aggregate.odktables.client.exception.RowOutOfSynchException;
import org.opendatakit.aggregate.odktables.client.exception.TableAlreadyExistsException;
import org.opendatakit.aggregate.odktables.client.exception.TableDoesNotExistException;
import org.opendatakit.common.ermodel.simple.AttributeType;
import android.content.ContentValues;

import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.ColumnProperties.ColumnType;
import yoonsung.odk.spreadsheet.data.DataUtil;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Table;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.sync.SyncUtil;

public class AggregateSyncProcessor {

	private static final Map<Integer, AttributeType> types = new HashMap<Integer, AttributeType>() {
		{
			put(ColumnType.COLLECT_FORM, AttributeType.STRING);
			put(ColumnType.DATE, AttributeType.DATETIME);
			put(ColumnType.DATE_RANGE, AttributeType.STRING);
			put(ColumnType.FILE, AttributeType.STRING);
			put(ColumnType.MC_OPTIONS, AttributeType.STRING);
			put(ColumnType.NONE, AttributeType.STRING);
			put(ColumnType.NUMBER, AttributeType.DECIMAL);
			put(ColumnType.PHONE_NUMBER, AttributeType.STRING);
			put(ColumnType.TABLE_JOIN, AttributeType.STRING);
			put(ColumnType.TEXT, AttributeType.STRING);
		}
		private static final long serialVersionUID = 1L;
	};

	private final SynchronizeAPI api;
	private final DbHelper helper;

	public AggregateSyncProcessor(SynchronizeAPI api, DbHelper helper) {
		this.api = api;
		this.helper = helper;
	}

	/**
	 * 
	 * @param tableId
	 * @throws IOException
	 * @throws AggregateInternalErrorException
	 * @throws TableAlreadyExistsException
	 * @throws ClientProtocolException
	 * @throws ColumnDoesNotExistException
	 * @throws PermissionDeniedException
	 * @throws TableDoesNotExistException
	 * @throws OutOfSynchException
	 * @throws RowOutOfSynchException
	 */
	public void synchronize(long tableId) throws ClientProtocolException,
			TableAlreadyExistsException, AggregateInternalErrorException,
			IOException, OutOfSynchException, TableDoesNotExistException,
			PermissionDeniedException, ColumnDoesNotExistException,
			RowOutOfSynchException {
		TableProperties tp = TableProperties.getTablePropertiesForTable(helper,
				tableId);
		DbTable table = DbTable.getDbTable(helper, tableId);

		beginTableTransaction(tp);

		Table rowsToInsert;
		Table rowsToUpdate;
		Table rowsToDelete;
		switch (tp.getSyncState()) {
		case SyncUtil.State.INSERTING:
			rowsToInsert = getRows(table, SyncUtil.State.INSERTING);

			beginRowsTransaction(table, rowsToInsert.getRowIds());

			createTable(tp);
			insertRows(tp, table, rowsToInsert);

			endRowsTransaction(table, rowsToInsert.getRowIds());

			break;
		case SyncUtil.State.UPDATING:
			updateFromServer(tp, table);

			rowsToInsert = getRows(table, SyncUtil.State.INSERTING);
			rowsToUpdate = getRows(table, SyncUtil.State.UPDATING);
			rowsToDelete = getRows(table, SyncUtil.State.DELETING);

			int[] rowIds = getAllRowIds(rowsToInsert, rowsToUpdate,
					rowsToDelete);
			beginRowsTransaction(table, rowIds);

			insertRows(tp, table, rowsToInsert);
			updateRows(tp, table, rowsToUpdate);
			deleteRows(tp, table, rowsToDelete);
			for (int rowId : rowsToDelete.getRowIds()) {
			    table.deleteRowActual(rowId);
			}

			rowIds = getAllRowIds(rowsToInsert, rowsToUpdate);
			endRowsTransaction(table, rowIds);

			break;
		case SyncUtil.State.DELETING:
			beginRowsTransaction(table, new int[0]);
			removeTableSynchronization(String.valueOf(tp.getTableId()));
			tp.deleteTableActual();

			break;
		}
		tp.setLastSyncTime(DataUtil.getNowInDbFormat());
		endTableTransaction(tp);
	}

	public void updateFromServer(TableProperties tp, DbTable table)
			throws ClientProtocolException, PermissionDeniedException,
			TableDoesNotExistException, AggregateInternalErrorException,
			IOException {
		Modification mod = api.synchronize(String.valueOf(tp.getTableId()),
				tp.getSyncModificationNumber());

		Table allRowIds = table.getRaw(new String[] { DbTable.DB_ROW_ID,
				DbTable.DB_SYNC_ID, DbTable.DB_SYNC_STATE }, null, null, null);
		List<SynchronizedRow> rowsToInsert = new ArrayList<SynchronizedRow>();
		List<SynchronizedRow> rowsToUpdate = new ArrayList<SynchronizedRow>();
		List<SynchronizedRow> rowsToConflict = new ArrayList<SynchronizedRow>();
		for (SynchronizedRow row : mod.getRows()) {
			boolean found = false;
			for (int i = 0; i < allRowIds.getHeight(); i++) {
				String syncRowId = allRowIds.getData(i, 1);
				int state = Integer.parseInt(allRowIds.getData(i, 2));
				if (row.getAggregateRowIdentifier().equals(syncRowId)) {
					row.setRowID(String.valueOf(allRowIds.getRowId(i)));
					found = true;
					if (state == SyncUtil.State.REST)
						rowsToUpdate.add(row);
					else
						rowsToConflict.add(row);
				}
			}
			if (!found)
				rowsToInsert.add(row);
		}

		// TODO: refactor these for loops?

		for (SynchronizedRow row : rowsToConflict) {
			ContentValues values = new ContentValues();
			values.put(DbTable.DB_SYNC_ID, row.getAggregateRowIdentifier());
			values.put(DbTable.DB_SYNC_TAG, row.getRevisionTag());
			values.put(DbTable.DB_SYNC_STATE, String.valueOf(
			        SyncUtil.State.CONFLICTING));
			values.put(DbTable.DB_TRANSACTIONING,
					String.valueOf(SyncUtil.Transactioning.FALSE));
			table.actualUpdateRowByRowId(Integer.valueOf(row.getRowID()),
			        values);

			for (Entry<String, String> entry : row.getColumnValuePairs()
					.entrySet())
				values.put(entry.getKey(), entry.getValue());
			
			table.actualAddRow(values);
		}

		for (SynchronizedRow row : rowsToUpdate) {
			ContentValues values = new ContentValues();
			values.put(DbTable.DB_SYNC_ID, row.getAggregateRowIdentifier());
			values.put(DbTable.DB_SYNC_TAG, row.getRevisionTag());
			values.put(DbTable.DB_SYNC_STATE, String.valueOf(SyncUtil.State.REST));
			values.put(DbTable.DB_TRANSACTIONING,
					String.valueOf(SyncUtil.Transactioning.FALSE));
			for (Entry<String, String> entry : row.getColumnValuePairs()
					.entrySet())
				values.put(entry.getKey(), entry.getValue());
			table.actualUpdateRowByRowId(Integer.valueOf(row.getRowID()),
			        values);
		}

		for (SynchronizedRow row : rowsToInsert) {
			ContentValues values = new ContentValues();
			values.put(DbTable.DB_SYNC_ID, row.getAggregateRowIdentifier());
			values.put(DbTable.DB_SYNC_TAG, row.getRevisionTag());
			values.put(DbTable.DB_SYNC_STATE, SyncUtil.State.REST);
			values.put(DbTable.DB_TRANSACTIONING,
			        SyncUtil.Transactioning.FALSE);
			for (Entry<String, String> entry : row.getColumnValuePairs()
					.entrySet())
				values.put(entry.getKey(), entry.getValue());
			table.actualAddRow(values);
		}
		tp.setSyncModificationNumber(mod.getModificationNumber());
	}

	public Table getRows(DbTable table, int state) {
		Table rows = table.getRaw(
				null,
				new String[] { DbTable.DB_SYNC_STATE, DbTable.DB_TRANSACTIONING },
				new String[] { String.valueOf(state),
						String.valueOf(SyncUtil.Transactioning.FALSE) }, null);
		return rows;
	}

	/**
	 * Creates the given table in Aggregate.
	 * 
	 * @param tp
	 * @throws ClientProtocolException
	 * @throws TableAlreadyExistsException
	 * @throws AggregateInternalErrorException
	 * @throws IOException
	 */
	public void createTable(TableProperties tp) throws ClientProtocolException,
			TableAlreadyExistsException, AggregateInternalErrorException,
			IOException {
		String tableID = String.valueOf(tp.getTableId());
		String tableName = tp.getDbTableName();
		ColumnProperties[] colProps = tp.getColumns();
		List<Column> columns = new ArrayList<Column>();
		for (ColumnProperties colProp : colProps) {
			String name = colProp.getColumnDbName();
			AttributeType type = types.get(colProp.getColumnType());
			boolean nullable = true;
			Column column = new Column(name, type, nullable);
			columns.add(column);
		}
		Modification mod = api.createSynchronizedTable(tableID, tableName,
				columns);
		tp.setSyncModificationNumber(mod.getModificationNumber());
	}

	/**
	 * Inserts the given rows into Aggregate.
	 * 
	 * @param tableID
	 *            the ID of the table to insert into
	 * @param modificationNumber
	 *            the current modification number of the table
	 * @param rowsToInsert
	 *            the rows to insert.
	 * @throws IOException
	 * @throws ColumnDoesNotExistException
	 * @throws AggregateInternalErrorException
	 * @throws PermissionDeniedException
	 * @throws TableDoesNotExistException
	 * @throws OutOfSynchException
	 * @throws ClientProtocolException
	 */
	public void insertRows(TableProperties tp, DbTable table, Table rowsToInsert)
			throws ClientProtocolException, OutOfSynchException,
			TableDoesNotExistException, PermissionDeniedException,
			AggregateInternalErrorException, ColumnDoesNotExistException,
			IOException {
		String tableID = String.valueOf(tp.getTableId());
		int modificationNumber = tp.getSyncModificationNumber();
		List<SynchronizedRow> newRows = new ArrayList<SynchronizedRow>();
		int numRows = rowsToInsert.getHeight();
		int numCols = rowsToInsert.getWidth();
		for (int i = 0; i < numRows; i++) {
			SynchronizedRow row = new SynchronizedRow();
			row.setRowID(String.valueOf(rowsToInsert.getRowId(i)));
			for (int j = 0; j < numCols; j++) {
				String colName = rowsToInsert.getHeader(j);
				if (!isSpecialDbColumn(colName))
					row.setValue(colName, rowsToInsert.getData(i, j));
			}
			newRows.add(row);
		}

		if (!newRows.isEmpty()) {
			Modification mod = api.insertSynchronizedRows(tableID,
					modificationNumber, newRows);

			tp.setSyncModificationNumber(mod.getModificationNumber());
			for (SynchronizedRow row : mod.getRows()) {
			    ContentValues values = new ContentValues();
				values.put(DbTable.DB_SYNC_ID, row.getAggregateRowIdentifier());
				values.put(DbTable.DB_SYNC_TAG, row.getRevisionTag());
				table.actualUpdateRowByRowId(Integer.parseInt(row.getRowID()),
				        values);
			}
		}
	}

	/**
	 * Updates the given rows into Aggregate.
	 * 
	 * @param tableID
	 *            the ID of the table to update
	 * @param modificationNumber
	 *            the current modification number of the table
	 * @param rowsToUpdate
	 *            the rows to update.
	 * @throws IOException
	 * @throws ColumnDoesNotExistException
	 * @throws AggregateInternalErrorException
	 * @throws RowOutOfSynchException
	 * @throws TableDoesNotExistException
	 * @throws OutOfSynchException
	 * @throws PermissionDeniedException
	 * @throws ClientProtocolException
	 */
	public void updateRows(TableProperties tp, DbTable table, Table rowsToUpdate)
			throws ClientProtocolException, PermissionDeniedException,
			OutOfSynchException, TableDoesNotExistException,
			RowOutOfSynchException, AggregateInternalErrorException,
			ColumnDoesNotExistException, IOException {

		String tableID = String.valueOf(tp.getTableId());
		int modificationNumber = tp.getSyncModificationNumber();
		List<SynchronizedRow> changedRows = new ArrayList<SynchronizedRow>();
		int numRows = rowsToUpdate.getHeight();
		int numCols = rowsToUpdate.getWidth();
		for (int i = 0; i < numRows; i++) {
			SynchronizedRow row = new SynchronizedRow();
			for (int j = 0; j < numCols; j++) {
				String colName = rowsToUpdate.getHeader(j);
				if (colName.equals(DbTable.DB_SYNC_ID)) {
					row.setAggregateRowIdentifier(rowsToUpdate.getData(i, j));
				} else if (colName.equals(DbTable.DB_SYNC_TAG)) {
					row.setRevisionTag(rowsToUpdate.getData(i, j));
				} else if (!isSpecialDbColumn(colName)) {
					row.setValue(colName, rowsToUpdate.getData(i, j));
				}
			}
			changedRows.add(row);
		}
		if (!changedRows.isEmpty())
		{
			Modification mod = api.updateSynchronizedRows(tableID,
					modificationNumber, changedRows);
	
			tp.setSyncModificationNumber(mod.getModificationNumber());
			for (SynchronizedRow row : mod.getRows()) {
			    ContentValues values = new ContentValues();
				values.put(DbTable.DB_SYNC_ID, row.getAggregateRowIdentifier());
				values.put(DbTable.DB_SYNC_TAG, row.getRevisionTag());
				table.actualUpdateRowBySyncId(row.getAggregateRowIdentifier(),
				        values);
			}
		}
	}

	public boolean isSpecialDbColumn(String colName) {
		return colName.equals(DbTable.DB_LAST_MODIFIED_TIME)
				|| colName.equals(DbTable.DB_ROW_ID)
				|| colName.equals(DbTable.DB_SRC_PHONE_NUMBER)
				|| colName.equals(DbTable.DB_SYNC_STATE)
				|| colName.equals(DbTable.DB_SYNC_ID)
				|| colName.equals(DbTable.DB_SYNC_TAG)
				|| colName.equals(DbTable.DB_TRANSACTIONING);
	}

	/**
	 * 
	 * @param tableID
	 * @param modificationNumber
	 * @param rowsToDelete
	 */
	public void deleteRows(TableProperties tp, DbTable table, Table rowsToDelete) {
		// TODO: implement once there is a deleteSynchronizedRows call
		//throw new RuntimeException("unimplemented");
	}

	/**
	 * @param tableID
	 * @throws IOException
	 * @throws AggregateInternalErrorException
	 * @throws TableDoesNotExistException
	 * @throws ClientProtocolException
	 */
	public void removeTableSynchronization(String tableID)
			throws ClientProtocolException, TableDoesNotExistException,
			AggregateInternalErrorException, IOException {
		api.removeTableSynchronization(tableID);
	}

	public int[] getAllRowIds(Table... tables) {
		List<Integer> rowIdsList = new ArrayList<Integer>();
		for (Table table : tables) {
			for (int rowId : table.getRowIds())
				rowIdsList.add(rowId);
		}
		int[] rowIds = new int[rowIdsList.size()];
		for (int i = 0; i < rowIds.length; i++)
			rowIds[i] = rowIdsList.get(i);
		return rowIds;
	}

	public void beginTableTransaction(TableProperties tp) {
		tp.setTransactioning(SyncUtil.Transactioning.TRUE);
	}

	public void endTableTransaction(TableProperties tp) {
		tp.setSyncState(SyncUtil.State.REST);
		tp.setTransactioning(SyncUtil.Transactioning.FALSE);
	}

	public void beginRowsTransaction(DbTable table, int[] rowIds) {
		updateRowsTransactioning(table, rowIds, SyncUtil.Transactioning.TRUE);
	}

	public void endRowsTransaction(DbTable table, int[] rowIds) {
		updateRowsState(table, rowIds, SyncUtil.State.REST);
		updateRowsTransactioning(table, rowIds, SyncUtil.Transactioning.FALSE);
	}

	public void updateRowsState(DbTable table, int[] rowIds, int state) {
		ContentValues values = new ContentValues();
		values.put(DbTable.DB_SYNC_STATE, state);
		for (int rowId : rowIds) {
			table.actualUpdateRowByRowId(rowId, values);
		}
	}

	public void updateRowsTransactioning(DbTable table, int[] rowIds,
			int transactioning) {
		ContentValues values = new ContentValues();
		values.put(DbTable.DB_TRANSACTIONING, String.valueOf(transactioning));
		for (int rowId : rowIds) {
			table.actualUpdateRowByRowId(rowId, values);
		}
	}
}
