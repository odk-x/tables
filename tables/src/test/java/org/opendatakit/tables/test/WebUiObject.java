package org.opendatakit.tables.test;

import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

/**
 * Created by Marshall on 16/06/2015.
 *
 * Wraps UiObject/UiDevice for better integration with Webview selections
 */

public class WebUiObject {
    private UiObject object;
    private UiDevice device;

    private static final long SELECT_TIMEOUT = 500; // timeout for selecting a textbox WebUiObject

    private static final int ZOOM_PERCENTAGE = 60;
    private static final int ZOOM_STEPS = 200;

    private static final int BOX_X = 50; // location for WebUiObject textboxes
    private static final int BOX_Y = 190;

    public WebUiObject(UiDevice device, UiSelector selector){
        this.object = device.findObject(selector);
        this.device = device;
    }

    public boolean click() throws UiObjectNotFoundException {
        return object.click();
    }

    public boolean setText(String text) throws InterruptedException, UiObjectNotFoundException {
        Thread.sleep(SELECT_TIMEOUT);
        device.click(BOX_X, BOX_Y); // TODO: Generalize
        Thread.sleep(SELECT_TIMEOUT);
        object.clearTextField(); // TODO: Fix
        boolean ret = object.setText(text);
        returnToDefaultZoom();
        return ret;
    }

    private void returnToDefaultZoom() throws UiObjectNotFoundException {
        UiObject window = device.findObject(new UiSelector()
                .descriptionContains("OpenDataKit Common Javascript Framework"));
        window.pinchIn(ZOOM_PERCENTAGE, ZOOM_STEPS); // pinch to zoom out
    }
}
