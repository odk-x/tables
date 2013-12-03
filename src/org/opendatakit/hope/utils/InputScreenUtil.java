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
package org.opendatakit.hope.utils;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.opendatakit.hope.data.ColumnProperties;
import org.opendatakit.hope.data.ColumnType;
import org.opendatakit.hope.data.DataUtil;

import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;


public class InputScreenUtil {
    
    private final Context context;
    private final DataUtil du;
    
    public InputScreenUtil(Context context) {
        this.context = context;
        du = DataUtil.getDefaultDataUtil();
    }
    
    public InputView getInputView(ColumnProperties cp) {
        return getInputView(cp, null);
    }
    
    public InputView getInputView(ColumnProperties cp, String value) {
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
                    cp.getDisplayChoicesMap(), value);
        } else {
            return new GeneralInputView(context, value);
        }
    }
    
    public abstract class InputView extends LinearLayout {
        
        public InputView(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
        }
        
        public abstract boolean isValidValue();
        
        public abstract String getDbValue();
    }
    
    private class GeneralInputView extends InputView {
        
        private final EditText field;
        
        public GeneralInputView(Context context, String value) {
            super(context);
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
    
    private class DateInputView extends InputView {
        
        private final EditText field;
        
        public DateInputView(Context context, String value) {
            super(context);
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
    
    private class DateTimeInputView extends InputView {
        
        private final EditText field;
        
        public DateTimeInputView(Context context, String value) {
            super(context);
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
    
    private class TimeInputView extends InputView {
        
        private final EditText field;
        
        public TimeInputView(Context context, String value) {
            super(context);
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
    
    private class DateRangeInputView extends InputView {
        
        private final EditText field;
        
        public DateRangeInputView(Context context, String value) {
            super(context);
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
    
    private class McOptionsInputView extends InputView {
        
        private final Spinner spinner;
        private final ArrayAdapter<String> adapter;
        private final String originalValue;
        
        public McOptionsInputView(Context context, ArrayList<String> arrayList,
                String value) {
            super(context);
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
