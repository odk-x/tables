/*
 * Copyright (C) 2014 University of Washington
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
import java.util.HashMap;
import java.util.Map;

import org.opendatakit.common.android.data.ElementType;
import org.opendatakit.common.android.utilities.DataUtil;
import org.opendatakit.tables.activities.AbsBaseActivity;

import android.content.Context;
import android.widget.LinearLayout;

public class ElementTypeManipulator {
  public static abstract class InputView extends LinearLayout {

     protected final DataUtil du;
     
      public InputView(Context context, DataUtil du) {
          super(context);
          this.du = du;
          setOrientation(LinearLayout.VERTICAL);
      }

      public abstract boolean isValidValue();

      public abstract String getDbValue();
  }

  public interface ITypeManipulatorFragment {
    public String   getElementTypeDisplayLabel();
    public String   getCollectType();
    /* 
     * DataUtil:
        if ( cp.getColumnType() == ColumnType.DATE ) {
            return formatLongDateTimeForUser(parseDateTimeFromDb(value));
        } else if ( cp.getColumnType() == ColumnType.DATETIME ) {
         // TODO: do we need special conversion
            return formatLongDateTimeForUser(parseDateTimeFromDb(value));
        } else if ( cp.getColumnType() == ColumnType.TIME ) {
         // TODO: do we need special conversion
            return formatLongDateTimeForUser(parseDateTimeFromDb(value));
        } else if ( cp.getColumnType() == ColumnType.DATE_RANGE ) {
            return formatLongIntervalForUser(parseIntervalFromDb(value));
        } else {
            return value;
        }

     * 
     */
    public String formatForCollect(DataUtil dataUtil, String databaseValue);
    public Class<?> getDatabaseType();
    /*
     * DataUtil:
        if ( cp.getColumnType() == ColumnType.DATE ) {
            return validifyDateValue(input);
        } else if ( cp.getColumnType() == ColumnType.DATETIME ) {
            return validifyDateTimeValue(input);
        } else if ( cp.getColumnType() == ColumnType.TIME ) {
            return validifyTimeValue(input);
        } else if ( cp.getColumnType() == ColumnType.DATE_RANGE ) {
            return validifyDateRangeValue(input);
        } else if ( cp.getColumnType() == ColumnType.NUMBER ) {
            return validifyNumberValue(input);
        } else if ( cp.getColumnType() == ColumnType.INTEGER ) {
            return validifyIntegerValue(input);
        } else if ( cp.getColumnType() == ColumnType.MC_OPTIONS ) {
            return validifyMultipleChoiceValue(cp, input);
        } else if ( cp.getColumnType() == ColumnType.GEOPOINT ) {
            return validifyLocationValue(input);
        } else {
            return input;
        }

     */
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue);
    /*
     * DataUtil:
     *        
      if (columnType == ColumnType.DATE) {
        throw new UnsupportedOperationException(
            "DATE parsing not implemented!");
      } else if (columnType == ColumnType.DATETIME) {
        throw new UnsupportedOperationException(
            "DATETIME parsing not implemented!");
      } else if (columnType == ColumnType.TIME) {
        throw new UnsupportedOperationException(
            "TIME parsing not implemented!");
      } else if (columnType == ColumnType.DATE_RANGE) {
        throw new UnsupportedOperationException(
            "DATE_RANGE parsing not implemented!");
      } else if (columnType == ColumnType.NUMBER) {
        double numberValue = Double.parseDouble(rawValue);
        contentValues.put(contentValuesKey, numberValue);
      } else if (columnType == ColumnType.INTEGER) {
        int integerValue = Integer.parseInt(rawValue);
        contentValues.put(contentValuesKey, integerValue);
      } else if (columnType == ColumnType.MC_OPTIONS) {
        throw new UnsupportedOperationException(
            "MC_OPTIONS parsing not implemented!");
      } else if (columnType == ColumnType.GEOPOINT) {
        throw new UnsupportedOperationException(
            "GEOPOINT parsing not implemented!");
      } else  if (columnType == ColumnType.STRING) {
        // take the raw string itself
        contentValues.put(contentValuesKey, rawValue);
      } else {
        // The list of column types I've elected to support above is based
        // entirely on those in validifyValue(). For not just alert that
        // something is fishy, as we otherwise need to figure out how to parse
        // it.
        throw new IllegalArgumentException(
            "not currently handling column type: " + columnType);
      }

     */
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue, Class<T> clazz);
    
    /*
     * InputScreenUtil
     *  
        if ( cp.getColumnType() == ColumnType.DATE ) {
            return new DateInputView(context, value);
        } else if ( cp.getColumnType() == ColumnType.DATETIME ) {
            return new DateTimeInputView(context, value);
        } else if ( cp.getColumnType() == ColumnType.TIME ) {
            return new TimeInputView(context, value);
        } else if ( cp.getColumnType() == ColumnType.DATE_RANGE ) {
            return new DateRangeInputView(context, value);
        } else if ( cp.getColumnType() == ColumnType.MC_OPTIONS ) {
            return new McOptionsInputView(context,
                    cp.getDisplayChoicesList(), value);
        } else {
            return new GeneralInputView(context, value);
        }

     */
    public InputView getInputView(AbsBaseActivity context, DataUtil du, String value);
  }
  
  private HashMap<String, ITypeManipulatorFragment> renderers = new HashMap<String, ITypeManipulatorFragment>();
  
  public void addTypeManipulatorFragment(String type, ITypeManipulatorFragment frag) {
    renderers.put(type,  frag);
  }
  
  public ITypeManipulatorFragment getSpecialRenderer(ElementType type) {
    ITypeManipulatorFragment r = renderers.get(type.getElementType());
    return r;
  }
  
  public ITypeManipulatorFragment getDefaultRenderer(ElementType type) {
    ITypeManipulatorFragment r = getSpecialRenderer(type);
    if ( r == null ) {
      r = ElementTypeManipulatorFactory.getCustomManipulatorFragment(type);
      renderers.put(type.getElementType(), r);
    }
    return r;
  }
}
