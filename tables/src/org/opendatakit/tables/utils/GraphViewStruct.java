/*
 * Copyright (C) 2012-2014 University of Washington
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

/**
 * Holds basic information about a saved graph view.
 * @author sudar.sam@gmail.com
 *
 */
public class GraphViewStruct {
  
  public String graphName;
  public String graphType;
  public boolean isDefault;
  
  public GraphViewStruct(
      String graphName,
      String graphType,
      boolean isDefault) {
    this.graphName = graphName;
    this.graphType = graphType;
    this.isDefault = isDefault;
  }
}
