package org.opendatakit.tables.activities;

import org.opendatakit.tables.fragments.TableManagerFragment;

import android.app.Activity;
import android.os.Bundle;

/**
 * The main activity for ODK Tables. It serves primarily as a holder for 
 * fragments.
 * @author sudar.sam@gmail.com
 *
 */
public class MainActivity extends Activity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TableManagerFragment tmf = (TableManagerFragment)
        this.getFragmentManager().findFragmentByTag("fr_tm");
    if (tmf == null) {
      tmf = new TableManagerFragment();
      this.getFragmentManager().beginTransaction().add(tmf, "fr_tm").commit();
    }
  }

}
