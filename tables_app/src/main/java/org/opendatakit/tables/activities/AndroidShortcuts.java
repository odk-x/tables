/*
 * Copyright (C) 2011 University of Washington
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

package org.opendatakit.tables.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.opendatakit.activities.BaseActivity;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.database.utilities.CursorUtils;
import org.opendatakit.provider.TableDefinitionsColumns;
import org.opendatakit.provider.TablesProviderAPI;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Allows the user to create desktop shortcuts to any form currently available to survey
 * You add it to the menu via the "widgets" section of the app grid
 *
 * @author ctsims
 * @author carlhartung (modified for ODK)
 */
public class AndroidShortcuts extends BaseActivity {

  private static final boolean EXIT = true;
  private ArrayList<Choice> choices = new ArrayList<>();

  /**
   * Verifies that the sd card is available, and sets up our intent to let the user select from a
   * list of "shortcuts" (widgets)
   *
   * @param savedInstanceState the saved state, largely unused
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // verify that the external SD Card is available.
    try {
      ODKFileUtils.verifyExternalStorageAvailability();
    } catch (RuntimeException e) {
      createErrorDialog(e.getMessage(), EXIT); // Will finish() when the user clicks ok
      return;
    }

    final Intent intent = getIntent();
    final String action = intent.getAction();

    // The Android needs to know what shortcuts are available, generate the list
    if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
      buildMenuList();
    }
  }

  /**
   * Builds a list of shortcuts, one per form. This is for the "Survey App" widget
   */
  private void buildMenuList() {
    Bitmap appIcon = BitmapFactory.decodeResource(getResources(), R.drawable.tables_app);
    Bitmap tableIcon = BitmapFactory.decodeResource(getResources(), R.drawable.tables_table);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.select_odk_shortcut));

    choices.clear();

    // Get a list of all the app names and iterate over them, adding one entry to choices for
    // each form
    File[] directories = ODKFileUtils.getAppFolders();
    if (directories != null) {
      for (File app : directories) {
        String appName = app.getName();
        Uri uri = Uri.withAppendedPath(TablesProviderAPI.CONTENT_URI, appName);
        // This will have the survey icon with a big red "App" on the top of it. The other
        // shortcuts will just have the regular survey app icon
        choices.add(new Choice(R.drawable.tables_app, appIcon, uri, appName, appName));

        // Iterate over all the tables in the app and add them to choices
        Cursor c = null;
        try {
          c = getContentResolver()
              .query(Uri.withAppendedPath(TablesProviderAPI.CONTENT_URI, appName), null, null, null,
                  null);

          if (c != null && c.getCount() > 0) {
            // Move to one before the first entry so that moveToNext will put us at the first entry
            c.moveToPosition(-1);
            while (c.moveToNext()) {
              String tableId = CursorUtils
                  .getIndexAsString(c, c.getColumnIndex(TableDefinitionsColumns.TABLE_ID));
              String tableName = app.getName() + " > " + tableId;
              uri = Uri
                  .withAppendedPath(Uri.withAppendedPath(TablesProviderAPI.CONTENT_URI, appName),
                      tableId);
              choices.add(new Choice(R.drawable.tables_table, tableIcon, uri, tableName, appName));
            }
          }
        } finally {
          if (c != null) {
            c.close();
          }
        }
      }
    } else {
      Toast.makeText(getApplicationContext(),
          getString(R.string.file_not_under_app_dir, "OpenDataKit"), Toast.LENGTH_LONG).show();
    }

    builder.setAdapter(new ArrayAdapter<Choice>(this, R.layout.shortcut_item, choices) {
      @SuppressLint("InflateParams")
      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View row;

        if (convertView == null) {
          row = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
              .inflate(R.layout.shortcut_item, null);
        } else {
          row = convertView;
        }
        TextView vw = row.findViewById(R.id.shortcut_title);
        Choice item = getItem(position);
        if (item == null) {
          TextView res = new TextView(AndroidShortcuts.this);
          res.setText(R.string.error);
          return res;
        }
        // Future: vw.setTextSize(Tables.getQuestionFontsize(getItem(position).appName));
        //noinspection MagicNumber
        vw.setTextSize(18);
        vw.setText(item.name);

        ImageView iv = row.findViewById(R.id.shortcut_icon);
        iv.setImageBitmap(item.icon);

        return row;
      }
    }, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Choice choice = choices.get(which);
        returnShortcut(choice);
      }
    });

    // If the user decides not to create a widget, finish() gracefully
    builder.setOnCancelListener(new OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        AndroidShortcuts sc = AndroidShortcuts.this;
        sc.setResult(RESULT_CANCELED);
        sc.finish();
      }
    });

    AlertDialog alert = builder.create();
    alert.show();
  }

  /**
   * Returns the results to the calling intent, called when the user clicks on a choice
   */
  private void returnShortcut(Choice choice) {
    Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
    shortcutIntent.putExtra(IntentConsts.INTENT_KEY_APP_NAME, choice.appName);
    shortcutIntent.setData(choice.command);

    Intent intent = new Intent();
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, choice.name);
    Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, choice.iconResourceId);
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

    // Now, return the result to the launcher

    setResult(RESULT_OK, intent);
    finish();
  }

  /**
   * Private helper method used to create a dialog. Doesn't use a dialog fragment, that's a TODO
   *
   * @param errorMsg   what message to display to the user
   * @param shouldExit whether we should finish() the activity if the user clicks the "yes" button
   */
  private void createErrorDialog(CharSequence errorMsg, final boolean shouldExit) {
    AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();
    mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
    mAlertDialog.setMessage(errorMsg);
    DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int i) {
        if (DialogInterface.BUTTON_POSITIVE == i && shouldExit) {
          finish();
        }
      }
    };
    mAlertDialog.setCancelable(false);
    mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), errorListener);
    mAlertDialog.show();
  }

  /**
   * standard getter for the app name
   *
   * @return the app name
   */
  @Override
  public String getAppName() {
    return Tables.getInstance().getVersionedToolName();
  }

  /**
   * We have to have this method because we implement DatabaseConnectionListener
   */
  @Override
  public void databaseAvailable() {
  }

  /**
   * We have to have this method because we implement DatabaseConnectionListener
   */
  @Override
  public void databaseUnavailable() {
  }

  private static class Choice {
    public final Bitmap icon;
    public final String name;
    public final String appName;
    final int iconResourceId;
    final Uri command;

    Choice(int iconResourceId, Bitmap icon, Uri command, String name, String appName) {
      this.iconResourceId = iconResourceId;
      this.icon = icon;
      this.command = command;
      this.name = name;
      this.appName = appName;
    }
  }

}
