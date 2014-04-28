package org.opendatakit.common.android.data;

/**
 * Class for interpreting the result of a test of the rule group. When this
 * is returned you are able to distinguish via the {@link didMatch} method
 * whether or not the rule should apply.
 * @author sudar.sam@gmail.com
 *
 */
public final class ColorGuide {

  private final int mForeground;
  private final int mBackground;

  public ColorGuide(int foreground, int background) {
    this.mForeground = foreground;
    this.mBackground = background;
  }

  public final int getForeground() {
    return mForeground;
  }

  public final int getBackground() {
    return mBackground;
  }
}