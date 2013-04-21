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
package org.opendatakit.tables.Activity;

import java.util.List;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.opendatakit.tables.DataStructure.ColorRule;
import org.opendatakit.tables.DataStructure.ColorRuleGroup;
import org.opendatakit.tables.DataStructure.RowColorRuler;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
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
 * Activity for managing {@link RowColorRule} objects. This is the activity you
 * use to interact with color rules for the rows.
 * @author sudar.sam@gmail.com
 *
 */
/*
 * NB! Atm this is a very close to direct copy/paste from 
 * ColorRuleManagerActivity, which is the one for column color rules. 
 * Eventually, I think this more general rule manager should be the superclass
 * of the column rule manager, but for now I'm just going to get it working.
 * 
 */
public class RowColorRuleManagerActivity extends SherlockListActivity {
  
  private static final String TAG = 
      RowColorRuleManagerActivity.class.getName();
  
  public static final String INTENT_KEY_TABLE_ID = "tableId";
  public static final String INTENT_KEY_ELEMENT_KEY = "elementKey";
  private static final String ACTIVITY_TITLE_SUFFIX = " Color Rules";
  private static final String EXAMPLE_STRING = "Rule Preview";

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
  private ColorRuleGroup mColorRuleGroup;
  private ColorRuleAdapter mColorRuleAdapter;
  private String mTableId;
  private KeyValueStoreHelper mColorRuleKvsh;
  private KeyValueHelper mAspectHelper;
  private TableProperties mTp;
  private ObjectMapper mMapper;
  private TypeFactory mTypeFactory;
  
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
          EditSavedColorRuleActivity.INTENT_KEY_RULE_POSITION, 
          EditSavedColorRuleActivity.INTENT_FLAG_NEW_RULE);
      newColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_EDIT_COLUMN, true);
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
          mColorRuleGroup.replaceColorRuleList(mColorRules);
          mColorRuleGroup.saveRuleList();
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
      Intent editColorRuleIntent = new Intent(RowColorRuleManagerActivity.this,
          EditSavedColorRuleActivity.class);
      editColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_TABLE_ID, mTableId);
      editColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_RULE_POSITION, position);
      editColorRuleIntent.putExtra(
          EditSavedColorRuleActivity.INTENT_KEY_EDIT_COLUMN, true);
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
    DbHelper dbh = DbHelper.getDbHelper(this);
    this.mTp = TableProperties.getTablePropertiesForTable(dbh, mTableId, 
        KeyValueStore.Type.ACTIVE);
    this.mColorRuleKvsh = 
        mTp.getKeyValueStoreHelper(RowColorRuler.KVS_PARTITION);
    this.mMapper = new ObjectMapper();
    this.mTypeFactory = mMapper.getTypeFactory();
    mMapper.setVisibilityChecker(
        mMapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
    mMapper.setVisibilityChecker(
        mMapper.getVisibilityChecker().withCreatorVisibility(Visibility.ANY));
    this.mColorRuleGroup = ColorRuleGroup.getTableColorRuleGroup(mTp);
    this.mColorRules = mColorRuleGroup.getColorRules();
    this.mColorRuleAdapter = new ColorRuleAdapter();
    setListAdapter(mColorRuleAdapter);
    this.setTitle(mTp.getDisplayName() + ACTIVITY_TITLE_SUFFIX);
  }
  
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Log.d(TAG, "list item clicked");
    Intent editColorRuleIntent = new Intent(RowColorRuleManagerActivity.this,
        EditSavedColorRuleActivity.class);
    editColorRuleIntent.putExtra(
        EditSavedColorRuleActivity.INTENT_KEY_TABLE_ID, mTableId);
    editColorRuleIntent.putExtra(
        EditSavedColorRuleActivity.INTENT_KEY_EDIT_COLUMN, true);
    editColorRuleIntent.putExtra(
        EditSavedColorRuleActivity.INTENT_KEY_RULE_POSITION, position);
    startActivity(editColorRuleIntent);
  }

  class ColorRuleAdapter extends ArrayAdapter<ColorRule> {
    
    ColorRuleAdapter() {
      super(RowColorRuleManagerActivity.this,
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
      TextView label = 
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_label);
      // We'll want the element key.
      String columnDisplayName = 
          mTp.getColumnByElementKey(mColorRules.get(currentPosition)
              .getColumnElementKey()).getDisplayName();
      label.setText(columnDisplayName + " " + ruleString);
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