package org.opendatakit.tables.preferences;

import java.util.HashMap;
import java.util.Map;

import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;

import android.R;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

public class EditFormDialogPreference extends DialogPreference {

  private static final String TAG = EditFormDialogPreference.class.getName();

  private Context mContext;
  // This is the Activity that calls this object.
  private EditSavedViewEntryHandler callingActivity;
  private EditText mEditText;
  private String mText;
  // The view that will be shown in the dialog.
  private View mDialogView;
  private TableProperties mTp;
  private FormType mFormType;
  // whether or not a default view is being used.
  private CheckBox mDefaultCheckBox;


  public EditFormDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
  }

  public EditFormDialogPreference(Context context, AttributeSet attrs,
      EditSavedViewEntryHandler callingActivity) {
    super(context, attrs);
    this.mContext = context;
  }

  public EditFormDialogPreference(Context context, TableProperties tp) {
    super(context, null);
    this.mContext = context;
    this.mTp = tp;
    CollectFormParameters params =
        CollectFormParameters.constructCollectFormParameters(tp);
    this.mFormType = new FormType(params, mTp);
  }

  /**
   * Set the calling activity as well as the listview name for this dialog.
   * @param callingActivity
   */
  public void setCallingActivity(EditSavedViewEntryHandler
      callingActivity) {
    this.callingActivity = callingActivity;
    mText = callingActivity.getCurrentViewName();
    // Display the name to the user.
    this.setSummary(mText);
  }

  @Override
  protected void onBindDialogView(View view) {
   Log.d(TAG, "in onBindDIalogView");

  }

  /**
   * Return the String that is currently in the dialog. NOT necessarily
   * what is in the EditText.
   * @return
   */
  public String getText() {
    return mText;
  }

  @Override
  protected View onCreateDialogView() {
    Log.d(TAG, "in onCreateDialogView");
    LayoutInflater inflater = 
        (LayoutInflater) this.mContext.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout view = (LinearLayout) inflater.inflate(
        org.opendatakit.tables.R.layout.edit_default_form_preference, null);
    this.mDefaultCheckBox = (CheckBox) view.findViewById(
        org.opendatakit.tables.R.id.edit_def_form_checkbox_use_default_form);
    LinearLayout paramLayout = this.mFormType.getDisplayView(getContext());
    view.addView(paramLayout);
    this.mDialogView = view;
    // Set the UI for the first state.
    this.mDefaultCheckBox.setChecked(!this.mFormType.isCustom());
    this.mDefaultCheckBox.setOnClickListener(new CheckBoxListener());
    this.updateUIForUseDefault(!this.mFormType.isCustom());
    return mDialogView;
  }

  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
      // If the use default form is checked, then we want to delete the custom
      // form. 
      if (this.mDefaultCheckBox.isChecked()) {
        if (this.mFormType.isCustom()) {
          this.mFormType.deleteCustomForm();
          Log.i(TAG, "deleting custom form");
          // And now update this object so that when if the preference is 
          // pressed again we'll be sure to have the correct contents.
          CollectFormParameters params =
              CollectFormParameters.constructCollectFormParameters(this.mTp);
          this.mFormType = new FormType(params, mTp);
        } else {
          // We're already displaying the default stuff, so we don't have to
          // change anything. If they edited the fields and they never got
          // saved but instead clicked the checkbox again, we're just letting
          // the contents of the EditTexts disappears, and no big deal.
        }
      } else {
        // We should be using a custom form.
        // TODO: some sort of checking to make sure the form actually exists
        // in Collect.
        Map<String, Object> newValues = new HashMap<String, Object>();
        String newFormTitle = ((EditText) mDialogView.findViewWithTag(
            FormType.TAG_FORM_ID)).getText().toString();
        String newRootElement = ((EditText) mDialogView.findViewWithTag(
            FormType.TAG_FORM_ROOT_ELEMENT)).getText().toString();
        newValues.put(FormType.TAG_FORM_ID, newFormTitle);
        newValues.put(FormType.TAG_FORM_ROOT_ELEMENT, newRootElement);
        mFormType.udateAndPersist(newValues);
      }
    }
  }
  
  /**
   * Update the UI for whether or not we are currently using the default form.
   * @param useDefault
   */
  private void updateUIForUseDefault(boolean useDefault) {
    EditText formIdBox = (EditText) mDialogView.findViewWithTag(
        FormType.TAG_FORM_ID);
    EditText rootElementBox = (EditText) mDialogView.findViewWithTag(
        FormType.TAG_FORM_ROOT_ELEMENT);
    formIdBox.setEnabled(false);
    rootElementBox.setEnabled(false);
    if (useDefault) {
      // Then we aren't using a custom form and these shouldn't be editable.
      formIdBox.setEnabled(false);
      rootElementBox.setEnabled(false);
    } else {
      // Then we're allowing a custom form, so enable these.
      formIdBox.setEnabled(true);
      rootElementBox.setEnabled(true);
    }
  }

  /**
   * Get the EditText in the dialog.
   * @return
   */
  public EditText getEditText() {
    return mEditText;
  }
  
  
  private class CheckBoxListener implements OnClickListener {

    @Override
    public void onClick(View v) {
      // We want to update the UI based on the state of the checkbox. 
      CheckBox checkBox = (CheckBox) v;
      EditFormDialogPreference.this.updateUIForUseDefault(
          checkBox.isChecked());
    }
    
  }

}
