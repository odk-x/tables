package org.opendatakit.tables.activities;

import org.opendatakit.tables.R;
import org.opendatakit.tables.fragments.AboutMenuFragment;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

public class AboutWrapperActivity extends AbsBaseActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // see if we saved the state
    this.setContentView(R.layout.about_wrapper_activity);
  }

}
