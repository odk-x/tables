/*
 * Copyright (C) 2012 University of Washington
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

import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;
import org.opendatakit.tables.utils.ElementTypeManipulator.InputView;
import org.opendatakit.utilities.DateUtils;

import java.util.ArrayList;

/**
 * Helper class that contains several static classes that extend InputView
 */
public class InputScreenUtil {

  private final AbsBaseActivity context;

  /**
   * Sets the context based on what it was passed
   *
   * @param context the context to execute in
   */
  public InputScreenUtil(AbsBaseActivity context) {
    this.context = context;
    //        du = new DateUtils(Locale.ENGLISH, TimeZone.getDefault());;
  }

  /**
   * Forwards the request to the three argument one, appears to be unused
   *
   * @param cd the column to get an input view for
   * @param du a DateUtils object to use to format and verify dates, times, datetimes and date
   *           ranges
   * @return {@see getInputView}
   */

  public InputView getInputView(ColumnDefinition cd, DateUtils du) {
    return getInputView(cd, du, null);
  }

  /**
   * Constructs an input view using some type manipulators and the context
   *
   * @param cd    the column to get an input view for
   * @param du    a DateUtils object to use to format and verify dates, times, datetimes and date
   *              ranges
   * @param value the initial value to put in the text box
   * @return An input view that can be used for
   */
  public InputView getInputView(ColumnDefinition cd, DateUtils du, String value) {
    ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance(context.getAppName());
    ITypeManipulatorFragment r = m.getDefaultRenderer(cd.getType());
    return r.getInputView(context, du, value);
  }

  /**
   * An input view that has an EditText field for users to enter text into
   */
  public static class GeneralInputView extends InputView {

    // The field for the user to enter text into
    private final EditText field;

    /**
     * Constructor that sets the context, sets the EditText field's preset text to the passed
     * value, and adds the field to the view
     *
     * @param context the context to execute in
     * @param du      a DateUtils object to use to format/interpret dates, unused
     * @param value   what to set the EditText field's text to when starting
     */
    public GeneralInputView(Context context, DateUtils du, String value) {
      super(context, du);
      value = (value == null) ? "" : value;
      field = new EditText(context);
      field.setText(value);
      addView(field);
    }

    /**
     * All text is considered valid for a simple text only input view
     *
     * @return true
     */
    public boolean isValidValue() {
      return true;
    }

    /**
     * Returns the text in the text editable box
     *
     * @return what's in the box
     */
    public String getDbValue() {
      return field.getText().toString();
    }
  }

  /**
   * An input view that has an EditText field that users can only enter valid dates into
   */
  public static class DateInputView extends InputView {

    // The field for the user to enter text into
    private final EditText field;

    /**
     * Constructor that sets the context and sets the EditText field's preset text to the
     * date it parses from passed value
     *
     * @param context the context to execute in
     * @param du      a DateUtils object to use to format/interpret dates, unused
     * @param value   what to set the EditText field's text to when starting
     */
    public DateInputView(Context context, DateUtils du, String value) {
      super(context, du);
      field = new EditText(context);
      if (value != null) {
        DateTime dt = du.parseDateTimeFromDb(value);
        field.setText(du.formatLongDateTimeForUser(dt));
      }
    }

    /**
     * Tries to parse the date in the text box, and returns false if it can't
     *
     * @return whether the text box contains a valid date
     */
    public boolean isValidValue() {
      String value = field.getText().toString();
      return (du.tryParseInstant(value) != null) || (du.tryParseInterval(value) != null);
    }

    /**
     * Tries to parse the date in the text box and return a database-safe representation of it if
     * possible
     *
     * @return a database-safe representation of what the user put in the text box
     */
    public String getDbValue() {
      String value = field.getText().toString();
      DateTime dt = du.tryParseInstant(value);
      if (dt != null) {
        return du.formatDateTimeForDb(dt);
      }
      Interval interval = du.tryParseInterval(value);
      if (interval == null) {
        return null;
      } else {
        return du.formatDateTimeForDb(interval.getStart());
      }
    }
  }

  /**
   * An input view that has an EditText field that users can only enter valid datetime values into
   */
  public static class DateTimeInputView extends InputView {

    // The field for the user to enter text into
    private final EditText field;

    /**
     * Constructor that sets the context and sets the EditText field's preset text to the
     * datetime it parses from passed value
     *
     * @param context the context to execute in
     * @param du      a DateUtils object to use to format/interpret dates, unused
     * @param value   what to set the EditText field's text to when starting
     */
    public DateTimeInputView(Context context, DateUtils du, String value) {
      super(context, du);
      field = new EditText(context);
      if (value != null) {
        DateTime dt = du.parseDateTimeFromDb(value);
        field.setText(du.formatLongDateTimeForUser(dt));
      }
    }

