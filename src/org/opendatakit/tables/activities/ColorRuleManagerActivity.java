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

import java.util.List;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColorRule;
import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.sync.SyncUtil;
import org.opendatakit.tables.utils.ColorRuleUtil;

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

  private List<ColorRule> mColorRules;
  private ColorRuleGroup mColorRuler;
  private ColorRuleAdapter mColorRuleAdapter;
  private String mTableId;
  // The element key of the column for which you're displaying the rules.
  private String mElementKey;
  private TableProperties mTp;
  private ColumnProperties mCp;
  private ObjectMapper mMapper;
  private TypeFactory mTypeFactory;
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
    case android.R.id.home:
      startActivity(new Intent(this, TableManager.class));
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
    return true;
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
    if (info.position <= 
        ColorRuleUtil.getDefaultSyncStateColorRules().size() - 1) {
      return;
    }
    menu.add(0, MENU_DELETE_ENTRY, 0, getString(R.string.delete_color_rule));
    menu.add(0, MENU_EDIT_ENTRY, 0, getString(R.string.edit_color_rule));
  }

  private void init() {
    this.mTableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.mElementKey = getIntent().getStringExtra(INTENT_KEY_ELEMENT_KEY);
    this.mType = ColorRuleGroup.Type.valueOf(
        getIntent().getStringExtra(INTENT_KEY_RULE_GROUP_TYPE));
    DbHelper dbh = DbHelper.getDbHelper(this);
    this.mTp = TableProperties.getTablePropertiesForTable(dbh, mTableId,
        KeyValueStore.Type.ACTIVE);
    this.mMapper = new ObjectMapper();
    this.mTypeFactory = mMapper.getTypeFactory();
    mMapper.setVisibilityChecker(
        mMapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
    mMapper.setVisibilityChecker(
        mMapper.getVisibilityChecker().withCreatorVisibility(Visibility.ANY));
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
      // We need to do a check to make sure the first rules are the default
      // rules. Later will be a reset to default or something, but for now we
      // just do a check.
      if (this.mColorRuler.getColorRules().size() != 
          ColorRuleUtil.getDefaultSyncStateColorRules().size()) {
        // Something's gone wrong, b/c these should be fixed. 
        // Wipe and start afresh.
        this.mColorRuler.replaceColorRuleList(
            ColorRuleUtil.getDefaultSyncStateColorRules());
        this.mColorRuler.saveRuleList();
      } else {
        // They might be correct, or something could have gone wrong. Test.
        for (int i = 0; i < this.mColorRuler.getColorRules().size(); i++) {
          ColorRule defaultRule = 
              ColorRuleUtil.getDefaultSyncStateColorRules().get(i);
          ColorRule unknownRule = this.mColorRuler.getColorRules().get(i);
          if (!defaultRule.equalsWithoutId(unknownRule)) {
            // Not a match. Wipe them out and start afresh.
            this.mColorRuler.replaceColorRuleList(
                ColorRuleUtil.getDefaultSyncStateColorRules());
            this.mColorRuler.saveRuleList();
            break;
          }
        }
      }
      this.setTitle(getString(R.string.color_rule_title_for, getString(R.string.status_column)));
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
    if (position <= ColorRuleUtil.getDefaultSyncStateColorRules().size() - 1) {
      // We don't want to do anything, b/c we assume these rules are at the top
      // and that they cannot be edited.
      return;
    }
    Intent editColorRuleIntent = new Intent(ColorRuleManagerActivity.this,
        EditSavedColorRuleActivity.class);
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
          int targetState = Integer.parseInt(colorRule.getVal());
          // For now we need to handle the special cases of the sync state.
          if (targetState == SyncUtil.State.INSERTING) {
            description = 
                getString(R.string.sync_state_equals_inserting_message);
          } else if (targetState == SyncUtil.State.UPDATING) {
            description = 
                getString(R.string.sync_state_equals_updating_message);
          } else if (targetState == SyncUtil.State.REST) {
            description = 
                getString(R.string.sync_state_equals_rest_message);
          } else if (targetState == SyncUtil.State.DELETING) {
            description = 
                getString(R.string.sync_state_equals_deleting_message);
          } else if (targetState == SyncUtil.State.CONFLICTING) {
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
      if (position <= 
          ColorRuleUtil.getDefaultSyncStateColorRules().size() - 1) {
        // -1 b/c zero indexed.
        editView.setVisibility(View.GONE);
      }
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
