package org.opendatakit.tables.activities;

import org.opendatakit.activities.IOdkCommonActivity;
import org.opendatakit.activities.IOdkDataActivity;

/**
 * @author mitchellsundt@gmail.com
 */
public interface IOdkTablesActivity extends IOdkCommonActivity, IOdkDataActivity {

  public String getUrlBaseLocation(boolean ifChanged);

}
