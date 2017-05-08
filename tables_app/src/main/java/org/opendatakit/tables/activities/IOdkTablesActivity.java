package org.opendatakit.tables.activities;

import org.opendatakit.activities.IOdkCommonActivity;
import org.opendatakit.activities.IOdkDataActivity;

/**
 * @author mitchellsundt@gmail.com
 */
public interface IOdkTablesActivity extends IOdkCommonActivity, IOdkDataActivity {

  String getUrlBaseLocation(boolean ifChanged, String fragmentID);

}
