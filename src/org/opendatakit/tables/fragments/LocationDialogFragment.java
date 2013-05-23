package org.opendatakit.tables.fragments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.tables.activities.TableActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class LocationDialogFragment extends DialogFragment {

  public static final String LOCATION_KEY = "locationkey";
  public static final String ELEMENT_NAME_TO_VALUE_KEY = "elementnametovaluekey";

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle bundle = getArguments();
    String location = bundle.getString(LOCATION_KEY);
    if (location != null) {
      // Use the Builder class for convenient dialog construction
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage("Would you like to add a row at: " + location + "?")
          .setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              ((TableActivity) getActivity()).addRow(getElementNameToValueMap());
            }
          }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              // User cancelled the dialog
            }
          });
      return builder.create();
    }
    return null;
  }

  private Map<String, String> getElementNameToValueMap() {
    Bundle bundle = getArguments();
    List<String> strings = bundle.getStringArrayList(ELEMENT_NAME_TO_VALUE_KEY);
    Map<String, String> elementNameToValue = new HashMap<String, String>();
    for (int i = 0; i < strings.size(); i += 2) {
      elementNameToValue.put(strings.get(i), strings.get(i + 1));
    }
    return elementNameToValue;
  }
}
