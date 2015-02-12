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
