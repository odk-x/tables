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

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.utilities.DataUtil;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;
import org.opendatakit.tables.utils.ElementTypeManipulator.InputView;

import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;


public class InputScreenUtil {

    private final AbsBaseActivity context;

    public InputScreenUtil(AbsBaseActivity context) {
        this.context = context;
//        du = new DataUtil(Locale.ENGLISH, TimeZone.getDefault());;
    }

    public InputView getInputView(ColumnDefinition cd, DataUtil du) {
        return getInputView(cd, du, null);
    }

    public InputView getInputView(ColumnDefinition cd, DataUtil du, String value) {
      ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance(context.getAppName());
      ITypeManipulatorFragment r = m.getDefaultRenderer(cd.getType());
      return r.getInputView(context, du, value);
    }

    public static class GeneralInputView extends InputView {

        private final EditText field;

        public GeneralInputView(Context context, DataUtil du, String value) {
            super(context, du);
            value = (value == null) ? "" : value;
            field = new EditText(context);
            field.setText(value);
            addView(field);
        }

        public boolean isValidValue() {
            return true;
        }

        public String getDbValue() {
            return field.getText().toString();
        }
    }

    public static class DateInputView extends InputView {

        private final EditText field;

        public DateInputView(Context context, DataUtil du, String value) {
            super(context, du);
            field = new EditText(context);
            if (value != null) {
                DateTime dt = du.parseDateTimeFromDb(value);
                field.setText(du.formatLongDateTimeForUser(dt));
            }
        }

        public boolean isValidValue() {
            String value = field.getText().toString();
            return (du.tryParseInstant(value) != null) ||
                (du.tryParseInterval(value) != null);
        }

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

    public static class DateTimeInputView extends InputView {

        private final EditText field;

        public DateTimeInputView(Context context, DataUtil du, String value) {
            super(context, du);
            field = new EditText(context);
            if (value != null) {
                DateTime dt = du.parseDateTimeFromDb(value);
                field.setText(du.formatLongDateTimeForUser(dt));
            }
        }

        public boolean isValidValue() {
        	// TODO: does this need to be altered/revised vs. DateInputView
            String value = field.getText().toString();
            return (du.tryParseInstant(value) != null) ||
                (du.tryParseInterval(value) != null);
        }

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

    public static class TimeInputView extends InputView {

        private final EditText field;

        public TimeInputView(Context context, DataUtil du, String value) {
            super(context, du);
            field = new EditText(context);
            if (value != null) {
                DateTime dt = du.parseDateTimeFromDb(value);
                field.setText(du.formatLongDateTimeForUser(dt));
            }
        }

        public boolean isValidValue() {
        	// TODO: does this need to be altered/revised vs. DateInputView
            String value = field.getText().toString();
            return (du.tryParseInstant(value) != null) ||
                (du.tryParseInterval(value) != null);
        }

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

    public static class DateRangeInputView extends InputView {

        private final EditText field;

        public DateRangeInputView(Context context, DataUtil du, String value) {
            super(context, du);
            field = new EditText(context);
            if (value != null) {
                Interval interval = du.parseIntervalFromDb(value);
                field.setText(du.formatLongIntervalForUser(interval));
            }
        }

        public boolean isValidValue() {
            String value = field.getText().toString();
            return du.tryParseInterval(value) != null;
        }

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

    public static class McOptionsInputView extends InputView {

        private final Spinner spinner;
        private final ArrayAdapter<String> adapter;
        private final String originalValue;

        public McOptionsInputView(Context context, DataUtil du, ArrayList<String> arrayList,
                String value) {
            super(context, du);
            originalValue = value;
            spinner = new Spinner(context);
            adapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_spinner_item, arrayList);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
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
            return spinner.getSelectedItemPosition() !=
                AdapterView.INVALID_POSITION;
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
