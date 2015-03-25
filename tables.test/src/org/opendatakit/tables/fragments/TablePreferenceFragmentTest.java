/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

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
