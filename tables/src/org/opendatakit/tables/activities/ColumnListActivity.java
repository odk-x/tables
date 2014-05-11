package org.opendatakit.tables.activities;

import org.opendatakit.tables.R;

import android.os.Bundle;

public class ColumnListActivity extends AbsTableActivity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.column_list);
  }

}
