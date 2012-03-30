package yoonsung.odk.spreadsheet.sync.aggregate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendatakit.aggregate.odktables.entity.Column;
import org.opendatakit.aggregate.odktables.entity.Row;
import org.opendatakit.aggregate.odktables.entity.api.RowResource;
import org.opendatakit.aggregate.odktables.entity.api.TableDefinition;
import org.opendatakit.aggregate.odktables.entity.api.TableResource;
import org.opendatakit.aggregate.odktables.entity.serialization.ListConverter;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.SimpleXmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.ColumnProperties.ColumnType;
import yoonsung.odk.spreadsheet.data.DataUtil;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Table;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.sync.SyncUtil;
import android.content.ContentValues;
import android.content.SyncResult;
import android.util.Log;

public class AggregateSyncProcessor {

	private static final String TAG = AggregateSyncProcessor.class
			.getSimpleName();

	private static final Map<Integer, Column.ColumnType> types = new HashMap<Integer, Column.ColumnType>() {
		{
			put(ColumnType.COLLECT_FORM, Column.ColumnType.STRING);
			put(ColumnType.DATE, Column.ColumnType.DATETIME);
			put(ColumnType.DATE_RANGE, Column.ColumnType.STRING);
			put(ColumnType.FILE, Column.ColumnType.STRING);
			put(ColumnType.MC_OPTIONS, Column.ColumnType.STRING);
			put(ColumnType.NONE, Column.ColumnType.STRING);
			put(ColumnType.NUMBER, Column.ColumnType.DECIMAL);
			put(ColumnType.PHONE_NUMBER, Column.ColumnType.STRING);
			put(ColumnType.TABLE_JOIN, Column.ColumnType.STRING);
			put(ColumnType.TEXT, Column.ColumnType.STRING);
		}
		private static final long serialVersionUID = 1L;
	};

	private final DataUtil du;
	private final RestTemplate rt;
	private final URI baseUri;
	private final DbHelper helper;
	private final SyncResult syncResult;
	private final HttpHeaders requestHeaders;

	public AggregateSyncProcessor(String aggregateUri, DbHelper helper,
			SyncResult syncResult) {
		// this.baseUri = UriBuilder.fromUri(aggregateUri).path("odktables")
		// .path("tables").build();
		this.du = DataUtil.getDefaultDataUtil();
		URI uri = URI.create(aggregateUri);
		uri = uri.resolve("/odktables/tables/");
		this.baseUri = uri;
		this.helper = helper;
		this.syncResult = syncResult;

		// ServiceFinder.setIteratorProvider(new Buscador());
		// DefaultApacheHttpClient4Config config = new
		// DefaultApacheHttpClient4Config();
		// config.getClasses().add(SimpleXMLMessageReaderWriter.class);
		// this.c = ApacheHttpClient4.create(config);
		this.rt = new RestTemplate();

		Registry registry = new Registry();
		Strategy strategy = new RegistryStrategy(registry);
		Serializer serializer = new Persister(strategy);
		ListConverter converter = new ListConverter(serializer);
		try {
			registry.bind(List.class, converter);
			registry.bind(ArrayList.class, converter);
			registry.bind(LinkedList.class, converter);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register list converters!", e);
		}
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		converters.add(new SimpleXmlHttpMessageConverter(serializer));
		this.rt.setMessageConverters(converters);

		List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
		acceptableMediaTypes.add(new MediaType("text", "xml"));

		this.requestHeaders = new HttpHeaders();
		this.requestHeaders.setAccept(acceptableMediaTypes);
		this.requestHeaders.setContentType(new MediaType("text", "xml"));
	}

	public void synchronize() {
		TableProperties[] tps = TableProperties
				.getTablePropertiesForAll(helper);
		for (TableProperties tp : tps) {
			if (tp.getSyncState() != SyncUtil.State.REST) {
				synchronizeTable(tp);
			}
		}
		TableProperties[] deleting = TableProperties
				.getTablePropertiesForDeleting(helper);
		for (TableProperties tp : deleting) {
			synchronizeTable(tp);
		}
	}

