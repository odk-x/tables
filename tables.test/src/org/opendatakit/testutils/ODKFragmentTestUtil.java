package org.opendatakit.testutils;

import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;

/**
 * This simplifies various things that are required for testing fragments.
 * @author sudar.sam@gmail.com
 *
 */
public class ODKFragmentTestUtil {

  /**
   * Starts a fragment on the TableDisplayActivity.
   * Before calling it, set
   * any defaults you need on your activity. Unlike the Robolectric
   * implementation, this also calls visible() on the activity.
   * @activityClass the activity class you want the fragment added to
   * @param fragment
   * @param tag the tag with which it adds the fragment. If null, defaults to
   * {@link TestConstants#DEFAULT_FRAGMENT_TAG}
   */
  public static TableDisplayActivity startFragmentForTableDisplayActivity(
      ViewFragmentType type, String tableId) {
      
    Intent anIntent = new Intent();
    anIntent.putExtra(Constants.IntentKeys.APP_NAME, TestConstants.TABLES_DEFAULT_APP_NAME);
    if ( tableId != null ) {
      anIntent.putExtra(Constants.IntentKeys.TABLE_ID, tableId);
    }

    Bundle aBundle = new Bundle();
    aBundle.putString(TableDisplayActivity.INTENT_KEY_CURRENT_FRAGMENT, type.name());
  
    ActivityController<TableDisplayActivity> controller =
        Robolectric.buildActivity(TableDisplayActivity.class).withIntent(anIntent)
          .create(aBundle);
    
    controller = controller
          .start()
          .resume()
          .visible();
    
    TableDisplayActivity activity = controller
          .get();
    return activity;
  }

  public static TableDisplayActivity startGraphViewWebFragmentForTableDisplayActivity(
      String tableId, String filename) {
      
    Intent anIntent = new Intent();
    anIntent.putExtra(Constants.IntentKeys.APP_NAME, TestConstants.TABLES_DEFAULT_APP_NAME);
    if ( tableId != null ) {
      anIntent.putExtra(Constants.IntentKeys.TABLE_ID, tableId);
    }
    if ( filename != null ) {
      anIntent.putExtra(Constants.IntentKeys.FILE_NAME, filename);
    }

    Bundle aBundle = new Bundle();
    aBundle.putString(TableDisplayActivity.INTENT_KEY_CURRENT_FRAGMENT, ViewFragmentType.GRAPH_VIEW.name());
  
    ActivityController<TableDisplayActivity> controller =
        Robolectric.buildActivity(TableDisplayActivity.class).withIntent(anIntent)
          .create(aBundle);
    
    controller = controller
          .start()
          .resume()
          .visible();
    
    TableDisplayActivity activity = controller
          .get();
    return activity;
  }

  public static TableDisplayActivity startListWebFragmentForTableDisplayActivity(
      String tableId, String filename) {
      
    Intent anIntent = new Intent();
    anIntent.putExtra(Constants.IntentKeys.APP_NAME, TestConstants.TABLES_DEFAULT_APP_NAME);
    if ( tableId != null ) {
      anIntent.putExtra(Constants.IntentKeys.TABLE_ID, tableId);
    }
    if ( filename != null ) {
      anIntent.putExtra(Constants.IntentKeys.FILE_NAME, filename);
    }

    Bundle aBundle = new Bundle();
    aBundle.putString(TableDisplayActivity.INTENT_KEY_CURRENT_FRAGMENT, ViewFragmentType.LIST.name());
  
    ActivityController<TableDisplayActivity> controller =
        Robolectric.buildActivity(TableDisplayActivity.class).withIntent(anIntent)
          .create(aBundle);
    
    controller = controller
          .start()
          .resume()
          .visible();
    
    TableDisplayActivity activity = controller
          .get();
    return activity;
  }

  public static TableDisplayActivity startDetailWebFragmentForTableDisplayActivity(
      String tableId, String filename, String rowId) {
      
    Intent anIntent = new Intent();
    anIntent.putExtra(Constants.IntentKeys.APP_NAME, TestConstants.TABLES_DEFAULT_APP_NAME);
    if ( tableId != null ) {
      anIntent.putExtra(Constants.IntentKeys.TABLE_ID, tableId);
    }
    if ( filename != null ) {
      anIntent.putExtra(Constants.IntentKeys.FILE_NAME, filename);
    }
    if ( rowId != null ) {
      anIntent.putExtra(Constants.IntentKeys.ROW_ID, rowId);
    }

    Bundle aBundle = new Bundle();
    aBundle.putString(TableDisplayActivity.INTENT_KEY_CURRENT_FRAGMENT, ViewFragmentType.DETAIL.name());
  
    ActivityController<TableDisplayActivity> controller =
        Robolectric.buildActivity(TableDisplayActivity.class).withIntent(anIntent)
          .create(aBundle);
    
    controller = controller
          .start()
          .resume()
          .visible();
    
    TableDisplayActivity activity = controller
          .get();
    return activity;
  }

  public static <T extends Activity> T startActivityAttachFragment(
      Class<T> activityClass, Fragment fragment, String tableId,
      String tag) {
    if (tag == null) {
      tag = TestConstants.DEFAULT_FRAGMENT_TAG;
    }
    Intent anIntent = new Intent();
    anIntent.putExtra(Constants.IntentKeys.APP_NAME, TestConstants.TABLES_DEFAULT_APP_NAME);
    if ( tableId != null ) {
      anIntent.putExtra(Constants.IntentKeys.TABLE_ID, tableId);
    }

    ActivityController<T> controller =
        Robolectric.buildActivity(activityClass).withIntent(anIntent)
          .create()
          .start()
          .resume();
    
    T activity = controller
          .get();
    
    if ( fragment != null ) {
      startFragmentHelper(activity, fragment, tag, true);
    }

    controller.visible();
    
    return activity;
  }


  public static MainActivity startMainActivity(
      String homeScreen) {
    Intent anIntent = new Intent();
    anIntent.putExtra(Constants.IntentKeys.APP_NAME, TestConstants.TABLES_DEFAULT_APP_NAME);
    if ( homeScreen != null ) {
      anIntent.putExtra(Constants.IntentKeys.FILE_NAME, homeScreen);
    }

    ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).withIntent(anIntent)
          .create()
          .start()
          .resume()
          .visible();
    
    MainActivity activity = controller
          .get();
    
    return activity;
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
