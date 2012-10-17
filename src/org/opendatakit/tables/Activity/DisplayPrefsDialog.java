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
package org.opendatakit.tables.Activity;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.DataStructure.ColColorRule;
import org.opendatakit.tables.DataStructure.ColColorRule.RuleType;
import org.opendatakit.tables.DataStructure.DisplayPrefs;
import org.opendatakit.tables.lib.ColorPickerDialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * The dialog for managing display preferences for a column.
 * 
 * Big freaking mess and currently in an indeterminate state! Oct13
 */
public class DisplayPrefsDialog extends Dialog {

  public static final String TAG = "DisplayPrefsDialog";

  private Context c;
  private DisplayPrefs dp;
  private String colName;
  // SS: going to set this as null and ONLY refresh from the db once. Otherwise
  // we overwrite our new rules. What we really want is the rules from the db
  // and then the new ones we add. and then those only committed to the db
  // if they are valid rules.
  private List<ColColorRule> colRules = null;
  // the number of original rules. this is so we know if we need to call
  // update or add.
  int numOriginalRules;
  private List<EditText> ruleInputFields;
  int lastFocusedRow;

  DisplayPrefsDialog(Context c, DisplayPrefs dp, String colName) {
    super(c);
    this.c = c;
    this.dp = dp;
    this.colName = colName;
    // get rid of the leading underscore, as this will probably confuse the
    // user. the underscore is just for us.
    setTitle("Conditional Colors: " + colName.substring(1));
  }
  

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    refreshView();
  }
  
  @Override 
  public void onRestoreInstanceState(Bundle savedState) {
    refreshView();
  }
  

  /**
   * SS: This is a method I've made to clean up the state after they hit the ok
   * button so that if you reopen you don't end up seeing things that haven't
   * been persisted. This perhaps should go in one of android's life cycle
   * functions, but I'm not sure which one. Quick and dirty I'm just putting
   * here.
   */
  public void cleanForReOpen() {
    colRules = null;
    numOriginalRules = 0;
    lastFocusedRow = -1;
  }

  private void refreshView() {

    if (colRules == null) {
      // only do this once, b/c it clears the list, removing any that we've
      // added here that are not yet in the database.
      lastFocusedRow = -1;
      colRules = dp.getColorRulesForCol(colName);
      // and now we want to remember which rules are original ones.
      numOriginalRules = colRules.size();
    }
    ruleInputFields = new ArrayList<EditText>();
    final LinearLayout ll = new LinearLayout(c);
    ll.setOrientation(LinearLayout.VERTICAL);
    final TableLayout tl = new TableLayout(c);
    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    tl.setLayoutParams(tlp);
    for (int i = 0; i < colRules.size(); i++) {
      TableRow row = getEditRow(i);
      tl.addView(row);
    }
    ll.addView(tl);
    Button addRuleButton = new Button(c);
    addRuleButton.setText("Add Rule");
    addRuleButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        updateLastRowVal();
        // dp.addRule(colName, ' ', "", Color.BLACK, Color.WHITE);
        // refreshView();
        // SS: so, we don't want to add this row like they have done.
        // this puts a blank row in the database, which is very much
        // not a good idea. Instead we're going to just add a blank
        // row and only add it when they hit ok if things have changed.
        tl.addView(getEditRow(-1));
      }
    });
    ll.addView(addRuleButton);
    Button closeButton = new Button(c);
    closeButton.setText("OK");
    closeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        updateLastRowVal(); // to save any changes that haven't been
        // clicked out of
        persistRows();
        // set the colRules to null so that we have to reload if they
        // open it again suddenly, so no added but unpersisted rows
        // can linger.
        cleanForReOpen();
        dismiss();
      }
    });
    tl.setColumnStretchable(1, true);
    ll.addView(closeButton);
    setContentView(ll);
  }

  /*
   * This returns a row that is ready for editing? I'm going to set a special
   * case for now that -1 gives you a blank row and doesn't put it in the
   * database.
   */
  private TableRow getEditRow(final int index) {
    final ColColorRule rule;
    if (index != -1) {
      rule = colRules.get(index);
    } else {
      // hmm, so i think the id is just the column in the db where the tableid
      // is located, or something? not clear to me.
      rule = new ColColorRule(colName, RuleType.NO_OP, "", Color.BLACK, 
          Color.WHITE);
      // and now I think we need to add this rule to the list...
      colRules.add(rule);
    }
    TableRow row = new TableRow(c);
    // preparing delete button
    TextView deleteButton = new TextView(c);
    deleteButton.setText("X");
    deleteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //updateLastRowVal();
        if (index != -1) {
          dp.deleteRule(rule);
          // take it out of the set and the list.
          colRules.remove(index);
          // if it isn't a new row, this also means that we want to decrement
          // the original row count, so that we don't call update on a new row.
          if (index < numOriginalRules) {
            numOriginalRules--;
          }
        } else {
          // we will want the delete button to remove the current row, I guess.
          // for the logic see the note in the focus change listener.
          int indOfNewRow = colRules.size() - 1;
          // also move the last touched edit pointer to we don't try and 
          // access an item outside the list. just move to front.
          if (lastFocusedRow == colRules.size() - 1)
            lastFocusedRow = 0;
          colRules.remove(indOfNewRow);
        }
        refreshView();
      }
    });
    row.addView(deleteButton);
    // preparing the text field
    EditText input = new EditText(c);
    if (index != -1) {
      input.setText((rule.compType.getSymbol() + " " + rule.val).trim());
    } else {
      input.setText(RuleType.NO_OP.getSymbol());
    }
    input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          if (index == -1) {
            // this somewhat peculiar case is because if the index is -1,
            // we want the last focused to be the the 0-reference of the list.
            // if we have no rules when this is called, we want the last focus
            // to be 0. if we have 10 including the new one, we want it to be
            // 9.
            lastFocusedRow = colRules.size() - 1;
          } else {
            lastFocusedRow = index;
          }
          return;
        } else {
          updateLastRowVal();
        }
      }
    });
    ruleInputFields.add(input);
    row.addView(input);
    // preparing the foreground color picker button
    Button foregroundPickButton = new Button(c);
    foregroundPickButton.setText("T");
    foregroundPickButton.setBackgroundColor(rule.foreground);
    foregroundPickButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        updateLastRowVal();
        ColorPickerDialog.OnColorChangedListener ccl = new ColorPickerDialog.OnColorChangedListener() {
          @Override
          public void colorChanged(int color) {
            rule.foreground = color;
            dp.updateRule(rule);
            refreshView();
          }
        };
        ColorPickerDialog cpd = new ColorPickerDialog(c, ccl, rule.foreground);
        cpd.show();
      }
    });
    row.addView(foregroundPickButton);
    // preparing the background color picker button
    Button backgroundPickButton = new Button(c);
    backgroundPickButton.setText("  ");
    backgroundPickButton.setBackgroundColor(rule.background);
    backgroundPickButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        updateLastRowVal();
        ColorPickerDialog.OnColorChangedListener ccl = new ColorPickerDialog.OnColorChangedListener() {
          @Override
          public void colorChanged(int color) {
            rule.background = color;
            dp.updateRule(rule);
            refreshView();
          }
        };
        ColorPickerDialog cpd = new ColorPickerDialog(c, ccl, rule.background);
        cpd.show();
      }
    });
    row.addView(backgroundPickButton);
    return row;
  }

  /*
   * SS: I am intending this to go through all the rules in the colRules list,
   * which we've shown to the user and allowed to modify, and then persist them
   * to the database. This means that the old rules that have been modified we
   * have to update, and the new rules we will add. We're going to do this by
   * just going through every row. If it already exists in the original rules we
   * call update, otherwise we add the rule. There is a case where a new row is
   * added and never touched, and therefore is never enforced. Catch this.
   */
  private void persistRows() {
    for (int i = 0; i < colRules.size(); i++) {
      if (i < numOriginalRules) {
        dp.updateRule(colRules.get(i));
      } else if (colRules.get(i).compType != ColColorRule.RuleType.NO_OP) {
        dp.addRule(colRules.get(i));
      }
    }
  }

  /*
   * I think this is just the marker to see which of the rules in the list was
   * being edited?
   */
  private void updateLastRowVal() {
    if (lastFocusedRow < 0) {
      return;
    }
    EditText inputEt = ruleInputFields.get(lastFocusedRow);
    String input = inputEt.getText().toString().trim();
    //if (input.equals("")) {
    //  return;
    //}
    ColColorRule rule = colRules.get(lastFocusedRow);
    ColColorRule.RuleType newType;
    try {
      // The input should be in the format "op val". So split it and get it.
      String[] opVal = input.split(" ");
      if (opVal.length != 2)
        throw new IllegalArgumentException("not: op val");
      newType = ColColorRule.RuleType.getEnumFromString(opVal[0]);
      rule.compType = newType;
      rule.val = opVal[1];
      // we shouldn't be updating until they hit ok
      // dp.updateRule(rule);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "illegal rule type: " + input);
      AlertDialog.Builder badRule = new AlertDialog.Builder(this.c);
      badRule.setTitle("Unrecognized Rule");
      badRule.setMessage("The accepted operators are: <, <=, =, >, >=");
      badRule.show();
    }
  }

}