    /**
     * Tries to parse the datetime in the text box, and returns false if it can't
     *
     * @return whether the text box contains a valid datetime
     */
    public boolean isValidValue() {
      // TODO: does this need to be altered/revised vs. DateInputView
      String value = field.getText().toString();
      return (du.tryParseInstant(value) != null) || (du.tryParseInterval(value) != null);
    }

    /**
     * Tries to parse the datetime in the text box and return a database-safe representation of
     * it if possible
     *
     * @return a database-safe representation of what the user put in the text box
     */
    public String getDbValue() {
      String value = field.getText().toString();
      DateTime dt = du.tryParseInstant(value);
      if (dt != null) {
        return du.formatDateTimeForDb(dt);
      }
      Interval interval = du.tryParseInterval(value);
      if (interval == null) {
        return null;
      } else {
        return du.formatDateTimeForDb(interval.getStart());
      }
    }
  }

  /**
   * An input view that has an EditText field that users can only enter valid time values into
   */
  public static class TimeInputView extends InputView {

    // The field for the user to enter text into
    private final EditText field;

    /**
     * Constructor that sets the context and sets the EditText field's preset text to the
     * time it parses from passed value
     *
     * @param context the context to execute in
     * @param du      a DateUtils object to use to format/interpret dates, unused
     * @param value   what to set the EditText field's text to when starting
     */
    public TimeInputView(Context context, DateUtils du, String value) {
      super(context, du);
      field = new EditText(context);
      if (value != null) {
        DateTime dt = du.parseDateTimeFromDb(value);
        field.setText(du.formatLongDateTimeForUser(dt));
      }
    }

    /**
     * Tries to parse the time in the text box, and returns false if it can't
     *
     * @return whether the text box contains a valid time
     */
    public boolean isValidValue() {
      // TODO: does this need to be altered/revised vs. DateInputView
      String value = field.getText().toString();
      return (du.tryParseInstant(value) != null) || (du.tryParseInterval(value) != null);
    }

    /**
     * Tries to parse the time in the text box and return a database-safe representation of it if
     * possible
     *
     * @return a database-safe representation of what the user put in the text box
     */
    public String getDbValue() {
      String value = field.getText().toString();
      DateTime dt = du.tryParseInstant(value);
      if (dt != null) {
        return du.formatDateTimeForDb(dt);
      }
      Interval interval = du.tryParseInterval(value);
      if (interval == null) {
        return null;
      } else {
        return du.formatDateTimeForDb(interval.getStart());
      }
    }
  }

  /**
   * An input view that has an EditText field that users can only enter valid date range values into
   */
  public static class DateRangeInputView extends InputView {

    // The field for the user to enter text into
    private final EditText field;

    /**
     * Constructor that sets the context and sets the EditText field's preset text to the
     * date range it parses from passed value
     *
     * @param context the context to execute in
     * @param du      a DateUtils object to use to format/interpret dates, unused
     * @param value   what to set the EditText field's text to when starting
     */
    public DateRangeInputView(Context context, DateUtils du, String value) {
      super(context, du);
      field = new EditText(context);
      if (value != null) {
        Interval interval = du.parseIntervalFromDb(value);
        field.setText(du.formatLongIntervalForUser(interval));
      }
    }

    /**
     * Tries to parse the date range in the text box, and returns false if it can't
     *
     * @return whether the text box contains a valid date range
     */
    public boolean isValidValue() {
      String value = field.getText().toString();
      return du.tryParseInterval(value) != null;
    }

    /**
     * Tries to parse the date range in the text box and return a database-safe representation of
     * it if possible
     *
     * @return a database-safe representation of what the user put in the text box
     */
    public String getDbValue() {
      String value = field.getText().toString();
      Interval interval = du.tryParseInterval(value);
      if (interval == null) {
        return null;
      } else {
        return du.formatIntervalForDb(interval);
      }
    }
  }

  /**
   * unused. Used to be used in ElementTypeManipulator but that class is a graveyard
   */
  public static class McOptionsInputView extends InputView {

    private final Spinner spinner;
    private final ArrayAdapter<String> adapter;
    private final String originalValue;

    public McOptionsInputView(Context context, DateUtils du, ArrayList<String> arrayList,
        String value) {
      super(context, du);
      originalValue = value;
      spinner = new Spinner(context);
      adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, arrayList);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      spinner.setAdapter(adapter);
      int optIndex = -1;
      for (int i = 0; i < arrayList.size(); i++) {
        if ((value != null) && value.equalsIgnoreCase(arrayList.get(i))) {
          optIndex = i;
        }
      }
      if (optIndex >= 0) {
        spinner.setSelection(optIndex);
      }
      addView(spinner);
    }

    public boolean isValidValue() {
      return spinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION;
    }

    public String getDbValue() {
      int pos = spinner.getSelectedItemPosition();
      if (pos == AdapterView.INVALID_POSITION) {
        return originalValue;
      } else {
        return adapter.getItem(pos);
      }
    }
  }
}
