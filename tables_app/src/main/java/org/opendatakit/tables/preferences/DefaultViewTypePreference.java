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
package org.opendatakit.tables.preferences;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.widget.ListAdapter;
import android.widget.Toast;
import org.opendatakit.activities.BaseActivity;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.data.TableViewType;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.data.PossibleTableViewTypes;
import org.opendatakit.tables.views.components.TableViewTypeAdapter;

import java.util.Arrays;

/**
 * A table level preference that lets the user select the default view type for the table,
 * depending on what's available. See {@see PossibleTableViewTypes}, {@see TablePreferenceFragment}
 */
public class DefaultViewTypePreference extends ListPreference {

  /**
   * Used for logging
   */
  private static final String TAG = DefaultViewTypePreference.class.getSimpleName();
  private final String mAppName;
  /**
   * The view types allowed for the table this preference will display.
   */
  private PossibleTableViewTypes mPossibleViewTypes = null;
  private Context mContext;

  /**
   * Constructs a new DefaultViewTypePreference object
   *
   * @param context used for getting the table view types values resources
   * @param attrs   unused
   */
  public DefaultViewTypePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
    if (context instanceof IAppAwareActivity) {
      mAppName = ((IAppAwareActivity) context).getAppName();
    } else {
      throw new IllegalArgumentException("Must be in an activity that knows the app name");
    }
  }

  /**
   * sets up the fields based on the given table id
   *
   * @param tableId      the id of the table to populate results for
   * @param orderedDefns the columns in the table
   * @throws ServicesAvailabilityException if the database is down
   */
  public void setFields(String tableId, OrderedColumns orderedDefns)
      throws ServicesAvailabilityException {

    TableViewType defaultViewType = null;
    CharSequence[] mEntryValues = mContext.getResources()
        .getTextArray(R.array.table_view_types_values);

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(mAppName);
      mPossibleViewTypes = new PossibleTableViewTypes(dbInterface, mAppName, db, tableId,
          orderedDefns);
      // Let's set the currently selected one.
      defaultViewType = TableUtil.get().getDefaultViewType(dbInterface, mAppName, db, tableId);
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(mAppName, db);
      }
    }

    if (defaultViewType == null || !mPossibleViewTypes.getAllPossibleViewTypes()
        .contains(defaultViewType)) {
      // default to spreadsheet.
      this.setValueIndex(0);
    } else {
      int index = Arrays.asList(mEntryValues).indexOf(defaultViewType.name());
      if (index < 0) {
        // default to spreadsheet.
        index = 0;
      }
      this.setValueIndex(index);
    }
  }

  @Override
  protected void onPrepareDialogBuilder(Builder builder) {
    // We want to enable/disable the correct list.
    if (mPossibleViewTypes == null) {
      // The user rotated the screen and TablePreferenceFragment.initializeDefaultViewType hasn't
      // been called yet to call setFields
      if (mContext instanceof AbsTableActivity) {
        AbsTableActivity act = (AbsTableActivity) mContext;
        try {
          setFields(act.getTableId(), act.getColumnDefinitions());
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(act.getAppName()).e(TAG, "Could not access database");
          WebLogger.getLogger(act.getAppName()).printStackTrace(e);
          Toast.makeText(act, R.string.database_unavailable, Toast.LENGTH_LONG).show();
        }
      }
    }
    ListAdapter adapter = new TableViewTypeAdapter(mContext, mAppName,
        android.R.layout.select_dialog_singlechoice, getEntries(), getEntryValues(),
        mPossibleViewTypes);
    builder.setAdapter(adapter, this);
    super.onPrepareDialogBuilder(builder);
  }
}
