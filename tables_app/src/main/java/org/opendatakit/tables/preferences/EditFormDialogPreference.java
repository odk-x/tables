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
package org.opendatakit.tables.preferences;

import android.app.Activity;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;

public class EditFormDialogPreference extends DialogPreference {

  // Used for logging
  private static final String TAG = EditFormDialogPreference.class.getName();

  // The context and activity we're running in
  private Context mContext;
  private AbsTableActivity mActivity;
  private FormType mFormType = null;
  // The EditText that the user actually types in
  private EditText mFormId = null;

  /**
   * Sets up the activity and makes sure that we're executing in a TableActivity
   *
   * @param context saved
   * @param attrs   unused
   */
  public EditFormDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
    Activity activity = (Activity) getContext();
    if (!(activity instanceof AbsTableActivity)) {
      throw new IllegalArgumentException(
          "EditFormDialogPreference must be associated with an AbsTableActivity");
    }
    mActivity = (AbsTableActivity) activity;
  }

  /**
   * Retrieve the {@link FormType} for the table.
   *
   * @return the form from the table activity
   * @throws ServicesAvailabilityException if the database is down
   */
  private FormType retrieveFormType() throws ServicesAvailabilityException {
    AbsTableActivity tableActivity = (AbsTableActivity) getContext();
    return FormType
        .constructFormType(tableActivity.getAppName(), tableActivity.getTableId());
  }

  /**
   * Called to create a dialog view to let the user select a form
   *
   * @return The dialog to be displayed to the user
   */
  @Override
  protected View onCreateDialogView() {
    AbsTableActivity tableActivity = (AbsTableActivity) getContext();
    WebLogger.getLogger(tableActivity.getAppName()).d(TAG, "in onCreateDialogView");
    LayoutInflater inflater = (LayoutInflater) this.mContext
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout view = (LinearLayout) inflater
        .inflate(R.layout.edit_default_form_preference, null);
    mFormId = (EditText) view.findViewById(R.id.edit_form_id);

    try {
      this.mFormType = retrieveFormType();
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(tableActivity.getAppName()).printStackTrace(e);
      Toast.makeText(getContext(), getContext().getString(R.string.unable_to_retrieve_form_type),
          Toast.LENGTH_LONG).show();
    }
    this.mFormId.setEnabled(true);
    this.mFormId.setText(mFormType.getFormId());

    return view;
  }

  /**
   * Called when the user clicks either ok or cancel
   *
   * @param positiveResult whether the user clicked ok or cancel
   */
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
      String formId = this.mFormId.getText().toString();
      if (formId.isEmpty()) {
        alertInvalidForm();
        return;
      }

      // TODO there's a better way to do this
      String formDir = ODKFileUtils
          .getFormFolder(mActivity.getAppName(), mActivity.getTableId(), formId);
      File f = new File(formDir);
      File formDefJson = new File(f, ODKFileUtils.FORMDEF_JSON_FILENAME);
      if (!f.exists() || !f.isDirectory() || !formDefJson.exists() || !formDefJson.isFile()) {
        alertInvalidForm();
        return;
      }

      this.mFormType.setFormId(formId);
      AbsTableActivity tableActivity = (AbsTableActivity) getContext();

      try {
        this.mFormType
            .persist(tableActivity.getAppName(), tableActivity.getTableId());
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(tableActivity.getAppName()).printStackTrace(e);
        Toast.makeText(getContext(), getContext().getString(R.string.unable_to_save_db_changes),
            Toast.LENGTH_LONG).show();
      }
    }
  }

  /**
   * Helper method to warn the user about having typed a form id for a form that doesn't exist
   */
  private void alertInvalidForm() {
    Toast.makeText(mContext, mActivity.getString(R.string.invalid_form), Toast.LENGTH_LONG).show();
  }
}