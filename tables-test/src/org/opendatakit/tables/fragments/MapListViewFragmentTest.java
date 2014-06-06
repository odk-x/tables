package org.opendatakit.tables.fragments;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.tables.views.webkits.TableData;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;

@RunWith(RobolectricTestRunner.class)
public class MapListViewFragmentTest {

  MapListViewFragmentStub fragment;
  Activity activity;

  @After
  public void after() {
    MapListViewFragmentStub.resetState();
  }

  @Before
  public void setupWithDefaults() {
    ShadowLog.stream = System.out;
    TableDisplayActivityStub.BUILD_MENU_FRAGMENT = false;
    MapListViewFragmentStub stub = new MapListViewFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        TableDisplayActivityStub.class,
        stub,
        null);
    this.fragment = stub;
    this.activity = this.fragment.getActivity();
  }

  @Test
  public void startsWithNoItemSelected() {
    org.fest.assertions.api.Assertions
      .assertThat(this.fragment.itemIsSelected()).isFalse();
  }

  @Test
  public void settingIndexMakesItemSelected() {
    int selectedIndex = 5;
    this.fragment.setIndexOfSelectedItem(selectedIndex);
    org.fest.assertions.api.Assertions
      .assertThat(this.fragment.itemIsSelected()).isTrue();
  }

  @Test
  public void settingNoItemSelectedResetsState() {
    int selectedIndex = 10;
    this.fragment.setIndexOfSelectedItem(selectedIndex);
    org.fest.assertions.api.Assertions
      .assertThat(this.fragment.itemIsSelected()).isTrue();
    // Now undo it.
    this.fragment.setNoItemSelected();
    org.fest.assertions.api.Assertions
      .assertThat(this.fragment.itemIsSelected()).isFalse();
  }
  
  @Test
  public void settingIndexCallsToTableData() {
    int selectedIndex = 5;
    TableData tableDataMock = MapListViewFragmentStub.TABLE_DATA;
    verify(tableDataMock, times(0)).setSelectedMapIndex(selectedIndex);
    this.fragment.setIndexOfSelectedItem(selectedIndex);
    verify(tableDataMock, times(1)).setSelectedMapIndex(selectedIndex);
  }
  
  @Test
  public void settingNoIndexSelectedCallsToTableData() {
    TableData tableDataMock = MapListViewFragmentStub.TABLE_DATA;
    verify(tableDataMock, times(0)).setNoItemSelected();
    this.fragment.setNoItemSelected();
    verify(tableDataMock, times(1)).setNoItemSelected();
  }


//  For some reason this isn't passing, but is returning the same object.
//  WebView issues?
//  @Test
//  public void resetViewChangesTheDataObject() {
//    MapListViewFragmentStub.FILE_NAME = "testeroo";
//    Object originalDataObject = this.getShadowWebView().getJavascriptInterface(
//        Constants.JavaScriptHandles.DATA);
//    Object sameObjectAgain = this.getShadowWebView().getJavascriptInterface(
//        Constants.JavaScriptHandles.DATA);
//    org.fest.assertions.api.Assertions.assertThat(originalDataObject)
//        .isSameAs(sameObjectAgain);
//    this.fragment.resetView();
//    Object newDataObject = this.getShadowWebView().getJavascriptInterface(
//        Constants.JavaScriptHandles.DATA);
//    org.fest.assertions.api.Assertions.assertThat(originalDataObject)
//        .isNotSameAs(newDataObject);
//  }

}
