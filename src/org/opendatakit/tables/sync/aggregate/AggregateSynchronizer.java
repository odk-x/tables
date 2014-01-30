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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.aggregate.odktables.rest.entity.PropertiesResource;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowResource;
import org.opendatakit.aggregate.odktables.rest.entity.RowResourceList;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinition;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinitionResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableProperties;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableResourceList;
import org.opendatakit.aggregate.odktables.rest.interceptor.AggregateRequestInterceptor;
import org.opendatakit.aggregate.odktables.rest.serialization.OdkJsonHttpMessageConverter;
import org.opendatakit.aggregate.odktables.rest.serialization.OdkXmlHttpMessageConverter;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebUtils;
import org.opendatakit.httpclientandroidlib.HttpResponse;
import org.opendatakit.httpclientandroidlib.HttpStatus;
import org.opendatakit.httpclientandroidlib.client.HttpClient;
import org.opendatakit.httpclientandroidlib.client.methods.HttpGet;
import org.opendatakit.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.opendatakit.httpclientandroidlib.impl.conn.BasicClientConnectionManager;
import org.opendatakit.httpclientandroidlib.params.HttpConnectionParams;
import org.opendatakit.httpclientandroidlib.params.HttpParams;
import org.opendatakit.tables.sync.IncomingModification;
import org.opendatakit.tables.sync.Modification;
import org.opendatakit.tables.sync.SyncRow;
import org.opendatakit.tables.sync.SyncUtil;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.sync.TextPlainHttpMessageConverter;
import org.opendatakit.tables.sync.exceptions.AccessDeniedException;
import org.opendatakit.tables.sync.exceptions.InvalidAuthTokenException;
import org.opendatakit.tables.sync.exceptions.RequestFailureException;
import org.opendatakit.tables.utils.FileUtils;
import org.opendatakit.tables.utils.TableFileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpClientAndroidlibRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import android.net.Uri;
import android.util.Log;

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
  /** Path to the file servlet on the Aggregate server. */
  private static final String FILES_PATH = "/odktables/files/";

  private static final String FILE_MANIFEST_PARAM_APP_ID = "app_id";
  private static final String FILE_MANIFEST_PARAM_TABLE_ID = "table_id";
  private static final String FILE_MANIFEST_PARAM_APP_LEVEL_FILES = "app_level_files";
  /** Value for {@link #FILE_MANIFEST_PARAM_APP_LEVEL_FILES}. */
  private static final String VALUE_TRUE = "true";

  private final String appName;
  private final String accessToken;
  private final RestTemplate rt;
  private final HttpHeaders requestHeaders;
  private final URI baseUri;
  private final Map<String, TableResource> resources;
  /** The uri for the file manifest on aggregate. */
  private final URI mFileManifestUri;
  /** The uri for the files on aggregate. */
  private final URI mFilesUri;

  /**
   * For downloading files. Should eventually probably switch to spring, but it
   * was idiotically complicated.
   */
  private final HttpClient mHttpClient;

  public AggregateSynchronizer(String appName, String aggregateUri, String accessToken)
      throws InvalidAuthTokenException {
    this.appName = appName;
    URI uri = URI.create(aggregateUri).normalize();
    uri = uri.resolve("/odktables/" + appName + "/").normalize();
    this.baseUri = uri;

    this.mHttpClient = new DefaultHttpClient(new BasicClientConnectionManager());
    final HttpParams params = mHttpClient.getParams();
    HttpConnectionParams.setConnectionTimeout(params, TableFileUtils.HTTP_REQUEST_TIMEOUT_MS);
    HttpConnectionParams.setSoTimeout(params, TableFileUtils.HTTP_REQUEST_TIMEOUT_MS);

    URI uriBase = URI.create(aggregateUri).normalize();
    URI fileManifestUri = uriBase.resolve(FILE_MANIFEST_PATH).normalize();
    this.mFileManifestUri = fileManifestUri;
    this.mFilesUri = getFilePathURI(aggregateUri);
    this.rt = new RestTemplate();

    List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
    // plain text
    converters.add(new TextPlainHttpMessageConverter());
    // JSON conversion...
    converters.add(new OdkJsonHttpMessageConverter(true));
    // XML conversion...
    converters.add(new OdkXmlHttpMessageConverter());

    this.rt.setMessageConverters(converters);
    this.rt.setErrorHandler(new ResponseErrorHandler() {

      @Override
      public void handleError(ClientHttpResponse resp) throws IOException {
        switch ( resp.getStatusCode().value() ) {
        case HttpStatus.SC_OK:
          throw new IllegalStateException("OK should not get here");
        case HttpStatus.SC_FORBIDDEN:
          throw new AccessDeniedException(resp.getStatusText());
        default:
          throw new RequestFailureException(resp.getStatusText());
        }

      }

      @Override
      public boolean hasError(ClientHttpResponse resp) throws IOException {
        org.springframework.http.HttpStatus status = resp.getStatusCode();
        int rc = status.value();
        return ( rc != HttpStatus.SC_OK );
      }});

    this.requestHeaders = new HttpHeaders();

    // select our preferred protocol...
    MediaType protocolType = MediaType.APPLICATION_JSON;
    this.requestHeaders.setContentType(protocolType);

    // set our preferred response media type to json using quality parameters
    List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();

    Map<String,String> mediaTypeParams;
    mediaTypeParams = new HashMap<String,String>();
    mediaTypeParams.put("charset", "utf-8");
    mediaTypeParams.put("q", "0.9");
    MediaType txmlUtf8 = new MediaType(MediaType.TEXT_XML.getType(), MediaType.TEXT_XML.getSubtype(), mediaTypeParams);
    mediaTypeParams = new HashMap<String,String>();
    mediaTypeParams.put("charset", "utf-8");
    mediaTypeParams.put("q", "0.8");
    MediaType axmlUtf8 = new MediaType(MediaType.APPLICATION_WILDCARD_XML.getType(), MediaType.APPLICATION_WILDCARD_XML.getSubtype(), mediaTypeParams);
    mediaTypeParams = new HashMap<String,String>();
    mediaTypeParams.put("q", "1.0");
    MediaType json = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), mediaTypeParams);

    acceptableMediaTypes.add(json);
    acceptableMediaTypes.add(txmlUtf8);
    acceptableMediaTypes.add(axmlUtf8);

    this.requestHeaders.setAccept(acceptableMediaTypes);

    // set the response entity character set to UTF-8
    this.requestHeaders.setAcceptCharset(Collections.singletonList(Charset.forName(ApiConstants.UTF8_ENCODE)));

    this.resources = new HashMap<String, TableResource>();

    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(uriBase, accessToken, acceptableMediaTypes));

    this.rt.setInterceptors(interceptors);

    HttpClientAndroidlibRequestFactory factory = new HttpClientAndroidlibRequestFactory(WebUtils.createHttpClient(WebUtils.CONNECTION_TIMEOUT, 1));
    factory.setConnectTimeout(WebUtils.CONNECTION_TIMEOUT);
    factory.setReadTimeout(2*WebUtils.CONNECTION_TIMEOUT);
    this.rt.setRequestFactory(factory);

    checkAccessToken(accessToken);
    this.accessToken = accessToken;

    // undo work-around for erroneous gzip on auth token interaction
    converters = new ArrayList<HttpMessageConverter<?>>();
    // plain text
    converters.add(new TextPlainHttpMessageConverter());
    // JSON conversion...
    converters.add(new OdkJsonHttpMessageConverter(false));
    // XML conversion...
    converters.add(new OdkXmlHttpMessageConverter());
    this.rt.setMessageConverters(converters);
  }

  /**
   * Get the URI for the file servlet on the Aggregate server located at
   * aggregateUri.
   *
   * @param aggregateUri
   * @return
   */
  public static URI getFilePathURI(String aggregateUri) {
    URI filesUri = URI.create(aggregateUri).normalize();
    filesUri = filesUri.resolve(FILES_PATH).normalize();
    return filesUri;
  }

  private void checkAccessToken(String accessToken) throws InvalidAuthTokenException {
    ResponseEntity<Object> responseEntity;
    try {
      responseEntity = rt.getForEntity(TOKEN_INFO + URLEncoder.encode(accessToken, ApiConstants.UTF8_ENCODE), Object.class);
      @SuppressWarnings("unused")
      Object o = responseEntity.getBody();
    } catch (HttpClientErrorException e) {
      Log.e(TAG, "HttpClientErrorException in checkAccessToken");
      Object o = null;
      try {
        o = ODKFileUtils.mapper.readValue(e.getResponseBodyAsString(), Object.class);
      } catch (Exception e1) {
        e1.printStackTrace();
        throw new InvalidAuthTokenException("Unable to parse response from auth token verification (" + e.toString() + ")", e);
      }
      if ( o != null && o instanceof Map ) {
        @SuppressWarnings("rawtypes")
        Map m = (Map) o;
        if ( m.containsKey("error")) {
          throw new InvalidAuthTokenException("Invalid auth token (" +  m.get("error").toString() + "): " + accessToken, e);
        } else {
          throw new InvalidAuthTokenException("Unknown response from auth token verification (" + e.toString() + ")", e);
        }
      }
    } catch (Exception e ) {
      Log.e(TAG, "HttpClientErrorException in checkAccessToken");
      Object o = null;
      throw new InvalidAuthTokenException("Invalid auth token (): " + accessToken, e);
    }
  }

  /**
   * Return a map of tableId to schemaETag.
   */
  @Override
  public List<TableResource> getTables() throws IOException {
    List<TableResource> tables = new ArrayList<TableResource>();

    TableResourceList tableResources;
    try {
      tableResources = rt.getForObject(baseUri, TableResourceList.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }

    for (TableResource tableResource : tableResources.getEntries()) {
      resources.put(tableResource.getTableId(), tableResource);
      tables.add(tableResource);
    }

    Collections.sort(tables, new Comparator<TableResource>(){

      @Override
      public int compare(TableResource lhs, TableResource rhs) {
        if ( lhs.getDisplayName() != null ) {
          return lhs.getDisplayName().compareTo(rhs.getDisplayName());
        }
        return -1;
      }});
    return tables;
  }

  @Override
  public TableDefinitionResource getTableDefinition(String tableDefinitionUri) {
    TableDefinitionResource definitionRes = rt.getForObject(tableDefinitionUri, TableDefinitionResource.class);

    return definitionRes;
  }

  @Override
  public SyncTag createTable(String tableId, SyncTag syncTag, ArrayList<Column> columns)
      throws IOException {

    // build request
    URI uri = baseUri.resolve(tableId);
    TableDefinition definition = new TableDefinition(tableId, syncTag.getSchemaETag(), columns);
    HttpEntity<TableDefinition> requestEntity = new HttpEntity<TableDefinition>(definition,
                                                                                requestHeaders);
    // create table
    ResponseEntity<TableResource> resourceEntity;
    try {
      // TODO: we also need to put up the key value store/properties.
      resourceEntity = rt.exchange(uri, HttpMethod.PUT, requestEntity, TableResource.class);
    } catch (ResourceAccessException e) {
      Log.e(TAG, "ResourceAccessException in createTable");
      throw new IOException(e.getMessage());
    }
    TableResource resource = resourceEntity.getBody();

    // save resource
    this.resources.put(resource.getTableId(), resource);

    // return sync tag
    SyncTag newSyncTag = new SyncTag(resource.getDataETag(), resource.getPropertiesETag(),
                                  resource.getSchemaETag());
    return newSyncTag;
  }

  @Override
  public TableResource getTable(String tableId) throws IOException {
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
  @Override
  public IncomingModification getUpdates(String tableId, SyncTag currentTag) throws IOException {
    IncomingModification modification = new IncomingModification();

    // get current and new sync tags
    TableResource resource = refreshResource(tableId);
    // This tag is ultimately returned. May8--make sure it works.
    SyncTag newTag = new SyncTag(resource.getDataETag(), resource.getPropertiesETag(),
                                 resource.getSchemaETag());

    // stop now if there are no updates
    if (newTag.equals(currentTag)) {
      modification.setTableSyncTag(currentTag);
      modification.setTablePropertiesChanged(false);
      return modification;
    }

    // get schema updates.
    // TODO: need to plumb support for this
    if (!newTag.getSchemaETag().equals(currentTag.getSchemaETag())) {
      TableDefinitionResource definitionRes;
      try {
        definitionRes = rt.getForObject(resource.getDefinitionUri(), TableDefinitionResource.class);
      } catch (ResourceAccessException e) {
        throw new IOException(e.getMessage());
      }

      modification.setTableSchemaChanged(true);
      modification.setTableDefinitionResource(definitionRes);
    }

    // get properties updates.
    // To do this we first check to see if the properties ETag is up to date.
    // If it is, we can do nothing. If it is out of date, we have to:
    // 1) get a TableDefinitionResource to see if we need to update the table
    // data structure of any of the columns.
    // 2) get a PropertiesResource to get all the key value entries.
    if (!newTag.getPropertiesETag().equals(currentTag.getPropertiesETag())) {
      PropertiesResource propertiesRes;
      try {
        propertiesRes = rt.getForObject(resource.getPropertiesUri(), PropertiesResource.class);
      } catch (ResourceAccessException e) {
        throw new IOException(e.getMessage());
      }

      modification.setTablePropertiesChanged(true);
      modification.setTableProperties(propertiesRes);
    }

    // TODO: need to loop here to process segments of change
    // vs. an entire bucket of changes.

    // get data updates
    if (!newTag.getDataETag().equals(currentTag.getDataETag())) {
      URI url;
      if (currentTag.getDataETag() == null) {
        url = URI.create(resource.getDataUri());
      } else {
        String diffUri = resource.getDiffUri();
        url = URI.create(diffUri + "?data_etag=" + currentTag.getDataETag()).normalize();
      }
      RowResourceList rows;
      try {
        rows = rt.getForObject(url, RowResourceList.class);
      } catch (ResourceAccessException e) {
        throw new IOException(e.getMessage());
      }

      List<SyncRow> syncRows = new ArrayList<SyncRow>();
      for (RowResource row : rows.getEntries()) {
        SyncRow syncRow = new SyncRow(row.getRowId(), row.getRowETag(), row.isDeleted(),
                                      row.getUriAccessControl(), row.getFormId(), row.getLocale(),
                                      row.getSavepointTimestamp(), row.getValues());
        syncRows.add(syncRow);
      }
      modification.setRows(syncRows);
    }

    modification.setTableSyncTag(newTag);
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
  public Modification insertRows(String tableId, SyncTag currentSyncTag, List<SyncRow> rowsToInsert)
      throws IOException {
    List<Row> newRows = new ArrayList<Row>();
    for (SyncRow syncRow : rowsToInsert) {
      Row row = Row.forInsert(syncRow.getRowId(),
          syncRow.getUriAccessControl(), syncRow.getFormId(), syncRow.getLocale(), syncRow.getSavepointTimestamp(),
          syncRow.getValues());
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
  public Modification updateRows(String tableId, SyncTag currentSyncTag, List<SyncRow> rowsToUpdate)
      throws IOException {
    List<Row> changedRows = new ArrayList<Row>();
    for (SyncRow syncRow : rowsToUpdate) {
      Row row = Row.forUpdate(syncRow.getRowId(), syncRow.getSyncTag(),
          syncRow.getUriAccessControl(), syncRow.getFormId(), syncRow.getLocale(), syncRow.getSavepointTimestamp(),
          syncRow.getValues());
      changedRows.add(row);
    }
    return insertOrUpdateRows(tableId, currentSyncTag, changedRows);
  }

  private Modification insertOrUpdateRows(String tableId, SyncTag currentSyncTag, List<Row> rows)
      throws IOException {
    TableResource resource = getTable(tableId);
    // SyncTag syncTag = SyncTag.valueOf(currentSyncTag);
    Map<String, String> rowTags = new HashMap<String, String>();
    SyncTag lastKnownServerSyncTag = new SyncTag(currentSyncTag.getDataETag(), currentSyncTag.getPropertiesETag(), currentSyncTag.getSchemaETag());
    if (!rows.isEmpty()) {
      for (Row row : rows) {
        URI url = URI.create(
            resource.getDataUri() + "/" + row.getRowId()).normalize();
        HttpEntity<Row> requestEntity = new HttpEntity<Row>(row, requestHeaders);
        ResponseEntity<RowResource> insertedEntity;
        try {
          insertedEntity = rt.exchange(url, HttpMethod.PUT, requestEntity, RowResource.class);
        } catch (ResourceAccessException e) {
          throw new IOException(e.getMessage());
        }
        RowResource inserted = insertedEntity.getBody();
        rowTags.put(inserted.getRowId(), inserted.getRowETag());
        Log.i(TAG, "[insertOrUpdateRows] setting data etag to row's last "
            + "known dataetag at modification: " + inserted.getDataETagAtModification());
        lastKnownServerSyncTag.setDataETag(inserted.getDataETagAtModification());
      }
    }

    Modification modification = new Modification();
    modification.setSyncTags(rowTags);
    modification.setTableSyncTag(lastKnownServerSyncTag);

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
  public SyncTag deleteRows(String tableId, SyncTag currentSyncTag, List<String> rowIds)
      throws IOException {
    TableResource resource = getTable(tableId);
    SyncTag syncTag = currentSyncTag;
    if (!rowIds.isEmpty()) {
      String lastKnownServerDataTag = null; // the data tag of the whole table.
      for (String rowId : rowIds) {
        URI url = URI.create(resource.getDataUri() + "/" + rowId).normalize();
        try {
          ResponseEntity<String> response = rt.exchange(url, HttpMethod.DELETE, null, String.class);
          lastKnownServerDataTag = response.getBody();
        } catch (ResourceAccessException e) {
          throw new IOException(e.getMessage());
        }
      }
      if (lastKnownServerDataTag == null) {
        // do something--b/c the delete hasn't worked.
        Log.e(TAG, "delete call didn't return a known data etag.");
      }
      Log.i(TAG, "[deleteRows] setting data etag to last known server tag: "
          + lastKnownServerDataTag);
      syncTag.setDataETag(lastKnownServerDataTag);
    }
    return syncTag;
  }

  @Override
  public SyncTag setTableProperties(String tableId, SyncTag currentTag,
                                   ArrayList<OdkTablesKeyValueStoreEntry> kvsEntries) throws IOException {
    TableResource resource = getTable(tableId);

    // put new properties
    TableProperties properties = new TableProperties(currentTag.getSchemaETag(), currentTag.getPropertiesETag(), tableId,
                                                     kvsEntries);
    HttpEntity<TableProperties> entity = new HttpEntity<TableProperties>(properties, requestHeaders);
    ResponseEntity<PropertiesResource> updatedEntity;
    try {
      updatedEntity = rt.exchange(resource.getPropertiesUri(), HttpMethod.PUT, entity,
          PropertiesResource.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }
    PropertiesResource propsResource = updatedEntity.getBody();

    SyncTag newTag = new SyncTag(currentTag.getDataETag(), propsResource.getPropertiesETag(),
                                  propsResource.getSchemaETag());
    return newTag;
  }

  @Override
  public void syncAppLevelFiles(boolean pushLocalFiles) throws ResourceAccessException {
    List<OdkTablesFileManifestEntry> manifest = getAppLevelFileManifest();
    for (OdkTablesFileManifestEntry entry : manifest) {
      compareAndDownloadFile(entry);
    }
    // And now get the files to upload. We only want those that exist on the
    // device but that do not exist on the manifest.
    Set<String> dirsToExclude = ODKFileUtils.getDirectoriesToExcludeFromSync(true);
    String appFolder = ODKFileUtils.getAppFolder(appName);
    List<String> relativePathsOnDevice = TableFileUtils.getAllFilesUnderFolder(appFolder,
        dirsToExclude);
    List<String> relativePathsToUpload = getFilesToBeUploaded(relativePathsOnDevice, manifest);
    Log.e(TAG, "[syncAppLevelFiles] relativePathsToUpload: " + relativePathsToUpload);
    // and then upload the files.
    if (pushLocalFiles) {
      Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
      for (String relativePath : relativePathsToUpload) {
        String wholePathToFile = appFolder + File.separator + relativePath;
        successfulUploads.put(relativePath, uploadFile(wholePathToFile, relativePath));
      }
    }
  }

  @Override
  public void syncAllFiles() throws ResourceAccessException {
    List<OdkTablesFileManifestEntry> manifest = getFileManifestForAllFiles();
    for (OdkTablesFileManifestEntry entry : manifest) {
      compareAndDownloadFile(entry);
    }
    // And now get the files to upload. We only want those that exist on the
    // device but that do not exist on the manifest.
    Set<String> dirsToExclude = ODKFileUtils.getDirectoriesToExcludeFromSync(false);
    String appFolder = ODKFileUtils.getAppFolder(appName);
    List<String> relativePathsOnDevice = TableFileUtils.getAllFilesUnderFolder(appFolder,
        dirsToExclude);
    List<String> relativePathsToUpload = getFilesToBeUploaded(relativePathsOnDevice, manifest);
    // and then upload the files.
    Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
    for (String relativePath : relativePathsToUpload) {
      String wholePathToFile = appFolder + File.separator + relativePath;
      successfulUploads.put(relativePath, uploadFile(wholePathToFile, relativePath));
    }
  }

  @Override
  public void syncNonRowDataTableFiles(String tableId, boolean pushLocal) throws ResourceAccessException {
    List<OdkTablesFileManifestEntry> manifest = getTableLevelFileManifest(tableId);
    for (OdkTablesFileManifestEntry entry : manifest) {
      compareAndDownloadFile(entry);
    }
    if (pushLocal) {
      // Then we actually do try and upload things. Otherwise we can just
      // continue straight on.
      String appFolder = ODKFileUtils.getAppFolder(appName);
      String tableFolder = ODKFileUtils.getTablesFolder(appName, tableId);
      Set<String> tableDirsToExclude = new HashSet<String>();
      // We don't want to sync anything in the instances directory, because this
      // contains things like media attachments. These should instead be synched
      // with a separate call.
      // tableDirsToExclude.add(TableFileUtils.DIR_INSTANCES);
      tableDirsToExclude.add(ODKFileUtils.INSTANCES_FOLDER_NAME);
      List<String> relativePathsToAppFolderOnDevice = TableFileUtils.getAllFilesUnderFolder(
          tableFolder, tableDirsToExclude);
      List<String> relativePathsToUpload = getFilesToBeUploaded(relativePathsToAppFolderOnDevice,
          manifest);
      Log.e(TAG, "[syncNonMediaTableFiles] files to upload: " + relativePathsToUpload);
      // and then upload the files.
      Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
      for (String relativePath : relativePathsToUpload) {
        String wholePathToFile = appFolder + File.separator + relativePath;
        successfulUploads.put(relativePath, uploadFile(wholePathToFile, relativePath));
      }
    }
  }

  public List<OdkTablesFileManifestEntry> getAppLevelFileManifest() throws ResourceAccessException {
    Uri.Builder uriBuilder = Uri.parse(mFileManifestUri.toString()).buildUpon();
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_APP_ID, appName);
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_APP_LEVEL_FILES, VALUE_TRUE);
    ResponseEntity<OdkTablesFileManifest> responseEntity;
    responseEntity = rt.exchange(uriBuilder.build().toString(),
            HttpMethod.GET, null, OdkTablesFileManifest.class);
    return responseEntity.getBody().getEntries();
  }

  public List<OdkTablesFileManifestEntry> getTableLevelFileManifest(String tableId) throws ResourceAccessException {
    Uri.Builder uriBuilder = Uri.parse(mFileManifestUri.toString()).buildUpon();
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_APP_ID, appName);
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_TABLE_ID, tableId);
    ResponseEntity<OdkTablesFileManifest> responseEntity;
      responseEntity = rt.exchange(uriBuilder.build().toString(),
              HttpMethod.GET, null, OdkTablesFileManifest.class);
    return responseEntity.getBody().getEntries();
  }

  public List<OdkTablesFileManifestEntry> getFileManifestForAllFiles()  throws ResourceAccessException {
    Uri.Builder uriBuilder = Uri.parse(mFileManifestUri.toString()).buildUpon();
    uriBuilder.appendQueryParameter(FILE_MANIFEST_PARAM_APP_ID, appName);
    ResponseEntity<OdkTablesFileManifest> responseEntity;
    responseEntity = rt.exchange(uriBuilder.build().toString(),
              HttpMethod.GET, null, OdkTablesFileManifest.class);
    return responseEntity.getBody().getEntries();
  }

  /**
   * Get the files that need to be uploaded. i.e. those files that are on the
   * phone but that do not appear on the manifest. Both the manifest and the
   * filesOnPhone are assumed to contain relative paths, not including the first
   * separator. Paths all relative to the app folder.
   *
   * @param filesOnPhone
   * @param manifest
   * @return
   */
  private List<String> getFilesToBeUploaded(List<String> relativePathsOnDevice,
                                            List<OdkTablesFileManifestEntry> manifest) {
    Set<String> filesToRetain = new HashSet<String>();
    filesToRetain.addAll(relativePathsOnDevice);
    for (OdkTablesFileManifestEntry entry : manifest) {
      filesToRetain.remove(entry.filename);
    }
    List<String> fileList = new ArrayList<String>();
    fileList.addAll(filesToRetain);
    return fileList;
  }

  private boolean uploadFile(String wholePathToFile, String pathRelativeToAppFolder) {
    File file = new File(wholePathToFile);
    FileSystemResource resource = new FileSystemResource(file);
    String escapedPath = SyncUtil.formatPathForAggregate(pathRelativeToAppFolder);
    URI filePostUri = URI.create(mFilesUri.toString())
        .resolve(appName + File.separator + escapedPath).normalize();
    Log.i(TAG, "[uploadFile] filePostUri: " + filePostUri.toString());
    RestTemplate rt = SyncUtil.getRestTemplateForFiles();
    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(mFileManifestUri, accessToken));
    rt.setInterceptors(interceptors);
    URI responseUri = rt.postForLocation(filePostUri, resource);
    // TODO: verify whether or not this worked.
    return true;
  }

  /**
   * Get the URI to which to post in order to upload the file.
   *
   * @param pathRelativeToAppFolder
   * @return
   */
  public URI getFilePostUri(String appName, String pathRelativeToAppFolder) {
    String escapedPath = SyncUtil.formatPathForAggregate(pathRelativeToAppFolder);
    URI filePostUri = URI.create(mFilesUri.toString())
        .resolve(appName + File.separator + escapedPath).normalize();
    return filePostUri;
  }

  private boolean compareAndDownloadFile(OdkTablesFileManifestEntry entry) {
    String basePath = ODKFileUtils.getAppFolder(appName);
    // now we need to look through the manifest and see where the files are
    // supposed to be stored.
    // make sure you don't return a bad string.
    if (entry.filename == null || entry.filename.equals("")) {
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
        // filesToDL.add(newFile);
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
          throw new Exception("status wasn't SC_OK when dl'ing file: " + downloadUrl);
        }

        // write connection to file
        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        try {
          InputStream isRaw = response.getEntity().getContent();
          is = new BufferedInputStream(isRaw);
          os = new BufferedOutputStream(new FileOutputStream(f));
          byte buf[] = new byte[8096];
          int len;
          while ((len = is.read(buf)) >= 0) {
            if ( len != 0 ) {
              os.write(buf, 0, len);
            }
          }
          os.flush();
          os.close();
          os = null;
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
              byte buf[] = new byte[8096];
              while (is.read(buf) >= 0);
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
    org.opendatakit.httpclientandroidlib.HttpEntity entity = response.getEntity();
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

  @Override
  public void syncRowDataFiles(String tableId) throws ResourceAccessException {
    // There are two things to do here, really. The first is to get the
    // manifest for each instance--or each row of the table. And download all
    // the files on the manifest.
    // TODO: handle deletion of files appropiately.
    // The second is to then upload the files in the instances folder to the
    // server.
    //
    // The implementation of this method will be very similar to the
    // implementation of syncNonMediaTableFiles when pushLocal==true.
    // The main logic is as follows:
    // 1) request the manifest.
    // 2) compare hashes of the files existing on the phone, downloading those
    // that do not exist or that have differing hashes.
    // 3) get all the files under the INSTANCES directory.
    // 4) remove those files that were on the manifest, as they can now be
    // assumed to be up to date.
    // 5) upload all the remaining files.

    // 1) Get the manifest.
    // TODO: this is currently just getting the same table-level manifest as
    // syncNonMediaTableFiles(). In reality it should be making its own call.
    List<OdkTablesFileManifestEntry> manifest = getTableLevelFileManifest(tableId);
    for (OdkTablesFileManifestEntry entry : manifest) {
      compareAndDownloadFile(entry);
    }
    // Then we actually do try and upload things. Otherwise we can just
    // continue straight on.
    String appFolder = ODKFileUtils.getAppFolder(appName);
    String instancesFolderFullPath = ODKFileUtils.getInstancesFolder(appName, tableId);
    List<String> relativePathsToAppFolderOnDevice = TableFileUtils.getAllFilesUnderFolder(
        instancesFolderFullPath, null);
    List<String> relativePathsToUpload = getFilesToBeUploaded(relativePathsToAppFolderOnDevice,
        manifest);
    Log.e(TAG, "[syncRowDataFiles] relativePathsToUpload: " + relativePathsToUpload);
    // and then upload the files.
    Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
    for (String relativePath : relativePathsToUpload) {
      String wholePathToFile = appFolder + File.separator + relativePath;
      successfulUploads.put(relativePath, uploadFile(wholePathToFile, relativePath));
    }
  }

}
