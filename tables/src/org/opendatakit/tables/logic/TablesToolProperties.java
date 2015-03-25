/*
 * Copyright (C) 2015 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.tables.logic;

import java.util.TreeMap;

import org.opendatakit.common.android.logic.CommonToolProperties;
import org.opendatakit.common.android.logic.PropertiesSingleton;
import org.opendatakit.common.android.logic.PropertiesSingletonFactory;

import android.content.Context;

public class TablesToolProperties {

  /****************************************************************
   * CommonToolPropertiesSingletonFactory (opendatakit.properties)
   */
  
  /*******************
   * General Settings
   */
  public static final String KEY_USE_HOME_SCREEN = "tables.useHomeScreen";

  /*******************
   * Admin Settings 
   */

  /***********************************************************************
   * Secure properties (always move into appName-secure location).
   * e.g., authentication codes and passwords.
   */

  /*******************
   * General Settings
   */

  
  public static void accumulateProperties( Context context, 
      TreeMap<String,String> plainProperties, TreeMap<String,String> secureProperties) {
    
    // Set default values as necessary
    
    // the properties managed through the general settings pages.
    // no defaults for these:
    // PreferencesActivity.KEY_USERNAME;
    // PreferencesActivity.KEY_PASSWORD;
    plainProperties.put(KEY_USE_HOME_SCREEN, "false");

    // the properties that are managed through the admin settings pages.

    // handle the secure properties. If these are in the incoming property file,
    // remove them and move them into the secure properties area.
    //

    /////
  }

  private static class SurveyPropertiesSingletonFactory extends PropertiesSingletonFactory {

    private SurveyPropertiesSingletonFactory(TreeMap<String,String> generalDefaults, TreeMap<String,String> adminDefaults) {
      super(generalDefaults, adminDefaults);
    }
  }
  
  private static SurveyPropertiesSingletonFactory factory = null;
  
  public static synchronized PropertiesSingleton get(Context context, String appName) {
    if ( factory == null ) {
      TreeMap<String,String> plainProperties = new TreeMap<String,String>();
      TreeMap<String,String> secureProperties = new TreeMap<String,String>();
      
      CommonToolProperties.accumulateProperties(context, plainProperties, secureProperties);
      TablesToolProperties.accumulateProperties(context, plainProperties, secureProperties);
      
      factory = new SurveyPropertiesSingletonFactory(plainProperties, secureProperties);
    }
    return factory.getSingleton(context, appName);
  }

}
