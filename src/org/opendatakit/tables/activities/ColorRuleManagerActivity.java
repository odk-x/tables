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
package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.provider.SyncState;
import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColorRule;
import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStoreType;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.utils.ColorRuleUtil;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;

/**
 * Activity for managing color rules.
 * @author sudar.sam@gmail.com
 *
 */
public class ColorRuleManagerActivity extends SherlockListActivity {

  private static final String TAG = ColorRuleManagerActivity.class.getName();

  public static final String INTENT_KEY_TABLE_ID = "tableId";
  public static final String INTENT_KEY_ELEMENT_KEY = "elementKey";
  public static final String INTENT_KEY_RULE_GROUP_TYPE = "ruleGroupType";

  /**
   * Menu ID for adding a new list view.
   */
  public static final int ADD_NEW_COLOR_RULE = 0;

  /**
   * Menu ID for deleting an entry.
   */
  public static final int MENU_DELETE_ENTRY = 1;

  /**
   * Menu ID for opening the edit rule activity.
   */
  public static final int MENU_EDIT_ENTRY = 2;

  /**
   * Menu ID for reverting to default color rules.
   */
  public static final int MENU_REVERT_TO_DEFAULT = 3;

  private List<ColorRule> mColorRules;
  private ColorRuleGroup mColorRuler;
  private ColorRuleAdapter mColorRuleAdapter;
  private String mTableId;
  // The element key of the column for which you're displaying the rules.
  private String mElementKey;
  private TableProperties mTp;
  private ColumnProperties mCp;
  private ColorRuleGroup.Type mType;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(org.opendatakit.tables.R.layout.color_rule_manager);
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    registerForContextMenu(getListView());
  }

  @Override
  public void onResume() {
    super.onResume();
    init();
  }

  @Override
  public boolean onMenuItemSelected(int featureId,
      com.actionbarsherlock.view.MenuItem item) {
    switch (item.getItemId()) {
    case ADD_NEW_COLOR_RULE:
      // If this is the case we need to launch the edit activity.
      Intent newColorRuleIntent =
          new Intent(this, EditSavedColorRuleActivity.class);
      newColorRuleIntent.putExtra(
          Controller.INTENT_KEY_APP_NAME, mTp.getAppName());
      newColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_TABLE_ID, mTableId);
      newColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_ELEMENT_KEY, mElementKey);
      newColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_RULE_GROUP_TYPE, mType.name());
      newColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_RULE_POSITION,
          EditSavedColorRuleActivity.INTENT_FLAG_NEW_RULE);
      startActivity(newColorRuleIntent);
      return true;
    case MENU_REVERT_TO_DEFAULT:
      // We need to wipe the color rules and add the default. We'll do this
      // with a dialog.
      AlertDialog confirmRevertAlert;
      AlertDialog.Builder alert = new AlertDialog.Builder(
          ColorRuleManagerActivity.this);
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
      return true;
    case android.R.id.home:
      Intent i = new Intent(this, TableManager.class);
      i.putExtra(Controller.INTENT_KEY_APP_NAME, mTp.getAppName());
      startActivity(i);
      return true;
    }
    return false;
  }

  @Override
  public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
    super.onCreateOptionsMenu(menu);
    com.actionbarsherlock.view.MenuItem addItem = menu.add(
        0, ADD_NEW_COLOR_RULE, 0, getString(R.string.add_new_color_rule));
    addItem.setIcon(R.drawable.content_new);
    addItem.setShowAsAction(
        com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
    // Add the button for reverting to default.
    com.actionbarsherlock.view.MenuItem revertItem = menu.add(
        0, MENU_REVERT_TO_DEFAULT, 0,
        getString(R.string.color_rule_revert_to_default_status_rules));
    revertItem.setIcon(android.R.drawable.ic_menu_revert);
    revertItem.setShowAsAction(
        com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_IF_ROOM);
    return true;
  }

  /**
   * Wipe the current rules and revert to the defaults for the given type.
   */
  private void revertToDefaults() {
    switch (this.mType) {
    case STATUS_COLUMN:
      // replace the rules.
      List<ColorRule> newList = new ArrayList<ColorRule>();
      newList.addAll(ColorRuleUtil.getDefaultSyncStateColorRules());
      this.mColorRuler.replaceColorRuleList(newList);
      this.mColorRuler.saveRuleList();
      this.mColorRules.clear();
      this.mColorRules.addAll(this.mColorRuler.getColorRules());
      this.mColorRuleAdapter.notifyDataSetChanged();
      break;
    case COLUMN:
    case TABLE:
      // We want to just wipe all the columns for both of these types.
      List<ColorRule> emptyList = new ArrayList<ColorRule>();
      this.mColorRuler.replaceColorRuleList(emptyList);
      this.mColorRuler.saveRuleList();
      this.mColorRules.clear();
      this.mColorRuleAdapter.notifyDataSetChanged();
      break;
    default:
      Log.e(TAG, "unrecognized type of column rule in revert to default: " +
          this.mType);
    }
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    // We need this so we can get the position of the thing that was clicked.
    AdapterContextMenuInfo menuInfo =
        (AdapterContextMenuInfo) item.getMenuInfo();
    final int position = menuInfo.position;
    switch(item.getItemId()) {
    case MENU_DELETE_ENTRY:
      // Make an alert dialog that will give them the option to delete it or
      // cancel.
      AlertDialog confirmDeleteAlert;
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.confirm_delete_color_rule);
      builder.setMessage(getString(R.string.are_you_sure_delete_color_rule,
          " " + mColorRules.get(position).getOperator().getSymbol() + " " +
          mColorRules.get(position).getVal()));

      // For the OK action we want to actually delete this list view.
      builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          // We need to delete the entry. First delete it in the key value
          // store.
          Log.d(TAG, "trying to delete rule at position: " + position);
          mColorRules.remove(position);
          mColorRuler.replaceColorRuleList(mColorRules);
          mColorRuler.saveRuleList();
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
    case MENU_EDIT_ENTRY:
      Log.d(TAG, "edit entry for color rule called");
      menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
      Intent editColorRuleIntent = new Intent(ColorRuleManagerActivity.this,
          EditSavedColorRuleActivity.class);
      editColorRuleIntent.putExtra(
          Controller.INTENT_KEY_APP_NAME, mTp.getAppName());
      editColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_TABLE_ID, mTableId);
      editColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_ELEMENT_KEY, mElementKey);
      editColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_RULE_POSITION, position);
      editColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_RULE_GROUP_TYPE, mType.name());
      startActivity(editColorRuleIntent);
      return true;
    default:
      Log.e(TAG, "android MenuItem id not recognized: " + item.getItemId());
      return false;
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    AdapterView.AdapterContextMenuInfo info =
        (AdapterView.AdapterContextMenuInfo) menuInfo;
    menu.add(0, MENU_DELETE_ENTRY, 0, getString(R.string.delete_color_rule));
    ColorRule rule = this.mColorRules.get(info.position);
    if (!ColorRuleUtil.getDefaultSyncStateColorRuleIds().contains(
        rule.getRuleId())) {
      // We only want to allow editing if it is not one of the default rules.
      menu.add(0, MENU_EDIT_ENTRY, 0, getString(R.string.edit_color_rule));
    }
  }

  private void init() {
    String appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    this.mTableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.mElementKey = getIntent().getStringExtra(INTENT_KEY_ELEMENT_KEY);
    this.mType = ColorRuleGroup.Type.valueOf(
        getIntent().getStringExtra(INTENT_KEY_RULE_GROUP_TYPE));
    this.mTp = TableProperties.getTablePropertiesForTable(this, appName, mTableId,
        KeyValueStoreType.ACTIVE);
    switch (mType) {
    case COLUMN:
      this.mCp = mTp.getColumnByElementKey(mElementKey);
      this.mColorRuler =
          ColorRuleGroup.getColumnColorRuleGroup(mTp, mElementKey);
      this.setTitle(getString(R.string.color_rule_title_for, mCp.getDisplayName()));
      break;
    case TABLE:
      this.mCp = null;
      this.mColorRuler = ColorRuleGroup.getTableColorRuleGroup(mTp);
      this.setTitle(getString(R.string.row_color_rule_title_for, mTp.getDisplayName()));
      break;
    case STATUS_COLUMN:
      this.mCp = null;
      this.mColorRuler = ColorRuleGroup.getStatusColumnRuleGroup(mTp);
      this.setTitle(getString(R.string.color_rule_title_for,
          getString(R.string.status_column)));
      break;
    default:
      Log.e(TAG, "uncrecognized type: " + mType);
    }
    this.mColorRules = mColorRuler.getColorRules();
    this.mColorRuleAdapter = new ColorRuleAdapter();
    setListAdapter(mColorRuleAdapter);

  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Log.d(TAG, "list item clicked");
    // We don't want to launch the edit activity if it is one of the default
    // status rules. We will get the rule and check its id.
    ColorRule colRule = this.mColorRules.get(position);
    if (ColorRuleUtil.getDefaultSyncStateColorRuleIds().contains(
        colRule.getRuleId())) {
      // We don't want to do anything, b/c we don't allow editing of the
      // default rules.
      return;
    }
    Intent editColorRuleIntent = new Intent(ColorRuleManagerActivity.this,
        EditSavedColorRuleActivity.class);
    editColorRuleIntent.putExtra(
        Controller.INTENT_KEY_APP_NAME, mTp.getAppName());
    editColorRuleIntent.putExtra(
        EditSavedColorRuleActivity.INTENT_KEY_TABLE_ID, mTableId);
    editColorRuleIntent.putExtra(
        EditSavedColorRuleActivity.INTENT_KEY_ELEMENT_KEY, mElementKey);
    editColorRuleIntent.putExtra(
        EditSavedColorRuleActivity.INTENT_KEY_RULE_POSITION, position);
    editColorRuleIntent.putExtra(
        EditSavedColorRuleActivity.INTENT_KEY_RULE_GROUP_TYPE, mType.name());
    startActivity(editColorRuleIntent);
  }

  class ColorRuleAdapter extends ArrayAdapter<ColorRule> {

    ColorRuleAdapter() {
      super(ColorRuleManagerActivity.this,
          org.opendatakit.tables.R.layout.touchlistview_row2,
          mColorRules);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row = convertView;
      if (row == null) {
        row = getLayoutInflater().inflate(
            org.opendatakit.tables.R.layout.row_for_edit_view_entry,
            parent, false);
      }
      final int currentPosition = position;
      // We'll need to display the display name if this is an editable field.
      // (ie if a status column or table rule)
      String description = "";
      boolean isMetadataRule = false;
      if (mType == ColorRuleGroup.Type.STATUS_COLUMN ||
          mType == ColorRuleGroup.Type.TABLE) {
        ColorRule colorRule = mColorRules.get(currentPosition);
        String elementKey = colorRule.getColumnElementKey();
        if (DbTable.getAdminColumns().contains(elementKey)) {
          isMetadataRule = true;
          // We know it must be a String rep of an int.
          SyncState targetState = SyncState.valueOf(colorRule.getVal());
          // For now we need to handle the special cases of the sync state.
          if (targetState == SyncState.inserting) {
            description =
                getString(R.string.sync_state_equals_inserting_message);
          } else if (targetState == SyncState.updating) {
            description =
                getString(R.string.sync_state_equals_updating_message);
          } else if (targetState == SyncState.rest) {
            description =
                getString(R.string.sync_state_equals_rest_message);
          } else if (targetState == SyncState.deleting) {
            description =
                getString(R.string.sync_state_equals_deleting_message);
          } else if (targetState == SyncState.conflicting) {
            description =
                getString(R.string.sync_state_equals_conflicting_message);
          } else {
            Log.e(TAG, "unrecognized sync state: " + targetState);
            description = "unknown";
          }
        } else {
          description =
              mTp.getColumnByElementKey(elementKey).getDisplayName();
        }
      }
      if (!isMetadataRule) {
        description += " " +
            mColorRules.get(currentPosition).getOperator().getSymbol() + " " +
            mColorRules.get(currentPosition).getVal();
      }
      TextView label =
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_label);
      label.setText(description);
      final int backgroundColor =
          mColorRules.get(currentPosition).getBackground();
      final int textColor =
          mColorRules.get(currentPosition).getForeground();
      // Will demo the color rule.
      TextView exampleView =
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_ext);
      exampleView.setText(getString(R.string.status_column));
      exampleView.setTextColor(textColor);
      exampleView.setBackgroundColor(backgroundColor);
      exampleView.setVisibility(View.VISIBLE);
      // The radio button is meaningless here, so get it off the screen.
      final RadioButton radioButton = (RadioButton)
          row.findViewById(org.opendatakit.tables.R.id.radio_button);
      radioButton.setVisibility(View.GONE);
      // And now the settings icon.
      final ImageView editView = (ImageView)
          row.findViewById(org.opendatakit.tables.R.id.row_options);
      final View holderView = row;
      editView.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          holderView.showContextMenu();
        }
      });
      return row;
    }
  }
}
