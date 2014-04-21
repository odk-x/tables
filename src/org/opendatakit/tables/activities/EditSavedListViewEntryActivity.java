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
package org.opendatakit.tables.activities;

import java.io.File;
import java.util.List;

import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utils.TableFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.preferences.EditNameDialogPreference;
import org.opendatakit.tables.preferences.EditSavedViewEntryHandler;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * This class is the point of interaction by which list views are added and
 * managed.
 *
 * @author sudar.sam@gmail.com
 *
 */
/*
 * All that I can think we need to have at this point is a name of the list view
 * and a filename associated with that list view. Perhaps we'll eventually also
 * want to set the type to be either collection view or regular? Unsure... It
 * should do checking for things like duplicate names, limit values and things
 * so that we won't be able to inject into the underlying SQL db, etc.
 */
public class EditSavedListViewEntryActivity extends PreferenceActivity implements
    EditSavedViewEntryHandler {

  private static final String TAG = EditSavedListViewEntryActivity.class.getName();

  private static final String OI_FILE_PICKER_INTENT_STRING = "org.openintents.action.PICK_FILE";

  /*
   * These are the keys in the preference_listview_entry.xml file that
   * correspond to the preferences for what they sound like.
   */
  private static final String PREFERENCE_KEY_LISTVIEW_NAME = "listview_name";
  private static final String PREFERENCE_KEY_LISTVIEW_FILE = "listview_file";

  /**
   * Intent key for the table id.
   */
  public static final String INTENT_KEY_TABLE_ID = "tableId";

  /**
   * Name of the list view. Also the name of the aspect where the info about the
   * last view resides.
   */
  public static final String INTENT_KEY_LISTVIEW_NAME = "listViewName";

  /**
   * Return code for having added a file name for the list view.
   */
  private static final int RETURN_CODE_NEW_FILE = 0;

  private String appName;
  // The table id to which this list view belongs.
  private String tableId;
  // The table properties object for the table to which it belongs.
  private TableProperties tp;
  // These are the partition and aspect helpers for setting info in the KVS.
  private KeyValueStoreHelper kvsh;
  private AspectHelper aspectHelper;
  // This is the user-defined name of the list view.
  private String listViewName;
  // This is the filename defined for this list view.
  private String listViewFilename;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    this.tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.listViewName = getIntent().getStringExtra(INTENT_KEY_LISTVIEW_NAME);
    this.tp = TableProperties.getTablePropertiesForTable(this, appName, tableId);
    this.kvsh = tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION_VIEWS);
    this.aspectHelper = kvsh.getAspectHelper(listViewName);
    if (kvsh.getAspectsForPartition().size() == 0) {
      setToDefault(listViewName);
    }
    this.listViewFilename = aspectHelper.getString(ListDisplayActivity.KEY_FILENAME);
    addPreferencesFromResource(org.opendatakit.tables.R.xml.preference_listview_entry);
  }

  @Override
  public void onResume() {
    super.onResume();
    init();
  }

  public String getCurrentViewName() {
    return listViewName;
  }

  /**
   * Just init some local things like the handlers we need to custom manager.
   */
  private void init() {
    // First set the appropriate summary information of the filename to
    // display to the user.
    EditNameDialogPreference namePreference = (EditNameDialogPreference) findPreference(PREFERENCE_KEY_LISTVIEW_NAME);
    namePreference.setCallingActivity(this);
    // We want the edit text to display the current name.
    Preference filePreference = findPreference(PREFERENCE_KEY_LISTVIEW_FILE);
    filePreference.setSummary(listViewFilename);
    // Changing the name of the listview is handled by android. We need to
    // handle the intent for launching and saving the file name ourselves.
    filePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      /**
       * If clicked, we want to launch the OI File Manager and be ready to have
       * it returned to us.
       * <p>
       * Will error if OI File Manager isn't installed.
       */
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent filePickerIntent = new Intent(OI_FILE_PICKER_INTENT_STRING);
        filePickerIntent.putExtra(Controller.INTENT_KEY_APP_NAME, appName);
        // Set the current filename.
        if (listViewFilename != null) {
          File adjustedFile = new File(ODKFileUtils.getAppFolder(appName),
              listViewFilename);
          filePickerIntent.setData(
              Uri.parse("file://" + adjustedFile.getAbsolutePath()));
        }
        try {
          startActivityForResult(filePickerIntent, RETURN_CODE_NEW_FILE);
        } catch (ActivityNotFoundException e) {
          e.printStackTrace();
          Toast.makeText(EditSavedListViewEntryActivity.this,
              getString(R.string.file_picker_not_found), Toast.LENGTH_LONG).show();
        }
        return true;
      }

    });
  }

  public void tryToSaveNewName(String newName) {
    EditNameDialogPreference namePreference = (EditNameDialogPreference) findPreference(PREFERENCE_KEY_LISTVIEW_NAME);
    if (newName.equals(listViewName)) {
      // Then nothing was changed, or the key did not exist.
      return;
    }
    // Otherwise, something changed and we need to update.
    // First let's do a check to see if it's a duplicate that we'll need to
    // disallow.
    List<String> existingListNames = kvsh.getAspectsForPartition();
    for (String name : existingListNames) {
      if (newName.equals(name)) {
        // Duplicate name, don't allow it.
        Toast.makeText(this, getString(R.string.error_in_use_list_view_name, newName), Toast.LENGTH_LONG)
            .show();
        return;
      }
    }
    // Otherwise, it changed and is an acceptable name. First we need to
    // delete the old entry, which we'll do by deleting the aspect.
    int numDeleted = aspectHelper.deleteAllEntriesInThisAspect();
    Log.d(TAG, "deleted " + numDeleted + " entries from aspect: " + listViewFilename);
    // Update the name.
    listViewName = newName;
    // Update the aspect helper so we are moving things to the correct place.
    aspectHelper = kvsh.getAspectHelper(listViewName);
    // If a filename exists, set it.
    if (listViewFilename != null && !listViewFilename.equals("")) {
      aspectHelper.setString(ListDisplayActivity.KEY_FILENAME, listViewFilename);
    }
    namePreference.setSummary(listViewName);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      return;
    }
    switch (requestCode) {
    case RETURN_CODE_NEW_FILE:
      Uri newFileUri = data.getData();
      String newFilename = newFileUri.getPath();
      if (newFilename != null && !newFilename.equals("")) {
        // Get the relative path under the app directory. This is what Tables
        // uses internally, as opposed to the full path as returned by OI
        // file picker.
        String relativePath = TableFileUtils.getRelativePath(newFilename);
        if (kvsh.getAspectsForPartition().size() == 0) {
          setToDefault(listViewName);
        }
        aspectHelper.setString(ListDisplayActivity.KEY_FILENAME, relativePath);
        listViewFilename = relativePath;
      } else {
        Log.d(TAG, "received null or empty string from file picker: " + newFilename);
      }
      break;
    default:
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void setToDefault(String nameOfListView) {
    KeyValueStoreHelper listViewKvsh =
        tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
    listViewKvsh.setString(ListDisplayActivity.KEY_LIST_VIEW_NAME,
        nameOfListView);
  }

  @Override
  protected void onPause() {
    super.onPause();
    // According to:
    // http://developer.android.com/guide/topics/ui/settings.html#Activity
    // the listener should be unregistered here.
    // getPreferenceScreen().getSharedPreferences()
    // .unregisterOnSharedPreferenceChangeListener(this);
  }

}
