package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.InitializeTaskDialogFragment;
import org.opendatakit.tables.fragments.TableManagerFragment;
import org.opendatakit.tables.tasks.InitializeTask;
import org.opendatakit.tables.utils.Constants;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

/**
 * The main activity for ODK Tables. It serves primarily as a holder for
 * fragments.
 * @author sudar.sam@gmail.com
 *
 */
public class MainActivity extends AbsBaseActivity implements
    InitializeTask.Callbacks {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(
        org.opendatakit.tables.R.layout.activity_main_activity);
    TableManagerFragment tmf = (TableManagerFragment)
        this.getFragmentManager().findFragmentByTag(
            Constants.FragmentTags.TABLE_MANAGER);
    if (tmf == null) {
      tmf = new TableManagerFragment();
      this.getFragmentManager().beginTransaction().add(
          R.id.main_activity_frame_layout,
          tmf,
          Constants.FragmentTags.TABLE_MANAGER).commit();
    }
    // It's possible we were configuring the app before an old instance was
    // destroyed. We'll check.
    InitializeTaskDialogFragment initializeTaskDialogFragment =
        (InitializeTaskDialogFragment) this.getFragmentManager()
          .findFragmentByTag(
              Constants.FragmentTags.INITIALIZE_TASK_DIALOG);
    if (initializeTaskDialogFragment != null) {
      initializeTaskDialogFragment.setCallbacks(this);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Check to see if we need to initialize.
    if ( Tables.getInstance().shouldRunInitializationTask(mAppName) ) {
      this.startInitializationTask();
    }
  }

  private void startInitializationTask() {
    InitializeTask initializeTask = new InitializeTask(
        this,
        this.getAppName());
    InitializeTaskDialogFragment initializeTaskDialogFragment =
        new InitializeTaskDialogFragment();
    initializeTaskDialogFragment.setTask(initializeTask);
    initializeTaskDialogFragment.setCallbacks(this);
    initializeTaskDialogFragment.setCancelable(false);
    initializeTask.setDialogFragment(initializeTaskDialogFragment);
    FragmentManager fragmentManager = this.getFragmentManager();
    initializeTaskDialogFragment.show(
        fragmentManager,
        InitializeTaskDialogFragment.TAG_FRAGMENT);
    // fire off the initializeTask
    Void v = null;
    initializeTask.execute(v);
    // if initialization task dies, don't try to restart it...
    Tables.getInstance().clearRunInitializationTask(mAppName);
  }

  @Override
  public Preferences getPrefs() {
    Preferences result = new Preferences(this, getAppName());
    return result;
  }

  /**
   * Refresh the list in the {@link TableManagerFragment} if it is present.
   */
  public void refreshTableManagerList() {
    FragmentManager fragmentManager = this.getFragmentManager();
    TableManagerFragment tableManagerFragment = (TableManagerFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.TABLE_MANAGER);
    FragmentTransaction transaction = fragmentManager.beginTransaction();
    if (tableManagerFragment != null) {
      // We'll make it redraw by detaching/reattaching.
      transaction.detach(tableManagerFragment).attach(tableManagerFragment);
    }
    transaction.replace(
        R.id.main_activity_frame_layout,
        tableManagerFragment,
        Constants.FragmentTags.TABLE_MANAGER);
    transaction.commit();
  }

  @Override
  public void onImportsComplete() {
    this.refreshTableManagerList();
  }

}
