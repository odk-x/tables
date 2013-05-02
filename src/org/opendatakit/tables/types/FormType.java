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
package org.opendatakit.tables.types;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Definition of the form data type.
 * @author sudar.sam@gmail.com
 *
 */
public class FormType {
  
  private static final String TAG = FormType.class.getName();
  
  /*
   * The two things that define a form in the system.
   */
  private CollectFormParameters mParams;
  // TODO: add in form version stuff
  private TableProperties mTp;
  private KeyValueStoreHelper mKvsh;
  private AspectHelper mAh;
  
  public static final String TITLE_FORM_ID = "Form Id";
  public static final String TITLE_ROOT_ELEMENT = "Root Element";
  
  /*
   * These are tags for marking the views so that we can come back and get them
   * upon saving.
   */
  public static final String TAG_FORM_ID = "formId";
  public static final String TAG_FORM_ROOT_ELEMENT = "rootElement";
  
  
  public FormType(CollectFormParameters params, TableProperties tp) {
    this.mParams = params;
    this.mTp = tp;
    this.mKvsh = tp.getKeyValueStoreHelper(CollectUtil.KVS_PARTITION);
    this.mAh = this.mKvsh.getAspectHelper(CollectUtil.KVS_ASPECT);
  }
  
  /**
   * Construct a form with the default info.
   * @param tp
   */
  public FormType(TableProperties tp) {
    // TODO: out of time, but need to do this!!
  }
  
  public String getFormId() {
    return this.mParams.getFormId();
  }
  
  public String getFormRootElement() {
    return this.mParams.getRootElement();
  }
  
  /**
   * Get the view that will display the information within this data type.
   * <p>
   * Eg for form it should have two text views--one for id, and one for root 
   * element.
   * @return
   */
  public View getDisplayView(Context context) {
    // TODO: be sure to save the information in these text views in the 
    // appropriate instance state methods.
    LinearLayout ll = new LinearLayout(context);
    ll.setOrientation(LinearLayout.VERTICAL);
    // Form Id text view title
    TextView idTitle = new TextView(context);
    idTitle.setText(TITLE_FORM_ID, TextView.BufferType.NORMAL);
    idTitle.setEnabled(true);
    // Form Id editable for the user to enter.
    EditText idText = new EditText(context);
    idText.setLayoutParams(
        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT));
    idText.setText(mParams.getFormId());
    idText.setEnabled(true);
    idText.setTag(TAG_FORM_ID);
    // Root Element Title
    TextView rootTitle = new TextView(context);
    rootTitle.setText(TITLE_ROOT_ELEMENT, TextView.BufferType.NORMAL);
    rootTitle.setEnabled(true);
    // Root Element editable for the user to enter.
    EditText rootText = new EditText(context);
    rootText.setText(mParams.getRootElement());
    rootText.setEnabled(true);
    rootText.setTag(TAG_FORM_ROOT_ELEMENT);
    ll.addView(idTitle);
    ll.addView(idText);
    ll.addView(rootTitle);
    ll.addView(rootText);
    return ll;
  }
  
  /**
   * Receive a map of the view tags in {@link getTagsInView} mapped to the new
   * values. Update this object to reflect the new values and update the new
   * values in the database. Only updates if the data is determined to be new.
   * @param newValues
   * @return
   */
  public void udateAndPersist(Map<String, Object> newValues) {
    // Maybe we should be making this transactional?
    String newFormId = (String) newValues.get(TAG_FORM_ID);
    String newRootElement = (String) newValues.get(TAG_FORM_ROOT_ELEMENT);
    if (!mParams.getFormId().equals(newFormId) || 
        !mParams.getRootElement().equals(newRootElement)) {
      this.mParams.setFormId(newFormId);
      this.mParams.setRootElement(newRootElement);
      mAh.deleteAllEntriesInThisAspect();
      mAh.setString(CollectUtil.KEY_FORM_ID, newFormId);
      mAh.setString(CollectUtil.KEY_FORM_ROOT_ELEMENT, newRootElement);
    }
  }
  
  /**
   * Get the set of tags of views that are of interest to the caller. In this 
   * case we're getting them so that upon success, they can be returned to the
   * user if they're changed so they can be persisted.
   * @return
   */
  public static Set<String> getTagsInView() {
    Set<String> tags = new HashSet<String>();
    tags.add(TAG_FORM_ID);
    tags.add(TAG_FORM_ROOT_ELEMENT);
    return tags;
  }

}
