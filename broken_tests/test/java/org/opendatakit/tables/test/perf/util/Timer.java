package org.opendatakit.tables.test.perf.util;

import android.util.Log;


public class Timer {
    
    private static final String TAG = "Timer";
    
    private long startTime;
    private long endTime;
    
    public Timer() {
        startTime = -1;
        endTime = -1;
    }
    
    public void start() {
        startTime = System.currentTimeMillis();
    }
    
    public void end() {
        endTime = System.currentTimeMillis();
    }
    
    public void print(String label) {
        long duration = endTime - startTime;
        Log.d(TAG, "<timing> " + label + ": " + duration + "ms");
    }
    
    public long getDuration() {
        return (endTime - startTime);
    }
}
