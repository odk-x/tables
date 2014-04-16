package org.opendatakit.tables.data;

import org.opendatakit.common.android.database.DataModelDatabaseHelper;

public enum KeyValueStoreType {
  ACTIVE(DataModelDatabaseHelper.KEY_VALUE_STORE_ACTIVE_TABLE_NAME),
  DEFAULT(DataModelDatabaseHelper.KEY_VALUE_STORE_DEFAULT_TABLE_NAME),
  SERVER(DataModelDatabaseHelper.KEY_VALUE_STORE_SERVER_TABLE_NAME);

  private String backingName;

  private KeyValueStoreType(String backingName) {
    this.backingName = backingName;
  }

  public String getBackingName() {
    return backingName;
  }
}