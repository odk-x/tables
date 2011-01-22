package yoonsung.odk.spreadsheet.Activity.importexport;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;

/**
 * An abstract parent class for import/export activities.
 */
public abstract class IETabActivity extends Activity {
	
	/** dialog IDs */
	protected static final int CSVEXPORT_SUCCESS_DIALOG = 1;
	protected static final int CSVIMPORT_SUCCESS_DIALOG = 2;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case CSVEXPORT_SUCCESS_DIALOG:
			return getDialog("File exported.");
		default:
			throw new IllegalArgumentException();
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
	
	/**
	 * To be called in case of errors.
	 * @param errMsg the message to display to the user
	 * TODO: make this useful
	 */
	protected void notifyOfError(String errMsg) {
		Log.d("OH NOES", errMsg);
	}
	
}
