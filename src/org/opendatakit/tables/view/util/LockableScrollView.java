package org.opendatakit.tables.view.util;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.ScrollView;


public class LockableScrollView extends ScrollView {
    
    private boolean scrollable;
    
    public LockableScrollView(Context context) {
        super(context);
        scrollable = true;
    }
    
    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ((event.getAction() == MotionEvent.ACTION_DOWN) && !scrollable) {
            return false;
        } else {
            return super.onTouchEvent(event);
        }
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!scrollable) {
            return false;
        } else {
            return super.onInterceptTouchEvent(event);
        }
    }
}
