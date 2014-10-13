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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.data.ColorRule;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.utilities.ColorRuleUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.views.components.ColorRuleAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
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

/**
 * Fragment for displaying a list of color rules. All methods for dealing with
 * color rules are hideous and need to be revisited.
 * @author sudar.sam@gmail.com
 *
 */
public class ColorRuleListFragment extends ListFragment {
  
  private static final String TAG =
      ColorRuleListFragment.class.getSimpleName();
  
  /** The group of color rules being displayed by this list. */
  ColorRuleGroup mColorRuleGroup;
  ColorRuleAdapter mColorRuleAdapter;
  
  public ColorRuleListFragment() {
    // required for fragments.
  }
  
  /**
   * Retrieve a new instance of {@list ColorRuleListFragment} with the
   * appropriate values set in its arguments.
   * @param colorRuleType
   * @return
   */
  public static ColorRuleListFragment newInstance(
      ColorRuleGroup.Type colorRuleType) {
    ColorRuleListFragment result = new ColorRuleListFragment();
    Bundle bundle = new Bundle();
    IntentUtil.addColorRuleGroupTypeToBundle(bundle, colorRuleType);
    result.setArguments(bundle);
    return result;
  }
    
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof TableLevelPreferencesActivity)) {
      throw new IllegalArgumentException(
          "must be attached to a " +
              TableLevelPreferencesActivity.class.getSimpleName());
    }
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    TableLevelPreferencesActivity activity =
        this.retrieveTableLevelPreferencesActivity();
    ColorRuleGroup.Type colorRuleGroupType = this.retrieveColorRuleType();
    activity.showEditColorRuleFragmentForExistingRule(
        colorRuleGroupType,
        activity.getElementKey(),
        position);
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(
        R.layout.fragment_color_rule_list,
        container,
        false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.setHasOptionsMenu(true);
    this.mColorRuleGroup = this.retrieveColorRuleGroup();
    this.mColorRuleAdapter = this.createColorRuleAdapter();
    this.setListAdapter(this.mColorRuleAdapter);
    this.registerForContextMenu(this.getListView());
  }
  
  ColorRuleAdapter createColorRuleAdapter() {
    ColorRuleGroup.Type type = this.retrieveColorRuleType();
    ColorRuleAdapter result = new ColorRuleAdapter(
        getActivity(),
        getAppName(),
        getTableId(),
        R.layout.row_for_edit_view_entry,
        this.mColorRuleGroup.getColorRules(),
        type);
    return result;
  }
  
  /**
   * Retrieve the {@link ColorRuleGroup.Type} from the arguments passed to this
   * fragment.
   * @return
   */
  ColorRuleGroup.Type retrieveColorRuleType() {
    ColorRuleGroup.Type result =
        IntentUtil.retrieveColorRuleTypeFromBundle(this.getArguments());
    return result;
  }
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // All of the color rule lists fragments are the same.
    inflater.inflate(R.menu.menu_color_rule_list, menu);
  }
  
  @Override
  public void onCreateContextMenu(
      ContextMenu menu,
      View v,
      ContextMenuInfo menuInfo) {
    MenuInflater menuInflater = this.getActivity().getMenuInflater();
    menuInflater.inflate(R.menu.context_menu_color_rule_list, menu);
    super.onCreateContextMenu(menu, v, menuInfo);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    ColorRuleGroup.Type colorRuleGroupType = this.retrieveColorRuleType();
    TableLevelPreferencesActivity activity =
        this.retrieveTableLevelPreferencesActivity();
    switch (item.getItemId()) {
    case R.id.menu_color_rule_list_new:
      // This is the same in every case.
      activity.showEditColorRuleFragmentForNewRule(
            colorRuleGroupType,
            activity.getElementKey());
      return true;
    case R.id.menu_color_rule_list_revert:
      AlertDialog confirmRevertAlert;
      AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
      alert.setTitle(
          getString(R.string.color_rule_confirm_revert_status_column));
      alert.setMessage(
          getString(R.string.color_rule_revert_are_you_sure));
      // OK action will be to revert to defaults.
      alert.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              revertToDefaults();
            }
          });
      alert.setNegativeButton(R.string.cancel,
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              // canceled, so do nothing!
            }
          });
      confirmRevertAlert = alert.create();
      confirmRevertAlert.show();
    default:
      return super.onOptionsItemSelected(item);
    }
    
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    TableLevelPreferencesActivity activity =
        this.retrieveTableLevelPreferencesActivity();
    final String appName = activity.getAppName();
    AdapterContextMenuInfo menuInfo =
        (AdapterContextMenuInfo) item.getMenuInfo();
    final int position = menuInfo.position;
    if (item.getItemId() == R.id.context_menu_delete_color_rule) {
      // Make an alert dialog that will give them the option to delete it or
      // cancel.
      AlertDialog confirmDeleteAlert;
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.confirm_delete_color_rule);
      builder.setMessage(
          getString(
              R.string.are_you_sure_delete_color_rule,
              " " + 
                  mColorRuleGroup.getColorRules()
                    .get(position)
                    .getOperator()
                    .getSymbol() +
                  " " +
                  mColorRuleGroup.getColorRules()
                    .get(position)
                    .getVal()));

      // For the OK action we want to actually delete this list view.
      builder.setPositiveButton(
          getString(R.string.ok),
          new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          // We need to delete the entry. First delete it in the key value
          // store.
          WebLogger.getLogger(appName).d(TAG, "trying to delete rule at position: " + position);
          mColorRuleGroup.getColorRules().remove(position);
          mColorRuleGroup.saveRuleList(ColorRuleListFragment.this.getActivity());
          mColorRuleAdapter.notifyDataSetChanged();
        }
      });

      builder.setNegativeButton(
          getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

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
   */
  private void revertToDefaults() {
    TableLevelPreferencesActivity activity =
        this.retrieveTableLevelPreferencesActivity();
    final String appName = activity.getAppName();
    ColorRuleGroup.Type colorRuleGroupType = this.retrieveColorRuleType();
    switch (colorRuleGroupType) {
    case STATUS_COLUMN:
      // replace the rules.
      List<ColorRule> newList = new ArrayList<ColorRule>();
      newList.addAll(ColorRuleUtil.getDefaultSyncStateColorRules());
      this.mColorRuleGroup.replaceColorRuleList(newList);
      this.mColorRuleGroup.saveRuleList(ColorRuleListFragment.this.getActivity());
      this.mColorRuleAdapter.notifyDataSetChanged();
      break;
    case COLUMN:
    case TABLE:
      // We want to just wipe all the columns for both of these types.
      List<ColorRule> emptyList = new ArrayList<ColorRule>();
      this.mColorRuleGroup.replaceColorRuleList(emptyList);
      this.mColorRuleGroup.saveRuleList(ColorRuleListFragment.this.getActivity());
      this.mColorRuleAdapter.notifyDataSetChanged();
      break;
    default:
      WebLogger.getLogger(appName).e(TAG, "unrecognized type of column rule in revert to default: " +
          colorRuleGroupType);
    }
  }
    
  /**
   * Retrieve the {@link TableLevelPreferencesActivity} hosting this fragment.
   * @return
   */
  TableLevelPreferencesActivity retrieveTableLevelPreferencesActivity() {
    TableLevelPreferencesActivity result =
        (TableLevelPreferencesActivity) this.getActivity();
    return result;
  }
  
  String getAppName() {
    TableLevelPreferencesActivity result = retrieveTableLevelPreferencesActivity();
    return result.getAppName();
  }
  
  String getTableId() {
    TableLevelPreferencesActivity result = retrieveTableLevelPreferencesActivity();
    return result.getTableId();
  }
  
  ColorRuleGroup retrieveColorRuleGroup() {
    ColorRuleGroup.Type type = this.retrieveColorRuleType();
    ColorRuleGroup result = null;
    switch (type) {
    case COLUMN:
      String elementKey =
          this.retrieveTableLevelPreferencesActivity().getElementKey();
      result = ColorRuleGroup.getColumnColorRuleGroup(
          getActivity(), getAppName(), getTableId(), elementKey);
      break;
    case STATUS_COLUMN:
      result = ColorRuleGroup.getStatusColumnRuleGroup(
          getActivity(), getAppName(), getTableId());
      break;
    case TABLE:
      result = ColorRuleGroup.getTableColorRuleGroup(
          getActivity(), getAppName(), getTableId());
      break;
    default:
      throw new IllegalArgumentException(
          "unrecognized color rule group type: " + type);
    }
    return result;
  }
  
}
