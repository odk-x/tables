package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import android.widget.AbsListView;

@RunWith(RobolectricTestRunner.class)
public class TablePreferenceFragmentTest {
  
  // TODO: wait on this until Robolectric publishes my fix for preferences as
  // part of the jar
  @Test
  public void meaninglessTest() {
    assertThat((AbsListView) null).isNull();
  }
  
//  TablePreferenceFragment fragment;
//  Activity activity;
//  
//  @Before
//  public void setup() {
//    TablePreferenceFragmentStub stub = new TablePreferenceFragmentStub();
//    this.fragment = stub;
//    ODKFragmentTestUtil.startFragmentForActivity(
//        AbsTableActivityStub.class,
//        fragment,
//        null);
//  }
//  
//  @Test
//  public void testFragmentIsNotNull() {
//    assertThat(this.fragment).isNotNull();
//  }
//  
//  @Test
//  public void canFindAPreference() {
//    EditTextPreference idPref = this.fragment.findEditTextPreference(
//        Constants.PreferenceKeys.Table.TABLE_ID);
//    assertThat(idPref).isNotNull();
//  }

}
