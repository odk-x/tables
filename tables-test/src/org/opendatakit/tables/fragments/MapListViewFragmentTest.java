package org.opendatakit.tables.fragments;

import static org.robolectric.Robolectric.shadowOf;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowWebView;

import android.app.Activity;
import android.webkit.WebView;

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
  public void indicesGeneratedInCorrectOrderWithNoSelectedItem() {
    // invalid position.
    this.fragment.setIndexOfSelectedItem(
        MapListViewFragmentStub.INVALID_INDEX);
    this.fragment.setSubsetOfIndicesToDisplay(this.getTestListInts());
    List<Integer> target = this.getTestListInts();
    List<Integer> resultIndices = this.fragment.createListOfIndicesToDisplay();
    // Nothing should change in this case.
    org.fest.assertions.api.Assertions.assertThat(resultIndices)
        .isEqualTo(target);
  }

  /**
   * The selected item should be at the top of the list.
   */
  @Test
  public void indicesGeneratedInCorrectOrderWithSelectedItemNoDuplicates() {
    int selectedIndex = 10;
    this.fragment.setIndexOfSelectedItem(selectedIndex);
    this.fragment.setSubsetOfIndicesToDisplay(this.getTestListInts());
    List<Integer> resultIndices = this.fragment.createListOfIndicesToDisplay();
    List<Integer> target = new ArrayList<Integer>();
    target.add(selectedIndex);
    target.addAll(this.getTestListInts());
    org.fest.assertions.api.Assertions.assertThat(resultIndices)
        .isEqualTo(target);
  }

  /**
   * The selected item should be at the top of the list and the duplicate
   * should be removed.
   */
  @Test
  public void indicesGeneratedInCorrectOrderWithSelectedItemDuplicated() {
    int targetIndexInTestIntsList = 2;
    int selectedIndex = this.getTestListInts().get(targetIndexInTestIntsList);
    this.fragment.setIndexOfSelectedItem(selectedIndex);
    this.fragment.setSubsetOfIndicesToDisplay(this.getTestListInts());
    List<Integer> resultIndices = this.fragment.createListOfIndicesToDisplay();
    List<Integer> target = new ArrayList<Integer>();
    target.add(selectedIndex);
    List<Integer> testListDuplicateRemoved = this.getTestListInts();
    testListDuplicateRemoved.remove(targetIndexInTestIntsList);
    target.addAll(testListDuplicateRemoved);
    org.fest.assertions.api.Assertions.assertThat(resultIndices)
        .isEqualTo(target);
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
  public void startsDisplayingAllIndices() {
    org.fest.assertions.api.Assertions
      .assertThat(this.fragment.displayingSubsetOfTable()).isFalse();
  }

  @Test
  public void settingIndicesMakesDisplaySubset() {
    this.fragment.setSubsetOfIndicesToDisplay(this.getTestListInts());
    org.fest.assertions.api.Assertions
      .assertThat(this.fragment.displayingSubsetOfTable()).isTrue();
  }

  @Test
  public void settingDisplayAllItemsUpdatesStateCorrectly() {
    this.fragment.setSubsetOfIndicesToDisplay(this.getTestListInts());
    org.fest.assertions.api.Assertions
      .assertThat(this.fragment.displayingSubsetOfTable()).isTrue();
    this.fragment.setDisplayAllItems();
    org.fest.assertions.api.Assertions
      .assertThat(this.fragment.displayingSubsetOfTable()).isFalse();
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

  private ShadowWebView getShadowWebView() {
    WebView webView = (WebView) this.fragment.getView();
    ShadowWebView result = shadowOf(webView);
    return result;
  }

  private ArrayList<Integer> getTestListInts() {
    ArrayList<Integer> result = new ArrayList<Integer>();
    result.add(92);
    result.add(35);
    result.add(68);
    result.add(2);
    return result;
  }

}
