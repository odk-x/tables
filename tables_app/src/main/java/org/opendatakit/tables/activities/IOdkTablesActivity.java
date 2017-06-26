package org.opendatakit.tables.activities;

import org.opendatakit.activities.IOdkCommonActivity;
import org.opendatakit.activities.IOdkDataActivity;

/**
 * @author mitchellsundt@gmail.com
 */
public interface IOdkTablesActivity extends IOdkCommonActivity, IOdkDataActivity {

  /**
   * Takes a fragment id and if that fragment exists and is a webkit, returns the url currently
   * being viewed in the fragment
   * @param ifChanged unused
   * @param fragmentID the id of the webview fragment, if there are more than one
   * @return The url of the requested fragment or null
   */
  String getUrlBaseLocation(boolean ifChanged, String fragmentID);

  /**
   * If there is a map view fragment, return the index of the selected item
   *
   * @return null if not a map view or no item selected; otherwise, selected item index.
   */
  Integer getIndexOfSelectedItem();
}
