/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.hope.views.components;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;


public class LockableHorizontalScrollView extends HorizontalScrollView {
    
    private boolean scrollable;
    
    public LockableHorizontalScrollView(Context context) {
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
