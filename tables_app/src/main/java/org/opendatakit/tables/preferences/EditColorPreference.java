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

import android.content.Context;
import androidx.preference.Preference;
import android.util.AttributeSet;
import org.opendatakit.tables.views.ColorPickerDialog;
import org.opendatakit.tables.views.ColorPickerDialog.OnColorChangedListener;

/**
 * Preference for editing a color.
 *
 * @author sudar.sam@gmail.com
 */
public class EditColorPreference extends Preference {

  private Context mContext;

  /**
   * Default constructor, saves context
   *
   * @param context Passed into ColorPickerDialog
   * @param attrs   unused
   */
  public EditColorPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
  }

  /**
   * Set the onPreferenceClickListener so that the color picker is created when
   * the preference is clicked.
   *
   * @param listener     the interface that will receive the call
   *                     back when the dialog receives input--likely the calling activity.
   * @param key          the String key that will be passed back via the callback so you
   *                     know which color preference has been edited
   * @param title        the title of the dialog
   * @param initialColor the initial color the dialog should be set to
   */
  public void initColorPickerListener(final OnColorChangedListener listener, final String key,
      final String title, final int initialColor) {
    this.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        new ColorPickerDialog(mContext, listener, key, initialColor, initialColor, title).show();
        return true;
      }
    });
  }

}
