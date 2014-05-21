package org.opendatakit.tables.provider;

import android.net.Uri;

public class TablesProviderAPI {
  public static final String AUTHORITY = "org.opendatakit.common.android.provider.tables";
  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");

  // This class cannot be instantiated
  private TablesProviderAPI() {
  }

}
