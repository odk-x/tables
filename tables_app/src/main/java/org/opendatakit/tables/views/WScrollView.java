/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

/**
 * Modified HorizontalScrollView that communicates scroll actions to interior
 * Vertical scroll view. From:
 * http://stackoverflow.com/questions/3866499/two-directional-scroll-view
 * <p/>
 * Usage: ScrollView sv = new ScrollView(this.getContext()); WScrollView hsv =
 * new WScrollView(this.getContext()); hsv.sv = sv;
 * <p/>
 * sv.addView(new ViewOfYourChoice(getContext()), new
 * LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
 * hsv.addView(sv, new LayoutParams(LayoutParams.WRAP_CONTENT,
 * LayoutParams.MATCH_PARENT)); setContentView(hsv, new
 * LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
 */
public class WScrollView extends HorizontalScrollView {
  /**
   * The internal scrollview, set by the client code
   */
  public ScrollView sv;

  /**
   * Default one-argument constructor
   * @param context unused
   */
  public WScrollView(Context context) {
    super(context);
  }

  /**
   * Default two-argument constructor
   * @param context unused
   * @param attrs unused
   */
  public WScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Default three-argument constructor
   * @param context unused
   * @param attrs unused
   * @param defStyle unused
   */
  public WScrollView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    try {
      boolean ret = super.onTouchEvent(event);
      ret |= sv.onTouchEvent(event);
      return ret;
    } catch (IllegalArgumentException ignored) {
      // just return false
    }
    return false;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    try {
      boolean ret = super.onInterceptTouchEvent(event);
      ret |= sv.onInterceptTouchEvent(event);
      return ret;
    } catch (IllegalArgumentException ignored) {
      // just return false
    }
    return false;
  }
}