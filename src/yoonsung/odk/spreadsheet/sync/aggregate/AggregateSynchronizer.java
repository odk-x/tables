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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import yoonsung.odk.spreadsheet.data.ColumnProperties.ColumnType;
import yoonsung.odk.spreadsheet.sync.IncomingModification;
import yoonsung.odk.spreadsheet.sync.Modification;
import yoonsung.odk.spreadsheet.sync.SyncRow;
import yoonsung.odk.spreadsheet.sync.Synchronizer;
import android.util.Log;

/**
 * Implementation of {@link Synchronizer} for ODK Aggregate.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public class AggregateSynchronizer implements Synchronizer {

  private static final String TAG = AggregateSynchronizer.class.getSimpleName();

  private static final Map<Integer, Column.ColumnType> types = new HashMap<Integer, Column.ColumnType>() {
    {
      put(ColumnType.COLLECT_FORM, Column.ColumnType.STRING);
      put(ColumnType.DATE, Column.ColumnType.STRING);
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

  private final RestTemplate rt;
  private final HttpHeaders requestHeaders;
  private final URI baseUri;
  private final Map<String, TableResource> resources;

  public AggregateSynchronizer(String aggregateUri) {
    URI uri = URI.create(aggregateUri);
    uri = uri.resolve("/odktables/tables/");
    this.baseUri = uri;
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

    this.resources = new HashMap<String, TableResource>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#createTable(java.lang
   * .String, java.util.List)
   */
  @Override
  public String createTable(String tableId, Map<String, Integer> cols) {
    List<Column> columns = new ArrayList<Column>();
    for (Entry<String, Integer> col : cols.entrySet()) {
      String name = col.getKey();
      Column.ColumnType type = types.get(col.getValue());
      Column column = new Column(name, type);
      columns.add(column);
    }

    URI uri = baseUri.resolve(tableId);

    TableDefinition definition = new TableDefinition(columns);
    HttpEntity<TableDefinition> requestEntity = new HttpEntity<TableDefinition>(definition,
        requestHeaders);

    ResponseEntity<TableResource> resourceEntity = rt.exchange(uri, HttpMethod.PUT, requestEntity,
        TableResource.class);
    TableResource resource = resourceEntity.getBody();

    this.resources.put(resource.getTableId(), resource);

    return resource.getDataEtag();
  }

  private TableResource getResource(String tableId) {
    if (resources.containsKey(tableId)) {
      return resources.get(tableId);
    } else {
      return refreshResource(tableId);
    }
  }

  private TableResource refreshResource(String tableId) {
    URI uri = baseUri.resolve(tableId);
    TableResource resource = rt.getForObject(uri, TableResource.class);
    resources.put(resource.getTableId(), resource);
    return resource;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#deleteTable(java.lang
   * .String)
   */
  @Override
  public void deleteTable(String tableId) {
    rt.delete(baseUri.resolve(tableId));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#getUpdates(java.lang
   * .String, java.lang.String)
   */
  @Override
  public IncomingModification getUpdates(String tableId, String currentSyncTag) {
    IncomingModification modification = new IncomingModification();
    TableResource resource = refreshResource(tableId);
    String newSyncTag = resource.getDataEtag();

    if (newSyncTag.equals(currentSyncTag)) {
      modification.setTableSyncTag(currentSyncTag);
      return modification;
    }

    String diffUri = resource.getDiffUri();
    URI url = URI.create(diffUri + "?data_etag=" + currentSyncTag).normalize();
    @SuppressWarnings("unchecked")
    List<RowResource> rows = rt.getForObject(url, List.class);

    List<SyncRow> syncRows = new ArrayList<SyncRow>();
    for (RowResource row : rows) {
      SyncRow syncRow = new SyncRow(row.getRowId(), row.getRowEtag(), row.isDeleted(),
          row.getValues());
      syncRows.add(syncRow);
    }
    modification.setRows(syncRows);
    modification.setTableSyncTag(newSyncTag);
    return modification;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#insertRows(java.lang
   * .String, java.util.List)
   */
  @Override
  public Modification insertRows(String tableId, List<SyncRow> rowsToInsert) {
    List<Row> newRows = new ArrayList<Row>();
    for (SyncRow syncRow : rowsToInsert) {
      Row row = Row.forInsert(syncRow.getRowId(), null, syncRow.getValues());
      newRows.add(row);
    }
    return insertOrUpdateRows(tableId, newRows);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#updateRows(java.lang
   * .String, java.util.List)
   */
  @Override
  public Modification updateRows(String tableId, List<SyncRow> rowsToUpdate) {
    List<Row> changedRows = new ArrayList<Row>();
    for (SyncRow syncRow : rowsToUpdate) {
      Row row = Row.forInsert(syncRow.getRowId(), syncRow.getSyncTag(), syncRow.getValues());
      changedRows.add(row);
    }
    return insertOrUpdateRows(tableId, changedRows);
  }

  private Modification insertOrUpdateRows(String tableId, List<Row> rows) {
    TableResource resource = getResource(tableId);
    Map<String, String> syncTags = new HashMap<String, String>();
    if (!rows.isEmpty()) {
      for (Row row : rows) {
        URI url = URI.create(resource.getDataUri() + "/" + row.getRowId()).normalize();
        HttpEntity<Row> requestEntity = new HttpEntity<Row>(row, requestHeaders);
        try {
          ResponseEntity<RowResource> insertedEntity = rt.exchange(url, HttpMethod.PUT,
              requestEntity, RowResource.class);
          RowResource inserted = insertedEntity.getBody();
          syncTags.put(inserted.getRowId(), inserted.getRowEtag());
        } catch (HttpClientErrorException e) {
          Log.e(TAG, e.getResponseBodyAsString());
          throw e;
        } catch (HttpServerErrorException e) {
          Log.e(TAG, e.getResponseBodyAsString());
          throw e;
        }
      }
      resource = refreshResource(tableId);
    }

    Modification modification = new Modification();
    modification.setSyncTags(syncTags);
    modification.setTableSyncTag(resource.getDataEtag());

    return modification;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#deleteRows(java.lang
   * .String, java.util.List)
   */
  @Override
  public String deleteRows(String tableId, List<String> rowIds) {
    TableResource resource = getResource(tableId);
    if (!rowIds.isEmpty()) {
      for (String rowId : rowIds) {
        URI url = URI.create(resource.getDataUri() + "/" + rowId).normalize();
        rt.delete(url);
      }
      resource = refreshResource(tableId);
    }
    return resource.getDataEtag();
  }
}
