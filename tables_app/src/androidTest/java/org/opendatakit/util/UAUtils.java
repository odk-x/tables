package org.opendatakit.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.opendatakit.tables.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.opendatakit.util.TestConstants.APP_INIT_TIMEOUT;
import static org.opendatakit.util.TestConstants.APP_START_TIMEOUT;
import static org.opendatakit.util.TestConstants.CUSTOM_HOME;
import static org.opendatakit.util.TestConstants.OBJ_WAIT_TIMEOUT;
import static org.opendatakit.util.TestConstants.TABLES_PKG_NAME;
import static org.opendatakit.util.TestConstants.TABLES_SPECIFIC_SETTINGS;

public class UAUtils {
  public static boolean turnOnCustomHome(UiDevice mDevice) {
    //Important!!
    //Run this before Espresso starts an activity

    if (mDevice == null) {
      throw new IllegalArgumentException("mDevice cannot be null");
    }

    try {
      startApp(mDevice, TABLES_PKG_NAME);

      //Wait for initialization
      String initialization = getString(R.string.configuring_app, getString(R.string.app_name));
      mDevice.wait(Until.findObject(By.text(initialization)), OBJ_WAIT_TIMEOUT);
      mDevice.wait(Until.gone(By.text(initialization)), APP_INIT_TIMEOUT);

      //When new table is added, a result dialog is displayed, dismiss it
      UiObject2 dialog = mDevice
          .wait(Until.findObject(By.text(getString(R.string.initialization_complete))),
              OBJ_WAIT_TIMEOUT);
      if (dialog != null) {
        mDevice.findObject(By.clazz(Button.class)).click();
      }

      //find the preference button
      UiObject2 preference = mDevice.wait(Until.findObject(By.res(Pattern.compile(
          "org.opendatakit.tables:id/menu_web_view_activity_table_manager" + "|"
              + "org.opendatakit.tables:id/menu_table_manager_preferences"))), OBJ_WAIT_TIMEOUT);

      //from the preference's content description see if custom home screen has been enabled
      if (preference.getContentDescription().equals(getString(R.string.preferences))) {
        preference.click();
        mDevice.wait(Until.findObject(By.text(TABLES_SPECIFIC_SETTINGS)), OBJ_WAIT_TIMEOUT).click();
        mDevice.wait(Until.findObject(By.text(CUSTOM_HOME)), OBJ_WAIT_TIMEOUT).click();
      }
    } catch (Exception e) {
      return false;
    } finally {
      mDevice.pressHome();
    }

    return true;
  }

  public static String getCheckedItemText(UiDevice mDevice, BySelector group) {
    assertThat("mDevice cannot be null", mDevice != null, is(true));
    assertThat("group cannot be null", group != null, is(true));

    return mDevice.findObject(group.checked(true)).getText();
  }

  public static void startApp(UiDevice mDevice, String pkgName) {
    mDevice.pressHome(); //Start from home screen

    //wait for package launcher
    mDevice.wait(Until.hasObject(By.pkg(getLauncherPackageName()).depth(0)), APP_START_TIMEOUT);

    //start app
    Context context = InstrumentationRegistry.getInstrumentation().getContext();
    final Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    context.startActivity(intent);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      for (int i = 0; i < 100; i++) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    //wait for app to start
    mDevice.wait(Until.hasObject(By.pkg(pkgName).depth(0)), APP_START_TIMEOUT);
  }

  /**
   * WARNING:
   * This might not work on all versions of Android
   * <p>
   * appName is name of app displayed on device, for example "ODK Tables"
   */
  public static void closeApp(UiDevice mDevice, String appName, String pkgName, int method)
      throws RemoteException, IOException {
    if (method == 1) {
      mDevice.pressRecentApps();
      //swipe to kill app
      mDevice.wait(Until.findObject(By.text(appName)), OBJ_WAIT_TIMEOUT)
          .swipe(Direction.RIGHT, 1.0f);
      mDevice.pressHome();
    } else {
      mDevice.executeShellCommand("am force-stop " + pkgName);
    }
  }

  public static String getString(int id) {
    return ApplicationProvider.getApplicationContext().getString(id);
  }

  public static String getString(int id, Object... formatArgs) {
    return ApplicationProvider.getApplicationContext().getString(id, formatArgs);
  }

  /**
   * Note: this doesn't work with low x and y values
   *
   * @param mDevice
   * @param x
   * @param y
   * @return
   */
  public static boolean longPress(UiDevice mDevice, int x, int y) {
    return mDevice.swipe(x, y, x, y, 200);
  }

  public static void assertInitSucess(boolean initStatus) {
    ViewMatchers.assertThat("Initialization unsuccessful.", initStatus, is(true));
  }

  public static void clickSpreadsheetRow(UiDevice mDevice, int row) {
    Point pt = getSpreadsheetRow(mDevice, row);
    mDevice.click(pt.x, pt.y);
  }

  public static void longPressSpreadsheetRow(UiDevice mDevice, int row) {
    Point pt = getSpreadsheetRow(mDevice, row);
    longPress(mDevice, pt.x, pt.y);
  }

  /**
   * Uses package manager to find the package name of the device launcher. Usually this package
   * is "com.android.launcher" but can be different at times. This is a generic solution which
   * works on all platforms.
   */
  private static String getLauncherPackageName() {
    // Create launcher Intent
    final Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_HOME);

    // Use PackageManager to get the launcher package name
    PackageManager pm = InstrumentationRegistry.getInstrumentation().getContext().getPackageManager();
    ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return resolveInfo.activityInfo.packageName;
  }

  private static Point getSpreadsheetRow(UiDevice mDevice, int row) {
    //Find all views that can potentially be a tabular view
    UiObject2 contentView = mDevice
        .wait(Until.findObject(By.res("android:id/content")), OBJ_WAIT_TIMEOUT);
    List<UiObject2> tabularViews = contentView.findObjects(By.clazz(View.class));

    Map<UiObject2, Rect> bounds = new HashMap<>();
    //Find the relevant tabular views using their area
    UiObject2 minArea = null; //upper left corner
    UiObject2 maxArea = null; //main tabular view
    for (int i = 0; i < tabularViews.size(); i++) {
      UiObject2 view = tabularViews.get(i);

      //Tabular view is a leaf node
      if (view.getChildCount() == 0) {
        bounds.put(view, view.getVisibleBounds());

        if (maxArea == null || getArea(bounds.get(view)) > getArea(bounds.get(maxArea))) {
          maxArea = view;
        }

        if (minArea == null || getArea(bounds.get(view)) < getArea(bounds.get(minArea))) {
          minArea = view;
        }
      }
    }

    if (bounds.size() != 4) {
      //there should be 4 TabularView
      throw new IllegalStateException("# of TabularView found: " + bounds.size());
    }

    int rowHeight = bounds.get(minArea).height();

    return new Point(bounds.get(maxArea).centerX(),
        rowHeight * row - rowHeight / 2 + bounds.get(maxArea).top);
  }

  private static int getArea(Rect rect) {
    return rect.width() * rect.height();
  }
}
