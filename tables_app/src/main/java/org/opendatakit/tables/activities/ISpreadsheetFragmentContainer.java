package org.opendatakit.tables.activities;

import org.opendatakit.tables.views.SpreadsheetProps;

/**
 * Created by Niles on 6/20/17.
 * Designates something that contains properties.
 */
public interface ISpreadsheetFragmentContainer {
  /**
   * Gets the spreadsheet properties (should be mutable!)
   *
   * @return the properties for the current view
   */
  SpreadsheetProps getProps();
}
