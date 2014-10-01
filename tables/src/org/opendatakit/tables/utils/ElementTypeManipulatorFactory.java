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
import java.util.Map;

import org.opendatakit.common.android.utilities.DataUtil;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;
import org.opendatakit.tables.utils.ElementTypeManipulator.InputView;
import org.opendatakit.tables.utils.InputScreenUtil.DateInputView;
import org.opendatakit.tables.utils.InputScreenUtil.DateRangeInputView;
import org.opendatakit.tables.utils.InputScreenUtil.DateTimeInputView;
import org.opendatakit.tables.utils.InputScreenUtil.GeneralInputView;
import org.opendatakit.tables.utils.InputScreenUtil.TimeInputView;

import android.content.Context;

public class ElementTypeManipulatorFactory {

  private static class DateManipulator implements ITypeManipulatorFragment {

    @Override
    public String getElementTypeDisplayLabel() {
      return "Date";
    }

    @Override
    public String getCollectType() {
      return "date";
    }

    @Override
    public Class<?> getDatabaseType() {
      return String.class;
    }

    @Override
    public String formatForCollect(DataUtil dataUtil, String databaseValue) {
      return dataUtil.formatLongDateTimeForUser(dataUtil.parseDateTimeFromDb(databaseValue));
    }

    @Override
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList,
        String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyDateValue(inValue);
    }

