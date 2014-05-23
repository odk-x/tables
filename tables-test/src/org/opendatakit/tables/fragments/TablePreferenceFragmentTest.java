package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.AbsTableActivityStub;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.robolectric.RobolectricTestRunner;

import android.app.Activity;
import android.preference.EditTextPreference;

@RunWith(RobolectricTestRunner.class)
public class TablePreferenceFragmentTest {
  
  TablePreferenceFragment fragment;
  Activity activity;
  
  @Before
  public void setup() {
    TablePreferenceFragmentStub stub = new TablePreferenceFragmentStub();
    this.fragment = stub;
    ODKFragmentTestUtil.startFragmentForActivity(
        AbsTableActivityStub.class,
        fragment,
        null);
  }
  
  @Test
  public void testFragmentIsNotNull() {
    assertThat(this.fragment).isNotNull();
  }
  
  @Test
  public void canFindAPreference() {
    EditTextPreference idPref = this.fragment.findEditTextPreference(
        Constants.PreferenceKeys.Table.TABLE_ID);
    assertThat(idPref).isNotNull();
  }

}
