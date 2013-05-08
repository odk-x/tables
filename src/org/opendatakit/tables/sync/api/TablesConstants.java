package org.opendatakit.tables.sync.api;

import java.util.HashSet;
import java.util.Set;

import org.opendatakit.common.android.provider.DataTableColumns;

/**
 * Contains various things that are constant in tables and must be known and
 * retained by Aggregate.
 * @author sudar.sam@gmail.com
 *
 */
public class TablesConstants {
  
  //TODO: should probably have an Aggregate Column object instead that will
  // allow you to specify type here.
  
  /*
   * These are the names of the shared columns. Included here so that they can
   * be accessed directly by aggregate.
   */
  public static final String URI_USER = DataTableColumns.URI_USER;
  public static final String TIMESTAMP = DataTableColumns.TIMESTAMP;
  public static final String FORM_ID = DataTableColumns.FORM_ID;
  public static final String INSTANCE_NAME = DataTableColumns.INSTANCE_NAME;
  public static final String LOCALE = DataTableColumns.LOCALE;  
  
  /**
   * This set contains the names of the  metadata columns that are present in
   * all ODKTables data tables. The data in these columns needs to be synched
   * to the server.
   */
  public static final Set<String> SHARED_COLUMN_NAMES;
  
  /**
   * This set contains the names of all the metadata columns that are specific
   * to each phone and whose data should NOT be synched onto the server and 
   * between phones.
   */
  public static final Set<String> CLIENT_ONLY_COLUMN_NAMES;
  
  static {
    SHARED_COLUMN_NAMES = new HashSet<String>();
    CLIENT_ONLY_COLUMN_NAMES = new HashSet<String>();
    SHARED_COLUMN_NAMES.add(DataTableColumns.URI_USER);
    SHARED_COLUMN_NAMES.add(DataTableColumns.TIMESTAMP);
    SHARED_COLUMN_NAMES.add(DataTableColumns.FORM_ID);
    SHARED_COLUMN_NAMES.add(DataTableColumns.INSTANCE_NAME);
    SHARED_COLUMN_NAMES.add(DataTableColumns.LOCALE);
    CLIENT_ONLY_COLUMN_NAMES.add(DataTableColumns.ID);
    CLIENT_ONLY_COLUMN_NAMES.add(DataTableColumns.ROW_ID);
    CLIENT_ONLY_COLUMN_NAMES.add(DataTableColumns.SAVED);
    CLIENT_ONLY_COLUMN_NAMES.add(DataTableColumns.SYNC_STATE);
    CLIENT_ONLY_COLUMN_NAMES.add(DataTableColumns.SYNC_TAG);
    CLIENT_ONLY_COLUMN_NAMES.add(DataTableColumns.TRANSACTIONING);
    }

}
