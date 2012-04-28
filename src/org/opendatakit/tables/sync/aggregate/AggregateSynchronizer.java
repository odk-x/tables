package org.opendatakit.tables.sync.aggregate;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendatakit.aggregate.odktables.entity.Column;
import org.opendatakit.aggregate.odktables.entity.Row;
import org.opendatakit.aggregate.odktables.entity.TableProperties;
import org.opendatakit.aggregate.odktables.entity.api.PropertiesResource;
import org.opendatakit.aggregate.odktables.entity.api.RowResource;
import org.opendatakit.aggregate.odktables.entity.api.TableDefinition;
import org.opendatakit.aggregate.odktables.entity.api.TableResource;
import org.opendatakit.aggregate.odktables.entity.serialization.ListConverter;
import org.opendatakit.tables.data.ColumnProperties.ColumnType;
import org.opendatakit.tables.sync.IncomingModification;
import org.opendatakit.tables.sync.Modification;
import org.opendatakit.tables.sync.SyncRow;
import org.opendatakit.tables.sync.Synchronizer;
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
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.SimpleXmlHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;


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

  public AggregateSynchronizer(String aggregateUri, String accessToken) {
    URI uri = URI.create(aggregateUri).normalize();
    uri = uri.resolve("/odktables/tables/").normalize();
    this.baseUri = uri;

    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(accessToken));

    this.rt = new RestTemplate();
    this.rt.setErrorHandler(new AggregateResponseErrorHandler());
    this.rt.setInterceptors(interceptors);

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

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, String> getTables() throws IOException {
    Map<String, String> tables = new HashMap<String, String>();

    List<TableResource> tableResources;
    try {
      tableResources = rt.getForObject(baseUri, List.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }

    for (TableResource tableResource : tableResources)
      tables.put(tableResource.getTableId(), tableResource.getTableName());

    return tables;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#createTable(java.lang
   * .String, java.util.List)
   */
  @Override
  public String createTable(String tableId, String tableName, Map<String, Integer> cols,
      String tableProperties) throws IOException {
    // create column objects
    List<Column> columns = new ArrayList<Column>();
    for (Entry<String, Integer> col : cols.entrySet()) {
      String name = col.getKey();
      Column.ColumnType type = types.get(col.getValue());
      Column column = new Column(name, type);
      columns.add(column);
    }

    // build request
    URI uri = baseUri.resolve(tableId);
    TableDefinition definition = new TableDefinition(tableName, columns, tableProperties);
    HttpEntity<TableDefinition> requestEntity = new HttpEntity<TableDefinition>(definition,
        requestHeaders);

    // create table
    ResponseEntity<TableResource> resourceEntity;
    try {
      resourceEntity = rt.exchange(uri, HttpMethod.PUT, requestEntity, TableResource.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }
    TableResource resource = resourceEntity.getBody();

    // save resource
    this.resources.put(resource.getTableId(), resource);

    // return sync tag
    SyncTag syncTag = new SyncTag(resource.getDataEtag(), resource.getPropertiesEtag());
    return syncTag.toString();
  }

  private TableResource getResource(String tableId) throws IOException {
    if (resources.containsKey(tableId)) {
      return resources.get(tableId);
    } else {
      return refreshResource(tableId);
    }
  }

  private TableResource refreshResource(String tableId) throws IOException {
    URI uri = baseUri.resolve(tableId);
    TableResource resource;
    try {
      resource = rt.getForObject(uri, TableResource.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }
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
  @SuppressWarnings("unchecked")
  @Override
  public IncomingModification getUpdates(String tableId, String currentSyncTag) throws IOException {
    IncomingModification modification = new IncomingModification();

    // get current and new sync tags
    TableResource resource = refreshResource(tableId);
    SyncTag currentTag;
    if (currentSyncTag != null)
      currentTag = SyncTag.valueOf(currentSyncTag);
    else
      currentTag = new SyncTag("", "");
    SyncTag newTag = new SyncTag(resource.getDataEtag(), resource.getPropertiesEtag());

    // stop now if there are no updates
    if (newTag.equals(currentTag)) {
      modification.setTableSyncTag(currentTag.toString());
      modification.setTablePropertiesChanged(false);
      return modification;
    }

    // get data updates
    if (!newTag.getDataEtag().equals(currentTag.getDataEtag())) {
      URI url;
      if (currentSyncTag == null) {
        url = URI.create(resource.getDataUri());
      } else {
        String diffUri = resource.getDiffUri();
        url = URI.create(diffUri + "?data_etag=" + currentTag.getDataEtag()).normalize();
      }
      List<RowResource> rows;
      try {
        rows = rt.getForObject(url, List.class);
      } catch (ResourceAccessException e) {
        throw new IOException(e.getMessage());
      }

      List<SyncRow> syncRows = new ArrayList<SyncRow>();
      for (RowResource row : rows) {
        SyncRow syncRow = new SyncRow(row.getRowId(), row.getRowEtag(), row.isDeleted(),
            row.getValues());
        syncRows.add(syncRow);
      }
      modification.setRows(syncRows);
    }

    // get properties updates
    if (!newTag.getPropertiesEtag().equals(currentTag.getPropertiesEtag())) {
      PropertiesResource properties;
      try {
        properties = rt.getForObject(resource.getPropertiesUri(), PropertiesResource.class);
      } catch (ResourceAccessException e) {
        throw new IOException(e.getMessage());
      }

      modification.setTablePropertiesChanged(true);
      modification.setTableProperties(properties.getMetadata());
    }

    modification.setTableSyncTag(newTag.toString());
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
  public Modification insertRows(String tableId, String currentSyncTag, List<SyncRow> rowsToInsert)
      throws IOException {
    List<Row> newRows = new ArrayList<Row>();
    for (SyncRow syncRow : rowsToInsert) {
      Row row = Row.forInsert(syncRow.getRowId(), null, syncRow.getValues());
      newRows.add(row);
    }
    return insertOrUpdateRows(tableId, currentSyncTag, newRows);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#updateRows(java.lang
   * .String, java.util.List)
   */
  @Override
  public Modification updateRows(String tableId, String currentSyncTag, List<SyncRow> rowsToUpdate)
      throws IOException {
    List<Row> changedRows = new ArrayList<Row>();
    for (SyncRow syncRow : rowsToUpdate) {
      Row row = Row.forUpdate(syncRow.getRowId(), syncRow.getSyncTag(), syncRow.getValues());
      changedRows.add(row);
    }
    return insertOrUpdateRows(tableId, currentSyncTag, changedRows);
  }

  private Modification insertOrUpdateRows(String tableId, String currentSyncTag, List<Row> rows)
      throws IOException {
    TableResource resource = getResource(tableId);
    SyncTag currentTag = SyncTag.valueOf(currentSyncTag);
    Map<String, String> syncTags = new HashMap<String, String>();

    if (!rows.isEmpty()) {
      for (Row row : rows) {
        URI url = URI.create(resource.getDataUri() + "/" + row.getRowId()).normalize();
        HttpEntity<Row> requestEntity = new HttpEntity<Row>(row, requestHeaders);
        ResponseEntity<RowResource> insertedEntity;
        try {
          insertedEntity = rt.exchange(url, HttpMethod.PUT, requestEntity, RowResource.class);
        } catch (ResourceAccessException e) {
          throw new IOException(e.getMessage());
        }
        RowResource inserted = insertedEntity.getBody();
        syncTags.put(inserted.getRowId(), inserted.getRowEtag());
      }
      // TODO: figure out error recovery if crash here
      resource = refreshResource(tableId);
    }

    SyncTag newTag = new SyncTag(resource.getDataEtag(), currentTag.getPropertiesEtag());

    Modification modification = new Modification();
    modification.setSyncTags(syncTags);
    modification.setTableSyncTag(newTag.toString());

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
  public String deleteRows(String tableId, String currentSyncTag, List<String> rowIds)
      throws IOException {
    TableResource resource = getResource(tableId);
    SyncTag currentTag = SyncTag.valueOf(currentSyncTag);

    if (!rowIds.isEmpty()) {
      for (String rowId : rowIds) {
        URI url = URI.create(resource.getDataUri() + "/" + rowId).normalize();
        try {
          rt.delete(url);
        } catch (ResourceAccessException e) {
          throw new IOException(e.getMessage());
        }
      }
      resource = refreshResource(tableId);
    }

    SyncTag newTag = new SyncTag(resource.getDataEtag(), currentTag.getPropertiesEtag());
    return newTag.toString();
  }

  @Override
  public String setTableProperties(String tableId, String currentSyncTag, String tableName,
      String tableProperties) throws IOException {
    TableResource resource = getResource(tableId);
    SyncTag currentTag = SyncTag.valueOf(currentSyncTag);

    // put new properties
    TableProperties properties = new TableProperties(currentTag.getPropertiesEtag(), tableName,
        tableProperties);
    HttpEntity<TableProperties> entity = new HttpEntity<TableProperties>(properties, requestHeaders);
    ResponseEntity<PropertiesResource> updatedEntity;
    try {
      updatedEntity = rt.exchange(resource.getPropertiesUri(), HttpMethod.PUT, entity,
          PropertiesResource.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }
    PropertiesResource propsResource = updatedEntity.getBody();

    SyncTag newTag = new SyncTag(currentTag.getDataEtag(), propsResource.getPropertiesEtag());
    return newTag.toString();
  }
}