	public void synchronizeTable(TableProperties tp) {
		String tableId = tp.getTableId();
		DbTable table = DbTable.getDbTable(helper, tableId);

		beginTableTransaction(tp);

		TableResource resource;
		Table rowsToInsert;
		Table rowsToUpdate;
		Table rowsToDelete;
		boolean success = true;

		switch (tp.getSyncState()) {
		case SyncUtil.State.INSERTING:
			rowsToInsert = getRows(table, SyncUtil.State.INSERTING);

			beginRowsTransaction(table, rowsToInsert.getRowIds());

			try {
				resource = createTable(tp);
				insertRows(tp, table, rowsToInsert, resource);
				success = true;
			} catch (Exception e) {
				Log.e(TAG, "Unexpected exception in synchronize on table: "
						+ tableId, e);
				success = false;
			}

			endRowsTransaction(table, rowsToInsert.getRowIds(), success);

			break;
		case SyncUtil.State.UPDATING:
			resource = getTableResource(tableId);
			updateFromServer(tp, table, resource);

			rowsToInsert = getRows(table, SyncUtil.State.INSERTING);
			rowsToUpdate = getRows(table, SyncUtil.State.UPDATING);
			rowsToDelete = getRows(table, SyncUtil.State.DELETING);

			String[] rowIds = getAllRowIds(rowsToInsert, rowsToUpdate,
					rowsToDelete);
			beginRowsTransaction(table, rowIds);

			try {
				resource = insertRows(tp, table, rowsToInsert, resource);
				resource = updateRows(tp, table, rowsToUpdate, resource);
				resource = deleteRows(tp, table, rowsToDelete, resource);
				for (String rowId : rowsToDelete.getRowIds()) {
					table.deleteRowActual(rowId);
					syncResult.stats.numDeletes++;
					syncResult.stats.numEntries++;
				}
				success = true;
			} catch (Exception e) {
				Log.e(TAG, "Unexpected exception in synchronize on table: "
						+ tableId, e);
				success = false;
			}

			rowIds = getAllRowIds(rowsToInsert, rowsToUpdate);
			endRowsTransaction(table, rowIds, success);

			break;
		case SyncUtil.State.DELETING:
			rt.delete(baseUri.resolve(tableId));
			tp.deleteTableActual();
			syncResult.stats.numDeletes++;
			syncResult.stats.numEntries++;

			break;
		}
		tp.setLastSyncTime(du.formatNowForDb());
		endTableTransaction(tp, success);
	}

	public TableResource getTableResource(String tableId) {
		// UriBuilder ub = UriBuilder.fromUri(baseUri);
		// WebResource r = c.resource(ub.path(tableId).build());
		// TableResource resource = r.accept(MediaType.TEXT_XML)
		// .type(MediaType.TEXT_XML).get(TableResource.class);
		URI uri = baseUri.resolve(tableId);
		TableResource resource = rt.getForObject(uri, TableResource.class);
		return resource;
	}

