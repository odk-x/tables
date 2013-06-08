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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.data.ColorRule;
import org.opendatakit.tables.sync.SyncUtil;

import android.graphics.Color;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class ColorRuleUtil {
  
  private static final int DEFAULT_SYNC_STATE_REST_FOREGROUND = Color.BLACK;
  private static final int DEFAULT_SYNC_STATE_REST_BACKGROUND = Color.WHITE;
  
  private static final int DEFAULT_SYNC_STATE_INSERTING_FOREGROUND = 
      Color.BLACK;
  private static final int DEFAULT_SYNC_STATE_INSERTING_BACKGROUND = 
      Color.GREEN;
  
  private static final int DEFAULT_SYNC_STATE_UPDATING_FOREGROUND =
      Color.BLACK;
  private static final int DEFAULT_SYNC_STATE_UPDATING_BACKGROUND = 
      Color.YELLOW;
  
  private static final int DEFAULT_SYNC_STATE_CONFLICT_FOREGROUND = 
      Color.BLACK;
  private static final int DEFAULT_SYNC_STATE_CONFLICT_BACKGROUND =
      Color.RED;
  
  private static final int DEFAULT_SYNC_STATE_DELETING_FOREGROUND =
      Color.BLACK;
  private static final int DEFAULT_SYNC_STATE_DELETING_BACKGROUND =
      Color.DKGRAY;
  
  private static List<ColorRule> defaultSyncStateColorRules;
  
  static {
    defaultSyncStateColorRules = new ArrayList<ColorRule>();
    defaultSyncStateColorRules.add(getColorRuleForSyncStateRest());
    defaultSyncStateColorRules.add(getColorRuleForSyncStateInserting());
    defaultSyncStateColorRules.add(getColorRuleForSyncStateUpdating());
    defaultSyncStateColorRules.add(getColorRuleForSyncStateConflict());
    defaultSyncStateColorRules.add(getColorRuleForSyncStateDeleting());
  }
  
  public static ColorRule getColorRuleForSyncStateRest() {
    return new ColorRule(DataTableColumns.SYNC_STATE, 
        ColorRule.RuleType.EQUAL, String.valueOf(SyncUtil.State.REST), 
        DEFAULT_SYNC_STATE_REST_FOREGROUND, 
        DEFAULT_SYNC_STATE_REST_BACKGROUND);
  }
  
  public static ColorRule getColorRuleForSyncStateInserting() {
    return new ColorRule(DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, String.valueOf(SyncUtil.State.INSERTING),
        DEFAULT_SYNC_STATE_INSERTING_FOREGROUND,
        DEFAULT_SYNC_STATE_INSERTING_BACKGROUND);
  }
  
  public static ColorRule getColorRuleForSyncStateUpdating() {
    return new ColorRule(DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, String.valueOf(SyncUtil.State.UPDATING),
        DEFAULT_SYNC_STATE_UPDATING_FOREGROUND,
        DEFAULT_SYNC_STATE_UPDATING_BACKGROUND);  
  }
  
  public static ColorRule getColorRuleForSyncStateDeleting() {
    return new ColorRule(DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, String.valueOf(SyncUtil.State.DELETING),
        DEFAULT_SYNC_STATE_DELETING_FOREGROUND,
        DEFAULT_SYNC_STATE_DELETING_BACKGROUND);
  }
  
  public static ColorRule getColorRuleForSyncStateConflict() {
    return new ColorRule(DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, String.valueOf(SyncUtil.State.CONFLICTING),
        DEFAULT_SYNC_STATE_CONFLICT_FOREGROUND,
        DEFAULT_SYNC_STATE_CONFLICT_BACKGROUND);
  }
  
  /**
   * Get a list of the default color rules for the various sync states.
   * @return
   */
  public static List<ColorRule> getDefaultSyncStateColorRules() {
    return Collections.unmodifiableList(defaultSyncStateColorRules);
  }
  
}
