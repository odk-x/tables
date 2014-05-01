package org.opendatakit.tables.activities;

import org.opendatakit.tables.R;

import android.os.Bundle;


/**
 * Displays preferences for a table to the user. This includes all preferences
 * that apply at a table level.
 * @author sudar.sam@gmail.com
 *
 */
public class TableLevelPreferencesActivity extends AbsTableActivity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.table_level_preferences);
  }

}
