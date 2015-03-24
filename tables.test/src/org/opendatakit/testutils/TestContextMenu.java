package org.opendatakit.testutils;

import org.robolectric.tester.android.view.TestMenu;

import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;

/**
 * This is a VERY basic implemention of ContextMenu so that we can just do
 * basic assertions about what the ContextMenu looks like when inflated. We
 * basically want to be able to say things about it when it's inflated.
 * @author sudar.sam@gmail.com
 *
 */
public class TestContextMenu extends TestMenu implements ContextMenu, Menu {

  @Override
  public void clearHeader() {
    // TODO Auto-generated method stub

  }

  @Override
  public ContextMenu setHeaderIcon(int arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ContextMenu setHeaderIcon(Drawable arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ContextMenu setHeaderTitle(int arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ContextMenu setHeaderTitle(CharSequence arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ContextMenu setHeaderView(View arg0) {
    // TODO Auto-generated method stub
    return null;
  }

}
