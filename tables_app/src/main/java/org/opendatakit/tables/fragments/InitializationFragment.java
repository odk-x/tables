/*
 * Copyright (C) 2012-2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.tables.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.activities.IInitResumeActivity;
import org.opendatakit.fragment.AlertDialogFragment;
import org.opendatakit.fragment.AlertDialogFragment.ConfirmAlertDialog;
import org.opendatakit.fragment.ProgressDialogFragment;
import org.opendatakit.fragment.ProgressDialogFragment.CancelProgressDialog;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.listener.InitializationListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;

import java.util.ArrayList;

/**
 * Attempt to initialize data directories using the APK Expansion files.
 *
 * @author mitchellsundt@gmail.com
 */
public class InitializationFragment extends Fragment
    implements InitializationListener, ConfirmAlertDialog, CancelProgressDialog,
    DatabaseConnectionListener {

  // used for logging
  private static final String TAG = InitializationFragment.class.getSimpleName();
  private static String appName;

  // The layout id, used for the view inflater
  private static final int ID = R.layout.copy_expansion_files_layout;

  // The types of dialogs we handle
  private enum DialogState {
    Init, Progress, Alert, None
  }

  // keys for the data being retained
  private static final String DIALOG_TITLE = "dialogTitle";
  private static final String DIALOG_MSG = "dialogMsg";
  private static final String DIALOG_STATE = "dialogState";

  // data to save across orientation changes
  private String mAlertTitle;
  private String mAlertMsg;
  private DialogState mDialogState = DialogState.Init;
  // not saved
  private DialogState mPendingDialogState = DialogState.Init;

  /**
   * Called when we're recreated
   *
   * @param inflater           an inflater used to create the view
   * @param container          also used to create the view
   * @param savedInstanceState a bundle that we use to pull the dialog title, message and state from
   * @return an inflated view
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    appName = ((IAppAwareActivity) getActivity()).getAppName();
    View view = inflater.inflate(ID, container, false);

    if (savedInstanceState != null) {

      // to restore alert dialog.
      if (savedInstanceState.containsKey(DIALOG_TITLE)) {
        mAlertTitle = savedInstanceState.getString(DIALOG_TITLE);
      }
      if (savedInstanceState.containsKey(DIALOG_MSG)) {
        mAlertMsg = savedInstanceState.getString(DIALOG_MSG);
      }
      if (savedInstanceState.containsKey(DIALOG_STATE)) {
        mDialogState = DialogState.valueOf(savedInstanceState.getString(DIALOG_STATE));
      }
    }

    return view;
  }

  /**
   * Starts the download task and shows the progress dialog.
   */
  private void intializeAppName() {
    // set up the first dialog, but don't show it...
    mAlertTitle = getString(R.string.configuring_app,
        getString(Tables.getInstance().getApkDisplayNameResourceId()));
    mAlertMsg = getString(R.string.please_wait);
    mDialogState = DialogState.Progress;

    restoreProgressDialog();

    // launch the copy operation
    WebLogger.getLogger(appName).i(TAG, "initializeAppName called ");
    Tables.getInstance().initializeAppName(((IAppAwareActivity) getActivity()).getAppName(), this);
  }

  /**
   * Saves the alert title, message and state to the output bundle
   *
   * @param outState the bundle to be saved across lifetimes
   */
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mAlertTitle != null) {
      outState.putString(DIALOG_TITLE, mAlertTitle);
    }
    if (mAlertMsg != null) {
      outState.putString(DIALOG_MSG, mAlertMsg);
    }
    outState.putString(DIALOG_STATE, mDialogState.name());
  }

  /**
   * Called when we resume from a pause, sets up the app name if it wasn't already set up and
   * restores the dialog if applicable, then attaches itself to the tables instance to receive
   * task notifications
   */
  @Override
  public void onResume() {
    super.onResume();

    if (mDialogState == DialogState.Init) {
      WebLogger.getLogger(appName).i(TAG, "onResume -- calling initializeAppName");
      intializeAppName();
    } else {

      if (mDialogState == DialogState.Progress) {
        restoreProgressDialog();
      } else if (mDialogState == DialogState.Alert) {
        restoreAlertDialog();
      }

      // re-attach to the task for task notifications...
      Tables.getInstance().establishInitializationListener(this);
    }
  }

  /**
   * Called on the first setup, just attaches to the tables instance to get task notifications
   */
  @Override
  public void onStart() {
    super.onStart();
    Tables.getInstance().possiblyFireDatabaseCallback(getActivity(), this);
  }

  /**
   * Called when we're going to be paused, removes the dialogs and sets the state to none
   */
  @Override
  public void onPause() {
    FragmentManager mgr = getFragmentManager();

    // dismiss dialogs...
    AlertDialogFragment alertDialog = (AlertDialogFragment) mgr.findFragmentByTag("alertDialog");
    if (alertDialog != null) {
      alertDialog.dismiss();
    }
    ProgressDialogFragment progressDialog = (ProgressDialogFragment) mgr
        .findFragmentByTag("progressDialog");
    if (progressDialog != null) {
      progressDialog.dismiss();
    }
    mPendingDialogState = DialogState.None;
    super.onPause();
  }

  /**
   * Called when we've successfully extracted all the files and are ready to open tables. Closes
   * any open progress dialogs and creates an alert dialog showing the results to the user
   *
   * @param overallSuccess whether initializing tables worked
   * @param result         a list of the tables we were able to create, forms we were able to
   *                       extract, etc..
   */
  @Override
  public void initializationComplete(boolean overallSuccess, ArrayList<String> result) {
    try {
      dismissProgressDialog();
    } catch (IllegalArgumentException e) {
      WebLogger.getLogger(appName)
          .i(TAG, "Attempting to close a dialog that was not previously opened");
    }

    Tables.getInstance().clearInitializationTask();

    if (overallSuccess && result.isEmpty()) {
      // do not require an OK if everything went well
      Fragment progress = getFragmentManager().findFragmentByTag("progressDialog");
      if (progress != null) {
        ((ProgressDialogFragment) progress).dismiss();
        mDialogState = DialogState.None;
      }

      ((IInitResumeActivity) getActivity()).initializationCompleted();
      return;
    }

    StringBuilder b = new StringBuilder();
    for (String k : result) {
      b.append(k);
      b.append("\n\n");
    }

    createAlertDialog(overallSuccess ?
        getString(R.string.initialization_complete) :
        getString(R.string.initialization_failed), b.toString().trim());
  }

  /**
   * Dismisses an alertDialog if it already exists, sets the message, title and state for an
   * existing progress dialog if one exists, otherwise creates a new dialog and shows it
   */
  private void restoreProgressDialog() {
    Fragment alert = getFragmentManager().findFragmentByTag("alertDialog");
    if (alert != null) {
      ((AlertDialogFragment) alert).dismiss();
    }

    Fragment dialog = getFragmentManager().findFragmentByTag("progressDialog");

    if (dialog != null && ((ProgressDialogFragment) dialog).getDialog() != null) {
      mDialogState = DialogState.Progress;
      ((ProgressDialogFragment) dialog).getDialog().setTitle(mAlertTitle);
      ((ProgressDialogFragment) dialog).setMessage(mAlertMsg);

    } else {

      ProgressDialogFragment f = ProgressDialogFragment.newInstance(mAlertTitle, mAlertMsg);

      mDialogState = DialogState.Progress;
      if (mPendingDialogState != mDialogState) {
        mPendingDialogState = mDialogState;
        f.show(getFragmentManager(), "progressDialog");
      }
    }
  }

  /**
   * Sets the alert title and message based on the passed message, then calls restoreProgressDialog
   *
   * @param message the message to show in the progress dialog
   */
  private void updateProgressDialogMessage(String message) {
    if (mDialogState == DialogState.Progress) {
      mAlertTitle = getString(R.string.configuring_app,
          getString(Tables.getInstance().getApkDisplayNameResourceId()));
      mAlertMsg = message;
      restoreProgressDialog();
    }
  }

  /**
   * Tries to find and dismiss a progressDialog, setting dialog state and pending dialog state to
   * none in the process
   */
  private void dismissProgressDialog() {
    if (mDialogState == DialogState.Progress) {
      mDialogState = DialogState.None;
    }
    Fragment dialog = getFragmentManager().findFragmentByTag("progressDialog");
    if (dialog != null) {
      ((ProgressDialogFragment) dialog).dismiss();
      mPendingDialogState = DialogState.None;
    }
  }

  /**
   * Dismisses a progressDialog if it already exists, sets the message, title and state for an
   * existing alert dialog if one exists, otherwise creates a new alert and shows it
   */
  private void restoreAlertDialog() {
    Fragment progress = getFragmentManager().findFragmentByTag("progressDialog");
    if (progress != null) {
      ((ProgressDialogFragment) progress).dismiss();
    }

    Fragment dialog = getFragmentManager().findFragmentByTag("alertDialog");

    if (dialog != null && ((AlertDialogFragment) dialog).getDialog() != null) {
      mDialogState = DialogState.Alert;
      ((AlertDialogFragment) dialog).getDialog().setTitle(mAlertTitle);
      ((AlertDialogFragment) dialog).setMessage(mAlertMsg);

    } else {

      AlertDialogFragment f = AlertDialogFragment.newInstance(getId(), mAlertTitle, mAlertMsg);

      mDialogState = DialogState.Alert;
      if (mPendingDialogState != mDialogState) {
        mPendingDialogState = mDialogState;
        f.show(getFragmentManager(), "alertDialog");
      }
    }
  }

  /**
   * Called when the user clicks the ok button on an alert dialog
   */
  @Override
  public void okAlertDialog() {
    mDialogState = DialogState.None;
    ((IInitResumeActivity) getActivity()).initializationCompleted();
  }

  /**
   * Creates an alert dialog with the given tite and message. If shouldExit is
   * set to true, the activity will exit when the user clicks "ok".
   *
   * @param title   the title for the dialog
   * @param message the message for the dialog
   */
  private void createAlertDialog(String title, String message) {
    mAlertMsg = message;
    mAlertTitle = title;
    restoreAlertDialog();
  }

  /**
   * Called when the Tables object we attached to sends us a new event to display
   *
   * @param displayString the string to set the ProgressDialog's text to
   */
  @Override
  public void initializationProgressUpdate(String displayString) {
    updateProgressDialogMessage(displayString);
  }

  /**
   * Called when someone presses the cancel button on the progress dialog, tell tables to cancel
   * the import
   */
  @Override
  public void cancelProgressDialog() {
    WebLogger.getLogger(appName).i(TAG, "cancelProgressDialog -- calling cancelInitializationTask");
    // signal the task that we want it to be cancelled.
    // but keep the notification path...
    // the task will call back with a copyExpansionFilesComplete()
    // to report status (cancelled).
    Tables.getInstance().cancelInitializationTask();
  }

  /**
   * Called when the database comes up. If we're in a "loading..." mode, tell Tables to initialze
   * the app name
   */
  @Override
  public void databaseAvailable() {
    if (mDialogState == DialogState.Progress) {
      Tables.getInstance().initializeAppName(appName, this);
    }
  }

  /**
   * Called when the database goes away, if we're in a "loading..." mode, then update the
   * progress dialog with an error message
   */
  @Override
  public void databaseUnavailable() {
    if (mDialogState == DialogState.Progress) {
      updateProgressDialogMessage(getString(R.string.database_unavailable));
    }
  }
}