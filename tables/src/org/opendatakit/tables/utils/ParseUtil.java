/*
 * Copyright (C) 2012-2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.utils;

import java.util.ArrayList;
import java.util.Map;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.utilities.DataUtil;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;

public class ParseUtil {

  public static String validifyValue(DataUtil du, ArrayList<Map<String,Object>> choices, ColumnDefinition cd, String input) {
      if ( input == null ) {
        // TODO: should we check for required values?
        // null values are always accepted (???)
        return input;
      }
      ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance();
      ITypeManipulatorFragment r = m.getDefaultRenderer(cd.getType());

      return r.verifyValidityAndNormalizeValue(du, choices, input);
  }

}
