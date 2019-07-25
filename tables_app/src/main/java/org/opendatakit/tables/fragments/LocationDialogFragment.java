/*
 * Copyright (C) 2014 University of Washington
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

import android.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;
import com.fasterxml.jackson.core.type.TypeReference;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The LocationDialogFragment is used when asking the user if they would like to
 * add a location to the map.
 *
 * @author Chris Gelon (cgelon)
 */
public class LocationDialogFragment extends DialogFragment {

  /**
   * The key in the argument bundle to grab the location that the row will be
   * added at.
   */
  public static final String LOCATION_KEY = "locationkey";
  /**
   * JSON stringify of Map<String,Object> for the elementKey -to- value map.
   */
  public static final String ELEMENT_KEY_TO_VALUE_MAP_KEY = "elementKeyToValueMapKey";

  /**
   * There is no way to store a map in a bundle, so I had to store it as a list,
   * alternating the key and the value. This recreates the map from the bundle.
   */
  private static Map<String, Object> getElementKeyToValueMap(
      String jsonStringifyElementKeyToValue) {
    HashMap<String, Object> elementKeyToValue = new HashMap<>();
    if (jsonStringifyElementKeyToValue != null) {
      TypeReference<HashMap<String, Object>> ref = new TypeReference<HashMap<String, Object>>() {
      };
      try {
        elementKeyToValue = ODKFileUtils.mapper.readValue(jsonStringifyElementKeyToValue, ref);
      } catch (IOException e) {
        WebLogger.getLogger(null).printStackTrace(e);
      }
    }
    return elementKeyToValue;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle bundle = getArguments();
    String location = bundle.getString(LOCATION_KEY);
    final Map<String, Object> mapping = getElementKeyToValueMap(
        bundle.getString(ELEMENT_KEY_TO_VALUE_MAP_KEY));
    if (location != null) {
      // Use the Builder class for convenient dialog construction
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage("Would you like to add a row at: " + location + "?")
          .setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              AbsTableActivity activity = (AbsTableActivity) getActivity();
              try {
                ActivityUtil
                    .addRow(activity, activity.getAppName(), activity.getTableId(), mapping);
              } catch (ServicesAvailabilityException e) {
                WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
                Toast.makeText(activity, "Unable to add row", Toast.LENGTH_LONG).show();
              }
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
}