	public void updateFromServer(TableProperties tp, DbTable table,
			TableResource resource) {
		String dataEtag = String.valueOf(tp.getSyncDataEtag());
		String newDataEtag = resource.getDataEtag();

		if (newDataEtag.equals(dataEtag))
			return;

		String diffUri = resource.getDiffUri();
		// WebResource r = c.resource(UriBuilder.fromUri(diffUri)
		// .queryParam("data_etag", tp.getSyncDataEtag()).build());

		// List<RowResource> rows = r.accept(MediaType.TEXT_XML)
		// .type(MediaType.TEXT_XML).get(ROW_RESOURCE_LIST);
		URI url = URI.create(diffUri + "?data_etag=" + tp.getSyncDataEtag())
				.normalize();
		List<RowResource> rows = rt.getForObject(url, List.class);

		Table allRowIds = table.getRaw(new String[] { DbTable.DB_ROW_ID,
				DbTable.DB_SYNC_STATE }, null, null, null);

		List<RowResource> rowsToConflict = new ArrayList<RowResource>();
		List<RowResource> rowsToUpdate = new ArrayList<RowResource>();
		List<RowResource> rowsToInsert = new ArrayList<RowResource>();
		List<RowResource> rowsToDelete = new ArrayList<RowResource>();

		for (RowResource row : rows) {
			boolean found = false;
			for (int i = 0; i < allRowIds.getHeight(); i++) {
				String rowId = allRowIds.getData(i, 0);
				int state = Integer.parseInt(allRowIds.getData(i, 1));
				if (row.getRowId().equals(rowId)) {
					found = true;
					if (state == SyncUtil.State.REST) {
						if (row.isDeleted())
							rowsToDelete.add(row);
						else
							rowsToUpdate.add(row);
					} else {
						rowsToConflict.add(row);
					}
				}
			}
			if (!found)
				rowsToInsert.add(row);
		}

		// TODO: how to conflict?
		// for (RowResource row : rowsToConflict) {
		// ContentValues values = new ContentValues();
		//
		// values.put(DbTable.DB_ROW_ID, row.getRowId());
		// values.put(DbTable.DB_SYNC_TAG, row.getRowEtag());
		// values.put(DbTable.DB_SYNC_STATE,
		// String.valueOf(SyncUtil.State.CONFLICTING));
		// values.put(DbTable.DB_TRANSACTIONING,
		// String.valueOf(SyncUtil.Transactioning.FALSE));
		// table.actualUpdateRowByRowId(row.getRowId(), values);
		//
		// for (Entry<String, String> entry : row.getValues().entrySet())
		// values.put(entry.getKey(), entry.getValue());
		//
		// table.actualAddRow(values);
		// syncResult.stats.numConflictDetectedExceptions++;
		// syncResult.stats.numEntries += 2;
		// }

		for (RowResource row : rowsToUpdate) {
			ContentValues values = new ContentValues();

			values.put(DbTable.DB_SYNC_TAG, row.getRowEtag());
			values.put(DbTable.DB_SYNC_STATE,
					String.valueOf(SyncUtil.State.REST));
			values.put(DbTable.DB_TRANSACTIONING,
					String.valueOf(SyncUtil.Transactioning.FALSE));

			for (Entry<String, String> entry : row.getValues().entrySet())
				values.put(entry.getKey(), entry.getValue());

			table.actualUpdateRowByRowId(row.getRowId(), values);
			syncResult.stats.numUpdates++;
			syncResult.stats.numEntries++;
		}

		for (RowResource row : rowsToInsert) {
			ContentValues values = new ContentValues();

			values.put(DbTable.DB_ROW_ID, row.getRowId());
			values.put(DbTable.DB_SYNC_TAG, row.getRowEtag());
			values.put(DbTable.DB_SYNC_STATE, SyncUtil.State.REST);
			values.put(DbTable.DB_TRANSACTIONING, SyncUtil.Transactioning.FALSE);

			for (Entry<String, String> entry : row.getValues().entrySet())
				values.put(entry.getKey(), entry.getValue());

			table.actualAddRow(values);
			syncResult.stats.numInserts++;
			syncResult.stats.numEntries++;
		}

		for (RowResource row : rowsToDelete) {
			table.deleteRowActual(row.getRowId());
			syncResult.stats.numDeletes++;
		}

		tp.setSyncDataEtag(newDataEtag);
	}

	public Table getRows(DbTable table, int state) {
		Table rows = table
				.getRaw(null,
						new String[] { DbTable.DB_SYNC_STATE,
								DbTable.DB_TRANSACTIONING },
						new String[] { String.valueOf(state),
								String.valueOf(SyncUtil.Transactioning.FALSE) },
						null);
		return rows;
	}

