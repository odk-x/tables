package org.opendatakit.tables.view.util;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.ScrollView;


public class LockableScrollView extends ScrollView {
    
    private boolean scrollable;
    
    /*
     * I'm adding stuff about onScrollStoppedListener to try and fix the 
     * fling problem when you're looking at an indexed table. Which I think 
     * means has a frozen column. You end up getting out of sync b/c the 
     * main data and the index data. Using the example at the url below I was
     * trying to get some sort of fix to this. It is currently a work in
     * progress.
     * 
     * http://stackoverflow.com/questions/8181828/android-detect-when-scrollview-stops-scrolling
     */
    private OnScrollStoppedListener onScrollStoppedListener;
    private int initialPosition;
    private Runnable scrollerTask;
    
    private int newCheck = 100;
    
    public LockableScrollView(Context context) {
        super(context);
        scrollable = true;
        
        scrollerTask = new Runnable() {
          public void run() {
            int newPosition = getScrollY();
            if (initialPosition - newPosition == 0) {
              //scroll stopped
               if (onScrollStoppedListener != null) {
                 onScrollStoppedListener.onScrollStopped();
               }
            } else {
              initialPosition = getScrollY();
              LockableScrollView.this.postDelayed(scrollerTask, newCheck);
            }
          }
        };
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
    
    public void setOnScrollStoppedListener(
        LockableScrollView.OnScrollStoppedListener listener) {
      onScrollStoppedListener = listener;
    }
    
    public void startScrollerTask() {
      initialPosition = getScrollY();
      LockableScrollView.this.postDelayed(scrollerTask, newCheck);
    }
    
    public interface OnScrollStoppedListener {
      void onScrollStopped();
    }
}
