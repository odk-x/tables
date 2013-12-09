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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.SyncState;
import org.opendatakit.tables.data.ColorRule;
import org.opendatakit.tables.sync.SyncUtil;

import android.graphics.Color;

/**
 *
 * @author sudar.sam@gmail.com
 *
 */
public class ColorRuleUtil {

  public static final String ID_REST_RULE = "syncStateRest";
  public static final String ID_CONFLICTING_RULE =
      "defaultRule_syncStateConflicting";
  public static final String ID_INSERTING_RULE =
      "defaultRule_syncStateInserting";
  public static final String ID_UPDATING_RULE =
      "defaultRule_syncStateUpdating";
  public static final String ID_DELETING_RULE =
      "defaultRule_syncStateDeleting";

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

  private static final List<ColorRule> defaultSyncStateColorRules;
  private static final Set<String> defaultSyncStateColorRuleIDs;

  static {
    List<ColorRule> ruleList = new ArrayList<ColorRule>();
    ruleList.add(getColorRuleForSyncStateRest());
    ruleList.add(getColorRuleForSyncStateInserting());
    ruleList.add(getColorRuleForSyncStateUpdating());
    ruleList.add(getColorRuleForSyncStateConflict());
    ruleList.add(getColorRuleForSyncStateDeleting());
    defaultSyncStateColorRules = Collections.unmodifiableList(ruleList);
    // Now the rule ID set.
    Set<String> idSet = new HashSet<String>();
    idSet.add(ID_REST_RULE);
    idSet.add(ID_CONFLICTING_RULE);
    idSet.add(ID_DELETING_RULE);
    idSet.add(ID_INSERTING_RULE);
    idSet.add(ID_UPDATING_RULE);
    defaultSyncStateColorRuleIDs = Collections.unmodifiableSet(idSet);
  }

  public static ColorRule getColorRuleForSyncStateRest() {
    return new ColorRule(ID_REST_RULE, DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, SyncState.rest.name(),
        DEFAULT_SYNC_STATE_REST_FOREGROUND,
        DEFAULT_SYNC_STATE_REST_BACKGROUND);
  }

  public static ColorRule getColorRuleForSyncStateInserting() {
    return new ColorRule(ID_INSERTING_RULE, DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, SyncState.inserting.name(),
        DEFAULT_SYNC_STATE_INSERTING_FOREGROUND,
        DEFAULT_SYNC_STATE_INSERTING_BACKGROUND);
  }

  public static ColorRule getColorRuleForSyncStateUpdating() {
    return new ColorRule(ID_UPDATING_RULE, DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, SyncState.updating.name(),
        DEFAULT_SYNC_STATE_UPDATING_FOREGROUND,
        DEFAULT_SYNC_STATE_UPDATING_BACKGROUND);
  }

  public static ColorRule getColorRuleForSyncStateDeleting() {
    return new ColorRule(ID_DELETING_RULE, DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, SyncState.deleting.name(),
        DEFAULT_SYNC_STATE_DELETING_FOREGROUND,
        DEFAULT_SYNC_STATE_DELETING_BACKGROUND);
  }

  public static ColorRule getColorRuleForSyncStateConflict() {
    return new ColorRule(ID_CONFLICTING_RULE, DataTableColumns.SYNC_STATE,
        ColorRule.RuleType.EQUAL, SyncState.conflicting.name(),
        DEFAULT_SYNC_STATE_CONFLICT_FOREGROUND,
        DEFAULT_SYNC_STATE_CONFLICT_BACKGROUND);
  }

  /**
   * Get an unmodifiable list of the default color rules for the various sync
   * states.
   * @return
   */
  public static List<ColorRule> getDefaultSyncStateColorRules() {
    return defaultSyncStateColorRules;
  }

  /**
   * Get an unmodifiable set of the default sync state color rule ids.
   * @return
   */
  public static Set<String> getDefaultSyncStateColorRuleIds() {
    return defaultSyncStateColorRuleIDs;
  }

}
