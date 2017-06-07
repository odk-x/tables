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

import org.apache.commons.lang3.StringUtils;
import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.aggregate.odktables.rest.ElementType;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;
import org.opendatakit.tables.utils.ElementTypeManipulator.InputView;
import org.opendatakit.tables.utils.InputScreenUtil.*;
import org.opendatakit.utilities.DateUtils;
import org.opendatakit.utilities.StaticStateManipulator;
import org.opendatakit.utilities.StaticStateManipulator.IStaticFieldManipulator;

import java.util.ArrayList;
import java.util.Map;

/**
 * Creates a singleton ElementTypeManipulator for each app name, returning the proper one from a
 * call to getInstance
 */
public class ElementTypeManipulatorFactory {

  /**
   * A date manipulator
   */
  private static class DateManipulator implements ITypeManipulatorFragment<Object> {

    /**
     * Returns the display name of the type of element that this can modify, which is Date
     * @return Date
     */
    @Override
    public String getElementTypeDisplayLabel() {
      return "Date";
    }

    /**
     * Returns the type that the sync endpoint expects, which is date
     * @return date
     */
    @Override
    public String getCollectType() {
      return "date";
    }

    /**
     * Returns the class that represents how the type that this object can modify is stored in
     * the database
     * @return String.class
     */
    @Override
    public Class<?> getDatabaseType() {
      return String.class;
    }

    /**
     * Formats the date in a way that the sync endpoint expects to see it
     * @param dataUtil a DateUtils object used for formatting the date
     * @param databaseValue the value that was stored in the local database
     * @return a string that the sync endpoint will be understand
     */
    @Override
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      return dataUtil.formatLongDateTimeForUser(dataUtil.parseDateTimeFromDb(databaseValue));
    }

