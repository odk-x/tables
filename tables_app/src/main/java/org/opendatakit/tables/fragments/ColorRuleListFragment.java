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

import android.app.AlertDialog;
import androidx.fragment.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import org.opendatakit.data.ColorRule;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.ColorRuleUtil;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fragment for displaying a list of color rules. All methods for dealing with
 * color rules are hideous and need to be revisited.
 *
 * @author sudar.sam@gmail.com
 */
public class ColorRuleListFragment extends ListFragment {

  private static final String TAG = ColorRuleListFragment.class.getSimpleName();

  /**
   * The group of color rules being displayed by this list.
   */
  ColorRuleGroup mColorRuleGroup;
  /**
   * An adapter for the group of color rules being displayed by this list.
   */
  ColorRuleAdapter mColorRuleAdapter;

  /**
   * Retrieve a new instance of {@link ColorRuleListFragment} with the
   * appropriate values set in its arguments.
   *
   * @param colorRuleType the color rule group type to put in the arguments of the new fragment
   * @return a color rule list fragment with the correct color rule group type
   */
  public static ColorRuleListFragment newInstance(ColorRuleGroup.Type colorRuleType) {
    ColorRuleListFragment result = new ColorRuleListFragment();
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
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    TableLevelPreferencesActivity activity = this.retrieveTableLevelPreferencesActivity();
    ColorRuleGroup.Type colorRuleGroupType = this.retrieveColorRuleType();
    activity.showEditColorRuleFragmentForExistingRule(colorRuleGroupType, activity.getElementKey(),
        position);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_color_rule_list, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.setHasOptionsMenu(true);
    PropertiesSingleton props = CommonToolProperties
        .get(getActivity().getApplication(), getAppName());
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
    this.mColorRuleAdapter = this.createColorRuleAdapter(tc.adminColumns, tc.localizedDisplayNames);
    this.setListAdapter(this.mColorRuleAdapter);
    this.registerForContextMenu(this.getListView());
  }

  /**
   * Creates an adapter using the same color rule type
   *
   * @param adminColumns    a list of hidden columns in the table
   * @param colDisplayNames a list of the display names of the columns in the table
   * @return a new ColorRuleAdapter with the right color rule type
   */
  ColorRuleAdapter createColorRuleAdapter(String[] adminColumns,
      Map<String, String> colDisplayNames) {
    ColorRuleGroup.Type type = this.retrieveColorRuleType();
    return new ColorRuleAdapter(getActivity(), getAppName(), R.layout.row_for_edit_view_entry,
        adminColumns, colDisplayNames, mColorRuleGroup.getColorRules(), type);
  }

