package org.opendatakit.uiautomator;

import android.os.SystemClock;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;

/**
 * Created by Marshall on 16/06/2015.
 *
 * Wraps UiObject/UiDevice for better integration with Webview selections
 */
public class WebUiObject {
    private UiSelector selector;
    private UiDevice device;

    private static final long EXISTS_TIMEOUT = 500L;
    private static final long UPDATE_TIMEOUT = 10000L; // default wait

    private static final long SELECT_TIMEOUT = 500; // timeout for selecting a textbox WebUiObject

    private static final int ZOOM_PERCENTAGE = 60;
    private static final int ZOOM_STEPS = 200;

    private static final int BOX_X = 50; // location for WebUiObject textboxes
    private static final int BOX_Y = 190;

    public WebUiObject(UiDevice device, UiSelector selector){
        this.selector = selector;
        this.device = device;
    }

    public boolean click() throws UiObjectNotFoundException {
       int i = 1;
       for (;;) {
          try {
            device.waitForIdle(UPDATE_TIMEOUT);
            UiObject object = device.findObject(selector);
            boolean outcome = object.click();
            device.waitForIdle(UPDATE_TIMEOUT);
            SystemClock.sleep(EXISTS_TIMEOUT);
            device.waitForIdle(UPDATE_TIMEOUT);
            return outcome;
          } catch ( UiObjectNotFoundException e ) {
            if ( i > 10 ) throw e;
            device.waitForIdle(UPDATE_TIMEOUT);
            SystemClock.sleep(EXISTS_TIMEOUT);
            Log.w("WebUiObject","attempt " + ++i);
          }
       }
    }

   public boolean clickWaitForWebkitRedraw(String descriptionSnippet) throws
       UiObjectNotFoundException {
      boolean outcome = false;
      int i = 1;
      for (;;) {
         try {
            device.waitForIdle(UPDATE_TIMEOUT);
            UiObject object = device.findObject(selector);
            outcome = object.click();
            device.waitForIdle(UPDATE_TIMEOUT);
            SystemClock.sleep(EXISTS_TIMEOUT);
            device.waitForIdle(UPDATE_TIMEOUT);
            break;
         } catch ( UiObjectNotFoundException e ) {
            if ( i > 10 ) throw e;
            device.waitForIdle(UPDATE_TIMEOUT);
            SystemClock.sleep(EXISTS_TIMEOUT);
            Log.w("WebUiObject","attempt " + ++i);
         }
      }

      UiSelector match = new UiSelector().descriptionContains(descriptionSnippet);
      i = 1;
      for (;;) {
         try {
			UiObject matchobject = device.findObject(match);
            matchobject.getVisibleBounds();
            device.waitForIdle(UPDATE_TIMEOUT);
            SystemClock.sleep(EXISTS_TIMEOUT);
            break;
         } catch ( UiObjectNotFoundException e ) {
            if ( i > 10 ) throw e;
            device.waitForIdle(UPDATE_TIMEOUT);
            SystemClock.sleep(EXISTS_TIMEOUT);
            Log.w("WebUiObject","attempt " + ++i);
         }
      }
      return outcome;
   }

   public boolean clickToFocus() throws UiObjectNotFoundException {
      int i = 1;
      for (;;) {
         try {
            device.waitForIdle(UPDATE_TIMEOUT);
			UiObject object = device.findObject(selector);
            object.getVisibleBounds();
            // TODO: figure out where within the bounds of this object it is safe to click
            device.click(50, 500);
            device.waitForIdle(UPDATE_TIMEOUT);
            SystemClock.sleep(EXISTS_TIMEOUT);
            device.waitForIdle(UPDATE_TIMEOUT);
            return true;
         } catch ( UiObjectNotFoundException e ) {
            if ( i > 10 ) throw e;
            device.waitForIdle(UPDATE_TIMEOUT);
            SystemClock.sleep(EXISTS_TIMEOUT);
            Log.w("WebUiObject","attempt " + ++i);
         }
      }
   }

    public boolean setText(String text) throws UiObjectNotFoundException {
        // set focus to window (??)
        SystemClock.sleep(SELECT_TIMEOUT);
        device.waitForIdle(UPDATE_TIMEOUT);
        device.click(BOX_X, BOX_Y); // TODO: Generalize
        device.waitForIdle(UPDATE_TIMEOUT);
        UiObject object = device.findObject(selector);
        object.clearTextField(); // TODO: Fix
        SystemClock.sleep(EXISTS_TIMEOUT);
        device.waitForIdle(UPDATE_TIMEOUT);
        boolean ret = object.setText(text);
        returnToDefaultZoom();
        return ret;
    }

    private void returnToDefaultZoom() throws UiObjectNotFoundException {
        UiObject window = device.findObject(new UiSelector()
                .descriptionContains("OpenDataKit Common Javascript Framework"));
        window.waitForExists(EXISTS_TIMEOUT);
        window.pinchIn(ZOOM_PERCENTAGE, ZOOM_STEPS); // pinch to zoom out
    }
}
