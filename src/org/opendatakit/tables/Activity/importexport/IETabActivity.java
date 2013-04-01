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
package org.opendatakit.tables.Activity.importexport;

import com.actionbarsherlock.app.SherlockActivity;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.util.TableFileUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * An abstract parent class for import/export activities.
 *
 * @author sudar.sam@gmail.com
 * @author unknown
 */
public abstract class IETabActivity extends SherlockActivity {

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

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case CSVEXPORT_SUCCESS_DIALOG:
			return getDialog("File exported.");
		case CSVIMPORT_SUCCESS_DIALOG:
			return getDialog("File imported.");
		case EXPORT_IN_PROGRESS_DIALOG:
			ProgressDialog epd = new ProgressDialog(this);
			epd.setMessage("exporting...");
			return epd;
		case IMPORT_IN_PROGRESS_DIALOG:
			ProgressDialog ipd = new ProgressDialog(this);
			ipd.setMessage("importing...");
			return ipd;
		case CSVIMPORT_FAIL_DIALOG:
			return getDialog("Failed to import.");
		case CSVEXPORT_FAIL_DIALOG:
			return getDialog("Failed to export.");
		case CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG:
		  return getDialog("Data exported, but some customized settings were " +
		  		"not able to exported.");
		case CSVIMPORT_FAIL_DUPLICATE_TABLE:
		  return getDialog("Failed to import. A table already exists with the " +
		  		"given table id or database name.");
		case CSVIMPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG:
		  return getDialog("Imported file, but was not able to recover all " +
		  		"customized settings.");
		default:
			throw new IllegalArgumentException();
		}
	}

    protected class PickFileButtonListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent("org.openintents.action.PICK_FILE");
            intent.setData(Uri.parse("file://" + ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME)));
            intent.putExtra("org.openintents.extra.TITLE", "Please select a file");
            startActivityForResult(intent, 1);
        }
    }

	/**
	 * Creates a simple alert dialog.
	 * @param message the dialog's message
	 * @return the dialog
	 */
	private AlertDialog getDialog(String message) {
		AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
		adBuilder = adBuilder.setMessage(message);
		adBuilder = adBuilder.setNeutralButton("OK",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
		});
		AlertDialog d = adBuilder.create();
		return d;
	}

}
