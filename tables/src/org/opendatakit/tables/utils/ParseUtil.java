package org.opendatakit.tables.utils;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.utilities.DataUtil;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;

public class ParseUtil {

  public static String validifyValue(DataUtil du, TableProperties tp, ColumnDefinition cd, String input) {
      if ( input == null ) {
        // TODO: should we check for required values?
        // null values are always accepted (???)
        return input;
      }
      ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance();
      ITypeManipulatorFragment r = m.getDefaultRenderer(cd.getType());
      return r.verifyValidityAndNormalizeValue(du, ColumnUtil.getDisplayChoicesList(tp, cd.getElementKey()), input);
  }

}
