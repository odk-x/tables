package org.opendatakit.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.*;
import org.opendatakit.tables.R;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.opendatakit.util.TestConstants.*;

public class UAUtils {
  public static boolean turnOnCustomHome(UiDevice mDevice) {
    //Important!!
    //Run this before Espresso starts an activity

    try {
      if (mDevice == null) {
        throw new IllegalArgumentException("mDevice cannot be null");
      }

      startApp(mDevice, TABLES_PKG_NAME);

      //find the preference button
      UiObject2 preference = mDevice.wait(Until.findObject(By.res(Pattern.compile(
          "org.opendatakit.tables:id/menu_web_view_activity_table_manager" + "|"
              + "org.opendatakit.tables:id/menu_table_manager_preferences"))), APP_INIT_TIMEOUT);

      //from the preference's content description see if custom home screen has been enabled
      if (preference.getContentDescription().equals(getString(R.string.preferences))) {
        preference.click();
        mDevice.wait(Until.findObject(By.text(InstrumentationRegistry.getTargetContext().getString(R.string.use_index_html))),
            OBJ_WAIT_TIMEOUT).click();
      }
    } catch (Exception e) {
      return false;
    } finally {
      if (mDevice != null) {
        mDevice.pressHome();
      }
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
    Context context = InstrumentationRegistry.getContext();
    final Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    context.startActivity(intent);

    //wait for app to start
    mDevice.wait(Until.hasObject(By.pkg(pkgName).depth(0)), APP_START_TIMEOUT);
  }

  /**
   * WARNING:
   * This might not work on all versions of Android
   *
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
    return InstrumentationRegistry.getTargetContext().getString(id);
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
    PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
    ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return resolveInfo.activityInfo.packageName;
  }
}
