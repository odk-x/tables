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
package org.opendatakit.tables.utils;

import org.opendatakit.tables.R;

import android.content.Context;
import android.graphics.Color;


public class LanguageUtil {

  private static final String TAG = "LanguageUtil";

    public static String getMapColorLabel(Context c, int color) {
        switch (color) {
        case Color.BLACK:
            return c.getString(R.string.black);
        case Color.BLUE:
            return c.getString(R.string.blue);
        case Color.GREEN:
            return c.getString(R.string.green);
        case Color.RED:
            return c.getString(R.string.red);
        case Color.YELLOW:
            return c.getString(R.string.yellow);
        default:
            throw new RuntimeException();
        }
    }
}
