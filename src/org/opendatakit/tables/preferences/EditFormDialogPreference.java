package org.opendatakit.tables.preferences;

import java.util.HashMap;
import java.util.Map;

import org.opendatakit.tables.Activity.util.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.types.FormType;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class EditFormDialogPreference extends DialogPreference {
  
  private static final String TAG = EditFormDialogPreference.class.getName();
  
  // This is the Activity that calls this object.
  private EditSavedViewEntryHandler callingActivity;
  private EditText mEditText;
  private String mText;
  // The view that will be shown in the dialog.
  private View mDialogView;
  private TableProperties mTp;
  private FormType mFormType;

  
  public EditFormDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs, org.opendatakit.tables.R.style.DialogWindowTitle_Sherlock);
  }
    
  public EditFormDialogPreference(Context context, AttributeSet attrs, 
      EditSavedViewEntryHandler callingActivity) {
    super(context, attrs);
  }
  
  public EditFormDialogPreference(Context context, TableProperties tp) {
    super(context, null);
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
    this.mDialogView = mFormType.getDisplayView(getContext());
    return mDialogView;
  }
  
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
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
  
  /**
   * Get the EditText in the dialog.
   * @return
   */
  public EditText getEditText() {
    return mEditText;
  }  

}
