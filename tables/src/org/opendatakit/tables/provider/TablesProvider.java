package org.opendatakit.tables.provider;

import org.opendatakit.common.android.provider.impl.TablesProviderImpl;

public class TablesProvider extends TablesProviderImpl {

  public String getTablesAuthority() {
    return TablesProviderAPI.AUTHORITY;
  }
}
