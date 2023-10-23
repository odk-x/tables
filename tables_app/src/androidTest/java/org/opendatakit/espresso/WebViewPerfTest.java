package org.opendatakit.espresso;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.test.espresso.web.webdriver.DriverAtoms;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.UAUtils;

import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.containsString;

/**
 * This test can only be used with the index from the large
 * data set app and is used for very specific purposes.
 * <p>
 * This should never be run on the build server!
 */
@LargeTest
public class WebViewPerfTest {

  // Run through the app for performance timings
  private static final int numOfTimesToRun = 101;
  private static final int numOfMsToSleep = 10;
  private static final int maxRetriesForIter = 25;
  private static final String LIMIT_TO_USE = "200";
  private static final String OFFSET_TO_USE = "0";
  private static final TEST_DB_TYPE DB_TO_USE = TEST_DB_TYPE.CUSTOM;
  // SERVICES_USED Options are true and false
  private static final TEST_TRUE_FALSE SERVICES_USED = TEST_TRUE_FALSE.TRUE;
  // ALL_IN_ONE_APK_USED Options are true and false
  private static final TEST_TRUE_FALSE ALL_IN_ONE_APK_USED = TEST_TRUE_FALSE.FALSE;

  private Boolean initSuccess = null;
  private UiDevice mDevice;

  // don't annotate used in chain rule
  private ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<MainActivity>(
      MainActivity.class, false, true) {
    @Override
    protected void beforeActivityLaunched() {
      super.beforeActivityLaunched();

      if (initSuccess == null) {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        initSuccess = UAUtils.turnOnCustomHome(mDevice);
      }
    }

    @Override
    protected void afterActivityLaunched() {
      super.afterActivityLaunched();

      onWebView().forceJavascriptEnabled();
    }
  };

  // don't annotate used in chain rule
  private GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION
  );

  @Rule
  public TestRule chainedRules = RuleChain
      .outerRule(grantPermissionRule)
      .around(mActivityRule);

  private static String getOsToUse() {
    return Build.VERSION.RELEASE;
  }

  private static String getDeviceToUse() {
    String manufacturer = Build.MANUFACTURER;
    String model = Build.MODEL;
    if (model.startsWith(manufacturer)) {
      return capitalize(model);
    }
    return capitalize(manufacturer) + " " + model;
  }

  private static String capitalize(String str) {
    if (str == null || str.length() == 0) {
      return "";
    }

    char[] arr = str.toCharArray();
    boolean capitalizeNext = true;

    //        String phrase = "";
    StringBuilder phrase = new StringBuilder();
    for (char c : arr) {
      if (capitalizeNext && Character.isLetter(c)) {
        //                phrase += Character.toUpperCase(c);
        phrase.append(Character.toUpperCase(c));
        capitalizeNext = false;
        continue;
      } else if (Character.isWhitespace(c)) {
        capitalizeNext = true;
      }
      //            phrase += c;
      phrase.append(c);
    }

    return phrase.toString();
  }

  private static TEST_DB_TYPE getDbInUse() {
    Context ctxt = null;
    try {
      ctxt = InstrumentationRegistry.getInstrumentation().getContext();
    } catch (Exception e) {
      e.printStackTrace();
      return TEST_DB_TYPE.ERROR;
    }
    if (ctxt == null) {
      return TEST_DB_TYPE.ERROR;
    }
    PackageInfo pInfo = null;
    try {
      pInfo = ctxt.getPackageManager().getPackageInfo("org.opendatakit.services", 0);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    if (pInfo == null) {
      return TEST_DB_TYPE.ERROR;
    }

    String version = pInfo.versionName;
    if (version.endsWith("builtinDB")) {
      return TEST_DB_TYPE.ANDROID;
    } else {
      return TEST_DB_TYPE.CUSTOM;
    }
  }

  @Before
  public void setup() {
    UAUtils.assertInitSucess(initSuccess);
  }

  @Test
  public void performanceTestForLargeDataSet() {
    if (true)
      return;

    // Set the limit
    onWebView().withElement(findElement(Locator.ID, "limit"))
        .perform(DriverAtoms.webKeys(LIMIT_TO_USE));

    // Set the offset
    onWebView().withElement(findElement(Locator.ID, "offset"))
        .perform(DriverAtoms.webKeys(OFFSET_TO_USE));

    // Set the os version
    onWebView().withElement(findElement(Locator.ID, "os"))
        .perform(DriverAtoms.webKeys(getOsToUse()));

    // Set the device
    onWebView().withElement(findElement(Locator.ID, "device"))
        .perform(DriverAtoms.webKeys(getDeviceToUse()));

    // Set the database
    switch (getDbInUse()) {
    case ANDROID:
      onWebView().withElement(findElement(Locator.ID, "db-android")).perform(webClick());
      break;
    case CUSTOM:
      onWebView().withElement(findElement(Locator.ID, "db-custom")).perform(webClick());
      break;
    }

    // Set if services is used
    switch (SERVICES_USED) {
    case TRUE:
      onWebView().withElement(findElement(Locator.ID, "services-true")).perform(webClick());
      break;
    case FALSE:
      onWebView().withElement(findElement(Locator.ID, "services-false")).perform(webClick());
      break;
    }

    // Set if all-in-one-apk is used
    switch (ALL_IN_ONE_APK_USED) {
    case TRUE:
      onWebView().withElement(findElement(Locator.ID, "all-in-one-true")).perform(webClick());
      break;
    case FALSE:
      onWebView().withElement(findElement(Locator.ID, "all-in-one-false")).perform(webClick());
      break;
    }

    // Click the submit button
    onWebView().withElement(findElement(Locator.ID, "submit")).perform(webClick());

    int numOfTimesNextButtonHit = 0;
    for (int i = 0; i < numOfTimesToRun; i++) {
      boolean nextButtonCntMatches = false;
      int iterRead = 0;
      while (!nextButtonCntMatches) {
        nextButtonCntMatches = true;
        try {
          String nextButtonIterStr = Integer.toString(numOfTimesNextButtonHit);

          onWebView().withElement(findElement(Locator.ID, "iter"))
              .check(webMatches(getText(), containsString(nextButtonIterStr)));

          onWebView()
              // Find the input element by ID
              .withElement(findElement(Locator.ID, "nextButton"))
              // Launch into teahouses
              .perform(webClick());
          // Increment the number of times the next button has been hit
          // We do not continue unless the test and app are in agreement
          numOfTimesNextButtonHit++;
          Thread.sleep(numOfMsToSleep);
        } catch (RuntimeException e) {
          //e.printStackTrace();
          System.out.println("Failed to find the iter element");
          nextButtonCntMatches = false;
        } catch (InterruptedException ie) {
          System.out.println("Error with thread sleep");
        } catch (junit.framework.AssertionFailedError afe) {
          nextButtonCntMatches = false;
          if (iterRead > maxRetriesForIter) {
            System.out.println("Max retry for next button hits exceeded: " + maxRetriesForIter);
            fail();
          } else {
            System.out.println("Next button hits != web view hits after retry:" + iterRead);
            iterRead++;
          }

        }
      }
      System.out.println("Number of iterations = " + i);
    }
  }

  // DB_TO_USE Options are android and custom
  enum TEST_DB_TYPE {
    ANDROID, CUSTOM, ERROR
  }

  enum TEST_TRUE_FALSE {
    TRUE, FALSE
  }
}
