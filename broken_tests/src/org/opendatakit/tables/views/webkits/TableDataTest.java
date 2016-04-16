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

package org.opendatakit.tables.views.webkits;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TableDataTest {
  
  TableDisplayActivity activity;
  TableData tableData;
  
  @Before
  public void before() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();

    TestCaseUtils.setThreeTableDataset(false);

    TableDisplayActivity activity = ODKFragmentTestUtil.startListWebFragmentForTableDisplayActivity(
        TestConstants.DEFAULT_TABLE_ID, "junkfilename");
    
    TableData tableData = new TableData(activity, 
        activity.getUserTable());
    this.activity = activity;
    this.tableData = tableData;
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    AbsBaseActivityStub.resetState();
    this.activity = null;
    this.tableData = null;
  }
  
  @Test
  public void displayIndexDoesNotNeedToBeCalculatedAtStart() {
    assertThat(this.tableData.displayIndexMustBeCalculated()).isFalse();
  }
  
  @Test
  public void displayIndexNeedsToBeCalculatedAfterSettingIndex() {
    this.tableData.setSelectedMapIndex(10);
    assertThat(this.tableData.displayIndexMustBeCalculated()).isTrue();
  }
  
  @Test
  public void displayIndexDoesNotNeedToBeCalculatedAfterReset() {
    this.tableData.setSelectedMapIndex(10);
    this.tableData.setNoItemSelected();
    assertThat(this.tableData.displayIndexMustBeCalculated()).isFalse();
  }
  
  @Test
  public void indicesCorrectWhenNoMapMarkerSelected() {
    this.assertIndicesAreNotRemapped();
  }
  
  @Test
  public void indicesCorrectWhenMarkerSelected() {
    // This should remap things as follows--the selected item is at the top
    // of the list. Everything else is -1 to give it account for the  selected
    // item shoe-horned into the front. After that position, there is no
    // remapping.
    // displayIndex: 0  1  2  3  4  5  6  7
    // dataIndex   : 5  0  1  2  3  4  6  7
    int selectedMarker = 5;
    this.tableData.setSelectedMapIndex(selectedMarker);
    this.assertIndicesCorrectForSelectedMarkerFive();
  }
  
  @Test
  public void indicesRemappedCorrectlyAfterTwoSelections() {
    int firstSelectedMarker = 25;
    int secondSelectedMarker = 5;
    this.tableData.setSelectedMapIndex(firstSelectedMarker);
    this.tableData.setSelectedMapIndex(secondSelectedMarker);
    this.assertIndicesCorrectForSelectedMarkerFive();
  }
  
  private void assertIndicesCorrectForSelectedMarkerFive() {
    int zero = this.tableData.getIndexIntoDataTable(0);
    int one = this.tableData.getIndexIntoDataTable(1);
    int two = this.tableData.getIndexIntoDataTable(2);
    int three = this.tableData.getIndexIntoDataTable(3);
    int four = this.tableData.getIndexIntoDataTable(4);
    int five = this.tableData.getIndexIntoDataTable(5);
    int six = this.tableData.getIndexIntoDataTable(6);
    int seven = this.tableData.getIndexIntoDataTable(7);
    int ninetyNine = this.tableData.getIndexIntoDataTable(99);
    int nineHundredTwelve = this.tableData.getIndexIntoDataTable(912);
    assertThat(zero).isEqualTo(5);
    assertThat(one).isEqualTo(0);
    assertThat(two).isEqualTo(1);
    assertThat(three).isEqualTo(2);
    assertThat(four).isEqualTo(3);
    assertThat(five).isEqualTo(4);
    assertThat(six).isEqualTo(6);
    assertThat(seven).isEqualTo(7);
    assertThat(ninetyNine).isEqualTo(99);
    assertThat(nineHundredTwelve).isEqualTo(912);
  }
  
  @Test
  public void indicesNotRemappedAfterSettingNoSelectedIndex() {
    int selectedMarker = 5;
    this.tableData.setSelectedMapIndex(selectedMarker);
    this.tableData.setNoItemSelected();
    this.assertIndicesAreNotRemapped();
  }
  
  @Test
  public void indicesCorrectWhenSelectedIndexIsZero() {
    int selectedMarker = 0;
    this.tableData.setSelectedMapIndex(selectedMarker);
    this.assertIndicesAreNotRemapped();
  }
  
  @Test
  public void indicesCorrectWhenSelectedIndexIsOne() {
    int selectedMarker = 1;
    // displayIndex: 0  1  2  3
    // dataIndex   : 1  0  2  3
    this.tableData.setSelectedMapIndex(selectedMarker);
    int zero = this.tableData.getIndexIntoDataTable(0);
    int one = this.tableData.getIndexIntoDataTable(1);
    int two = this.tableData.getIndexIntoDataTable(2);
    int three = this.tableData.getIndexIntoDataTable(3);
    assertThat(zero).isEqualTo(1);
    assertThat(one).isEqualTo(0);
    assertThat(two).isEqualTo(2);
    assertThat(three).isEqualTo(3);
  }
  
  /**
   * Asserts that the requested index values are spit out unmodified.
   */
  private void assertIndicesAreNotRemapped() {
    int zero = this.tableData.getIndexIntoDataTable(0);
    int one = this.tableData.getIndexIntoDataTable(1);
    int two = this.tableData.getIndexIntoDataTable(2);
    int three = this.tableData.getIndexIntoDataTable(3);
    int four = this.tableData.getIndexIntoDataTable(4);
    int five = this.tableData.getIndexIntoDataTable(5);
    int six = this.tableData.getIndexIntoDataTable(6);
    int seven = this.tableData.getIndexIntoDataTable(7);
    int ninetyNine = this.tableData.getIndexIntoDataTable(99);
    int nineHundredTwelve = this.tableData.getIndexIntoDataTable(912);
    assertThat(zero).isEqualTo(0);
    assertThat(one).isEqualTo(1);
    assertThat(two).isEqualTo(2);
    assertThat(three).isEqualTo(3);
    assertThat(four).isEqualTo(4);
    assertThat(five).isEqualTo(5);
    assertThat(six).isEqualTo(6);
    assertThat(seven).isEqualTo(7);
    assertThat(ninetyNine).isEqualTo(99);
    assertThat(nineHundredTwelve).isEqualTo(912);
  }
  

}
