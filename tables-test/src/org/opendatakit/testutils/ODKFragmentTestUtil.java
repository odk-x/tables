package org.opendatakit.testutils;

import org.opendatakit.tables.activities.AbsTableActivityStub;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;

/**
 * This simplifies various things that are required for testing fragments.
 * @author sudar.sam@gmail.com
 *
 */
public class ODKFragmentTestUtil {
  
  /**
   * Starts a fragment with on an AbsTableActivityStub. Before calling it, set
   * any defaults you need on AbsTableActivityStub. Unlike the Robolectric
   * implementation, this also calls visible() on the activity.
   * @activityClass the activity class you want the fragment added to
   * @param fragment
   * @param tag the tag with which it adds the fragment. If null, defaults to
   * {@link TestConstants#DEFAULT_FRAGMENT_TAG}
   */
  public static <T extends Activity> void startFragmentForTableActivity(
      Class<T> activityClass,
      Fragment fragment,
      String tag) {
    if (tag == null) {
      tag = TestConstants.DEFAULT_FRAGMENT_TAG;
    }
    Activity activity = 
        Robolectric.buildActivity(activityClass)
          .create()
          .start()
          .resume()
          .get();
    startFragmentHelper(activity, fragment, tag, true);
  }
  
  /**
   * Attaches the fragment to the activity.
   * @param activity activity the fragment is added to
   * @param tag tag to add the fragment
   * @param visible true if the activity should have visible called
   */
  private static void startFragmentHelper(
      Activity activity,
      Fragment fragment,
      String tag,
      boolean visible) {
    if (visible) {
      ActivityController.of(activity).visible();
    }
    FragmentManager fragmentManager = activity.getFragmentManager();
    fragmentManager.beginTransaction().add(fragment, tag).commit();
  }

}
