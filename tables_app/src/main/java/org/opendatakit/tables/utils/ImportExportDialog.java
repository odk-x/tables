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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.tasks.ImportTask;

/**
 * An abstract parent class for import/export activities.
 *
 * @author sudar.sam@gmail.com
 * @author unknown
 */
public class ImportExportDialog extends DialogFragment {


	/** dialog IDs */
	public static final int CSVEXPORT_SUCCESS_DIALOG = 1;
	public static final int CSVIMPORT_SUCCESS_DIALOG = 2;
	public static final int EXPORT_IN_PROGRESS_DIALOG = 3;
	public static final int IMPORT_IN_PROGRESS_DIALOG = 4;
	public static final int CSVIMPORT_FAIL_DIALOG = 5;
	public static final int CSVEXPORT_FAIL_DIALOG = 6;
	// This is intended to say that "your csv exported successfully, but there
	// was a problem with the key value store setting mapping.
	public static final int
	  CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG = 7;
	public static final int
	  CSVIMPORT_FAIL_DUPLICATE_TABLE = 8;
	protected static final int
	  CSVIMPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG = 9;
	private static final int ALERT_DIALOG = 0;
	private static final int PROGRESS_DIALOG = 1;


	public static ImportExportDialog newInstance(int id, AbsBaseActivity act) {
		String message;
		int type = ALERT_DIALOG;
		switch(id) {
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
		Bundle args = new Bundle();
		args.putString("message", message);
		args.putInt("which", id);
		args.putInt("type", type);
		frag.setArguments(args);
		frag.show(act.getFragmentManager(), "dialog");
		return frag;
	}


	 public void updateProgressDialogStatusString(ImportTask task, final String status) {
		 // TODO

		 //task.runOnUiThread(new Runnable() {
			 //@Override public void run() {
				 Dialog d = getDialog();
				 if ( d != null ) {
					 if ( d instanceof ProgressDialog ) {
						 ProgressDialog epd = (ProgressDialog) d;
						 epd.setMessage(status);
					 }
				 }
			 //}
		 //});

	 }


	/**
	 * @param args the dialog's message, type and id
	 * @return the dialog
	 */
	public Dialog onCreateDialog(Bundle args) {
		if (args == null) {
			args = getArguments();
		}
		AlertDialog.Builder builder;
		if (args.getInt("type") == ALERT_DIALOG)
		  builder = new AlertDialog.Builder(getActivity());
		else
			builder = new ProgressDialog.Builder(getActivity());
		builder = builder.setMessage(args.getString("message"));
		builder = builder.setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
		});
		return builder.create();
	}

}
