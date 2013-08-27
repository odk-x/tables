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
package org.opendatakit.tables.sync.aggregate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.aggregate.odktables.rest.entity.PropertiesResource;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinition;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinitionResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableProperties;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableType;
import org.opendatakit.aggregate.odktables.rest.interceptor.AggregateRequestInterceptor;
import org.opendatakit.aggregate.odktables.rest.serialization.JsonObjectHttpMessageConverter;
import org.opendatakit.aggregate.odktables.rest.serialization.SimpleXMLSerializerForAggregate;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.httpclientandroidlib.HttpResponse;
import org.opendatakit.httpclientandroidlib.HttpStatus;
import org.opendatakit.httpclientandroidlib.client.HttpClient;
import org.opendatakit.httpclientandroidlib.client.methods.HttpGet;
import org.opendatakit.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.opendatakit.httpclientandroidlib.impl.conn.BasicClientConnectionManager;
import org.opendatakit.httpclientandroidlib.params.HttpConnectionParams;
import org.opendatakit.httpclientandroidlib.params.HttpParams;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.sync.IncomingModification;
import org.opendatakit.tables.sync.Modification;
import org.opendatakit.tables.sync.SyncRow;
import org.opendatakit.tables.sync.SyncUtil;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.sync.exceptions.InvalidAuthTokenException;
import org.opendatakit.tables.sync.files.SyncUtilities;
import org.opendatakit.tables.utils.FileUtils;
import org.opendatakit.tables.utils.TableFileUtils;
import org.simpleframework.xml.Serializer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.SimpleXmlHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import android.net.Uri;
import android.os.Debug;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Implementation of {@link Synchronizer} for ODK Aggregate.
 *
 * @author the.dylan.price@gmail.com
 * @author sudar.sam@gmail.com
 *
 */
public class AggregateSynchronizer implements Synchronizer {

  private static final String TAG = AggregateSynchronizer.class.getSimpleName();
  private static final String TOKEN_INFO = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=";

  private static final String FILE_MANIFEST_PATH = "/odktables/filemanifest/";
  private static final String FILES_PATH = "/odktables/files/";
  
  private static final String FILE_MANIFEST_PARAM_APP_ID = "app_id";
  private static final String FILE_MANIFEST_PARAM_TABLE_ID = "table_id";
  private static final String FILE_MANIFEST_PARAM_APP_LEVEL_FILES = 
      "app_level_files";
  /** Value for {@link #FILE_MANIFEST_PARAM_APP_LEVEL_FILES}. */
  private static final String VALUE_TRUE = "true";
    
  // TODO: how do we support new column types without breaking this map???
  // This map should be handled on the aggregate side, not on the Tables side.
  // This is because a column definition stores the type on Aggregate, and that
  // type is what's pulled back down during sync. Therefore you lose 
  // information when you pull back down from the server, as you don't know
  // what it originally was. 
//  public static final Map<ColumnType, Column.ColumnType> types =
//		  				new HashMap<ColumnType, Column.ColumnType>() {
//    {
//        put(ColumnType.NONE, Column.ColumnType.STRING);
//        put(ColumnType.TEXT, Column.ColumnType.STRING);
//        put(ColumnType.INTEGER, Column.ColumnType.INTEGER);
//        put(ColumnType.NUMBER, Column.ColumnType.DECIMAL);
//        put(ColumnType.DATE, Column.ColumnType.STRING);
//        put(ColumnType.DATETIME, Column.ColumnType.STRING);
//        put(ColumnType.TIME, Column.ColumnType.STRING);
//        put(ColumnType.BOOLEAN, Column.ColumnType.BOOLEAN); // TODO: confirm this propagates OK?
//        put(ColumnType.MIMEURI, Column.ColumnType.STRING); // TODO: need File + contentType entry in Aggregate (as JSON in Tables)
//        put(ColumnType.MULTIPLE_CHOICES, Column.ColumnType.STRING); // TODO: should be extra-wide storage or split out in Aggregate???
//        put(ColumnType.GEOPOINT, Column.ColumnType.STRING); // TODO: can we handle this generically?
//      put(ColumnType.DATE_RANGE, Column.ColumnType.STRING); // not in Collect, Aggregate
//      put(ColumnType.PHONE_NUMBER, Column.ColumnType.STRING); // not in Collect, Aggregate
//      put(ColumnType.COLLECT_FORM, Column.ColumnType.STRING); // not in Collect, Aggregate
//
//      // TODO: goes away -- becomes MULTIPLE_CHOICES + item element type
//      put(ColumnType.MC_OPTIONS, Column.ColumnType.STRING); // select1/select - not in Collect, Aggregate
//
//      // TODO: what is this for???
//      put(ColumnType.TABLE_JOIN, Column.ColumnType.STRING);// not in Collect; needs to be in Aggregate
//      
//      put(ColumnType.IMAGEURI, Column.ColumnType.STRING);
//    }
//    private static final long serialVersionUID = 1L;
//  };