	public TableResource createTable(TableProperties tp) {
		String tableId = String.valueOf(tp.getTableId());
		ColumnProperties[] colProps = tp.getColumns();

		List<Column> columns = new ArrayList<Column>();
		for (ColumnProperties colProp : colProps) {
			String name = colProp.getColumnDbName();
			Column.ColumnType type = types.get(colProp.getColumnType());
			Column column = new Column(name, type);
			columns.add(column);
		}

		// UriBuilder ub = UriBuilder.fromUri(baseUri);
		// WebResource r = c.resource(ub.path(tableId).build());
		// TableResource resource = r.accept(MediaType.TEXT_XML)
		// .type(MediaType.TEXT_XML)
		// .put(TableResource.class, new TableDefinition(columns));
		URI uri = baseUri.resolve(tableId);

		TableDefinition definition = new TableDefinition(columns);
		HttpEntity<TableDefinition> requestEntity = new HttpEntity<TableDefinition>(
				definition, requestHeaders);

		ResponseEntity<TableResource> resourceEntity = rt.exchange(uri,
				HttpMethod.PUT, requestEntity, TableResource.class);
		TableResource resource = resourceEntity.getBody();

		tp.setSyncDataEtag(resource.getDataEtag());
		return resource;
	}

	public TableResource insertRows(TableProperties tp, DbTable table,
			Table rowsToInsert, TableResource resource) {

		List<Row> newRows = new ArrayList<Row>();
		int numRows = rowsToInsert.getHeight();
		int numCols = rowsToInsert.getWidth();
		for (int i = 0; i < numRows; i++) {
			String rowId = String.valueOf(rowsToInsert.getRowId(i));
			Map<String, String> values = new HashMap<String, String>();
			for (int j = 0; j < numCols; j++) {
				String colName = rowsToInsert.getHeader(j);
				if (!isSpecialDbColumn(colName))
					values.put(colName, rowsToInsert.getData(i, j));
			}
			Row row = Row.forInsert(rowId, null, values);
			newRows.add(row);
		}

		return insertOrUpdateRows(newRows, tp, table, resource);
	}

	public TableResource updateRows(TableProperties tp, DbTable table,
			Table rowsToUpdate, TableResource resource) {

		List<Row> changedRows = new ArrayList<Row>();
		int numRows = rowsToUpdate.getHeight();
		int numCols = rowsToUpdate.getWidth();
		for (int i = 0; i < numRows; i++) {
			String rowId = null;
			String rowEtag = null;
			Map<String, String> values = new HashMap<String, String>();
			for (int j = 0; j < numCols; j++) {
				String colName = rowsToUpdate.getHeader(j);
				if (colName.equals(DbTable.DB_ROW_ID)) {
					rowId = rowsToUpdate.getData(i, j);
				} else if (colName.equals(DbTable.DB_SYNC_TAG)) {
					rowEtag = rowsToUpdate.getData(i, j);
				} else if (!isSpecialDbColumn(colName)) {
					values.put(colName, rowsToUpdate.getData(i, j));
				}
			}
			Row row = Row.forUpdate(rowId, rowEtag, values);
			changedRows.add(row);
		}

		return insertOrUpdateRows(changedRows, tp, table, resource);
	}

	private TableResource insertOrUpdateRows(List<Row> rows,
			TableProperties tp, DbTable table, TableResource resource) {
		if (!rows.isEmpty()) {
			for (Row row : rows) {
				// UriBuilder builder =
				// UriBuilder.fromUri(resource.getDataUri())
				// .path(row.getRowId());
				// WebResource r = c.resource(builder.build());
				// RowResource inserted = r.accept(MediaType.TEXT_XML)
				// .type(MediaType.TEXT_XML).put(RowResource.class, row);
				URI url = URI.create(
						resource.getDataUri() + "/" + row.getRowId())
						.normalize();
				HttpEntity<Row> requestEntity = new HttpEntity<Row>(row,
						requestHeaders);
				ResponseEntity<RowResource> insertedEntity = rt.exchange(url,
						HttpMethod.PUT, requestEntity, RowResource.class);
				RowResource inserted = insertedEntity.getBody();

				ContentValues values = new ContentValues();
				values.put(DbTable.DB_SYNC_TAG, inserted.getRowEtag());

				table.actualUpdateRowByRowId(inserted.getRowId(), values);
			}

			// resource = c.resource(resource.getSelfUri())
			// .accept(MediaType.TEXT_XML).get(TableResource.class);
			resource = rt.getForObject(resource.getSelfUri(),
					TableResource.class);
			tp.setSyncDataEtag(resource.getDataEtag());
		}
		return resource;
	}

