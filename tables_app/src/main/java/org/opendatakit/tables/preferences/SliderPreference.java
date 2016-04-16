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

import org.opendatakit.tables.R;

import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;


public class SliderPreference extends Preference {

    private final SliderDialog mDialog;
    private int mDefaultValue;
    
    public SliderPreference(Context context, AttributeSet attrs) {
      super(context, attrs);
      this.mDialog = new SliderDialog(context);
      this.mDefaultValue = 0;
    }

    @Override
    protected void onClick() {
      mDialog.show();
    }

    public void setDialogTitle(String title) {
      mDialog.setTitle(title);
    }

    public void setMaxValue(int maxValue) {
      mDialog.setMaxSliderValue(maxValue);
    }

    public void setValue(int value) {
      mDialog.setSliderValue(value);
    }

    /** adds an option to use default */
    public void addDefaultOption(boolean useDefault) {
      mDialog.addDefaultCheckbox();
    }

    /** checks the checkBox **/
    public void checkCheckBox(boolean check) {
      mDialog.checkCheckBox(check);
    }

    /** true if checked */
    public boolean isChecked() {
    	return mDialog.isChecked();
    }

    /** enables the slider */
    public void setSliderEnabled(boolean enable) {
      mDialog.setSliderEnabled(enable);
    }

    private class SliderDialog extends Dialog {

        int value = 0;
        private TextView seekLabel;
        private SeekBar seekBar;
        private CheckBox checkBox;
        private LinearLayout checkBoxWrap;

        public SliderDialog(Context context) {
            super(context);
            prepareView(context);
        }

        private void prepareView(Context context) {
            seekLabel = new TextView(context);

            // checkBox to use default fontSize (relevant only when DisplayPref is
            // called through Controller)
            checkBox = new CheckBox(context);
            checkBox.setText(context.getString(R.string.use_default));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				  mDialog.setSliderEnabled(!isChecked);
				  mDialog.setSliderValue(mDefaultValue);
				}
			});
            checkBoxWrap = new LinearLayout(context);
            checkBoxWrap.addView(checkBox);
            checkBoxWrap.setVisibility(View.GONE);

            seekBar = new SeekBar(context);
            seekBar.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                        boolean fromUser) {
                    seekLabel.setText((Integer.valueOf(progress)).toString());
                    value = progress;
                }
            });
            Button okButton = new Button(context);
            okButton.setText(context.getString(R.string.ok));
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callChangeListener(value);
                    dismiss();
                }
            });
            Button cancelButton = new Button(context);
            cancelButton.setText(context.getString(R.string.cancel));
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            LinearLayout seekWrap = new LinearLayout(context);
            seekWrap.addView(seekLabel);
            LinearLayout.LayoutParams seekBarLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
            seekBarLp.weight = 1;
            seekWrap.addView(seekBar, seekBarLp);
            LinearLayout controlWrap = new LinearLayout(context);
            controlWrap.addView(okButton);
            controlWrap.addView(cancelButton);
            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams seekWrapLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
            seekWrapLp.weight = 1;
            wrapper.addView(seekWrap, seekWrapLp);
            wrapper.addView(checkBoxWrap);
            wrapper.addView(controlWrap);
            setContentView(wrapper);
        }

        public void setMaxSliderValue(int maxValue) {
            seekBar.setMax(maxValue);
        }

        public void setSliderValue(int value) {
            this.value = value;
            seekLabel.setText((Integer.valueOf(value)).toString());
            seekBar.setProgress(value);
        }

        // adds the checkbox to use default font size
        public void addDefaultCheckbox() {
        	checkBoxWrap.setVisibility(View.VISIBLE);
        	setSliderEnabled(false);
        }

        // enables seekBar if a custom font size is going to be set
        public void setSliderEnabled(boolean enabled) {
        	seekBar.setEnabled(enabled);
        }

        // checks the checkbox
        public void checkCheckBox(boolean check) {
        	checkBox.setChecked(check);
        }

        // true if checked
        public boolean isChecked() {
        	return checkBox.isChecked();
        }
    }
}