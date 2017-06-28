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
package org.opendatakit.tables.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Basic dialog to allow editing a text field
 * <p>
 * Based on Android's EditTextPreference class.
 *
 * @author sudar.sam@gmail.com
 */
public class EditNameDialogPreference extends DialogPreference {

  /**
   * Default constructor
   *
   * @param context unused
   * @param attrs   unused
   */
  public EditNameDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

}