	public boolean isSpecialDbColumn(String colName) {
		return colName.equals(DbTable.DB_ROW_ID)
				|| colName.equals(DbTable.DB_SYNC_STATE)
				|| colName.equals(DbTable.DB_SYNC_TAG)
				|| colName.equals(DbTable.DB_TRANSACTIONING);
	}

	public TableResource deleteRows(TableProperties tp, DbTable table,
			Table rowsToDelete, TableResource resource) {
		List<String> rowIds = new ArrayList<String>();
		int numRows = rowsToDelete.getHeight();
		int numCols = rowsToDelete.getWidth();
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				String colName = rowsToDelete.getHeader(j);
				if (colName.equals(DbTable.DB_ROW_ID)) {
					rowIds.add(rowsToDelete.getData(i, j));
				}
			}
		}

		if (!rowIds.isEmpty()) {
			for (String rowId : rowIds) {
				// UriBuilder builder =
				// UriBuilder.fromUri(resource.getDataUri())
				// .path(rowId);
				// WebResource r = c.resource(builder.build());
				// r.delete();
				URI url = URI.create(resource.getDataUri() + "/" + rowId)
						.normalize();
				rt.delete(url);
			}
			// resource = c.resource(resource.getSelfUri())
			// .accept(MediaType.TEXT_XML).type(MediaType.TEXT_XML)
			// .get(TableResource.class);
			resource = rt.getForObject(resource.getSelfUri(),
					TableResource.class);
			tp.setSyncDataEtag(resource.getDataEtag());
		}

		return resource;
	}

	public String[] getAllRowIds(Table... tables) {
		List<String> rowIdsList = new ArrayList<String>();
		for (Table table : tables) {
			for (String rowId : table.getRowIds())
				rowIdsList.add(rowId);
		}
		String[] rowIds = new String[rowIdsList.size()];
		for (int i = 0; i < rowIds.length; i++)
			rowIds[i] = rowIdsList.get(i);
		return rowIds;
	}

	public void beginTableTransaction(TableProperties tp) {
		tp.setTransactioning(SyncUtil.Transactioning.TRUE);
	}

	public void endTableTransaction(TableProperties tp, boolean success) {
		if (success)
			tp.setSyncState(SyncUtil.State.REST);
		tp.setTransactioning(SyncUtil.Transactioning.FALSE);
	}

	public void beginRowsTransaction(DbTable table, String[] rowIds) {
		updateRowsTransactioning(table, rowIds, SyncUtil.Transactioning.TRUE);
	}

	public void endRowsTransaction(DbTable table, String[] rowIds,
			boolean success) {
		if (success)
			updateRowsState(table, rowIds, SyncUtil.State.REST);
		updateRowsTransactioning(table, rowIds, SyncUtil.Transactioning.FALSE);
	}

	public void updateRowsState(DbTable table, String[] rowIds, int state) {
		ContentValues values = new ContentValues();
		values.put(DbTable.DB_SYNC_STATE, state);
		for (String rowId : rowIds) {
			table.actualUpdateRowByRowId(rowId, values);
		}
	}

	public void updateRowsTransactioning(DbTable table, String[] rowIds,
			int transactioning) {
		ContentValues values = new ContentValues();
		values.put(DbTable.DB_TRANSACTIONING, String.valueOf(transactioning));
		for (String rowId : rowIds) {
			table.actualUpdateRowByRowId(rowId, values);
		}
	}
}