  /**
   * Retrieve the {@link ColorRuleGroup.Type} from the arguments passed to this
   * fragment.
   *
   * @return the color rule type this fragment was created with
   */
  ColorRuleGroup.Type retrieveColorRuleType() {
    return IntentUtil.retrieveColorRuleTypeFromBundle(getArguments());
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // All of the color rule lists fragments are the same.
    inflater.inflate(R.menu.menu_color_rule_list, menu);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    MenuInflater menuInflater = this.getActivity().getMenuInflater();
    menuInflater.inflate(R.menu.context_menu_color_rule_list, menu);
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    ColorRuleGroup.Type colorRuleGroupType = this.retrieveColorRuleType();
    TableLevelPreferencesActivity activity = this.retrieveTableLevelPreferencesActivity();

    int itemId = item.getItemId();

    if (itemId == R.id.menu_color_rule_list_new) {
      activity.showEditColorRuleFragmentForNewRule(colorRuleGroupType, activity.getElementKey());
      return true;
    } else if (itemId == R.id.menu_color_rule_list_revert) {
      AlertDialog confirmRevertAlert;
      AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
      alert.setTitle(getString(R.string.color_rule_confirm_revert_status_column));
      alert.setMessage(getString(R.string.color_rule_revert_are_you_sure));

      alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          try {
            revertToDefaults();
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(getAppName()).printStackTrace(e);
            WebLogger.getLogger(getAppName()).e(TAG, "Error while restoring color rules");
            Toast.makeText(getActivity(), "Error while restoring color rules", Toast.LENGTH_LONG).show();
          }
        }
      });

      alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          // canceled, so do nothing!
        }
      });

      confirmRevertAlert = alert.create();
      confirmRevertAlert.show();
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }


  @Override
  public boolean onContextItemSelected(MenuItem item) {
    TableLevelPreferencesActivity activity = this.retrieveTableLevelPreferencesActivity();
    final String appName = activity.getAppName();
    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
    final int position = menuInfo.position;
    if (item.getItemId() == R.id.context_menu_delete_color_rule) {
      // Make an alert dialog that will give them the option to delete it or
      // cancel.
      AlertDialog confirmDeleteAlert;
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.confirm_delete_color_rule);
      builder.setMessage(getString(R.string.are_you_sure_delete_color_rule,
          " " + mColorRuleGroup.getColorRules().get(position).getOperator().getSymbol() + " "
              + mColorRuleGroup.getColorRules().get(position).getVal()));

      // For the OK action we want to actually delete this list view.
      builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          // We need to delete the entry. First delete it in the key value
          // store.
          WebLogger.getLogger(appName).d(TAG, "trying to delete rule at position: " + position);
          mColorRuleGroup.getColorRules().remove(position);
          try {
            mColorRuleGroup.saveRuleList(Tables.getInstance().getDatabase());
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(getAppName()).printStackTrace(e);
            WebLogger.getLogger(getAppName()).e(TAG, "Error while saving color rules");
            Toast.makeText(getActivity(), "Error while saving color rules", Toast.LENGTH_LONG)
                .show();
          }
          mColorRuleAdapter.notifyDataSetChanged();
        }
      });

      builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          // Canceled. Do nothing.
        }
      });
      confirmDeleteAlert = builder.create();
      confirmDeleteAlert.show();
      return true;
    } else {
      return super.onContextItemSelected(item);
    }
  }

  /**
   * Wipe the current rules and revert to the defaults for the given type.
   *
   * @throws ServicesAvailabilityException if the database is down
   */
  private void revertToDefaults() throws ServicesAvailabilityException {
    ColorRuleGroup.Type colorRuleGroupType = this.retrieveColorRuleType();
    switch (colorRuleGroupType) {
    case STATUS_COLUMN:
      // replace the rules.
      List<ColorRule> newList = new ArrayList<>(ColorRuleUtil.getDefaultSyncStateColorRules());
      this.mColorRuleGroup.replaceColorRuleList(newList);
      this.mColorRuleGroup.saveRuleList(Tables.getInstance().getDatabase());
      this.mColorRuleAdapter.notifyDataSetChanged();
      break;
    case COLUMN:
    case TABLE:
      // We want to just wipe all the columns for both of these types.
      List<ColorRule> emptyList = new ArrayList<>();
      this.mColorRuleGroup.replaceColorRuleList(emptyList);
      this.mColorRuleGroup.saveRuleList(Tables.getInstance().getDatabase());
      this.mColorRuleAdapter.notifyDataSetChanged();
      break;
    }
  }

  /**
   * Retrieve the {@link TableLevelPreferencesActivity} hosting this fragment.
   *
   * @return the activity casted to something with a getTableId and getElementKey method
   */
  TableLevelPreferencesActivity retrieveTableLevelPreferencesActivity() {
    return (TableLevelPreferencesActivity) this.getActivity();
  }

  String getAppName() {
    return retrieveTableLevelPreferencesActivity().getAppName();
  }

  String getTableId() {
    return retrieveTableLevelPreferencesActivity().getTableId();
  }

  /**
   * Returns a color rule group with the color rules for the color rule type
   *
   * @param dbInterface  a database handle to use
   * @param db           an open database connection
   * @param adminColumns a list of the hidden columns in the table
   * @return a ColorRuleGroup containing all the color rules for the status column
   * @throws ServicesAvailabilityException if the database is down
   */
  ColorRuleGroup retrieveColorRuleGroup(UserDbInterface dbInterface, DbHandle db,
      String[] adminColumns) throws ServicesAvailabilityException {
    ColorRuleGroup.Type type = retrieveColorRuleType();
    ColorRuleGroup result;
    switch (type) {
    case COLUMN:
      String elementKey = retrieveTableLevelPreferencesActivity().getElementKey();
      result = ColorRuleGroup
          .getColumnColorRuleGroup(dbInterface, getAppName(), db, getTableId(), elementKey,
              adminColumns);
      break;
    case STATUS_COLUMN:
      result = ColorRuleGroup
          .getStatusColumnRuleGroup(dbInterface, getAppName(), db, getTableId(), adminColumns);
      break;
    case TABLE:
      result = ColorRuleGroup
          .getTableColorRuleGroup(dbInterface, getAppName(), db, getTableId(), adminColumns);
      break;
    default:
      throw new IllegalArgumentException("Color rule group type was not present in the intent");
    }
    return result;
  }

}
