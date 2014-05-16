package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.tables.R;
import org.opendatakit.tables.tasks.InitializeTask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

/**
 * This fragment presents a dialog with the import status.
 * @author sudar.sam@gmail.com
 *
 */
/*
 * My initial jumping-in point for Fragments and Tasks was this tutorial, on
 * which this implementation is based:
 * http://creeder.com/?&page=AsyncTask
 */
public class InitializeTaskDialogFragment extends DialogFragment {

  public static final String TAG_FRAGMENT =
      InitializeTaskDialogFragment.class.getSimpleName();

  private InitializeTask mTask;
  private InitializeTask.Callbacks mCallbacks;

  private static final String TAG = InitializeTaskDialogFragment.class.getSimpleName();

  public InitializeTaskDialogFragment() {
    // explicit empty constructor for fragments.
  }

  /**
   * Set the {@link InitializeTask} for which this fragment will be
   * responsible.
   * @param task
   */
  public void setTask(InitializeTask task) {
    this.mTask = task;
  }

  /**
   * Set the callbacks that will be invoked on behalf of the
   * {@link InitializeTask}.
   * @param callbacks
   */
  public void setCallbacks(InitializeTask.Callbacks callbacks) {
    this.mCallbacks = callbacks;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // We want to keep this around even in the case of rotation--hold onto all
    // of the pertinent data.
    this.setRetainInstance(true);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mTask == null) {
      dismiss();
    }
  }

  /**
   * Return the preferences from this objects calbacks.
   * @return
   */
  public Preferences getPreferencesFromContext() {
    return this.mCallbacks.getPrefs();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Note: don't try and hold on to this dialog as a member variable. I think
    // that this can interfere with Android's DialogFragment lifecycle stuff.
    ProgressDialog dialog = new ProgressDialog(getActivity());
    dialog.setTitle(getString(R.string.configuring_tables));
    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    dialog.setCancelable(false);
    return dialog;
  }

  @Override
  public void onDestroyView() {
    // Some hacky stuff to ensure that the fragment actually survives
    // orientation changes. See discussion in numerous places online, including
    // here:
    // http://creeder.com/?&page=AsyncTask
    if (getDialog() != null && getRetainInstance()) {
      getDialog().setDismissMessage(null);
    }
    super.onDestroyView();
  }

  /**
   * Called by the task to update the dialog's progress.
   * @param progressString
   */
  public void updateProgress(String progressString) {
    ProgressDialog dialog = (ProgressDialog) getDialog();
    dialog.setMessage(progressString);
  }

  public void onTaskFinishedWithErrors(boolean poorlyFormattedConfigFile) {
    // We'll just pass a dummy value for the modified time and presume that
    // onTaskFinished handles it appropriately.
    onTaskFinished(false, poorlyFormattedConfigFile, null);
  }

  /**
   *
   * @param message the message that should be displayed in an alert dialog to
   * the user.
   */
  public void onTaskFinishedSuccessfully(String message) {
    onTaskFinished(true, false, message);
  }

  /**
   * Should be called whenever the task is finished.
   */
  private void onTaskFinished(boolean success,
      boolean poorlyFormattedConfigFile, String successMessage) {
    if (isResumed()) {
      dismiss();
    }
    mTask = null; // so we can dismiss ourselves in onResume if necessary
    if (mCallbacks == null) {
      Log.e(TAG, "InitializeTask completed but callbacks in dialog fragment" +
      		" were null! Not invoking.");
      return;
    }
    // Otherwise we create the alert dialog saying things went well.
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setCancelable(false);
    builder.setNeutralButton(getString(R.string.ok),
        new DialogInterface.OnClickListener() {

      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        mCallbacks.onImportsComplete();
      }
    });
    if (!success) {
      // Then something went wrong.
      if (poorlyFormattedConfigFile) {
        // Then the reason was a poorly formatted configuration file.
        builder.setTitle(getString(R.string.bad_config_properties_file));
      } else {
        builder.setTitle(getString(R.string.error));
      }
    } else {
      // Then we need to send ye' olde message to the user.
      builder.setTitle(getString(R.string.config_summary));
      builder.setMessage(successMessage);
    }
    AlertDialog resultDialog = builder.create();
    resultDialog.show();
  }


}