    /**
     * Returns the inValue, but properly validated and formatted
     * @param dataUtil a DateUtils object used for formatting and parsing the date
     * @param displayChoicesList unused
     * @param inValue the input string to be verified
     * @return a properly validated and formatted representation of inValue
     */
    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      // TODO: verify against choices list
      // TODO: what did he mean by this
      return dataUtil.validifyDateValue(inValue);
    }

    /**
     * Do not call this method
     * @param dataUtil unused
     * @param displayChoicesList unused
     * @param inValue unused
     * @param clazz unused
     * @return can't return
     */
    @Override
    public Object parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<Object> clazz) {
      // TODO: verify against choices list
      throw new UnsupportedOperationException("DATE parsing not implemented!");
    }

    /**
     * Returns a simple input view defined in InputScreenUtils that the user can use to enter a
     * date with a default value
     * @param context the context to execute in
     * @param du a DateUtils object used for verifying and parsing dates
     * @param value the default value for the text box
     * @return a DateInputView
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      return new DateInputView(context, du, value);
    }
  }

  private static class DateTimeManipulator implements ITypeManipulatorFragment<Object> {

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
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      return dataUtil.formatLongDateTimeForUser(dataUtil.parseDateTimeFromDb(databaseValue));
    }

    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyDateTimeValue(inValue);
    }

    @Override
    public Object parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<Object> clazz) {
      // TODO: verify against choices list
      throw new UnsupportedOperationException("DATETIME parsing not implemented!");
    }

    /**
     * Returns a simple input view defined in InputScreenUtils that the user can use to enter a
     * date and time with a default value
     * @param context the context to execute in
     * @param du a DateUtils object used for verifying and parsing dates
     * @param value the default value for the text box
     * @return a DateTimeInputView
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      return new DateTimeInputView(context, du, value);
    }
  }

  private static class TimeManipulator implements ITypeManipulatorFragment<Object> {

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
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      return dataUtil.formatLongDateTimeForUser(dataUtil.parseDateTimeFromDb(databaseValue));
    }

    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyTimeValue(inValue);
    }

    @Override
    public Object parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<Object> clazz) {
      // TODO: verify against choices list
      throw new UnsupportedOperationException("TIME parsing not implemented!");
    }

    /**
     * Returns a simple input view defined in InputScreenUtils that the user can use to enter a
     * time with a default value
     * @param context the context to execute in
     * @param du a DateUtils object used for verifying and parsing dates
     * @param value the default value for the text box
     * @return a TimeInputView
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      return new TimeInputView(context, du, value);
    }
  }

  private static class DateRangeManipulator implements ITypeManipulatorFragment<Object> {
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
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      return dataUtil.formatLongIntervalForUser(dataUtil.parseIntervalFromDb(databaseValue));
    }

    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyDateRangeValue(inValue);
    }

    @Override
    public Object parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<Object> clazz) {
      // TODO: verify against choices list
      throw new UnsupportedOperationException("DATE_RANGE parsing not implemented!");
    }

    /**
     * Returns a simple input view defined in InputScreenUtils that the user can use to enter a
     * date range with a default value
     * @param context the context to execute in
     * @param du a DateUtils object used for verifying and parsing dates
     * @param value the default value for the text box
     * @return a DateRangeInputView
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      return new DateRangeInputView(context, du, value);
    }
  }

  private static class IntegerManipulator implements ITypeManipulatorFragment<Integer> {

    ElementType type;

    IntegerManipulator(ElementType type) {
      this.type = type;
    }

    IntegerManipulator() {
      this.type = null;
    }

    @Override
    public String getElementTypeDisplayLabel() {
      if (type != null) {
        return StringUtils.capitalize(type.getElementType());
      } else {
        return "Integer";
      }
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
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      return databaseValue;
    }

    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyIntegerValue(inValue);
    }

    @Override
    public Integer parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<Integer> clazz) {
      // TODO: verify against choices list
      if (inValue == null) {
        return null;
      }
      return Integer.parseInt(inValue);
    }

    /**
     * Returns a simple input view defined in InputScreenUtils that the user can use to enter
     * some text with a default value
     * @param context the context to execute in
     * @param du a DateUtils object used for verifying and parsing dates
     * @param value the default value for the text box
     * @return a GeneralInputView
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      return new GeneralInputView(context, du, value);
    }
  }

  private static class NumberManipulator implements ITypeManipulatorFragment<Double> {

    ElementType type;

    NumberManipulator(ElementType type) {
      this.type = type;
    }

    NumberManipulator() {
      this.type = null;
    }

    @Override
    public String getElementTypeDisplayLabel() {
      if (type != null) {
        return StringUtils.capitalize(type.getElementType());
      } else {
        return "Number";
      }
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
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      return databaseValue;
    }

    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      // TODO: verify against choices list
      return dataUtil.validifyNumberValue(inValue);
    }

    @Override
    public Double parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<Double> clazz) {
      // TODO: verify against choices list
      if (inValue == null) {
        return null;
      }
      return Double.parseDouble(inValue);
    }

    /**
     * Returns a simple input view defined in InputScreenUtils that the user can use to enter
     * some text with a default value
     * @param context the context to execute in
     * @param du a DateUtils object used for verifying and parsing dates
     * @param value the default value for the text box
     * @return a General
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      return new GeneralInputView(context, du, value);
    }
  }

  private static class BoolManipulator implements ITypeManipulatorFragment<Integer> {

    ElementType type;

    BoolManipulator(ElementType type) {
      this.type = type;
    }

    BoolManipulator() {
      this.type = null;
    }

    @Override
    public String getElementTypeDisplayLabel() {
      if (type != null) {
        return StringUtils.capitalize(type.getElementType());
      } else {
        return "Boolean";
      }
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
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      if (databaseValue == null) {
        return null;
      }
      int intValue = Integer.parseInt(databaseValue);
      if (intValue == 0) {
        return "false";
      }
      return "true";
    }

    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      // TODO: verify against choices list
      if (inValue == null) {
        return null;
      }
      if (inValue.equalsIgnoreCase("true")) {
        return "true";
      }
      if (inValue.equalsIgnoreCase("false")) {
        return "false";
      }
      throw new IllegalArgumentException("Unexpected boolean value: " + inValue);
    }

    @Override
    public Integer parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<Integer> clazz) {
      // TODO: verify against choices list
      if (inValue == null) {
        return null;
      }
      if (inValue.equalsIgnoreCase("true")) {
        return 1;
      }
      if (inValue.equalsIgnoreCase("false")) {
        return 0;
      }
      throw new IllegalArgumentException("invalid boolean value: " + inValue);
    }

    /**
     * Returns a simple input view defined in InputScreenUtils that the user can use to enter
     * some text with a default value
     * @param context the context to execute in
     * @param du a DateUtils object used for verifying and parsing dates
     * @param value the default value for the text box
     * @return a General
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      return new GeneralInputView(context, du, value);
    }
  }

  private static class StringManipulator implements ITypeManipulatorFragment<String> {

    ElementType type;

    StringManipulator(ElementType type) {
      this.type = type;
    }

    StringManipulator() {
      this.type = null;
    }

    @Override
    public String getElementTypeDisplayLabel() {
      if (type != null) {
        return StringUtils.capitalize(type.getElementType());
      } else {
        return "Text";
      }
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
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      return databaseValue;
    }

    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      // TODO: verify against choices list
      return inValue;
    }

    @Override
    public String parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<String> clazz) {
      // TODO: verify against choices list
      return inValue;
    }

    /**
     * Returns a simple input view defined in InputScreenUtils that the user can use to enter
     * some text with a default value
     * @param context the context to execute in
     * @param du a DateUtils object used for verifying and parsing dates
     * @param value the default value for the text box
     * @return a General
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      return new GeneralInputView(context, du, value);
    }
  }

  private static class ObjectManipulator implements ITypeManipulatorFragment<Object> {

    ElementType type;

    ObjectManipulator(ElementType type) {
      this.type = type;
    }

    @Override
    public String getElementTypeDisplayLabel() {
      return StringUtils.capitalize(type.getElementType());
    }

    @Override
    public String getCollectType() {
      throw new UnsupportedOperationException("this should not be called");
    }

    @Override
    public Class<?> getDatabaseType() {
      throw new UnsupportedOperationException("this should not be called");
    }

    @Override
    public String formatForCollect(DateUtils dataUtil, String databaseValue) {
      throw new UnsupportedOperationException("this should not be called");
    }

    @Override
    public String verifyValidityAndNormalizeValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue) {
      throw new UnsupportedOperationException("this should not be called");
    }

    @Override
    public Object parseStringValue(DateUtils dataUtil,
        ArrayList<Map<String, Object>> displayChoicesList, String inValue, Class<Object> clazz) {
      throw new UnsupportedOperationException("this should not be called");
    }

    /**
     * Do not call this method
     * @param context unused
     * @param du unused
     * @param value unused
     * @return doesn't return
     */
    @Override
    public InputView getInputView(AbsBaseActivity context, DateUtils du, String value) {
      throw new UnsupportedOperationException("this should not be called");
    }
  }

  //  public static final String IMAGEURI = "imageUri";
  //  public static final String AUDIOURI = "audioUri";
  //  public static final String VIDEOURI = "videoUri";
  //  public static final String MIMEURI = "mimeUri";

  private static String gAppName = null;
  private static ElementTypeManipulator gManipulator = null;

  public static synchronized ElementTypeManipulator getInstance(String appName) {
    if (gManipulator == null || (gAppName != null && !gAppName.equals(appName))) {
      ElementTypeManipulator manipulator = new ElementTypeManipulator();
      manipulator.addTypeManipulatorFragment("date", new DateManipulator());
      manipulator.addTypeManipulatorFragment("dateTime", new DateTimeManipulator());
      manipulator.addTypeManipulatorFragment("time", new TimeManipulator());
      manipulator.addTypeManipulatorFragment("dateRange", new DateRangeManipulator());
      manipulator.addTypeManipulatorFragment("integer", new IntegerManipulator());
      manipulator.addTypeManipulatorFragment("number", new NumberManipulator());
      manipulator.addTypeManipulatorFragment("bool", new BoolManipulator());
      manipulator.addTypeManipulatorFragment("string", new StringManipulator());
      gAppName = appName;
      gManipulator = new ElementTypeManipulator();
    }

    return gManipulator;
  }

  static {
    StaticStateManipulator.get().register(90, new IStaticFieldManipulator() {
      @Override
      public void reset() {
        gAppName = null;
        gManipulator = null;
      }
    });
  }

  static ITypeManipulatorFragment getCustomManipulatorFragment(ElementType type) {
    if (type.getDataType() == ElementDataType.array) {
      return new ObjectManipulator(type);
    }
    if (type.getDataType() == ElementDataType.bool) {
      return new BoolManipulator();
    }
    if (type.getDataType() == ElementDataType.configpath) {
      return new StringManipulator(type);
    }
    if (type.getDataType() == ElementDataType.integer) {
      return new IntegerManipulator();
    }
    if (type.getDataType() == ElementDataType.number) {
      return new NumberManipulator();
    }
    if (type.getDataType() == ElementDataType.object) {
      return new ObjectManipulator(type);
    }
    if (type.getDataType() == ElementDataType.rowpath) {
      return new StringManipulator(type);
    }
    if (type.getDataType() == ElementDataType.string) {
      return new StringManipulator(type);
    }
    throw new IllegalStateException("unknown ElementDataType: " + type.getDataType().name());
  }
}