    @Override
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue,
        Class<T> clazz) {
      // TODO: verify against choices list
      throw new UnsupportedOperationException(
          "DATE parsing not implemented!");
    }

    @Override
    public InputView getInputView(Context context, DataUtil du, String value) {
      return new DateInputView(context, du, value);
    }
  }

  private static class DateTimeManipulator implements ITypeManipulatorFragment {

    @Override
    public String getElementTypeDisplayLabel() {
      return "Date and Time";
    }

    @Override
    public String getCollectType() {
      return "dateTime";
    }

    @Override
    public Class<?> getDatabaseType() {
      return String.class;
    }

    @Override
    public String formatForCollect(DataUtil dataUtil, String databaseValue) {
      return dataUtil.formatLongDateTimeForUser(dataUtil.parseDateTimeFromDb(databaseValue));
    }

    @Override
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList,
        String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyDateTimeValue(inValue);
    }

    @Override
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue,
        Class<T> clazz) {
      // TODO: verify against choices list
      throw new UnsupportedOperationException(
          "DATETIME parsing not implemented!");
    }

    @Override
    public InputView getInputView(Context context, DataUtil du, String value) {
      return new DateTimeInputView(context, du, value);
    }
  }


  private static class TimeManipulator implements ITypeManipulatorFragment {

    @Override
    public String getElementTypeDisplayLabel() {
      return "Time";
    }

    @Override
    public String getCollectType() {
      return "time";
    }

    @Override
    public Class<?> getDatabaseType() {
      return String.class;
    }

    @Override
    public String formatForCollect(DataUtil dataUtil, String databaseValue) {
      return dataUtil.formatLongDateTimeForUser(dataUtil.parseDateTimeFromDb(databaseValue));
    }

    @Override
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList,
        String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyTimeValue(inValue);
    }

    @Override
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue,
        Class<T> clazz) {
      // TODO: verify against choices list
      throw new UnsupportedOperationException(
          "TIME parsing not implemented!");
    }

    @Override
    public InputView getInputView(Context context, DataUtil du, String value) {
      return new TimeInputView(context, du, value);
    }
  }

  private static class DateRangeManipulator implements ITypeManipulatorFragment {

    @Override
    public String getElementTypeDisplayLabel() {
      return "Date Range";
    }

    @Override
    public String getCollectType() {
      return "string";
    }

    @Override
    public Class<?> getDatabaseType() {
      return String.class;
    }

    @Override
    public String formatForCollect(DataUtil dataUtil, String databaseValue) {
      return dataUtil.formatLongIntervalForUser(dataUtil.parseIntervalFromDb(databaseValue));
    }

    @Override
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList,
        String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyDateRangeValue(inValue);
    }

    @Override
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue,
        Class<T> clazz) {
      // TODO: verify against choices list
      throw new UnsupportedOperationException(
          "DATE_RANGE parsing not implemented!");
    }

    @Override
    public InputView getInputView(Context context, DataUtil du, String value) {
      return new DateRangeInputView(context, du, value);
    }
  }

  private static class IntegerManipulator implements ITypeManipulatorFragment {

    @Override
    public String getElementTypeDisplayLabel() {
      return "Integer";
    }

    @Override
    public String getCollectType() {
      return "integer";
    }

    @Override
    public Class<?> getDatabaseType() {
      return Integer.class;
    }

    @Override
    public String formatForCollect(DataUtil dataUtil, String databaseValue) {
      return databaseValue;
    }

    @Override
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList,
        String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyIntegerValue(inValue);
    }

    @Override
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue,
        Class<T> clazz) {
      // TODO: verify against choices list
      if ( inValue == null ) {
        return null;
      }
      int integerValue = Integer.parseInt(inValue);
      return (T) Integer.valueOf(integerValue);
    }

    @Override
    public InputView getInputView(Context context, DataUtil du, String value) {
      return new GeneralInputView(context, du, value);
    }
  }

  private static class NumberManipulator implements ITypeManipulatorFragment {

    @Override
    public String getElementTypeDisplayLabel() {
      return "Number";
    }

    @Override
    public String getCollectType() {
      return "number";
    }

    @Override
    public Class<?> getDatabaseType() {
      return Double.class;
    }

    @Override
    public String formatForCollect(DataUtil dataUtil, String databaseValue) {
      return databaseValue;
    }

    @Override
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList,
        String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyNumberValue(inValue);
    }

    @Override
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue,
        Class<T> clazz) {
      // TODO: verify against choices list
      if ( inValue == null ) {
        return null;
      }
      double numberValue = Double.parseDouble(inValue);
      return (T) Double.valueOf(numberValue);
    }

    @Override
    public InputView getInputView(Context context, DataUtil du, String value) {
      return new GeneralInputView(context, du, value);
    }
  }

  private static class BoolManipulator implements ITypeManipulatorFragment {

    @Override
    public String getElementTypeDisplayLabel() {
      return "Boolean";
    }

    @Override
    public String getCollectType() {
      return "string";
    }

    @Override
    public Class<?> getDatabaseType() {
      return Integer.class;
    }

    @Override
    public String formatForCollect(DataUtil dataUtil, String databaseValue) {
      if ( databaseValue == null ) {
        return null;
      }
      int intValue = Integer.parseInt(databaseValue);
      if ( intValue == 0 ) {
        return "false";
      }
      return "true";
    }

    @Override
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList,
        String inValue) {
      // TODO: verify against choices list
      if ( inValue == null ) {
        return null;
      }
      if ( inValue.equalsIgnoreCase("true") ) {
        return "true";
      }
      if ( inValue.equalsIgnoreCase("false") ) {
        return "false";
      }
      throw new IllegalArgumentException("Unexpected boolean value: " + inValue);
    }

    @Override
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue,
        Class<T> clazz) {
      // TODO: verify against choices list
      if ( inValue == null ) {
        return null;
      }
      if ( inValue.equalsIgnoreCase("true") ) {
        return (T) Integer.valueOf(1);
      }
      if ( inValue.equalsIgnoreCase("false") ) {
        return (T) Integer.valueOf(0);
      }
      throw new IllegalArgumentException("invalid boolean value: " + inValue);
    }

    @Override
    public InputView getInputView(Context context, DataUtil du, String value) {
      return new GeneralInputView(context, du, value);
    }
  }

  private static class StringManipulator implements ITypeManipulatorFragment {

    @Override
    public String getElementTypeDisplayLabel() {
      return "Text";
    }

    @Override
    public String getCollectType() {
      return "string";
    }

    @Override
    public Class<?> getDatabaseType() {
      return String.class;
    }

    @Override
    public String formatForCollect(DataUtil dataUtil, String databaseValue) {
      return databaseValue;
    }

    @Override
    public String verifyValidityAndNormalizeValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList,
        String inValue) {
      // TODO: verify against choices list
      return inValue;
    }

    @Override
    public <T> T parseStringValue(DataUtil dataUtil, ArrayList<Map<String,Object>> displayChoicesList, String inValue,
        Class<T> clazz) {
      // TODO: verify against choices list
      return (T) inValue;
    }

    @Override
    public InputView getInputView(Context context, DataUtil du, String value) {
      return new GeneralInputView(context, du, value);
    }
  }

//  public static final String IMAGEURI = "imageUri";
//  public static final String AUDIOURI = "audioUri";
//  public static final String VIDEOURI = "videoUri";
//  public static final String MIMEURI = "mimeUri";

  static ElementTypeManipulator manipulator = null;
  
  public static final synchronized ElementTypeManipulator getInstance() {
    if ( manipulator == null ) {
      manipulator = new ElementTypeManipulator();
      manipulator.addTypeManipulatorFragment("date", new DateManipulator());
      manipulator.addTypeManipulatorFragment("dateTime", new DateTimeManipulator());
      manipulator.addTypeManipulatorFragment("time", new TimeManipulator());
      manipulator.addTypeManipulatorFragment("dateRange", new DateRangeManipulator());
      manipulator.addTypeManipulatorFragment("integer", new IntegerManipulator());
      manipulator.addTypeManipulatorFragment("number", new NumberManipulator());
      manipulator.addTypeManipulatorFragment("bool", new BoolManipulator());
      manipulator.addTypeManipulatorFragment("string", new StringManipulator());
    }

    return manipulator;
  }

}