  private final RestTemplate rt;
  private final HttpHeaders requestHeaders;
  private final URI baseUri;
  private final Map<String, TableResource> resources;
  /** The uri for the file manifest on aggregate. */
  private final URI mFileManifestUri;
  /** The uri for the files on aggregate. */
  private final URI mFilesUri;
  
  /** 
   * For downloading files. Should eventually probably switch to spring, but
   * it was idiotically complicated.
   */
  private final HttpClient mHttpClient;

  public AggregateSynchronizer(String aggregateUri, String accessToken)
      throws InvalidAuthTokenException {
    URI uri = URI.create(aggregateUri).normalize();
    uri = uri.resolve("/odktables/tables/").normalize();
    this.baseUri = uri;
    
    this.mHttpClient = 
        new DefaultHttpClient(new BasicClientConnectionManager());
    final HttpParams params = mHttpClient.getParams();
    HttpConnectionParams.setConnectionTimeout(params,
        TableFileUtils.HTTP_REQUEST_TIMEOUT_MS);
    HttpConnectionParams.setSoTimeout(params,
        TableFileUtils.HTTP_REQUEST_TIMEOUT_MS);

    URI fileManifestUri = URI.create(aggregateUri).normalize();
    fileManifestUri = fileManifestUri.resolve(FILE_MANIFEST_PATH).normalize();
    this.mFileManifestUri = fileManifestUri;
    URI filesUri = URI.create(aggregateUri).normalize();
    filesUri = filesUri.resolve(FILES_PATH).normalize();
    this.mFilesUri = filesUri;
    
    List<ClientHttpRequestInterceptor> interceptors =
        new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(accessToken));

    this.rt = new RestTemplate();
    this.rt.setInterceptors(interceptors);

    Serializer serializer = SimpleXMLSerializerForAggregate.getSerializer();
    List<HttpMessageConverter<?>> converters =
        new ArrayList<HttpMessageConverter<?>>();
    converters.add(new JsonObjectHttpMessageConverter());
    converters.add(new SimpleXmlHttpMessageConverter(serializer));

    this.rt.setMessageConverters(converters);

    List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
    acceptableMediaTypes.add(new MediaType("text", "xml"));

    this.requestHeaders = new HttpHeaders();
    this.requestHeaders.setAccept(acceptableMediaTypes);
    this.requestHeaders.setContentType(new MediaType("text", "xml"));

    this.resources = new HashMap<String, TableResource>();

