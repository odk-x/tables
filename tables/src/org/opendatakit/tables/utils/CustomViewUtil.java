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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.type.TypeReference;
import org.opendatakit.common.android.utilities.ODKFileUtils;

public class CustomViewUtil {
  
  /**
   * A {@link TypeReference} for a {@link HashMap} parameterized for String
   * keys and String values.
   */
  private static final TypeReference<HashMap<String, String>> MAP_REF =
      new TypeReference<HashMap<String, String>>() {};
  
  /**
   * The HTML to be displayed when loading a screen.
   */
  public static final String LOADING_HTML_MESSAGE = 
      "<html><body><p>Loading, please wait...</p></body></html>";
  
  /**
   * Retrieve a map from a simple json map that has been stringified.
   *
   * @param jsonMap
   * @return null if the mapping fails, else the map
   */
  public static Map<String, String> getMapFromJson(String jsonMap) {
    Map<String, String> map = null;
    try {
      map = ODKFileUtils.mapper.readValue(jsonMap, MAP_REF);
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return map;
  }
  
}
