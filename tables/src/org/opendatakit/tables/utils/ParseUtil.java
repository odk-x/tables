package org.opendatakit.tables.utils;

import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.utilities.DataUtil;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;

public class ParseUtil {

  public static String validifyValue(DataUtil du, ColumnProperties cp, String input) {
      if ( input == null ) {
        // TODO: should we check for required values?
        // null values are always accepted (???)
        return input;
      }
      ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance();
      ITypeManipulatorFragment r = m.getDefaultRenderer(cp.getColumnType());
      return r.verifyValidityAndNormalizeValue(du, cp.getDisplayChoicesList(), input);
  }

}
