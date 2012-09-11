package org.opendatakit.tables.util;

/**
 * This is a general place for utils regarding odktables files. These are files
 * that are associated with various tables, such as html files for different
 * views, etc.
 * @author sudar.sam@gmail.com
 *
 */
public class TableFileUtils {
  
  /** The base path to which the files are stored. */
  public static final String filesDirectory = "odktables/tableFiles/";
  
  /** Timeout (in ms) we specify for each http request */
  public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;
  
  /** URI from base to get the manifest for a server. */
  public static final String MANIFEST_ADDR_URI = "/tableKeyValueManifest";
  
  /** The url parameter name of the tableId. */
  public static final String TABLE_ID_PARAM = "tableId";
  
  /** The response type expected from the server for a json object. */
  public static final String RESP_TYPE_JSON = "application/json; charset=utf-8";

}
