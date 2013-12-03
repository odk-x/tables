package org.opendatakit.hope.views.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

/**
 * Modified HorizontalScrollView that communicates scroll actions to interior
 * Vertical scroll view. From:
 * http://stackoverflow.com/questions/3866499/two-directional-scroll-view
 *
 * Usage: ScrollView sv = new ScrollView(this.getContext()); WScrollView hsv =
 * new WScrollView(this.getContext()); hsv.sv = sv;
 *
 * sv.addView(new ViewOfYourChoice(getContext()), new
 * LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
 * hsv.addView(sv, new LayoutParams(LayoutParams.WRAP_CONTENT,
 * LayoutParams.MATCH_PARENT)); setContentView(hsv, new
 * LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
 *
 */
public class WScrollView extends HorizontalScrollView {
	public ScrollView sv;

	public WScrollView(Context context) {
		super(context);
	}

	public WScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public WScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean ret = super.onTouchEvent(event);
		ret = ret | sv.onTouchEvent(event);
		return ret;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		boolean ret = super.onInterceptTouchEvent(event);
		ret = ret | sv.onInterceptTouchEvent(event);
		return ret;
	}
}