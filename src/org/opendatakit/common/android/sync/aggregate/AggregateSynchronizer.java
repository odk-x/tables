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
package org.opendatakit.common.android.sync.aggregate;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.CharEncoding;
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
import org.opendatakit.common.android.sync.IncomingRowModifications;
import org.opendatakit.common.android.sync.RowModification;
import org.opendatakit.common.android.sync.SyncRow;
import org.opendatakit.common.android.sync.SyncUtil;
import org.opendatakit.common.android.sync.Synchronizer;
import org.opendatakit.common.android.sync.exceptions.AccessDeniedException;
import org.opendatakit.common.android.sync.exceptions.InvalidAuthTokenException;
import org.opendatakit.common.android.sync.exceptions.RequestFailureException;
import org.opendatakit.common.android.sync.files.SyncUtilities;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebUtils;
import org.opendatakit.httpclientandroidlib.Header;
import org.opendatakit.httpclientandroidlib.HttpResponse;
import org.opendatakit.httpclientandroidlib.HttpStatus;
import org.opendatakit.httpclientandroidlib.client.methods.HttpGet;
import org.opendatakit.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.opendatakit.httpclientandroidlib.impl.conn.BasicClientConnectionManager;
import org.opendatakit.httpclientandroidlib.params.HttpConnectionParams;
import org.opendatakit.httpclientandroidlib.params.HttpParams;
import org.opendatakit.tables.application.Tables;
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

  /** Timeout (in ms) we specify for each http request */
  public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;
  /** Path to the file servlet on the Aggregate server. */

  private final String appName;
  private final String aggregateUri;
  private final String accessToken;
  private final RestTemplate tokenRt;
  private final RestTemplate rt;
  private final HttpHeaders requestHeaders;
  private final Map<String, TableResource> resources;
  /** normalized aggregateUri */
  private final URI baseUri;

  /**
   * For downloading files. Should eventually probably switch to spring, but it
   * was idiotically complicated.
   */
  private final DefaultHttpClient mHttpClient;

  private String getTablesUriFragment() {
    /** Path to the tables servlet (the one that manages table definitions) on the Aggregate server. */
    return "/odktables/" + appName + "/tables/";
  }

  private String getManifestUriFragment() {
    /** Path to the tables servlet (the one that manages table definitions) on the Aggregate server. */
    String versionCode = Tables.getInstance().getVersionCodeString();
    // the javascript API and file representation are the 100's and higher place in the versionCode.
    String odkClientVersion = versionCode.substring(0, versionCode.length()-2);
    return "/odktables/" + appName + "/manifest/" + odkClientVersion + "/";
  }

  /**
   * Get the URI for the file servlet on the Aggregate server located at
   * aggregateUri.
   *
   * @param aggregateUri
   * @return
   */
  private String getFilePathURI() {
    String versionCode = Tables.getInstance().getVersionCodeString();
    // the javascript API and file representation are the 100's and higher place in the versionCode.
    String odkClientVersion = versionCode.substring(0, versionCode.length()-2);
    return "/odktables/" + appName + "/files/" + odkClientVersion + "/";
  }

  public AggregateSynchronizer(String appName, String aggregateUri, String accessToken)
      throws InvalidAuthTokenException {
    this.appName = appName;
    this.aggregateUri = aggregateUri;
    this.baseUri = SyncUtilities.normalizeUri(aggregateUri, "/");

    this.mHttpClient = new DefaultHttpClient(new BasicClientConnectionManager());
    final HttpParams params = mHttpClient.getParams();
    HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
    HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
    this.rt = new RestTemplate();
    this.tokenRt = new RestTemplate();

    ResponseErrorHandler handler = new ResponseErrorHandler() {

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
      }
    };
    this.rt.setErrorHandler(handler);
    this.tokenRt.setErrorHandler(handler);

    this.requestHeaders = new HttpHeaders();

    // select our preferred protocol...
    MediaType protocolType = MediaType.APPLICATION_JSON;
    this.requestHeaders.setContentType(protocolType);

    // set our preferred response media type to json using quality parameters
    List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();

    Map<String,String> mediaTypeParams;
    // we really like JSON
    mediaTypeParams = new HashMap<String,String>();
    mediaTypeParams.put("q", "1.0");
    MediaType json = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), mediaTypeParams);
    // XML is OK
    mediaTypeParams = new HashMap<String,String>();
    mediaTypeParams.put("charset", CharEncoding.UTF_8.toLowerCase(Locale.ENGLISH));
    mediaTypeParams.put("q", "0.8");
    MediaType txmlUtf8 = new MediaType(MediaType.TEXT_XML.getType(), MediaType.TEXT_XML.getSubtype(), mediaTypeParams);
    // application/*+xml is odd but OK
    mediaTypeParams = new HashMap<String,String>();
    mediaTypeParams.put("charset", CharEncoding.UTF_8.toLowerCase(Locale.ENGLISH));
    mediaTypeParams.put("q", "0.6");
    MediaType axmlUtf8 = new MediaType(MediaType.APPLICATION_WILDCARD_XML.getType(), MediaType.APPLICATION_WILDCARD_XML.getSubtype(), mediaTypeParams);
    // don't really want plaintext...
    mediaTypeParams = new HashMap<String,String>();
    mediaTypeParams.put("charset", CharEncoding.UTF_8.toLowerCase(Locale.ENGLISH));
    mediaTypeParams.put("q", "0.4");
    MediaType tplainUtf8 = new MediaType(MediaType.TEXT_PLAIN.getType(), MediaType.TEXT_PLAIN.getSubtype(), mediaTypeParams);

    acceptableMediaTypes.add(json);
    acceptableMediaTypes.add(txmlUtf8);
    acceptableMediaTypes.add(axmlUtf8);
    acceptableMediaTypes.add(tplainUtf8);

    this.requestHeaders.setAccept(acceptableMediaTypes);

    // set the response entity character set to CharEncoding.UTF_8
    this.requestHeaders.setAcceptCharset(Collections.singletonList(Charset.forName(CharEncoding.UTF_8)));

    this.resources = new HashMap<String, TableResource>();

    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(this.baseUri, accessToken, acceptableMediaTypes));

    this.rt.setInterceptors(interceptors);
    this.tokenRt.setInterceptors(interceptors);

    HttpClientAndroidlibRequestFactory factory = new HttpClientAndroidlibRequestFactory(WebUtils.createHttpClient(WebUtils.CONNECTION_TIMEOUT, 1));
    factory.setConnectTimeout(WebUtils.CONNECTION_TIMEOUT);
    factory.setReadTimeout(2*WebUtils.CONNECTION_TIMEOUT);
    this.rt.setRequestFactory(factory);
    this.tokenRt.setRequestFactory(factory);

    List<HttpMessageConverter<?>> converters;

    converters = new ArrayList<HttpMessageConverter<?>>();
    // JSON conversion...
    converters.add(new OdkJsonHttpMessageConverter(false));
    // XML conversion...
    converters.add(new OdkXmlHttpMessageConverter());
    this.rt.setMessageConverters(converters);

    // undo work-around for erroneous gzip on auth token interaction
    converters = new ArrayList<HttpMessageConverter<?>>();
    // JSON conversion...
    converters.add(new OdkJsonHttpMessageConverter(true));
    // XML conversion...
    converters.add(new OdkXmlHttpMessageConverter());
    this.tokenRt.setMessageConverters(converters);

    checkAccessToken(accessToken);
    this.accessToken = accessToken;

  }

  private void checkAccessToken(String accessToken) throws InvalidAuthTokenException {
    ResponseEntity<Object> responseEntity;
    try {
      responseEntity = tokenRt.getForEntity(TOKEN_INFO + URLEncoder.encode(accessToken, ApiConstants.UTF8_ENCODE), Object.class);
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
      URI uri = SyncUtilities.normalizeUri(aggregateUri, getTablesUriFragment());
      tableResources = rt.getForObject(uri, TableResourceList.class);
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

  private void verifyResource( String tableId, String propertiesETag, String schemaETag ) {
    TableResource tr = resources.get(tableId);
    if ( tr == null ) return;
    if (!( tr.getPropertiesETag() == propertiesETag ||
          (tr.getPropertiesETag() != null && tr.getPropertiesETag().equals(propertiesETag))) ) {
      // dataETag is stale...
      resources.remove(tableId);
      return;
    }
    if (!( tr.getSchemaETag() == schemaETag ||
          (tr.getSchemaETag() != null && tr.getSchemaETag().equals(schemaETag))) ) {
      // dataETag is stale...
      resources.remove(tableId);
      return;
    }
  }

  private void updateResource( String tableId, SyncTag syncTag) {
    TableResource tr = resources.get(tableId);
    if ( tr == null ) return;
    if (!( tr.getSchemaETag() == syncTag.getSchemaETag() ||
          (tr.getSchemaETag() != null && tr.getSchemaETag().equals(syncTag.getSchemaETag()))) ) {
      // schemaETag is stale...
      resources.remove(tableId);
      return;
    }
    if (!( tr.getPropertiesETag() == syncTag.getPropertiesETag() ||
        (tr.getPropertiesETag() != null && tr.getPropertiesETag().equals(syncTag.getPropertiesETag()))) ) {
      // propertiesETag is stale...
      resources.remove(tableId);
      return;
    }
    // otherwise, update the dataETag
    tr.setDataETag(syncTag.getDataETag());
  }

  private void verifyResource( String tableId, String schemaETag ) {
    TableResource tr = resources.get(tableId);
    if ( tr == null ) return;
    if (!( tr.getSchemaETag() == schemaETag ||
          (tr.getSchemaETag() != null && tr.getSchemaETag().equals(schemaETag))) ) {
      // dataETag is stale...
      resources.remove(tableId);
      return;
    }
  }

  @Override
  public TableDefinitionResource getTableDefinition(String tableDefinitionUri) {
    TableDefinitionResource definitionRes = rt.getForObject(tableDefinitionUri, TableDefinitionResource.class);

    verifyResource(definitionRes.getTableId(), definitionRes.getSchemaETag());
    return definitionRes;
  }

  @Override
  public TableResource createTable(String tableId, SyncTag syncTag, ArrayList<Column> columns)
      throws IOException {

    // build request
    URI uri = SyncUtilities.normalizeUri(aggregateUri, getTablesUriFragment() + tableId);
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
    return resource;
  }

  public boolean hasTable(String tableId) {
    return resources.containsKey(tableId);
  }

  @Override
  public TableResource getTable(String tableId) throws IOException {
    if (resources.containsKey(tableId)) {
      return resources.get(tableId);
    } else {
      return refreshResource(tableId);
    }
  }

  @Override
  public TableResource getTableOrNull(String tableId) throws IOException {
    // TODO: need to discriminate failure modes for server responses
    // this is not very efficient...
    List<TableResource> resources = getTables();
    TableResource resource = null;
    for ( TableResource t : resources ) {
      if ( t.getTableId().equals(tableId) ) {
        resource = t;
        break;
      }
    }

    if ( resource == null ) {
      return null;
    }
    return resource;
  }

  private TableResource refreshResource(String tableId) throws IOException {
    URI uri = SyncUtilities.normalizeUri(aggregateUri, getTablesUriFragment() + tableId);
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
    URI uri = SyncUtilities.normalizeUri(aggregateUri, getTablesUriFragment() + tableId);
    rt.delete(uri);
    this.resources.remove(tableId);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#getUpdates(java.lang
   * .String, java.lang.String)
   */
  @Override
  public IncomingRowModifications getUpdates(String tableId, SyncTag currentTag) throws IOException {
    IncomingRowModifications modification = new IncomingRowModifications();

    TableResource resource = getTable(tableId);

    // get current and new sync tags
    // This tag is ultimately returned. May8--make sure it works.
    SyncTag newTag = new SyncTag(resource.getDataETag(), resource.getPropertiesETag(),
                                 resource.getSchemaETag());

    // TODO: need to loop here to process segments of change
    // vs. an entire bucket of changes.

    // get data updates
    if (newTag.getDataETag() != currentTag.getDataETag() &&
        (newTag.getDataETag() == null || !newTag.getDataETag().equals(currentTag.getDataETag()))) {
      URI url;
      if (currentTag.getDataETag() == null) {
        url = SyncUtilities.normalizeUri(resource.getDataUri(), "/");;
      } else {
        String diffUri = resource.getDiffUri();
        url = SyncUtilities.normalizeUri(diffUri, "?data_etag=" + currentTag.getDataETag());
      }
      RowResourceList rows;
      try {
        rows = rt.getForObject(url, RowResourceList.class);
      } catch (ResourceAccessException e) {
        throw new IOException(e.getMessage());
      }

      Map<String, SyncRow> syncRows = new HashMap<String, SyncRow>();
      for (RowResource row : rows.getEntries()) {
        SyncRow syncRow = new SyncRow(row.getRowId(), row.getRowETag(), row.isDeleted(),
                                      row.getFormId(), row.getLocale(),
                                      row.getSavepointType(), row.getSavepointTimestamp(),
                                      row.getSavepointCreator(),
                                      row.getFilterScope(),
                                      row.getValues());
        syncRows.put(row.getRowId(), syncRow);
      }
      modification.setRows(syncRows);
    }

    modification.setTableSyncTag(newTag);
    return modification;
  }

  /**
   * Insert or update the given row in the table on the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @param rowToInsertOrUpdate
   *          the row to insert or update
   * @return a RowModification containing the (rowId, rowETag, table dataETag) after the modification
   */
  public RowModification insertOrUpdateRow(String tableId, SyncTag currentSyncTag, SyncRow rowToInsertOrUpdate)
      throws IOException {
        TableResource resource = getTable(tableId);
        SyncTag lastKnownServerSyncTag = new SyncTag(currentSyncTag.getDataETag(), currentSyncTag.getPropertiesETag(), currentSyncTag.getSchemaETag());

        Row row = Row.forUpdate(rowToInsertOrUpdate.getRowId(), rowToInsertOrUpdate.getRowETag(),
            rowToInsertOrUpdate.getFormId(), rowToInsertOrUpdate.getLocale(),
            rowToInsertOrUpdate.getSavepointType(),
            rowToInsertOrUpdate.getSavepointTimestamp(), rowToInsertOrUpdate.getSavepointCreator(),
            rowToInsertOrUpdate.getFilterScope(),
            rowToInsertOrUpdate.getValues());

        URI url = SyncUtilities.normalizeUri(resource.getDataUri(), row.getRowId());
        HttpEntity<Row> requestEntity = new HttpEntity<Row>(row, requestHeaders);
        ResponseEntity<RowResource> insertedEntity;
        try {
          insertedEntity = rt.exchange(url, HttpMethod.PUT, requestEntity, RowResource.class);
        } catch (ResourceAccessException e) {
          throw new IOException(e.getMessage());
        }
        RowResource inserted = insertedEntity.getBody();
        Log.i(TAG, "[insertOrUpdateRows] setting data etag to row's last "
            + "known dataetag at modification: " + inserted.getDataETagAtModification());
        lastKnownServerSyncTag.setDataETag(inserted.getDataETagAtModification());

        updateResource(tableId, lastKnownServerSyncTag);

        return new RowModification( inserted.getRowId(), inserted.getRowETag(), lastKnownServerSyncTag);
      }

  /*
   * (non-Javadoc)
   *
   * @see
   * yoonsung.odk.spreadsheet.sync.aggregate.Synchronizer#deleteRows(java.lang
   * .String, java.util.List)
   */
  @Override
  public RowModification deleteRow(String tableId, SyncTag currentSyncTag, SyncRow rowToDelete)
      throws IOException {
    TableResource resource = getTable(tableId);
    SyncTag syncTag = currentSyncTag;
    String lastKnownServerDataTag = null; // the data tag of the whole table.
    String rowId = rowToDelete.getRowId();
    URI url = SyncUtilities.normalizeUri(resource.getDataUri(), rowId);
    try {
      ResponseEntity<String> response = rt.exchange(url, HttpMethod.DELETE, null, String.class);
      lastKnownServerDataTag = response.getBody();
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }
    if (lastKnownServerDataTag == null) {
      // do something--b/c the delete hasn't worked.
      Log.e(TAG, "delete call didn't return a known data etag.");
    }
    Log.i(TAG, "[deleteRows] setting data etag to last known server tag: "
        + lastKnownServerDataTag);
    syncTag.setDataETag(lastKnownServerDataTag);

    updateResource(tableId, syncTag);

    return new RowModification(rowToDelete.getRowId(), null, syncTag);
  }

  @Override
  public PropertiesResource getTablePropertiesResource(String propertiesUri, SyncTag currentTag) throws IOException {
    PropertiesResource propsResource;
    try {
      propsResource = rt.getForObject(propertiesUri, PropertiesResource.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }

    verifyResource(propsResource.getTableId(), propsResource.getPropertiesETag(), propsResource.getSchemaETag());

    return propsResource;
  }

  @Override
  public SyncTag setTablePropertiesResource(String propertiesUri, SyncTag currentTag, String tableId,
                                   ArrayList<OdkTablesKeyValueStoreEntry> kvsEntries) throws IOException {

    // put new properties
    TableProperties properties = new TableProperties(currentTag.getSchemaETag(), currentTag.getPropertiesETag(), tableId,
                                                     kvsEntries);
    HttpEntity<TableProperties> entity = new HttpEntity<TableProperties>(properties, requestHeaders);
    ResponseEntity<PropertiesResource> updatedEntity;
    try {
      updatedEntity = rt.exchange(propertiesUri, HttpMethod.PUT, entity,
          PropertiesResource.class);
    } catch (ResourceAccessException e) {
      throw new IOException(e.getMessage());
    }
    PropertiesResource propsResource = updatedEntity.getBody();

    SyncTag newTag = new SyncTag(currentTag.getDataETag(), propsResource.getPropertiesETag(),
                                  propsResource.getSchemaETag());

    updateResource(propsResource.getTableId(), newTag);

    return newTag;
  }

  public static List<String> filterOutTableIdAssetFiles(List<String> relativePaths) {
    List<String> newList = new ArrayList<String>();
    for ( String relativePath : relativePaths ) {
      if ( relativePath.startsWith("assets/csv/") ) {
        // by convention, the files here begin with their identifying tableId
        continue;
      } else {
        newList.add(relativePath);
      }
    }
    return newList;
  }

  public static List<String> filterInTableIdFiles(List<String> relativePaths, String tableId) {
    List<String> newList = new ArrayList<String>();
    for ( String relativePath : relativePaths ) {
      if ( relativePath.startsWith("assets/csv/") ) {
        // by convention, the files here begin with their identifying tableId
        String[] parts = relativePath.split("/");
        if ( parts.length >= 3 ) {
          String[] nameElements = parts[2].split("\\.");
          if ( nameElements[0].equals(tableId) ) {
            newList.add(relativePath);
          }
        }
      }
    }
    return newList;
  }

  /**
   * Get all the files under the given folder, excluding those directories that
   * are the concatenation of folder and a member of excluding. If the member
   * of excluding is a directory, none of its children will be synched either.
   * <p>
   * If the folder doesn't exist it returns an empty list.
   * <p>
   * If the file exists but is not a directory, logs an error and returns an
   * empty list.
   * @param folder
   * @param excluding can be null--nothing will be excluded. Should be relative
   * to the given folder.
   * @param relativeTo the path to which the returned paths will be relative.
   * A null value makes them relative to the folder parameter. If it is non
   * null, folder must start with relativeTo, or else the files in
   * folder could not possibly be relative to relativeTo. In this case will
   * throw an IllegalArgumentException.
   * @return the relative paths of the files under the folder--i.e. the paths
   * after the folder parameter, not including the first separator
   * @throws IllegalArgumentException if relativeTo is not a substring of
   * folder.
   */
  public static List<String> getAllFilesUnderFolder(String folder,
      final Set<String> excludingNamedItemsUnderFolder) {
    final File baseFolder = new File(folder);
    String appName = ODKFileUtils.extractAppNameFromPath(baseFolder);

    // Return an empty list of the folder doesn't exist or is not a directory
    if (!baseFolder.exists()) {
      return new ArrayList<String>();
    } else if (!baseFolder.isDirectory()) {
      Log.e(TAG, "[getAllFilesUnderFolder] folder is not a directory: "
          + folder);
      return new ArrayList<String>();
    }

    // construct the set of starting directories and files to process
    File[] partials = baseFolder.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        if ( excludingNamedItemsUnderFolder == null ) {
          return true;
        } else {
          return !excludingNamedItemsUnderFolder.contains(pathname.getName());
        }
      }});

    LinkedList<File> unexploredDirs = new LinkedList<File>();
    List<File> nondirFiles = new ArrayList<File>();

    // copy the starting set into a queue of unexploredDirs
    // and a list of files to be sync'd
    for ( int i = 0 ; i < partials.length ; ++i ) {
      if ( partials[i].isDirectory() ) {
        unexploredDirs.add(partials[i]);
      } else {
        nondirFiles.add(partials[i]);
      }
    }

    while (!unexploredDirs.isEmpty()) {
      File exploring = unexploredDirs.removeFirst();
      File[] files = exploring.listFiles();
      for (File f : files) {
        if (f.isDirectory()) {
          // we'll need to explore it
          unexploredDirs.add(f);
        } else {
          // we'll add it to our list of files.
          nondirFiles.add(f);
        }
      }
    }

    List<String> relativePaths = new ArrayList<String>();
    // we want the relative path, so drop the necessary bets.
    for (File f : nondirFiles) {
      // +1 to exclude the separator.
      relativePaths.add(ODKFileUtils.asRelativePath(appName, f));
    }
    return relativePaths;
  }

  @Override
  public void syncAppLevelFiles(boolean pushLocalFiles) throws ResourceAccessException {
    List<OdkTablesFileManifestEntry> manifest = getAppLevelFileManifest();
    for (OdkTablesFileManifestEntry entry : manifest) {
      compareAndDownloadFile(entry);
    }

    if (pushLocalFiles) {
      // And now get the files to upload. We only want those that exist on the
      // device but that do not exist on the manifest.
      Set<String> dirsToExclude = ODKFileUtils.getDirectoriesToExcludeFromSync(true);
      String appFolder = ODKFileUtils.getAppFolder(appName);
      List<String> relativePathsOnDevice = getAllFilesUnderFolder(appFolder,
          dirsToExclude);
      relativePathsOnDevice = filterOutTableIdAssetFiles(relativePathsOnDevice);
      List<String> relativePathsToUpload = getFilesToBeUploaded(relativePathsOnDevice, manifest);
      Log.e(TAG, "[syncAppLevelFiles] relativePathsToUpload: " + relativePathsToUpload);
      // and then upload the files.
      Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
      for (String relativePath : relativePathsToUpload) {
        String wholePathToFile = appFolder + File.separator + relativePath;
        successfulUploads.put(relativePath, uploadFile(wholePathToFile, relativePath));
      }
    }
  }

  @Override
  public void syncTableLevelFiles(String tableId, OnTablePropertiesChanged onChange, boolean pushLocal) throws ResourceAccessException {
    String tableIdPropertiesFile = "tables" + File.separator + tableId + File.separator + "properties.csv";

    boolean tablePropertiesChanged = false;
    List<OdkTablesFileManifestEntry> manifest = getTableLevelFileManifest(tableId);
    for (OdkTablesFileManifestEntry entry : manifest) {
      boolean outcome = compareAndDownloadFile(entry);
      // and if it was the table properties file, remember whether it changed.
      if ( entry.filename.equals(tableIdPropertiesFile) ) {
        tablePropertiesChanged = outcome;
      }
    }

    if ( tablePropertiesChanged && (onChange != null) ) {
      // update this table's KVS values...
      onChange.onTablePropertiesChanged(tableId);
    }

    if (pushLocal) {
      // Then we actually do try and upload things. Otherwise we can just
      // continue straight on.
      String appFolder = ODKFileUtils.getAppFolder(appName);
      String tableFolder = ODKFileUtils.getTablesFolder(appName, tableId);

      // Get any assets/csv files that begin with tableId
      Set<String> dirsToExclude = new HashSet<String>();
      String assetsCsvFolder = ODKFileUtils.getAssetsFolder(appName) + "/csv";
      List<String> relativePathsToTableIdAssetsCsvOnDevice = getAllFilesUnderFolder(
          assetsCsvFolder, dirsToExclude);
      relativePathsToTableIdAssetsCsvOnDevice = filterInTableIdFiles(relativePathsToTableIdAssetsCsvOnDevice, tableId);

      // We don't want to sync anything in the instances directory, because this
      // contains things like media attachments. These should instead be synched
      // with a separate call.
      // tableDirsToExclude.add(TableFileUtils.DIR_INSTANCES);
      dirsToExclude.add(ODKFileUtils.INSTANCES_FOLDER_NAME);
      List<String> relativePathsToAppFolderOnDevice = getAllFilesUnderFolder(
          tableFolder, dirsToExclude);

      // mix in the assets files for this tableId, if any...
      relativePathsToAppFolderOnDevice.addAll(relativePathsToTableIdAssetsCsvOnDevice);
      List<String> relativePathsToUpload = getFilesToBeUploaded(relativePathsToAppFolderOnDevice,
          manifest);
      Log.i(TAG, "[syncNonMediaTableFiles] files to upload: " + relativePathsToUpload);
      // and then upload the files.
      Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
      for (String relativePath : relativePathsToUpload) {
        String wholePathToFile = appFolder + File.separator + relativePath;
        successfulUploads.put(relativePath, uploadFile(wholePathToFile, relativePath));
      }
    }
  }

  public List<OdkTablesFileManifestEntry> getAppLevelFileManifest() throws ResourceAccessException {
    URI fileManifestUri = SyncUtilities.normalizeUri(aggregateUri, getManifestUriFragment());
    Uri.Builder uriBuilder = Uri.parse(fileManifestUri.toString()).buildUpon();
    ResponseEntity<OdkTablesFileManifest> responseEntity;
    responseEntity = rt.exchange(uriBuilder.build().toString(),
            HttpMethod.GET, null, OdkTablesFileManifest.class);
    return responseEntity.getBody().getEntries();
  }

  public List<OdkTablesFileManifestEntry> getTableLevelFileManifest(String tableId) throws ResourceAccessException {
    URI fileManifestUri = SyncUtilities.normalizeUri(aggregateUri, getManifestUriFragment() + tableId);
    Uri.Builder uriBuilder = Uri.parse(fileManifestUri.toString()).buildUpon();
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
    URI filesUri = SyncUtilities.normalizeUri(aggregateUri, getFilePathURI() + escapedPath);
    Log.i(TAG, "[uploadFile] filePostUri: " + filesUri.toString());
    RestTemplate rt = SyncUtil.getRestTemplateForFiles();
    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(this.baseUri, accessToken));
    rt.setInterceptors(interceptors);
    URI responseUri = rt.postForLocation(filesUri, resource);
    // TODO: verify whether or not this worked.
    return true;
  }

  private boolean uploadInstanceFile(String wholePathToFile, String tableId, String pathRelativeToInstancesFolder) {
    File file = new File(wholePathToFile);
    FileSystemResource resource = new FileSystemResource(file);
    String escapedPath = SyncUtil.formatPathForAggregate(pathRelativeToInstancesFolder);
    URI filesUri = SyncUtilities.normalizeUri(aggregateUri, getTablesUriFragment() + tableId + "/attachments/file/" + escapedPath);
    Log.i(TAG, "[uploadFile] filePostUri: " + filesUri.toString());
    RestTemplate rt = SyncUtil.getRestTemplateForFiles();
    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(this.baseUri, accessToken));
    rt.setInterceptors(interceptors);
    URI responseUri = rt.postForLocation(filesUri, resource);
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
    URI filesUri = SyncUtilities.normalizeUri(aggregateUri, getFilePathURI() + escapedPath);
    return filesUri;
  }

  /**
   *
   * @param entry
   * @return
   */
  private boolean compareAndDownloadFile(OdkTablesFileManifestEntry entry) {
    String basePath = ODKFileUtils.getAppFolder(appName);
    // now we need to look through the manifest and see where the files are
    // supposed to be stored.
    // make sure you don't return a bad string.
    if (entry.filename == null || entry.filename.equals("")) {
      Log.i(TAG, "returned a null or empty filename");
      return false;
    } else {
      // filename is the unrooted path of the file, so prepend the basepath.
      String path = basePath + File.separator + entry.filename;
      // Before we try dl'ing the file, we have to make the folder,
      // b/c otherwise if the folders down to the path have too many non-
      // existent folders, we'll get a FileNotFoundException when we open
      // the FileOutputStream.
      File newFile = new File(path);
      String folderPath = newFile.getParent();
      ODKFileUtils.createFolder(folderPath);
      if (!newFile.exists()) {
        // the file doesn't exist on the system
        // filesToDL.add(newFile);
        try {
          return downloadFile(newFile, entry.downloadUrl);
        } catch (Exception e) {
          e.printStackTrace();
          Log.e(TAG, "trouble downloading file for first time");
          return false;
        }
      } else {
        // file exists, see if it's up to date
        String md5hash = ODKFileUtils.getMd5Hash(newFile);
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
          // no change
          return false;
        }
      }
    }
  }

  /**
   *
   * @param destFile
   * @param downloadUrl
   * @return true if the download was successful
   * @throws Exception
   */
  public boolean downloadFile(File destFile, String downloadUrl) throws Exception {
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
      req.setHeader(ApiConstants.ACCEPT_CONTENT_ENCODING_HEADER, ApiConstants.GZIP_CONTENT_ENCODING);
      req.setHeader(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION);
      GregorianCalendar g = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
      g.setTime(new Date());
      SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zz");
      formatter.setCalendar(g);
      req.setHeader(ApiConstants.DATE_HEADER, formatter.format(new Date()));

      if (accessToken != null && uri != null) {
        if (uri.getHost().equals(baseUri.getHost())
            && uri.getPort() == baseUri.getPort()) {
          req.setHeader("Authorization", "Bearer " + accessToken);
        }
      }

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

        File tmp = new File(destFile.getParentFile(), destFile.getName() + ".tmp");
        // write connection to file
        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        try {
          Header[] encodings = response.getHeaders(ApiConstants.CONTENT_ENCODING_HEADER);
          boolean isCompressed = false;
          if (encodings != null ) {
            for ( int i = 0 ; i < encodings.length ; ++i ) {
              if ( encodings[i].getValue().equalsIgnoreCase(ApiConstants.GZIP_CONTENT_ENCODING) ) {
                isCompressed = true;
                break;
              }
            }
          }
          InputStream isRaw;
          if ( isCompressed ) {
            isRaw = new GZIPInputStream(response.getEntity().getContent());
          } else {
            isRaw = response.getEntity().getContent();
          }

          is = new BufferedInputStream(isRaw);
          os = new BufferedOutputStream(new FileOutputStream(tmp));
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
          success = tmp.renameTo(destFile);
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
          if ( tmp.exists() ) {
            tmp.delete();
          }
        }

      } catch (Exception e) {
        e.printStackTrace();
        if (attemptCount != 1) {
          throw e;
        }
      }
    }
    return success;
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
  public void syncRowDataFiles(String tableId, boolean pushLocalInstanceFiles) throws ResourceAccessException {
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
    String pathPrefix = "tables/" + tableId + "/instances/";

    // 1) Get the manifest.
    // TODO: this is currently just getting the same table-level manifest as
    // syncNonMediaTableFiles(). In reality it should be making its own call.
    List<OdkTablesFileManifestEntry> manifest;

    // get the list of all instance files (should not do this in production!)
    URI instanceFileManifestUri = SyncUtilities.normalizeUri(aggregateUri, getTablesUriFragment() + tableId + "/attachments/manifest/" );
    Uri.Builder uriBuilder = Uri.parse(instanceFileManifestUri.toString()).buildUpon();
    ResponseEntity<OdkTablesFileManifest> responseEntity;
      responseEntity = rt.exchange(uriBuilder.build().toString(),
              HttpMethod.GET, null, OdkTablesFileManifest.class);
    manifest = responseEntity.getBody().getEntries();

    for (OdkTablesFileManifestEntry entry : manifest) {
      entry.filename = pathPrefix + entry.filename;
      compareAndDownloadFile(entry);
    }

    if ( pushLocalInstanceFiles ) {
      // Then we actually do try and upload things. Otherwise we can just
      // continue straight on.
      String appFolder = ODKFileUtils.getAppFolder(appName);
      String instancesFolderFullPath = ODKFileUtils.getInstancesFolder(appName, tableId);
      List<String> relativePathsToAppFolderOnDevice = getAllFilesUnderFolder(
          instancesFolderFullPath, null);
      List<String> relativePathsToUpload = getFilesToBeUploaded(relativePathsToAppFolderOnDevice,
          manifest);
      Log.e(TAG, "[syncRowDataFiles] relativePathsToUpload: " + relativePathsToUpload);
      // and then upload the files.
      Map<String, Boolean> successfulUploads = new HashMap<String, Boolean>();
      for (String relativePath : relativePathsToUpload) {
        String wholePathToFile = appFolder + File.separator + relativePath;
        String partialPath = relativePath.substring(pathPrefix.length());
        successfulUploads.put(relativePath, uploadInstanceFile(wholePathToFile, tableId, partialPath));
      }
    }
  }
}
