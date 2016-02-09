package org.opendatakit.tables.activities;

import org.opendatakit.common.android.activities.IOdkCommonActivity;
import org.opendatakit.common.android.activities.IOdkDataActivity;

/**
 * @author mitchellsundt@gmail.com
 */
public interface IOdkTablesActivity extends IOdkCommonActivity, IOdkDataActivity {

  public String getUrlBaseLocation(boolean ifChanged);

}
