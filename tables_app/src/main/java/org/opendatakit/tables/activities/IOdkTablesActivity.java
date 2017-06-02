package org.opendatakit.tables.activities;

import org.opendatakit.activities.IOdkCommonActivity;
import org.opendatakit.activities.IOdkDataActivity;

/**
 * @author mitchellsundt@gmail.com
 */
public interface IOdkTablesActivity extends IOdkCommonActivity, IOdkDataActivity {

  String getUrlBaseLocation(boolean ifChanged, String fragmentID);

  /**
   * If there is a map view fragment, return the index of the selected item
   *
   * @return null if not a map view or no item selected; otherwise, selected item index.
   */
  Integer getIndexOfSelectedItem();
}
