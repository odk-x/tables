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

package org.opendatakit.tables.activities;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import android.widget.AbsListView;


/**
 *
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class TableLevelPreferencesActivityTest {

  // TODO: Robolectric doesn't currently support PreferenceFragments. Wait for
  // now.
  
  @Test
  public void meaninglessTest() {
    assertThat((AbsListView) null).isNull();
  }

//  TableLevelPreferencesActivityStub activity;
//
//  @Before
//  public void setup() {
//    // Create a mock TableProperties that will give us known results.
//    TableProperties propertiesMock = mock(TableProperties.class);
//    TableLevelPreferencesActivityStub.TABLE_PROPERTIES = propertiesMock;
//    this.activity =
//        Robolectric.buildActivity(TableLevelPreferencesActivityStub.class)
//          .create()
//          .start()
//          .resume()
//          .visible()
//          .get();
//  }
//
//  @After
//  public void after() {
//    TableLevelPreferencesActivityStub.resetState();
//  }

//  @Test
//  public void tablePreferenceFragmentNotNull() {
//    Fragment fragment = this.activity.getFragmentManager()
//        .findFragmentById(R.id.fragment_table_preference);
//    assertThat(fragment).isNotNull();
//  }

}