    checkAccessToken(accessToken);
  }

  private void checkAccessToken(String accessToken) throws
      InvalidAuthTokenException {
    try {
      rt.getForObject(TOKEN_INFO + accessToken, JsonObject.class);
    } catch (HttpClientErrorException e) {
      Log.e(TAG, "HttpClientErrorException in checkAccessToken");
      JsonParser parser = new JsonParser();
      JsonObject resp = parser.parse(e.getResponseBodyAsString())
          .getAsJsonObject();
      if (resp.has("error") && resp.get("error").getAsString()
          .equals("invalid_token")) {
        throw new InvalidAuthTokenException("Invalid auth token: "
          + accessToken, e);
      }
    }
  }

  /**
   * Return a map of tableId to tableKey.
   */
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
      tables.put(tableResource.getTableId(), tableResource.getTableKey());

    return tables;
  }

  /*
   * (non-Javadoc)
   * @see org.opendatakit.tables.sync.Synchronizer#createTable(j
   * ava.lang.String, java.util.List, java.lang.String, java.lang.String,
   * org.opendatakit.aggregate.odktables.entity.api.TableType,
   * java.lang.String)
   */
  @Override
  public String createTable(String tableId, List<Column> columns,
      String tableKey, String dbTableName, TableType type,
      String tableIdAccessControls) throws IOException {

    // build request
    URI uri = baseUri.resolve(tableId);
    TableDefinition definition =
        new TableDefinition(tableId, columns, tableKey, dbTableName,
            type, tableIdAccessControls);
    HttpEntity<TableDefinition> requestEntity =
        new HttpEntity<TableDefinition>(definition, requestHeaders);

    // create table
    ResponseEntity<TableResource> resourceEntity;
    try {
      // TODO: we also need to put up the key value store/properties.
      resourceEntity = rt.exchange(uri, HttpMethod.PUT, requestEntity,
          TableResource.class);
    } catch (ResourceAccessException e) {
      Log.e(TAG, "ResourceAccessException in createTable");
      throw new IOException(e.getMessage());
    }
    TableResource resource = resourceEntity.getBody();

    // save resource
    this.resources.put(resource.getTableId(), resource);

    // return sync tag
    SyncTag syncTag = new SyncTag(resource.getDataEtag(),
        resource.getPropertiesEtag());
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
  public IncomingModification getUpdates(String tableId, String currentSyncTag)
      throws IOException {
    IncomingModification modification = new IncomingModification();

    // get current and new sync tags
    TableResource resource = refreshResource(tableId);
    SyncTag currentTag;
    if (currentSyncTag != null)
      currentTag = SyncTag.valueOf(currentSyncTag);
    else
      currentTag = new SyncTag("", "");
    // This tag is ultimately returned. May8--make sure it works.
    SyncTag newTag = new SyncTag(resource.getDataEtag(),
        resource.getPropertiesEtag());

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
        url = URI.create(diffUri + "?data_etag=" +
            currentTag.getDataEtag()).normalize();
      }
      List<RowResource> rows;
      try {
        rows = rt.getForObject(url, List.class);
      } catch (ResourceAccessException e) {
        throw new IOException(e.getMessage());
      }

      List<SyncRow> syncRows = new ArrayList<SyncRow>();
      for (RowResource row : rows) {
        SyncRow syncRow = new SyncRow(row.getRowId(), row.getRowEtag(),
            row.isDeleted(), row.getValues());
        syncRows.add(syncRow);
      }
      modification.setRows(syncRows);
    }

    // get properties updates.
    // To do this we first check to see if the properties Etag is up to date.
    // If it is, we can do nothing. If it is out of date, we have to:
    // 1) get a TableDefinitionResource to see if we need to update the table
    // data structure of any of the columns.
    // 2) get a PropertiesResource to get all the key value entries.
    if (!newTag.getPropertiesEtag().equals(currentTag.getPropertiesEtag())) {
      TableDefinitionResource definitionRes;
      PropertiesResource propertiesRes;
      try {
        propertiesRes = rt.getForObject(resource.getPropertiesUri(),
            PropertiesResource.class);
        definitionRes = rt.getForObject(resource.getDefinitionUri(),
            TableDefinitionResource.class);
      } catch (ResourceAccessException e) {
        throw new IOException(e.getMessage());
      }

      modification.setTablePropertiesChanged(true);
      modification.setTableProperties(propertiesRes);
      modification.setTableDefinitionResource(definitionRes);
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
      Row row = Row.forInsert(syncRow.getRowId(), syncRow.getValues());
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
//    SyncTag syncTag = SyncTag.valueOf(currentSyncTag);
    Map<String, String> rowTags = new HashMap<String, String>();
    SyncTag lastKnownServerSyncTag = SyncTag.valueOf(currentSyncTag);
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
        rowTags.put(inserted.getRowId(), inserted.getRowEtag());
        lastKnownServerSyncTag.setDataEtag(row.getDataEtagAtModification());
//        syncTag.incrementDataEtag();
      }
    }

    Modification modification = new Modification();
    modification.setSyncTags(rowTags);
    modification.setTableSyncTag(lastKnownServerSyncTag.toString());

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
    SyncTag syncTag = SyncTag.valueOf(currentSyncTag);
    String lastKnownServerDataTag = null; // the data tag of the whole table.
    if (!rowIds.isEmpty()) {
      for (String rowId : rowIds) {
        URI url = URI.create(resource.getDataUri() + "/" + rowId).normalize();
        try {
          ResponseEntity<String> response = 
              rt.exchange(url, HttpMethod.DELETE, null, String.class);
          lastKnownServerDataTag = response.getBody();
        } catch (ResourceAccessException e) {
          throw new IOException(e.getMessage());
        }
      }
    }
    if (lastKnownServerDataTag == null) {
      // do something--b/c the delete hasn't worked.
      Log.e(TAG, "delete call didn't return a known data etag.");
    }
    syncTag.setDataEtag(lastKnownServerDataTag);
    return syncTag.toString();
  }

  /*
   * (non-Javadoc)
   * @see org.opendatakit.tables.sync.Synchronizer#setTableProperties(
   * java.lang.String, java.lang.String, java.lang.String, java.util.List)
   */
  @Override
  public String setTableProperties(String tableId, String currentSyncTag,
      String tableKey, List<OdkTablesKeyValueStoreEntry> kvsEntries)
          throws IOException {
    TableResource resource = getResource(tableId);
    SyncTag currentTag = SyncTag.valueOf(currentSyncTag);

    // put new properties
    TableProperties properties =
        new TableProperties(currentTag.getPropertiesEtag(), tableId,
            kvsEntries);
    HttpEntity<TableProperties> entity =
        new HttpEntity<TableProperties>(properties, requestHeaders);
    ResponseEntity<PropertiesResource> updatedEntity;
    try {
      updatedEntity = rt.exchange(resource.getPropertiesUri(), HttpMethod.PUT,
          entity, PropertiesResource.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }
    PropertiesResource propsResource = updatedEntity.getBody();

    SyncTag newTag = new SyncTag(currentTag.getDataEtag(),
        propsResource.getPropertiesEtag());
    return newTag.toString();
  }

  @Override
  public void syncAppLevelFiles(boolean pushLocalFiles) throws IOException {
    List<OdkTablesFileManifestEntry> manifest = getAppLevelFileManifest();
    for (OdkTablesFileManifestEntry entry : manifest) {
      compareAndDownloadFile(entry);
    }
    // And now get the files to upload. We only want those that exist on the 
    // device but that do not exist on the manifest.
    Set<String> dirsToExclude = TableFileUtils.getUnsynchedDirectories();
    dirsToExclude.add(TableFileUtils.DIR_TABLES); // no table files.
    String appFolder = 
        ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
    List<String> relativePathsOnDevice = 
        TableFileUtils.getAllFilesUnderFolder(appFolder, dirsToExclude, null);
    List<String> relativePathsToUpload = 
        getFilesToBeUploaded(relativePathsOnDevice, manifest);
    Log.e(TAG, "[syncAppLevelFiles] relativePathsToUpload: " 
        + relativePathsToUpload);
    // and then upload the files.
    if (pushLocalFiles) {
      Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
      for (String relativePath : relativePathsToUpload) {
        String wholePathToFile = appFolder + File.separator + relativePath;
        successfulUploads.put(relativePath, uploadFile(wholePathToFile, 
            relativePath));
      }
    }
  }
  

  @Override
  public void syncAllFiles() throws IOException {
    List<OdkTablesFileManifestEntry> manifest = getFileManifestForAllFiles();
    for (OdkTablesFileManifestEntry entry : manifest) {
      compareAndDownloadFile(entry);
    }
    // And now get the files to upload. We only want those that exist on the 
    // device but that do not exist on the manifest.
    Set<String> dirsToExclude = TableFileUtils.getUnsynchedDirectories();
    String appFolder = 
        ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
    List<String> relativePathsOnDevice = 
        TableFileUtils.getAllFilesUnderFolder(appFolder, dirsToExclude, null);
    List<String> relativePathsToUpload = 
        getFilesToBeUploaded(relativePathsOnDevice, manifest);
    // and then upload the files.
    Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
    for (String relativePath : relativePathsToUpload) {
      String wholePathToFile = appFolder + File.separator + relativePath;
      successfulUploads.put(relativePath, uploadFile(wholePathToFile, 
          relativePath));
    }
  }

  @Override
  public void syncTableFiles(String tableId) throws IOException {
    List<OdkTablesFileManifestEntry> manifest = 
        getTableLevelFileManifest(tableId);
    for (OdkTablesFileManifestEntry entry : manifest) {
      compareAndDownloadFile(entry);
    }
    String appFolder = 
        ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
    String relativePathToTableFolder = TableFileUtils.DIR_TABLES + 
        File.separator + tableId;
    String tableFolder = appFolder + File.separator + 
        relativePathToTableFolder;
    List<String> relativePathsToAppFolderOnDevice = 
        TableFileUtils.getAllFilesUnderFolder(tableFolder, null, appFolder);
    // TODO: relative to folder doesn't match up with the filename given by
    // aggregate, so uploads everything unnecessarily. fix it. fix it fix it fix it!
    List<String> relativePathsToUpload = 
        getFilesToBeUploaded(relativePathsToAppFolderOnDevice, manifest);
    // and then upload the files.
    Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
    for (String relativePath : relativePathsToUpload) {
      String wholePathToFile = appFolder + File.separator + relativePath;
      successfulUploads.put(relativePath, uploadFile(wholePathToFile, 
          relativePath));
    }
  }

  private List<OdkTablesFileManifestEntry> getAppLevelFileManifest() throws 
      JsonParseException, JsonMappingException, IOException {
    Uri.Builder uriBuilder = 
        Uri.parse(mFileManifestUri.toString()).buildUpon();
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_APP_ID, 
        TableFileUtils.ODK_TABLES_APP_NAME);
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_APP_LEVEL_FILES,
        VALUE_TRUE);
    RestTemplate rt = SyncUtil.getRestTemplateForString();
    ResponseEntity<String> responseEntity = 
        rt.exchange(uriBuilder.build().toString(), HttpMethod.GET, null, 
            String.class);
    return SyncUtilities.getManifestEntriesFromResponse(
        responseEntity.getBody());
  }

  private List<OdkTablesFileManifestEntry> getTableLevelFileManifest(
      String tableId) throws JsonParseException, JsonMappingException, 
      IOException {
    Uri.Builder uriBuilder = 
        Uri.parse(mFileManifestUri.toString()).buildUpon();
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_APP_ID, 
        TableFileUtils.ODK_TABLES_APP_NAME);
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_TABLE_ID, tableId);
    RestTemplate rt = SyncUtil.getRestTemplateForString();
    ResponseEntity<String> responseEntity = 
        rt.exchange(uriBuilder.build().toString(), HttpMethod.GET, null, 
            String.class);
    return SyncUtilities.getManifestEntriesFromResponse(
        responseEntity.getBody());
  }

  private List<OdkTablesFileManifestEntry> getFileManifestForAllFiles() throws 
      JsonParseException, JsonMappingException, IOException {
    Uri.Builder uriBuilder = 
        Uri.parse(mFileManifestUri.toString()).buildUpon();
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_APP_ID, 
        TableFileUtils.ODK_TABLES_APP_NAME);
    RestTemplate rt = SyncUtil.getRestTemplateForString();
    ResponseEntity<String> responseEntity = 
        rt.exchange(uriBuilder.build().toString(), HttpMethod.GET, null, 
            String.class);
    return SyncUtilities.getManifestEntriesFromResponse(
        responseEntity.getBody());
  }
  
  /**
   * Get the files that need to be uploaded. i.e. those files that are on the
   * phone but that do not appear on the manifest. Both the manifest and the
   * filesOnPhone are assumed to contain relative paths, not including the 
   * first separator. Paths all relative to the app folder.
   * @param filesOnPhone
   * @param manifest
   * @return
   */
  private List<String> getFilesToBeUploaded(List<String> relativePathsOnDevice, 
      List<OdkTablesFileManifestEntry> manifest) {
    Set<String> filesToRetain = new HashSet<String>();
    filesToRetain.addAll(relativePathsOnDevice);
    for (OdkTablesFileManifestEntry entry: manifest) {
      filesToRetain.remove(entry.filename);
    }
    List<String> fileList = new ArrayList<String>();
    fileList.addAll(filesToRetain);
    return fileList;
  }
  
  private boolean uploadFile(String wholePathToFile, 
      String pathRelativeToAppFolder) {
    File file = new File(wholePathToFile);
    FileSystemResource resource = new FileSystemResource(file);
    String escapedPath = 
        SyncUtil.formatPathForAggregate(pathRelativeToAppFolder);
    URI filePostUri = URI.create(mFilesUri.toString()).resolve(
        TableFileUtils.ODK_TABLES_APP_NAME + File.separator + escapedPath)
          .normalize();
    Log.i(TAG, "[uploadFile] filePostUri: " + filePostUri.toString());
    RestTemplate rt = SyncUtil.getRestTemplateForFiles();
    URI responseUri = rt.postForLocation(filePostUri, resource);
    // TODO: verify whether or not this worked.
    return true;
  }
  
  private boolean compareAndDownloadFile(
      OdkTablesFileManifestEntry entry) {
    String basePath = ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
    // now we need to look through the manifest and see where the files are
    // supposed to be stored.
      // make sure you don't return a bad string.
    if (entry.filename.equals("") || entry.filename == null) {
      Log.i(TAG, "returned a null or empty filename");
      return false;
     } else {
      // filename is the unrooted path of the file, so append the tableId
      // and the basepath.
      String path = basePath + File.separator + entry.filename;
      // Before we try dl'ing the file, we have to make the folder,
      // b/c otherwise if the folders down to the path have too many non-
      // existent folders, we'll get a FileNotFoundException when we open
      // the FileOutputStream.
      int lastSlash = path.lastIndexOf(File.separator);
      String folderPath = path.substring(0, lastSlash);
      FileUtils.createFolder(folderPath);
      File newFile = new File(path);
      if (!newFile.exists()) {
        // the file doesn't exist on the system
        //filesToDL.add(newFile);
        try {
          downloadFile(newFile, entry.downloadUrl);
          return true;
        } catch (Exception e) {
          e.printStackTrace();
          Log.e(TAG, "trouble downloading file for first time");
          return false;
        }
      } else {
        // file exists, see if it's up to date
        String md5hash = FileUtils.getMd5Hash(newFile);
        md5hash = "md5:" + md5hash;
        // so as it comes down from the manifest, the md5 hash includes a
        // "md5:" prefix. Add taht and then check.
        if (!md5hash.equals(entry.md5hash)) {
          // it's not up to date, we need to download it.
          try {
            downloadFile(newFile, entry.downloadUrl);
            return true;
          } catch (Exception e) {
            e.printStackTrace();
            // TODO throw correct exception
            Log.e(TAG, "trouble downloading new version of existing file");
            return false;
          }
        } else {
          return true;
        }
      }
    }    
  }
  
  private void downloadFile(File f, String downloadUrl) throws Exception {
    URI uri = null;
    try {
      Log.i(TAG, "[downloadFile] downloading at url: " + downloadUrl);
      URL url = new URL(downloadUrl);
      uri = url.toURI();
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw e;
    } catch (URISyntaxException e) {
      e.printStackTrace();
      throw e;
    }

    // WiFi network connections can be renegotiated during a large form download
    // sequence.
    // This will cause intermittent download failures. Silently retry once after
    // each
    // failure. Only if there are two consecutive failures, do we abort.
    boolean success = false;
    int attemptCount = 0;
    while (!success && attemptCount++ <= 2) {

      // set up request...
      HttpGet req = new HttpGet(downloadUrl);

      HttpResponse response = null;
      try {
        response = mHttpClient.execute(req);
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != HttpStatus.SC_OK) {
          discardEntityBytes(response);
          if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            // clear the cookies -- should not be necessary?
            // ss: might just be a collect thing?
          }
          throw new Exception("status wasn't SC_OK when dl'ing file: " 
              + downloadUrl);
        }

        // write connection to file
        InputStream is = null;
        OutputStream os = null;
        try {
          is = response.getEntity().getContent();
          os = new FileOutputStream(f);
          byte buf[] = new byte[1024];
          int len;
          while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
          }
          os.flush();
          success = true;
        } finally {
          if (os != null) {
            try {
              os.close();
            } catch (Exception e) {
            }
          }
          if (is != null) {
            try {
              // ensure stream is consumed...
              final long count = 1024L;
              while (is.skip(count) == count)
                ;
            } catch (Exception e) {
              // no-op
            }
            try {
              is.close();
            } catch (Exception e) {
            }
          }
        }

      } catch (Exception e) {
        e.printStackTrace();
        if (attemptCount != 1) {
          throw e;
        }
      }
    }
  }
  
  /**
   * Utility to ensure that the entity stream of a response is drained of bytes.
   *
   * @param response
   */
  private void discardEntityBytes(HttpResponse response) {
    // may be a server that does not handle
    org.opendatakit.httpclientandroidlib.HttpEntity entity = 
        response.getEntity();
    if (entity != null) {
      try {
        // have to read the stream in order to reuse the connection
        InputStream is = response.getEntity().getContent();
        // read to end of stream...
        final long count = 1024L;
        while (is.skip(count) == count)
          ;
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  
}
