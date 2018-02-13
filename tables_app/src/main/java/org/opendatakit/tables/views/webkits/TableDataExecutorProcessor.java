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

package org.opendatakit.tables.views.webkits;

import android.content.Context;
import android.widget.Toast;
import org.opendatakit.data.ColorGuide;
import org.opendatakit.data.ColorGuideGroup;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.RowColorObject;
import org.opendatakit.database.data.KeyValueStoreEntry;
import org.opendatakit.database.data.UserTable;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.IOdkTablesActivity;
import org.opendatakit.views.ExecutorContext;
import org.opendatakit.views.ExecutorProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mitchellsundt@gmail.com
 */
public class TableDataExecutorProcessor extends ExecutorProcessor {

  private static final String ROW_COLORS = "rowColors";
  private static final String STATUS_COLORS = "statusColors";
  private static final String COLUMN_COLORS = "columnColors";
  private static final String MAP_INDEX = "mapIndex";
  private IOdkTablesActivity mActivity;

  /**
   * Constructs a TableExecutorProcessor with the tables object given
   *
   * @param context  unused
   * @param activity saved for later so we can store the index of the selected map item in the
   *                 metadata
   */
  public TableDataExecutorProcessor(ExecutorContext context, IOdkTablesActivity activity) {
    super(context);
    mActivity = activity;
  }

  private static void constructRowColorObjects(UserDbInterface dbInterface, DbHandle db,
      UserTable userTable, String[] adminCols, Collection<RowColorObject> colors, Object crType,
      String elementKey) throws ServicesAvailabilityException {
    // Should reuse this code for column and status color rules

    ColorRuleGroup crg;

    // Get the table color rules and determine which rows are affected
    if (crType == ColorRuleType.TABLE) {
      crg = ColorRuleGroup
          .getTableColorRuleGroup(dbInterface, userTable.getAppName(), db, userTable.getTableId(),
              adminCols);
    } else if (crType == ColorRuleType.COLUMN) {
      crg = ColorRuleGroup
          .getColumnColorRuleGroup(dbInterface, userTable.getAppName(), db, userTable.getTableId(),
              elementKey, adminCols);
    } else if (crType == ColorRuleType.STATUS) {
      crg = ColorRuleGroup
          .getStatusColumnRuleGroup(dbInterface, userTable.getAppName(), db, userTable.getTableId(),
              adminCols);

    } else {
      return;
    }

    ColorGuideGroup cgg = new ColorGuideGroup(crg, userTable);

    // Loop through the rows
    for (int i = 0; i < userTable.getNumberOfRows(); i++) {
      ColorGuide tcg = cgg.getColorGuideForRowIndex(i);

      if (tcg != null) {
        //String hexFgString = "#" + Integer.toHexString(0x00FFFFFF & tcg.getForeground());
        //noinspection MagicNumber NOTE THAT NUMBER IS ONLY 3 BYTES, NOT 4!
        String hexFgString = String.format("#%06X", 0xFFFFFF & tcg.getForeground());
        //String hexBgString = "#" + Integer.toHexString(0x00FFFFFF & tcg.getBackground());
        //noinspection MagicNumber
        String hexBgString = String.format("#%06X", 0xFFFFFF & tcg.getBackground());
        RowColorObject rco = new RowColorObject(userTable.getRowId(i), i, hexFgString, hexBgString);
        colors.add(rco);
      }
    }
  }

  @Override
  protected void extendQueryMetadata(UserDbInterface dbInterface, DbHandle db,
      List<KeyValueStoreEntry> entries, UserTable userTable, Map<String, Object> metadata) {
    // TODO: construct color rule data here...
    String[] adminCols = ADMIN_COLUMNS.toArray(new String[ADMIN_COLUMNS.size()]);

    Collection<RowColorObject> rowColors = new ArrayList<>();
    Collection<RowColorObject> statusColors = new ArrayList<>();
    Map<String, ArrayList<RowColorObject>> colColors = new HashMap<>();

    try {
      // Need to get the tables color rules and determine which rows are affected
      constructRowColorObjects(dbInterface, db, userTable, adminCols, rowColors,
          ColorRuleType.TABLE, null);

      // Need to get the status color rules and determine which rows are affected
      constructRowColorObjects(dbInterface, db, userTable, adminCols, statusColors,
          ColorRuleType.STATUS, null);

      // Need to get column color rules working
      Object ekm = metadata.get("elementKeyMap");
      if (ekm == null || !(ekm instanceof Map)) {
        throw new IllegalStateException("this should be a Map<String,Integer>");
      }
      // from the calling code path, the Map is always a Map<String,Integer>.
      @SuppressWarnings("unchecked")
      Map<String, Integer> elementKeyMap = (Map<String, Integer>) ekm;
      for (String elementKey : elementKeyMap.keySet()) {
        ArrayList<RowColorObject> colColorGuide = new ArrayList<>();
        constructRowColorObjects(dbInterface, db, userTable, adminCols, colColorGuide,
            ColorRuleType.COLUMN, elementKey);
        if (!colColorGuide.isEmpty()) {
          colColors.put(elementKey, colColorGuide);
        }
      }

    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(mActivity.getAppName()).printStackTrace(e);
      if (mActivity instanceof Context) {
        String text = ((Context) mActivity).getString(R.string.database_unavailable);
        Toast.makeText((Context) mActivity, text, Toast.LENGTH_LONG).show();
      }
    }

    metadata.put(ROW_COLORS, rowColors);
    metadata.put(STATUS_COLORS, statusColors);
    metadata.put(COLUMN_COLORS, colColors);

    if (mActivity != null) {
      Integer indexOfSelectedItem = mActivity.getIndexOfSelectedItem();
      if (indexOfSelectedItem != null) {
        metadata.put(MAP_INDEX, indexOfSelectedItem);
      }
    }
  }

  /**
   * Not to be confused with ColorRule.Type or ColorRuleGroup.Type
   */
  private enum ColorRuleType {
    TABLE, COLUMN, STATUS
  }
}
