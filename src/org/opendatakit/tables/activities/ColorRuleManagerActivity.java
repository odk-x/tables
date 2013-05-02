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
import org.opendatakit.tables.data.ColorRule;
import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;

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
  private static final String ACTIVITY_TITLE_SUFFIX = " Color Rules";
  private static final String EXAMPLE_STRING = "Rule Preview";
  private static final String STATUS_COLUMN_TITLE = "Status Column";

  /**
   * Menu ID for adding a new list view.
   */
  public static final int ADD_NEW_LIST_VIEW = 0;

  /**
   * The char sequence for the add new list view item.
   */
  public static final String ADD_NEW_LIST_VIEW_TEXT = "Add New Color Rule";

  /**
   * Menu ID for deleting an entry.
   */
  public static final int MENU_DELETE_ENTRY = 1;

  /**
   * Text for the entry deletion.
   */
  public static final String MENU_TEXT_DELETE_ENTRY = "Delete this Rule";

  /**
   * Menu ID for opening the edit rule activity.
   */
  public static final int MENU_EDIT_ENTRY = 2;
  /**
   * Text for the rule editing.
   */
  public static final String MENU_TEXT_EDIT_ENTRY = "Edit this Color Rule";

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
    case ADD_NEW_LIST_VIEW:
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
        0, ADD_NEW_LIST_VIEW, 0, ADD_NEW_LIST_VIEW_TEXT);
    addItem.setIcon(org.opendatakit.tables.R.drawable.content_new);
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
      builder.setTitle("Delete this rule?");
      // For the OK action we want to actually delete this list view.
      builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

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

      builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

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
    menu.add(0, MENU_DELETE_ENTRY, 0, MENU_TEXT_DELETE_ENTRY);
    menu.add(0, MENU_EDIT_ENTRY, 0, MENU_TEXT_EDIT_ENTRY);
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
      this.setTitle(mCp.getDisplayName() + ACTIVITY_TITLE_SUFFIX);
      break;
    case TABLE:
      this.mCp = null;
      this.mColorRuler = ColorRuleGroup.getTableColorRuleGroup(mTp);
      this.setTitle(mTp.getDisplayName() + ACTIVITY_TITLE_SUFFIX);
      break;
    case STATUS_COLUMN:
      this.mCp = null;
      this.mColorRuler = ColorRuleGroup.getStatusColumnRuleGroup(mTp);
      this.setTitle(STATUS_COLUMN_TITLE + ACTIVITY_TITLE_SUFFIX);
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
      final String ruleString = mColorRules.get(currentPosition).toString();
      // The user-friendly string rep of the rule.
      // We'll need to display the display name if this is an editable field.
      // (ie if a status column or table rule)
      String columnDisplayName = "";
      if (mType == ColorRuleGroup.Type.STATUS_COLUMN ||
          mType == ColorRuleGroup.Type.TABLE) {
        columnDisplayName =
            mTp.getColumnByElementKey(mColorRules.get(currentPosition)
                .getColumnElementKey()).getDisplayName() + " ";
      }
      TextView label =
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_label);
      label.setText(columnDisplayName + ruleString);
      final int backgroundColor =
          mColorRules.get(currentPosition).getBackground();
      final int textColor =
          mColorRules.get(currentPosition).getForeground();
      // Will demo the color rule.
      TextView exampleView =
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_ext);
      exampleView.setText(EXAMPLE_STRING);
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
