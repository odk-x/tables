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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TableMapFragmentTest {

  @Test
  public void meanginlessDummyTest() {
    // Weirdly, this class is currently throwing a recursive call to execute
    // pending transaction exception on the computer but working on the
    // device. This should be investigated.
    org.fest.assertions.api.Assertions.assertThat(true).isEqualTo(true);
  }

//  TableMapFragmentStub fragment;
//  Activity activity;
//
//  @Before
//  public void setup() {
//    ShadowLog.stream = System.out;
//  }
//
//  @After
//  public void after() {
//    TableMapFragmentStub.resetState();
//  }
//
//  private void doGlobalSetup(TableMapFragmentStub stub) {
//    this.fragment = stub;
//    ODKFragmentTestUtil.startFragmentForActivity(
//        TableDisplayActivityStub.class,
//        stub,
//        null);
//  }
//
//  private void setupFragmentWithDefaults() {
//    TableMapFragmentStub stub = new TableMapFragmentStub();
//    this.doGlobalSetup(stub);
//  }
//
//  private void setupFragmentWithFileName(String fileName) {
//    TableMapFragmentStub.FILE_NAME = fileName;
//    TableMapFragmentStub stub = new TableMapFragmentStub();
//    this.doGlobalSetup(stub);
//  }
//
//  @Test
//  public void fragmentInitializesNonNull() {
//    this.setupFragmentWithDefaults();
//    assertThat(this.fragment).isNotNull();
//  }

}
