package org.opendatakit.tables.lib;

import org.opendatakit.tables.util.ColorPickerDialog.OnColorChangedListener;
import org.opendatakit.tables.util.ColorPickerDialog;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Preference for editing a color.
 * @author sudar.sam@gmail.com
 *
 */
public class EditColorPreference extends Preference {
  
  private EditSavedViewEntryActivity mCallingActivity;
  private Context mContext;  
  
  public EditColorPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
  }
  
  public EditColorPreference(Context context, AttributeSet attrs,
      EditSavedViewEntryActivity callingActivity) {
    super(context, attrs);
    this.mCallingActivity = callingActivity;
    this.mContext = context;
  }
  
  public void setCallingActivity(EditSavedViewEntryActivity callingActivity) {
    this.mCallingActivity = callingActivity;
  }
  
  /**
   * Set the onPreferenceClickListener so that the color picker is created when
   * the preference is clicked.
   * @param onColorChangedListener the interface that will receive the call 
   * back when the dialog receives input--likely the calling activity.
   * @param key the String key that will be passed back via the callback so you
   * know which color preference has been edited
   * @param title the title of the dialog
   * @param initialColor the initial color the dialog should be set to
   */
  public void initColorPickerListener(final OnColorChangedListener listener,
      final String key, final String title, final int initialColor) {
    this.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      
      @Override
      public boolean onPreferenceClick(Preference preference) {
        ColorPickerDialog dialog = new ColorPickerDialog(mContext,
            listener, key, initialColor, initialColor, title);
        dialog.show();
        return true;
      }
    });
  }

}
