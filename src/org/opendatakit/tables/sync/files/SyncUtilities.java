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
package org.opendatakit.tables.sync.files;

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
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.httpclientandroidlib.HttpEntity;
import org.opendatakit.httpclientandroidlib.HttpResponse;
import org.opendatakit.httpclientandroidlib.HttpStatus;
import org.opendatakit.httpclientandroidlib.client.HttpClient;
import org.opendatakit.httpclientandroidlib.client.methods.HttpGet;
import org.opendatakit.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.opendatakit.httpclientandroidlib.impl.conn.BasicClientConnectionManager;
import org.opendatakit.httpclientandroidlib.params.HttpConnectionParams;
import org.opendatakit.httpclientandroidlib.params.HttpParams;
import org.opendatakit.httpclientandroidlib.util.EntityUtils;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.utils.FileUtils;
import org.opendatakit.tables.utils.TableFileUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri.Builder;
import android.util.Log;

/**
 * Methods that perform the muscle of actually syncing the files.
 * <p>
 * All the files are stored in the same directory. This is the directory
 * specified in {@link TableFileUtils} and is relative to the external storage
 * device. These files are compared to the manifest from the server and
 * necessary download are made.
 * @author sudar.sam@gmail.com
 *
 */
public class SyncUtilities {

  public static final String TAG = "SyncUtilities";

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Pull the key value entries for a particular table from the server.
   * <p>
   * This is only the Server->Phone direction. It overwrites all the key value
   * entries for the table.
   * @param dbh
   * @param appName
   * @param aggregateUri
   * @param authToken
   * @param tp the tableProperties you should be modifying
   */
  public static void pullKeyValueEntriesForTable(DbHelper dbh, String appName,
      String aggregateUri, String authToken, TableProperties tp) {
    String tableId = tp.getTableId();
    List<OdkTablesKeyValueStoreEntry> allEntries =
        getKeyValueEntries(aggregateUri, authToken, tableId);

    // so now allEntries should have had their file entries updated.
    // begin the database stuff.
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    // So I'm a bit confused as to why I have the dbhelper and pass
	    // it in here, and then just pass the db directly in? Hmm.
	    // I guess the helper is ensuring that the correct table has
	    // been asserted?
	    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
	    db.beginTransaction();
	    try {
	      // TODO fix the order here. here you can download new files
	      // but not get the values in the d.b. set. this is wrong.
	      // I need to figure out the correct way to fail here.
	      // I'm putting this w/in the transaction so if you change table
	      // properties and things fail, you don't end up with the entries in
	      // the key value store changed. You will still have downloaded the
	      // files though.
	      downloadFilesAndUpdateValues(appName, allEntries);
//	      KeyValueStore serverKVS = kvms.getStoreForTable(tableId,
//	          KeyValueStore.Type.SERVER);
	      KeyValueStore backingKVS = kvsm.getStoreForTable(tableId,
	          tp.getBackingStoreType());
	      // When the table properties are passed down from the server in the key
	      // value store, we will want to clear the original entries and overwrite
	      // them with the current state of the server. For now, however, we aren't
	      // going to do that, b/c the properties aren't coming down from the
	      // server via the manifest.
	      //kvs.clearKeyValuePairs(db);
	      backingKVS.addEntriesToStore(db, allEntries);
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      // TODO notify that the sync failed--resolve the file issue above
	      e.printStackTrace();
	    } finally {
	      db.endTransaction();
	    }
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
    }
  }

  /**
   * Get the manifest from the server. Based on NetworkUtilities syncContacts
   * method from the SyncAdapter example code.
   * @param aggregateUri
   * @param authToken
   * @param tableId the tableId of the table to get the manifest for
   * @return
   */
  public static String getManifest(String aggregateUri, String authToken,
      String tableId) {
    // We're going to navigate to the site and get the manifest for the
    // particular table. First add the argument and the table name.
    // make the URI using a builder.
    Builder builder = new Builder();
    builder.encodedPath(aggregateUri + TableFileUtils.MANIFEST_ADDR_URI);
    builder.appendQueryParameter(TableFileUtils.TABLE_ID_PARAM, tableId);
    //UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
    //ParameterValuePair params =
      //  sanitizer.new ParameterValuePair(TableFileUtils.TABLE_ID_PARAM, tableId);
    //builder.encodedQuery(params.mValue);
    HttpGet get = new HttpGet(builder.build().toString());
    try {
      HttpResponse resp = getHttpClient().execute(get);
      HttpEntity entity = resp.getEntity();
      // DURING DEBUG is coming back as html, not json.
      // make sure it is a json string.
      if (!entity.getContentType().getValue().equals(TableFileUtils.RESP_TYPE_JSON)) {
        Log.d(TAG, "entity returned in manifest request was not type JSON");
        // if it's not of type JSON, just return an empty string that can
        // then be parsed to a list with no problems. Ultimately you might want
        // to sync the files first or something.
        return "[]";
      }
      return EntityUtils.toString(entity);
    } catch (IOException e) {
      // TODO--figure out what to do here. Tell the SyncManager somehow?
      throw new IllegalStateException("the httpresponse failed");
    }
  }

  /**
   * Get an HttpClient.
   * @return HttpClient
   */
  public static HttpClient getHttpClient() {
    HttpClient httpClient = new DefaultHttpClient(new BasicClientConnectionManager());
    final HttpParams params = httpClient.getParams();
    HttpConnectionParams.setConnectionTimeout(params,
        TableFileUtils.HTTP_REQUEST_TIMEOUT_MS);
    HttpConnectionParams.setSoTimeout(params,
        TableFileUtils.HTTP_REQUEST_TIMEOUT_MS);
    return httpClient;
  }

  /**
   * Gets the key value entries from the store on the server for a particular
   * table.
   * @param aggregateUri
   * @param authToken
   * @param tableId
   * @return a list of OdkKeyValueStoreEntry objects.
   */
  public static List<OdkTablesKeyValueStoreEntry> getKeyValueEntries(
      String aggregateUri, String authToken, String tableId) {
    // Get and parse the manifest, which is a JSON string.
    String manifest = getManifest(aggregateUri, authToken, tableId);
    TypeReference<ArrayList<OdkTablesKeyValueStoreEntry>> typeRef =
        new TypeReference<ArrayList<OdkTablesKeyValueStoreEntry>>() {};
    try {
      ArrayList<OdkTablesKeyValueStoreEntry> entries =
          mapper.readValue(manifest, typeRef);
      return entries;
    //TODO--throw the correct errors here! these are hacks.
    } catch (JsonMappingException e) {
      throw new IllegalStateException("problem mapping in getKeyValueEntries");
    } catch (JsonParseException e) {
      throw new IllegalStateException("problem parsing in getKeyValueEntries");
    } catch (IOException e) {
      throw new IllegalStateException("io trouble in getKeyValueEntries");
    }
  }

  public static void downloadFilesAndUpdateValues(String appName,
      List<OdkTablesKeyValueStoreEntry> allEntries) {
    // If the entry is a file, we see if we need to download it and update
    // the value with the path to the file. Otherwise, we leave the entry.
    TypeReference<OdkTablesFileManifestEntry> typeRef =
        new TypeReference<OdkTablesFileManifestEntry>() {};
    for (OdkTablesKeyValueStoreEntry entry : allEntries) {
      String entryType = entry.type;
      String fileDesignation = Type.FILE.getTitle();
      if (entryType.equals(fileDesignation)) {
        String fileString = entry.value;
        String tableId = entry.tableId;
        try {
          OdkTablesFileManifestEntry fileEntry = mapper.readValue(
              fileString, typeRef);
          if (compareAndDownloadFile(appName, tableId, entry.key, fileEntry)) {
            String basePath = ODKFileUtils.getTablesFolder(appName, tableId);
            String path = basePath + File.separator + fileEntry.filename;
            entry.value = path;
          } else {
            // TODO:
            // there was an error downloading the file. remove the entry.
            // IS THIS A SAFE WAY TO DO IT?
            allEntries.remove(entry);
          }
        //TODO--throw the correct errors here! these are hacks.
        } catch (JsonMappingException e) {
          throw new IllegalStateException("problem mapping in getFileEntries");
        } catch (JsonParseException e) {
          throw new IllegalStateException("problem parsing in getFileEntries");
        } catch (IOException e) {
          throw new IllegalStateException("io trouble in getFileEntries");
        }
      }
    }
  }

  /**
   * Get only the file entries, leaving out the other entries.
   * @param allEntries List of all the entries on the manifest
   * @return a List of the OdkTablesFileManifestEntry objects
   */
  public static List<OdkTablesFileManifestEntry> getFileEntries(
      List<OdkTablesKeyValueStoreEntry> allEntries) {
    // first get all the entries, and retain only the file entries.
    List<OdkTablesFileManifestEntry> fileEntries =
        new ArrayList<OdkTablesFileManifestEntry>();
    TypeReference<OdkTablesFileManifestEntry> typeRef =
        new TypeReference<OdkTablesFileManifestEntry>() {};
    for (OdkTablesKeyValueStoreEntry entry : allEntries) {
      String entryType = entry.type;
      String fileDesignation = Type.FILE.getTitle();
      if (entryType.equals(fileDesignation)) {
        String fileString = (String) entry.value;
        try {
          fileEntries.add((OdkTablesFileManifestEntry) mapper.readValue(
              fileString, typeRef));
        //TODO--throw the correct errors here! these are hacks.
        } catch (JsonMappingException e) {
          throw new IllegalStateException("problem mapping in getFileEntries");
        } catch (JsonParseException e) {
          throw new IllegalStateException("problem parsing in getFileEntries");
        } catch (IOException e) {
          throw new IllegalStateException("io trouble in getFileEntries");
        }

      }
    }
    return fileEntries;
  }

  /**
   * Compares a single file to the information contained in an
   * OdkTablesFileManifestEntry and downloads the new file if necessary.
   * Returns true if the file was successfully downloaded and there were no
   * errors, or if the current version of the file already exists at the
   * given path externalFilesDir/tableId/filename.
   * @param context
   * @param tableId
   * @param key the key for the entry
   * @param fileEntry
   * @return
   */
  public static boolean compareAndDownloadFile(String appName, String tableId,
      String key, OdkTablesFileManifestEntry fileEntry) {
    Log.d(TAG, "in compareAndDownloadFile");
    // the path for the base of where the app can save its files.
    String basePath =  ODKFileUtils.getTablesFolder(appName, tableId);
    // now we need to look through the manifest and see where the files are
    // supposed to be stored.
      // make sure you don't return a bad string.
    if (fileEntry.filename.equals("") || fileEntry.filename == null) {
      Log.i(TAG, "returned a null or empty filename");
      return false;
     } else {
      // filename is the unrooted path of the file, so append the tableId
      // and the basepath.
      String path = basePath + File.separator + fileEntry.filename;
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
          downloadFile(newFile, fileEntry.downloadUrl);
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
        if (!md5hash.equals(fileEntry.md5hash)) {
          // it's not up to date, we need to download it.
          try {
            downloadFile(newFile, fileEntry.downloadUrl);
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

  /**
   * Make sure the files on the phone are up to date. Downloads the files
   * to the directory:
   *
   *   ODKFileUtils.getTablesFolder(TableFileUtils.ODK_TABLES_APP_NAME, tableId) +
   *    File.separator + fileEntry.filename
   *
   * @param context
   * @param manFiles
   * @return a List of files that need to be downloaded.
   */
  public static void compareAndDownloadFiles(Context context, String appName, String tableId,
      List<OdkTablesFileManifestEntry> manFiles) {
    Log.d(TAG, "in compareAndDownloadFiles");
    // the path for the base of where the app can save its files.
    String basePath =  ODKFileUtils.getTablesFolder(appName, tableId);
    // now we need to look through the manifest and see where the files are
    // supposed to be stored.
    for (OdkTablesFileManifestEntry fileEntry : manFiles) {
      // make sure you don't return a bad string.
      if (fileEntry.filename.equals("") || fileEntry.filename == null) {
        Log.i(TAG, "returned a null or empty filename");
      } else {
        // filename is the unrooted path of the file, so append the tableId
        // and the basepath.
        String path = basePath + File.separator + fileEntry.filename;
        File newFile = new File(path);
        if (!newFile.exists()) {
          // the file doesn't exist on the system
          //filesToDL.add(newFile);
          try {
            downloadFile(newFile, fileEntry.downloadUrl);
          } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "trouble downloading file for first time");
          }
        } else {
          // file exists, see if it's up to date
          String md5hash = FileUtils.getMd5Hash(newFile);
          md5hash = "md5:" + md5hash;
          // so as it comes down from the manifest, the md5 hash inclues a
          // "md5:" prefix. Add taht and then check.
          if (!md5hash.equals(fileEntry.md5hash)) {
            // it's not up to date, we need to download it.
            try {
              downloadFile(newFile, fileEntry.downloadUrl);
            } catch (Exception e) {
              e.printStackTrace();
              // TODO throw correct exception
              Log.e(TAG, "trouble downloading new version of existing file");
              throw new IllegalStateException("trouble dl'ing file");
            }
          }
        }
      }
    }
  }

  /**
   * sudar.sam@gmail.com: Took from Collect and purged xform stuff.
   * <p>
   * Common routine to download a document from the downloadUrl and save the
   * contents in the file 'f'. Shared by media file download and form file
   * download.
   *
   * @param f
   * @param downloadUrl
   * @throws Exception
   */
  private static void downloadFile(File f, String downloadUrl) throws Exception {
    URI uri = null;
    try {
      // assume the downloadUrl is escaped properly
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
      HttpClient httpclient = getHttpClient();

      // set up request...
      HttpGet req = new HttpGet(downloadUrl);

      HttpResponse response = null;
      try {
        response = httpclient.execute(req);
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != HttpStatus.SC_OK) {
          discardEntityBytes(response);
          if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            // clear the cookies -- should not be necessary?
            // ss: might just be a collect thing?
          }
          throw new Exception("status wasn't SC_OK when dl'ing file");
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

  // TODO: remove this??? Only used for FILE type.
  // TODO: Make ColumnType consistent impl between Aggregate and Tables
  /**
   * NB: This is currently copy/pasted from the aggregate workspace. Likely
   * going to want to get this working from a JAR or something.
   * <p>
   * These are the types that are currently supported in the datastore. They are
   * important for knowing how to generate the manifest of what needs to be
   * pushed to the phone.
   *
   * @author sudars
   *
   */
  public enum Type {
    STRING("string"), INTEGER("integer"), FILE("file");

    private final String title; // what you call the enum

    Type(String title) {
      this.title = title;
    }

    public String getTitle() {
      return this.title;
    }

  }

  /**
   * Utility to ensure that the entity stream of a response is drained of bytes.
   *
   * @param response
   */
  public static final void discardEntityBytes(HttpResponse response) {
    // may be a server that does not handle
    HttpEntity entity = response.getEntity();
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
