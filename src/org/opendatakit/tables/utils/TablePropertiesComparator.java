package org.opendatakit.tables.utils;

import java.util.Comparator;

import org.opendatakit.common.android.data.TableProperties;

/**
 * Compares two {@link TableProperties} objects based on their display names.
 * @author sudar.sam@gmail.com
 *
 */
public class TablePropertiesComparator implements Comparator<TableProperties> {

  @Override
  public int compare(TableProperties lhs, TableProperties rhs) {
    return String.CASE_INSENSITIVE_ORDER.compare(lhs.getDisplayName(), 
        rhs.getDisplayName());
  }

}
