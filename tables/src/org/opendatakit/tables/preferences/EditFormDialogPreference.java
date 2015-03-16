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

import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

import android.app.Activity;
import android.content.Context;
import android.os.RemoteException;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
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
  private RadioGroup mRadioChoice;
  private EditText mFormId;
  private TextView mFormXmlRootElementLabel;
  private EditText mFormXmlRootElement;

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
   * @throws RemoteException 
   */
  FormType retrieveFormType() throws RemoteException {
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
    mRadioChoice = (RadioGroup) view.findViewById(R.id.edit_def_form_choice);
    mFormId = (EditText) view.findViewById(R.id.edit_form_id);
    mFormXmlRootElementLabel = (TextView) view.findViewById(R.id.label_root_element);
    mFormXmlRootElement = (EditText) view.findViewById(R.id.edit_root_element);
    this.mDialogView = view;
    try {
      this.mFormType = retrieveFormType();
      // Set the UI for the first state.
      if (mFormType.isCollectForm()) {
        if (mFormType.isCustom()) {
          this.mRadioChoice.check(R.id.edit_def_form_use_collect_form);
          this.mFormId.setEnabled(true);
          this.mFormId.setText(mFormType.getFormId());
          this.mFormXmlRootElementLabel.setVisibility(View.VISIBLE);
          this.mFormXmlRootElement.setVisibility(View.VISIBLE);
          this.mFormXmlRootElement.setEnabled(true);
          this.mFormXmlRootElement.setText(mFormType.getFormRootElement());
        } else {
          this.mRadioChoice.check(R.id.edit_def_form_use_default_collect_form);
          this.mFormId.setEnabled(false);
          this.mFormId.setText(mFormType.getFormId());
          this.mFormXmlRootElementLabel.setVisibility(View.VISIBLE);
          this.mFormXmlRootElement.setVisibility(View.VISIBLE);
          this.mFormXmlRootElement.setEnabled(false);
          this.mFormXmlRootElement.setText(mFormType.getFormRootElement());
        }
      } else {
        this.mRadioChoice.check(R.id.edit_def_form_use_survey_form);
        this.mFormId.setEnabled(true);
        this.mFormId.setText(mFormType.getFormId());
        this.mFormXmlRootElementLabel.setVisibility(View.GONE);
        this.mFormXmlRootElement.setVisibility(View.GONE);
        this.mFormXmlRootElement.setEnabled(false);
        this.mFormXmlRootElement.setText("");
      }
    } catch ( RemoteException e ) {
      Toast.makeText(getContext(), "Unable to access database", Toast.LENGTH_LONG).show();
    }
    mRadioChoice.setOnCheckedChangeListener(new RadioGroupListener());
    return mDialogView;
  }

  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
      // If the use default form is checked, then we want to delete the custom
      // form.
      int id = this.mRadioChoice.getCheckedRadioButtonId();
      if (id == R.id.edit_def_form_use_survey_form) {
        String formId = this.mFormId.getText().toString();
        if (formId == null || formId.length() == 0) {
          // TODO: should throw an error or prevent the close?
          return;
        }
        this.mFormType.setIsCollectForm(false);
        this.mFormType.setIsCustom(true);
        this.mFormType.setFormId(formId);
      } else if (id == R.id.edit_def_form_use_collect_form) {
        String formId = this.mFormId.getText().toString();
        if (formId == null || formId.length() == 0) {
          // TODO: should throw an error or prevent the close?
          return;
        }
        String formRootElement = this.mFormXmlRootElement.getText().toString();
        if (formRootElement == null || formRootElement.length() == 0) {
          // TODO: should throw an error or prevent the close?
          return;
        }
        this.mFormType.setIsCollectForm(true);
        this.mFormType.setIsCustom(true);
        this.mFormType.setFormId(formId);
        this.mFormType.setFormRootElement(formRootElement);
      } else {
        this.mFormType.setIsCollectForm(true);
        this.mFormType.setIsCustom(false);
      }
      AbsTableActivity tableActivity = (AbsTableActivity) getContext();

      try {
        this.mFormType.persist(tableActivity, tableActivity.getAppName(), tableActivity.getTableId());
      } catch (RemoteException e) {
        WebLogger.getLogger(tableActivity.getAppName()).printStackTrace(e);
        Toast.makeText(getContext(), "Unable to save changes to database", Toast.LENGTH_LONG).show();
      }
    }
  }

  private class RadioGroupListener implements OnCheckedChangeListener {

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
      AbsTableActivity tableActivity = (AbsTableActivity) getContext();

      try {
        if (checkedId == R.id.edit_def_form_use_survey_form) {
          SurveyFormParameters params = SurveyFormParameters.constructSurveyFormParameters(
              tableActivity, tableActivity.getAppName(), tableActivity.getTableId());
          String formId;
          if (params.isUserDefined()) {
            formId = params.getFormId();
          } else {
            formId = "";
          }
          mFormType.setIsCollectForm(false);
          mFormType.setIsCustom(true);
          mFormType.setFormId(formId);
  
          mFormId.setEnabled(true);
          mFormId.setText(formId);
          mFormXmlRootElementLabel.setVisibility(View.GONE);
          mFormXmlRootElement.setVisibility(View.GONE);
          mFormXmlRootElement.setEnabled(false);
          mFormXmlRootElement.setText("");
        } else if (checkedId == R.id.edit_def_form_use_collect_form) {
          String formId;
          String formRootElement;
          CollectFormParameters params = CollectFormParameters.constructCollectFormParameters(
              tableActivity, tableActivity.getAppName(), tableActivity.getTableId());
          if (params.isCustom()) {
            formId = params.getFormId();
            formRootElement = params.getRootElement();
          } else {
            formId = "";
            formRootElement = "";
          }
          mFormType.setIsCollectForm(true);
          mFormType.setIsCustom(true);
          mFormType.setFormId(formId);
          mFormType.setFormRootElement(formRootElement);
  
          mFormId.setEnabled(true);
          mFormId.setText(formId);
          mFormXmlRootElementLabel.setVisibility(View.VISIBLE);
          mFormXmlRootElement.setVisibility(View.VISIBLE);
          mFormXmlRootElement.setEnabled(true);
          mFormXmlRootElement.setText(formRootElement);
        } else {
          CollectFormParameters params = CollectFormParameters.constructDefaultCollectFormParameters(
              tableActivity, tableActivity.getAppName(), tableActivity.getTableId());
          String formId = params.getFormId();
          String formRootElement = params.getRootElement();
          mFormType.setIsCollectForm(true);
          mFormType.setFormId(formId);
          mFormType.setFormRootElement(formRootElement);
          mFormType.setIsCustom(false);
  
          mFormId.setEnabled(false);
          mFormId.setText(formId);
          mFormXmlRootElementLabel.setVisibility(View.VISIBLE);
          mFormXmlRootElement.setVisibility(View.VISIBLE);
          mFormXmlRootElement.setEnabled(false);
          mFormXmlRootElement.setText(formRootElement);
        }
      } catch ( RemoteException e) {
        Toast.makeText(getContext(), "Unable to access database", Toast.LENGTH_LONG).show();
      }
    }

  }

}
