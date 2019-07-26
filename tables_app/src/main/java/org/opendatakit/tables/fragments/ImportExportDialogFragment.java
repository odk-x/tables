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
package org.opendatakit.tables.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;

/**
 * renamed from AbstractImportExportActivity to be both not abstract and not an activity, and to
 * use a DialogFragment instead of multiple Dialog objects. Niles 06/01/17
 *
 * @author sudar.sam@gmail.com
 * @author unknown
 */
public class ImportExportDialogFragment extends DialogFragment {

  /**
   * The ID that tells us to show the export success dialog
   */
  public static final int CSVEXPORT_SUCCESS_DIALOG = 1;
  /**
   * The ID that tells us to show the import success dialog
   */
  public static final int CSVIMPORT_SUCCESS_DIALOG = 2;
  /**
   * The ID that tells us to show the export in progress dialog
   */
  public static final int EXPORT_IN_PROGRESS_DIALOG = 3;
  /**
   * The ID that tells us to show the import in progress dialog
   */
  public static final int IMPORT_IN_PROGRESS_DIALOG = 4;
  /**
   * The ID that tells us to show the import failure dialog
   */
  public static final int CSVIMPORT_FAIL_DIALOG = 5;
  /**
   * The ID that tells us to show the export failure dialog
   */
  public static final int CSVEXPORT_FAIL_DIALOG = 6;
  /**
   * This is intended to say that "your csv exported successfully, but there was a problem with
   * the key value store setting mapping.
   */
  public static final int CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG = 7;
  private static final String TAG = ImportExportDialogFragment.class.getSimpleName();
  // private IDs that are put in the bundle of arguments to determine which type of dialog to create
  // can't use an enum because you can't (safely) put an enum in a bundle
  private static final int ALERT_DIALOG = 0;
  private static final int PROGRESS_DIALOG = 1;
  /**
   * the active dialog holder. Dismissing it won't set this to null, so make sure it's still
   * displayed when you go to change its message
   */
  public static ImportExportDialogFragment activeDialogFragment = null;
  /**
   * both ImportCSVActivity and ExportCSVActivity set a valid fragment manager in their onCreate
   * handlers. This means if an ImportTask or an ExportTask tries to create a dialog (i.e. an
   * Import Complete alert), but their invoking ImportCSVActivity has been destroyed because the
   * user rotated the screen while the import was in progress, it will have been populated with a
   * new and valid fragment manager instead of trying to use the one saved off in context which
   * will then be invalid. The only thing we keep context around for is getAppName which
   * thankfully doesn't crash the app when you call it on a destroyed object, unlike
   * getFragmentManager
   */
  public static FragmentManager fragman = null;
  /**
   * Used for logging
   */
  private String appName;

  /**
   * Public method that returns a new ImportExportDialogFragment. SET A FRAGMENT MANAGER BEFORE CALLING
   * NEWINSTANCE
   *
   * @param id  which dialog to create
   * @param act an activity. We need to take an activity because you can't call getString from a
   *            static method
   * @return a new ImportExportDialogFragment that has already been shown. If it's a progress dialog,
   * the caller is expected to dismiss it. If it's an AlertDialog, the user can dismiss it
   */
  public static ImportExportDialogFragment newInstance(int id, AbsBaseActivity act) {
    String message;
    int type = ALERT_DIALOG;
    switch (id) {
    case CSVEXPORT_SUCCESS_DIALOG:
      message = act.getString(R.string.export_success);
      break;
    case CSVIMPORT_SUCCESS_DIALOG:
      message = act.getString(R.string.import_success);
      break;
    case EXPORT_IN_PROGRESS_DIALOG:
      type = PROGRESS_DIALOG;
      message = act.getString(R.string.export_in_progress_generic);
      break;
    case IMPORT_IN_PROGRESS_DIALOG:
      type = PROGRESS_DIALOG;
      message = act.getString(R.string.import_in_progress_generic);
      break;
    case CSVIMPORT_FAIL_DIALOG:
      message = act.getString(R.string.import_failure);
      break;
    case CSVEXPORT_FAIL_DIALOG:
      message = act.getString(R.string.export_failure);
      break;
    case CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG:
      message = act.getString(R.string.export_partial_success);
      break;
    default:
      throw new IllegalArgumentException();
    }

    ImportExportDialogFragment frag = new ImportExportDialogFragment();
    frag.appName = act.getAppName(); // it's private
    // Stuff we put in args can be accessed from onCreateDialog by
    Bundle args = new Bundle();
    args.putString("message", message);
    args.putInt("which", id); // unused
    args.putInt("type", type);
    frag.setArguments(args);
    if (fragman != null) {
      frag.show(fragman, "dialog");
    } else {
      WebLogger.getLogger(frag.appName).a(TAG, "Someone forgot to give me a fragment manager. "
          + "Trying to use the one from context, but it will almost certainly crash if android "
          + "reloaded it");
      frag.show(act.getSupportFragmentManager(), "dialog");
    }
    return frag;
  }

  /**
   * Takes a message and sets the dialog's text to that message. That's all.
   *
   * @param task   used for running on the UI thread
   * @param id     the string resource to get
   * @param status the message to set the dialog's text to
   */
  public void updateProgressDialogStatusString(Activity task, final int id,
      final int status, final int total) {
    task.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (getActivity() == null) {
          // not attached, happens when we get an update while the user is in the middle of
          // rotating the screen
          return;
        }
        Dialog d = activeDialogFragment.getDialog();
        if (d == null) {
          WebLogger.getLogger(appName).a(TAG, "Undismissable dialog was dismissed somehow!");
          return;
        }
        if (getArguments().getInt("type") == PROGRESS_DIALOG) {
          String message = getString(id, status, total);
          ((AlertDialog) d).setMessage(message);
          getArguments().putString("message", message); // in case the screen is rotated and the
          // dialog gets recreated, don't reset to the default message
        }
      }
    });
  }

  /**
   * DO NOT CALL THIS METHOD
   * It's called automatically by the parent class.
   * If you do call this method on a new ImportExportDialogFragment(), args will be empty and you'll get
   * a null pointer exception. If you call this method on an ImportExportDialogFragment created with
   * newInstance you'll get a dialog that you'll have to manage yourself, defeating the purpose
   * of using a DialogFragment in the first place.
   * <p>
   * It creates a new Dialog object based on the type and message specified in the args that were
   * put into it in newInstance.
   *
   * @param savedState unused
   * @return the dialog object to be displayed to the user.
   */
  @Override
  public Dialog onCreateDialog(Bundle savedState) {
    activeDialogFragment = this;
    super.onCreateDialog(savedState);
    Bundle args = getArguments();
    if (args.getInt("type") == PROGRESS_DIALOG) {
      // This took a solid hour to figure out. For some reason a ProgressDialog.Buidler's build
      // method sometimes returns an AlertDialog. Don't use it or we won't be able to call
      // setMessage in updateProgressDialogStatusString
      ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
      dialog.setMessage(args.getString("message"));
      dialog.setIndeterminate(true);
      dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      dialog.setCancelable(false);
      dialog.setCanceledOnTouchOutside(getRetainInstance());
      // Unfortunately they can still dismiss it by pressing the back button to disable the soft
      // keyboard. Don't tell anyone
      return dialog;
    }
    AlertDialog.Builder builder = new ProgressDialog.Builder(getActivity());
    builder = builder.setMessage(args.getString("message"));
    builder = builder
        .setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        });
    return builder.create();
  }
}
