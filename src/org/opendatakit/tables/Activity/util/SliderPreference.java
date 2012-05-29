package org.opendatakit.tables.Activity.util;

import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;


public class SliderPreference extends Preference {
    
    private final SliderDialog dialog;
    
    public SliderPreference(Context context) {
        super(context);
        dialog = new SliderDialog(context);
    }
    
    @Override
    protected void onClick() {
        dialog.show();
    }
    
    public void setDialogTitle(String title) {
        dialog.setTitle(title);
    }
    
    public void setMaxValue(int maxValue) {
        dialog.setMaxSliderValue(maxValue);
    }
    
    public void setValue(int value) {
        dialog.setSliderValue(value);
    }
    
    private class SliderDialog extends Dialog {
        
        int value = 0;
        private TextView seekLabel;
        private SeekBar seekBar;
        
        public SliderDialog(Context context) {
            super(context);
            prepareView(context);
        }
        
        private void prepareView(Context context) {
            seekLabel = new TextView(context);
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
                    seekLabel.setText((new Integer(progress)).toString());
                    value = progress;
                }
            });
            Button okButton = new Button(context);
            okButton.setText("OK");
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callChangeListener(value);
                    dismiss();
                }
            });
            Button cancelButton = new Button(context);
            cancelButton.setText("Cancel");
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
                        LinearLayout.LayoutParams.FILL_PARENT,
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
                        LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
            seekWrapLp.weight = 1;
            wrapper.addView(seekWrap, seekWrapLp);
            wrapper.addView(controlWrap);
            setContentView(wrapper);
        }
        
        public void setMaxSliderValue(int maxValue) {
            seekBar.setMax(maxValue);
        }
        
        public void setSliderValue(int value) {
            this.value = value;
            seekLabel.setText((new Integer(value)).toString());
            seekBar.setProgress(value);
        }
    }
}