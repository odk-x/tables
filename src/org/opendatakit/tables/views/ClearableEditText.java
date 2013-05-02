package org.opendatakit.tables.views;

import org.opendatakit.tables.R;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

/**
 * A class to serve as an edit text that gives you a button to clear the 
 * contents of the edit box. Based on the code found at:
 * http://arunbadole1209.wordpress.com/2011/12/16/how-to-create-edittext-with-crossx-button-at-end-of-it/
 * @author sudar.sam@gmail.com
 *
 */
public class ClearableEditText extends RelativeLayout {
  
  LayoutInflater inflater = null;
  private EditText editText;
  private Button clearButton;

  public ClearableEditText(Context context) {
    super(context);
    initViews();
  }
  
  private void initViews() {
    inflater = (LayoutInflater)getContext()
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.edit_text_clearable, this, true);
    editText = (EditText) findViewById(R.id.clearable_edit);
    clearButton = (Button) findViewById(R.id.clearable_button_clear);
    clearButton.setVisibility(RelativeLayout.INVISIBLE);
    initClearTextListener();
    initTextWatcher();
    
  }
  
  /**
   * Return the EditText backing this ClearableEditText.
   * @return
   */
  public EditText getEditText() {
    return editText;
  }
  
  public Button getClearButton() {
    return clearButton;
  }
  
  /*
   * Init the listener for when you click the X button to clear the text.
   */
  private void initClearTextListener() {
    clearButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        editText.setText("");
      }
    });
  }
  
  /*
   * Init the text watcher to show/hide the button as appropriate.
   */
  private void initTextWatcher() {
    editText.addTextChangedListener(new TextWatcher() {

      @Override
      public void afterTextChanged(Editable s) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() > 0) {
          clearButton.setVisibility(RelativeLayout.VISIBLE);
        } else {
          // s.length == 0
          clearButton.setVisibility(RelativeLayout.INVISIBLE);
        }  
      }
    });
  }

}
