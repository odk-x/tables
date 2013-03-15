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
package org.opendatakit.tables.lib;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

/**
 * Basic dialog to allow editing of a name of a list view. 
 * <p>
 * Based on Android's EditTextPreference class.
 * @author sudar.sam@gmail.com
 *
 */
public class EditNameDialogPreference extends DialogPreference {
  
  // This is the Activity that calls this object.
  private EditSavedViewEntryActivity callingActivity;
  private EditText mEditText;
  private String mText;
  
  public EditNameDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
    
  public EditNameDialogPreference(Context context, AttributeSet attrs, 
      EditSavedViewEntryActivity callingActivity) {
    super(context, attrs);
  }
  
  /**
   * Set the calling activity as well as the listview name for this dialog.
   * @param callingActivity
   */
  public void setCallingActivity(EditSavedViewEntryActivity 
      callingActivity) {
    this.callingActivity = callingActivity;
    mText = callingActivity.getCurrentViewName();
    // Display the name to the user.
    this.setSummary(mText);
  }
  

  /**
   * We need to override this so that we can get at our edit text that we've 
   * defined in the layout xml.
   * <p>
   * Normally we would find the view from a layout resource and then call 
   * findViewById on that view to get at our widgets we've defined in that xml.
   * However, we have no way of getting at that layout, as we've hooked it into
   * this class in the xml for this class, and the dialog is created for us,
   * which is functionality offered by PreferenceDialog. This is common 
   * practice, so to get at your view and thus offer access to the objects they
   * give you this method to override and bind to data.
   */
  @Override
  protected void onBindDialogView(View view) {
	int viewName = org.opendatakit.tables.R.id.edit_view_name;
    mEditText = (EditText) 
        view.findViewById(viewName);
    mEditText.setText(mText);
 
  }
  
  /**
   * Return the String that is currently in the dialog. NOT necessarily
   * what is in the EditText.
   * @return
   */
  public String getText() {
    return mText;
  }
  
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
      String value = mEditText.getText().toString();
      callingActivity.tryToSaveNewName(value);
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
