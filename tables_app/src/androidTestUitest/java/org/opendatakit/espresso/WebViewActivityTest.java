package org.opendatakit.espresso;

import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;

import android.Manifest;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.web.model.Atom;
import androidx.test.espresso.web.model.ElementReference;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.UAUtils;

/**
 * Basic sample that shows the usage of Espresso web showcasing API.
 * <p/>
 * The sample has a simple layout which contains a single {@link WebView}. The HTML page displays
 * a form with an input tag and buttons to submit the form.
 */
@LargeTest
public class WebViewActivityTest {

  private Boolean initSuccess = null;
  private UiDevice mDevice;
  private ActivityScenario<MainActivity> scenario;


 @Rule
  public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION
  );

    private void beforeActivityLaunched() {
        if (initSuccess == null) {
            mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            initSuccess = UAUtils.turnOnCustomHome(mDevice);
        }
    }


  @Before
  public void setup() {

      beforeActivityLaunched();
      scenario = ActivityScenario.launch(MainActivity.class);
      onWebView().forceJavascriptEnabled();
      UAUtils.assertInitSucess(initSuccess);
  }

    @After
    public void cleanUp(){
        scenario.close();
    }

  @Test
  public void infiniteTestToReplicateSigabrt() {
    if (true)
      return;

    // Run through the Tables app an infinite number of times to get a
    // crash
    int numOfTimesToRun = 400;
    int numOfMsToSleep = 0;
    for (int i = 0; i < numOfTimesToRun; i++) {
      boolean found = false;
      Atom<ElementReference> elementFound = findElement(Locator.ID, "launch-button");
      while (!found) {
        found = true;
        try {
          Thread.sleep(numOfMsToSleep);
          onWebView()
              // Find the input element by ID
              .withElement(findElement(Locator.ID, "launch-button"))
              // Launch into teahouses
              .perform(webClick());
        } catch (RuntimeException e) {
          //e.printStackTrace();
          System.out.println("Failed to find the launch button");
          found = false;
        } catch (InterruptedException ie) {
          System.out.println("Error with thread sleep");
        }
      }

      found = false;
      while (!found) {
        found = true;
        try {
          Thread.sleep(numOfMsToSleep);
          // Find View Tea Houses button
          onWebView().withElement(findElement(Locator.ID, "view-houses"))
              // Click the button
              .perform(webClick());
        } catch (RuntimeException e) {
          //e.printStackTrace();
          System.out.println("Failed to find the View Tea Houses button");
          found = false;
        } catch (InterruptedException ie) {
          System.out.println("Error with thread sleep");
        }
      }

      // Find the li
      found = false;
      while (!found) {
        found = true;
        try {
          Thread.sleep(numOfMsToSleep);
          onWebView().withElement(findElement(Locator.ID, "t3"))
              // Simulate a click via javascript
              .perform(webClick());
        } catch (RuntimeException e) {
          //e.printStackTrace();
          System.out.println("Failed to find li");
          found = false;
        } catch (InterruptedException ie) {
          System.out.println("Error with thread sleep");
        }
      }

      elementFound = findElement(Locator.ID, "tea_button");
      found = false;
      while (!found) {
        found = true;
        try {
          Thread.sleep(numOfMsToSleep);
          // Find the response element by ID
          onWebView().withElement(findElement(Locator.ID, "tea_button"))
              // Could also be id FIELD_16
              // Verify that the response page contains the entered text
              .perform(webClick());
        } catch (RuntimeException e) {
          //e.printStackTrace();
          System.out.println("Failed to find the Teas button on detail view");
          found = false;
        } catch (InterruptedException ie) {
          System.out.println("Error with thread sleep");
        }
      }

      Espresso.pressBack();
      Espresso.pressBack();
      Espresso.pressBack();
      Espresso.pressBack();

      System.out.println("Number of iterations = " + i);
    }
  }
}
