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
package org.opendatakit.tables.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;

/**
 * renamed from AbstractImportExportActivity to be both not abstract and not an activity, and to
 * use a DialogFragment instead of multiple Dialog objects. Niles 06/01/17
 *
 * @author sudar.sam@gmail.com
 * @author unknown
 */
public class ImportExportDialog extends DialogFragment {

  // dialog IDs, passed in to newInstance
  public static final int CSVEXPORT_SUCCESS_DIALOG = 1;
  public static final int CSVIMPORT_SUCCESS_DIALOG = 2;
  public static final int EXPORT_IN_PROGRESS_DIALOG = 3;
  public static final int IMPORT_IN_PROGRESS_DIALOG = 4;
  public static final int CSVIMPORT_FAIL_DIALOG = 5;
  public static final int CSVEXPORT_FAIL_DIALOG = 6;
  // This is intended to say that "your csv exported successfully, but there
  // was a problem with the key value store setting mapping.
  public static final int CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG = 7;
  public static final int CSVIMPORT_FAIL_DUPLICATE_TABLE = 8;
  protected static final int CSVIMPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG = 9;
  // private IDs that are put in the bundle of arguments to determine which type of dialog to create
  // can't use an enum because you can't (safely) put an enum in a bundle
  private static final int ALERT_DIALOG = 0;
  private static final int PROGRESS_DIALOG = 1;

  /**
   * Public method that returns a new ImportExportDialog
   *
   * @param id  which dialog to create
   * @param act an activity. We need to take an activity because you can't call getString from a
   *            static method
   * @return a new ImportExportDialog that has already been shown. If it's a progress dialog,
   * the caller is expected to dismiss it. If it's an AlertDialog, the user can dismiss it
   */
  public static ImportExportDialog newInstance(int id, AbsBaseActivity act) {
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
      message = act.getString(R.string.export_in_progress);
      break;
    case IMPORT_IN_PROGRESS_DIALOG:
      type = PROGRESS_DIALOG;
      message = act.getString(R.string.import_in_progress);
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
    case CSVIMPORT_FAIL_DUPLICATE_TABLE:
      message = act.getString(R.string.import_failure_existing_table);
      break;
    case CSVIMPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG:
      message = act.getString(R.string.import_partial_success);
      break;
    default:
      throw new IllegalArgumentException();
    }

    ImportExportDialog frag = new ImportExportDialog();
    // Stuff we put in args can be accessed from onCreateDialog by
    Bundle args = new Bundle();
    args.putString("message", message);
    args.putInt("which", id); // unused
    args.putInt("type", type);
    frag.setArguments(args);
    frag.show(act.getFragmentManager(), "dialog");
    return frag;
  }

  /**
   * Takes a message and sets the dialog's text to that message. That's all.
   *
   * @param task   used for running on the UI thread
   * @param status the message to set the dialog's text to
   */
  public void updateProgressDialogStatusString(AbsBaseActivity task, final String status) {
    // new Runnable()? Remind me again why we can't use lambdas
    task.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Dialog d = getDialog();
        if (getArguments().getInt("type") == PROGRESS_DIALOG) {
          ((ProgressDialog) d).setMessage(status);
        }
      }
    });
  }

  /**
   * DO NOT CALL THIS METHOD
   * It's called automatically by the parent class. Even though there's no @Override tag. I don't
   * know how that works
   * If you do call this method on a new ImportExportDialog(), args will be empty and you'll get
   * a null pointer exception. If you call this method on an ImportExportDialog created with
   * newInstance you'll get a dialog that you'll have to manage yourself, defeating the purpose
   * of using a DialogFragment in the first place.
   * <p>
   * It creates a new Dialog object based on the type and message specified in the args that were
   * put into it in newInstance.
   *
   * @param savedState unused
   * @return the dialog object to be displayed to the user.
   */
  public Dialog onCreateDialog(Bundle savedState) {
    Bundle args = getArguments();
    if (args.getInt("type") == PROGRESS_DIALOG) {
      // This took a solid hour to figure out. For some reason a ProgressDialog.Buidler's build
      // method sometimes returns an AlertDialog. Don't use it or we won't be able to call
      // setMessage in updateProgressDialogStatusString
      ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
      dialog.setMessage(args.getString("message"));
      dialog.setIndeterminate(true);
      dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
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
