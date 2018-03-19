package org.opendatakit.espresso;

import android.Manifest;
import android.graphics.Rect;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.view.View;
import android.widget.ScrollView;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.opendatakit.data.ColorRule;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.util.DisableAnimationsRule;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.opendatakit.util.TestConstants.APP_NAME;
import static org.opendatakit.util.TestConstants.COLUMNS_LIST;
import static org.opendatakit.util.TestConstants.TABLE_COLOR;
import static org.opendatakit.util.TestConstants.T_HOUSE_E_TABLE_ID;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ColorRuleTest extends AbsBaseTest {
  @ClassRule
  public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();

  private final String tableId = T_HOUSE_E_TABLE_ID;
  private final String elementKeyName = "House id";
  private final String elementKeyId = "House_id";

  private Boolean initSuccess = null;
  private UiDevice mDevice;
  private DbHandle db;
  private String[] adminColumns;

  // don't annotate used in chain rule
  private ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<MainActivity>(
      MainActivity.class) {
    @Override
    protected void beforeActivityLaunched() {
      super.beforeActivityLaunched();

      if (c == null) {
        try {
          new AbsBaseTest()._setUpC();
        } catch (Exception ignored) {}
      }

      if (initSuccess == null) {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        initSuccess = UAUtils.turnOnCustomHome(mDevice);
      }

      try {
        db = c.getDatabase().openDatabase(APP_NAME);
        adminColumns = TableUtil.get()
            .getTableColumns(Locale.US.getCountry(), c.getDatabase(), APP_NAME,
                db, tableId).adminColumns;
      } catch (ServicesAvailabilityException e) {
        e.printStackTrace();
      }
    }

    @Override
    protected void afterActivityFinished() {
      super.afterActivityFinished();

      try {
        c.getDatabase().closeDatabase(APP_NAME, db);
      } catch (ServicesAvailabilityException e) {
        e.printStackTrace();
      }
    }
  };

  // don't annotate used in chain rule
  private GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION
  );

  @Rule
  public TestRule chainedRules = RuleChain
      .outerRule(grantPermissionRule)
      .around(mActivityRule);

  @Before
  public void setup() {
    UAUtils.assertInitSucess(initSuccess);
    assertThat("Failed to obtain db", db, notNullValue(DbHandle.class));

    EspressoUtils.openTableManagerFromCustomHome();

    //click "Tea Houses Editable"
    onData(ODKMatchers.withTable(tableId)).perform(click());

    //go to table pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
  }

  @Test
  public void colorRule_addTableRule() {
    onData(withKey(TABLE_COLOR)).perform(click());

    List<ColorRule> currentRules = null;
    try {
      //backup + empty out current rules
      currentRules = emptyCRG(ColorRuleGroup.Type.TABLE);

      //add some rules
      List<ColorRule> newRules = new ArrayList<>();
      newRules.add(addColorRule(false, false, false));
      newRules.add(addColorRule(false, true, false));
      newRules.add(addColorRule(false, false, true));
      newRules.add(addColorRule(false, true, true));

      CRGCheck(newRules, ColorRuleGroup.Type.TABLE);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    } finally {
      if (currentRules != null) {
        try {
          ColorRuleGroup crg = getCRG(ColorRuleGroup.Type.TABLE, db, adminColumns);
          crg.replaceColorRuleList(currentRules);
          crg.saveRuleList(c.getDatabase());
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Test
  public void colorRule_deleteTableRule() {
    onData(withKey(TABLE_COLOR)).perform(click());

    List<ColorRule> currentRules = null;
    try {
      //backup + empty out current rules
      currentRules = emptyCRG(ColorRuleGroup.Type.TABLE);

      //add some rules
      List<ColorRule> newRules = new ArrayList<>();
      newRules.add(addColorRule(false, true, false));
      newRules.add(addColorRule(false, false, true));

      //delete one rule
      deleteColorRule(newRules.get(1));
      newRules.remove(1);
      CRGCheck(newRules, ColorRuleGroup.Type.TABLE);

      //delete all rules
      onView(withId(R.id.menu_color_rule_list_revert)).perform(click());
      onView(withId(android.R.id.button1)).perform(click());
      CRGCheck(new ArrayList<ColorRule>(), ColorRuleGroup.Type.TABLE);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    } finally {
      if (currentRules != null) {
        try {
          ColorRuleGroup crg = getCRG(ColorRuleGroup.Type.TABLE, db, adminColumns);
          crg.replaceColorRuleList(currentRules);
          crg.saveRuleList(c.getDatabase());
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Test
  public void colorRule_addColumnRule() {
    onData(withKey(COLUMNS_LIST)).perform(click());
    onData(is(elementKeyName)).perform(click());
    onData(withKey("column_pref_color_rules")).perform(click());

    List<ColorRule> currentRules = null;
    try {
      currentRules = emptyCRG(ColorRuleGroup.Type.COLUMN);

      List<ColorRule> newRules = new ArrayList<>();
      newRules.add(addColorRule(true, false, false));
      newRules.add(addColorRule(true, true, false));
      newRules.add(addColorRule(true, false, true));
      newRules.add(addColorRule(true, true, true));

      CRGCheck(newRules, ColorRuleGroup.Type.COLUMN);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    } finally {
      if (currentRules != null) {
        try {
          ColorRuleGroup crg = getCRG(ColorRuleGroup.Type.COLUMN, db, adminColumns);
          crg.replaceColorRuleList(currentRules);
          crg.saveRuleList(c.getDatabase());
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Test
  public void colorRule_deleteColumnRule() {
    onData(withKey(COLUMNS_LIST)).perform(click());
    onData(is(elementKeyName)).perform(click());
    onData(withKey("column_pref_color_rules")).perform(click());

    List<ColorRule> currentRules = null;
    try {
      //backup + empty out current rules
      currentRules = emptyCRG(ColorRuleGroup.Type.COLUMN);

      //add some rules
      List<ColorRule> newRules = new ArrayList<>();
      newRules.add(addColorRule(true, true, false));
      newRules.add(addColorRule(true, false, true));

      //delete one rule
      deleteColorRule(newRules.get(1));
      newRules.remove(1);
      CRGCheck(newRules, ColorRuleGroup.Type.COLUMN);

      //delete all rules
      onView(withId(R.id.menu_color_rule_list_revert)).perform(click());
      onView(withId(android.R.id.button1)).perform(click());
      CRGCheck(new ArrayList<ColorRule>(), ColorRuleGroup.Type.COLUMN);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    } finally {
      if (currentRules != null) {
        try {
          ColorRuleGroup crg = getCRG(ColorRuleGroup.Type.COLUMN, db, adminColumns);
          crg.replaceColorRuleList(currentRules);
          crg.saveRuleList(c.getDatabase());
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private int pickColor() {
    //Don't move this line
    //The Espresso method takes advantage of Espresso's ui sync
    Matcher<View> colorPickerMatcher = withClassName(endsWith("ColorPickerView"));
    onView(colorPickerMatcher).perform(click());

    //obtain color of "OK" button
    int color = EspressoUtils.getColor(colorPickerMatcher, 11, 317);

    //obtain bounds of color picker
    Rect bounds = mDevice.findObject(By.clazz(ScrollView.class)).getVisibleBounds();
    //center coordinate of "OK" button is hard coded in ColorRulesDialog.java
    // TODO update this? ColorRulesDialog.java is dead
    mDevice.click(74 + bounds.left, 336 + bounds.top);

    return color;
  }

  private List<ColorRule> emptyCRG(ColorRuleGroup.Type type) throws ServicesAvailabilityException {
    ColorRuleGroup crg = getCRG(type, db, adminColumns);

    List<ColorRule> rules = new ArrayList<>();
    for (ColorRule cr : crg.getColorRules()) {
      rules.add(
          new ColorRule(cr.getRuleId(), cr.getColumnElementKey(), cr.getOperator(), cr.getVal(),
              cr.getForeground(), cr.getBackground()));
    }

    crg.replaceColorRuleList(new ArrayList<ColorRule>());
    crg.saveRuleList(c.getDatabase());

    return rules;
  }

  private void CRGCheck(List<ColorRule> rules, ColorRuleGroup.Type type)
      throws ServicesAvailabilityException {
    //Make sure we are in color rule edit window
    onView(withId(R.id.menu_color_rule_list_new)).check(matches(isCompletelyDisplayed()));

    //must re-obtain ColorRuleGroup (a bug?)
    List<ColorRule> newRules = getCRG(type, db, adminColumns).getColorRules();

    //check the size first
    assertThat("Size mismatch", rules.size() == newRules.size(), is(true));
    onView(withId(android.R.id.list)).check(matches(ODKMatchers.withSize(rules.size())));

    //Check both database and ui for rules
    for (int i = 0; i < rules.size(); i++) {
      assertThat(rules.get(i).equalsWithoutId(newRules.get(i)), is(true));
      onData(ODKMatchers.withColorRule(rules.get(i))).check(matches(isCompletelyDisplayed()));
    }
  }

  private ColorRuleGroup getCRG(ColorRuleGroup.Type type, DbHandle db, String[] adminColumns)
      throws ServicesAvailabilityException {
    if (type == ColorRuleGroup.Type.TABLE) {
      return ColorRuleGroup
          .getTableColorRuleGroup(c.getDatabase(), APP_NAME, db, tableId,
              adminColumns);
    } else if (type == ColorRuleGroup.Type.COLUMN) {
      return ColorRuleGroup
          .getColumnColorRuleGroup(c.getDatabase(), APP_NAME, db, tableId,
              elementKeyId, adminColumns);
    } else {
      return ColorRuleGroup
          .getStatusColumnRuleGroup(c.getDatabase(), APP_NAME, db, tableId,
              adminColumns);
    }
  }

  private ColorRule addColorRule(boolean isColumn, boolean setTextColor, boolean setBgColor) {
    //Make sure we are in color rule edit window
    onView(withId(R.id.menu_color_rule_list_new)).check(matches(isCompletelyDisplayed()))
        .perform(click());

    //start with a default rule
    ColorRule rule = new ColorRule(elementKeyId, null,
        EspressoUtils.getString(mActivityRule, R.string.compared_to_value),
        Constants.DEFAULT_TEXT_COLOR, Constants.DEFAULT_BACKGROUND_COLOR);

    //this option doesn't exist for column color rules
    if (!isColumn) {
      final String elementKey = "pref_color_rule_element_key";

      onData(withKey(elementKey)).perform(click());
      onData(is(elementKeyName)).perform(click());
    }

    final String comparisonKey = "pref_color_rule_comp_type";
    onData(withKey(comparisonKey)).perform(click());
    EspressoUtils.getFirstItem().perform(click());
    rule.setOperator(
        ColorRule.RuleType.getEnumFromString(EspressoUtils.getPrefSummary(comparisonKey)));

    if (setTextColor) {
      onData(withKey("pref_color_rule_text_color")).perform(click());
      rule.setForeground(pickColor());
    }

    if (setBgColor) {
      onData(withKey("pref_color_rule_background_color")).perform(click());
      rule.setBackground(pickColor());
    }

    //save & exit
    onData(withKey("pref_color_rule_save")).perform(click());
    pressBack();

    return rule;
  }

  private void deleteColorRule(ColorRule cr) {
    //Make sure we are in color rule edit window
    onView(withId(R.id.menu_color_rule_list_new)).check(matches(isCompletelyDisplayed()));

    onData(ODKMatchers.withColorRule(cr)).perform(longClick());
    onView(withText(is(EspressoUtils.getString(mActivityRule, R.string.delete_color_rule))))
        .perform(click());
    onView(withId(android.R.id.button1)).perform(click());
  }
}
