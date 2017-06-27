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
package org.opendatakit.tables.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.views.components.ColorRuleAdapter;

import java.util.Map;

/**
 * Fragment for displaying a list of status color rules.
 */
public class StatusColorRuleListFragment extends ListFragment {

  private static final String TAG = StatusColorRuleListFragment.class.getSimpleName();

  /**
   * The group of color rules being displayed by this list.
   */
  ColorRuleGroup mColorRuleGroup = null;
  ColorRuleAdapter mColorRuleAdapter = null;

  /**
   * Retrieve a new instance of {@link StatusColorRuleListFragment} with the
   * appropriate values set in its arguments.
   *
   * @param colorRuleType the color group type to use
   * @return a new StatusColorRuleListFragment, configured with the requested color rule group type
   */
  public static StatusColorRuleListFragment newInstance(ColorRuleGroup.Type colorRuleType) {
    StatusColorRuleListFragment result = new StatusColorRuleListFragment();
    Bundle bundle = new Bundle();
    IntentUtil.addColorRuleGroupTypeToBundle(bundle, colorRuleType);
    result.setArguments(bundle);
    return result;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof TableLevelPreferencesActivity)) {
      throw new IllegalArgumentException(
          "must be attached to a " + TableLevelPreferencesActivity.class.getSimpleName());
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_color_rule_list, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    //this.setHasOptionsMenu(true);
    PropertiesSingleton props = CommonToolProperties.get(Tables.getInstance(), getAppName());
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();
    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    TableUtil.TableColumns tc = null;
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(getAppName());

      tc = TableUtil.get()
          .getTableColumns(userSelectedDefaultLocale, dbInterface, getAppName(), db, getTableId());
      this.mColorRuleGroup = this.retrieveColorRuleGroup(dbInterface, db, tc.adminColumns);
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      throw new IllegalStateException("Unable to access database");
    } finally {
      if (db != null) {
        try {
          dbInterface.closeDatabase(getAppName(), db);
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(getAppName()).printStackTrace(e);
          WebLogger.getLogger(getAppName()).e(TAG, "Error while initializing color rule list");
          Toast.makeText(getActivity(), "Error while initializing color rule list",
              Toast.LENGTH_LONG).show();
        }
      }
    }
    this.mColorRuleAdapter = createColorRuleAdapter(tc.adminColumns, tc.localizedDisplayNames);
    this.setListAdapter(this.mColorRuleAdapter);
    this.registerForContextMenu(this.getListView());
  }

  ColorRuleAdapter createColorRuleAdapter(String[] adminColumns,
      Map<String, String> colDisplayNames) {
    ColorRuleGroup.Type type = this.retrieveColorRuleType();
    return new ColorRuleAdapter(getActivity(), getAppName(), R.layout.row_for_view_entry,
        adminColumns, colDisplayNames, mColorRuleGroup.getColorRules(), type);
  }

  /**
   * Retrieve the {@link ColorRuleGroup.Type} from the arguments passed to this
   * fragment.
   *
   * @return the color rule type we were created with
   */
  ColorRuleGroup.Type retrieveColorRuleType() {
    return IntentUtil.retrieveColorRuleTypeFromBundle(this.getArguments());
  }

  /**
   * Retrieve the {@link TableLevelPreferencesActivity} hosting this fragment.
   *
   * @return getActivity casted to a TableLevelPreferencesActivity
   */
  TableLevelPreferencesActivity retrieveTableLevelPreferencesActivity() {
    Activity act = getActivity();
    if (act instanceof TableLevelPreferencesActivity) {
      return (TableLevelPreferencesActivity) act;
    }
    throw new IllegalStateException("Must be inside something with a table ID");
  }

  String getAppName() {
    return ((IAppAwareActivity) getActivity()).getAppName();
  }

  String getTableId() {
    return retrieveTableLevelPreferencesActivity().getTableId();
  }

  ColorRuleGroup retrieveColorRuleGroup(UserDbInterface dbInterface, DbHandle db,
      String[] adminColumns) throws ServicesAvailabilityException {
    ColorRuleGroup.Type type = this.retrieveColorRuleType();
    ColorRuleGroup result;
    switch (type) {
    case STATUS_COLUMN:
      result = ColorRuleGroup
          .getStatusColumnRuleGroup(dbInterface, getAppName(), db, getTableId(), adminColumns);
      break;
    default:
      throw new IllegalArgumentException("unrecognized color rule group type: " + type);
    }
    return result;
  }

}
