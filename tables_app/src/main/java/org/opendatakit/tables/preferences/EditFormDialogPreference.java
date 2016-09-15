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

import org.opendatakit.common.android.exception.ServicesAvailabilityException;
import org.opendatakit.common.android.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.types.FormType;

import android.app.Activity;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class EditFormDialogPreference extends DialogPreference {

  private static final String TAG = EditFormDialogPreference.class.getName();

  private Context mContext;
  private String mText;
  // This is the Activity that calls this object.
  private EditSavedViewEntryHandler callingActivity;
  // The view that will be shown in the dialog.
  private View mDialogView;
  private FormType mFormType;
  private EditText mFormId;

  public EditFormDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
    Activity activity = (Activity) getContext();
    if (!(activity instanceof AbsTableActivity)) {
      throw new IllegalArgumentException("EditFormDialogPreference must "
          + "be associated with an AbsTableActivity");
    }
  }

  /**
   * A wrapper around {@link DialogPreference#showDialog}. For use in testing.
   */
  void showDialog() {
    this.showDialog(getExtras());
  }

  /**
   * Retrieve the {@link FormType} for the table.
   * 
   * @return
   * @throws ServicesAvailabilityException
   */
  FormType retrieveFormType() throws ServicesAvailabilityException {
    AbsTableActivity tableActivity = (AbsTableActivity) getContext();
    return FormType.constructFormType(tableActivity, tableActivity.getAppName(),
        tableActivity.getTableId());
  }

  @Override
  protected View onCreateDialogView() {
    AbsTableActivity tableActivity = (AbsTableActivity) getContext();
    WebLogger.getLogger(tableActivity.getAppName()).d(TAG, "in onCreateDialogView");
    LayoutInflater inflater = (LayoutInflater) this.mContext
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout view = (LinearLayout) inflater
        .inflate(R.layout.edit_default_form_preference, null);
    mFormId = (EditText) view.findViewById(R.id.edit_form_id);
    this.mDialogView = view;

    try {
      this.mFormType = retrieveFormType();
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(tableActivity.getAppName()).printStackTrace(e);
      Toast.makeText(getContext(), getContext().getString(R.string.unable_to_retrieve_form_type),
              Toast.LENGTH_LONG).show();
    }
    this.mFormId.setEnabled(true);
    this.mFormId.setText(mFormType.getFormId());
    return mDialogView;
  }

  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
      String formId = this.mFormId.getText().toString();
      if (formId == null || formId.length() == 0) {
        // TODO: should throw an error or prevent the close?
        return;
      }
      this.mFormType.setFormId(formId);
      AbsTableActivity tableActivity = (AbsTableActivity) getContext();

      try {
        this.mFormType.persist(tableActivity, tableActivity.getAppName(), tableActivity.getTableId());
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(tableActivity.getAppName()).printStackTrace(e);
        Toast.makeText(getContext(), getContext().getString(R.string.unable_to_save_db_changes),
                Toast.LENGTH_LONG).show();
      }
    }
  }
}