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

import android.os.RemoteException;
import org.opendatakit.common.android.data.ColorGuide;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.RowColorObject;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.views.ExecutorContext;
import org.opendatakit.common.android.views.ExecutorProcessor;
import org.opendatakit.database.service.KeyValueStoreEntry;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.application.Tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mitchellsundt@gmail.com
 */
public class TableDataExecutorProcessor extends ExecutorProcessor {

  enum colorRuleType {
    TABLE,
    COLUMN,
    STATUS
  };

  public TableDataExecutorProcessor(ExecutorContext context) {
    super(context);
  }

  @Override
  protected void extendQueryMetadata(OdkDbHandle db, List<KeyValueStoreEntry> entries, UserTable userTable, Map<String, Object> metadata) {
    // TODO: construct color rule data here...
    String [] adminCols = ADMIN_COLUMNS.toArray(new String[0]);


    ArrayList<RowColorObject> rowColors = new ArrayList<RowColorObject>();
    ArrayList<RowColorObject> statusColors = new ArrayList<RowColorObject>();
    HashMap<String, ArrayList<RowColorObject>> colColors = new HashMap<String, ArrayList<RowColorObject>>();

    try {
      // Need to get the tables color rules and determine which rows are affected
      constructRowColorObjects(db, userTable, adminCols, rowColors, colorRuleType.TABLE, null);

      // Need to get the status color rules and determine which rows are affected
      constructRowColorObjects(db, userTable, adminCols, statusColors, colorRuleType.STATUS, null);

      // Need to get column color rules working
      Map<String, Integer> elementKeyMap = (Map<String, Integer>) metadata.get("elementKeyMap");
      for (String elementKey : elementKeyMap.keySet()) {
        ArrayList<RowColorObject> colColorGuide = new ArrayList<RowColorObject>();
        constructRowColorObjects(db, userTable, adminCols, colColorGuide, colorRuleType.COLUMN, elementKey);
        if (colColorGuide.size() > 0) {
          colColors.put(elementKey, colColorGuide);
        }
      }

    } catch (RemoteException e) {
      e.printStackTrace();
    }

    metadata.put("rowColors", rowColors);
    metadata.put("statusColors", statusColors);
    metadata.put("columnColors", colColors);

  }

  private void constructRowColorObjects(OdkDbHandle db, UserTable userTable, String[] adminCols, ArrayList<RowColorObject>colors, colorRuleType crType, String elementKey) throws RemoteException {
    // Should reuse this code for column and status color rules

    ColorRuleGroup crg = null;

    // Get the table color rules and determine which rows are affected
    if (crType == colorRuleType.TABLE) {
      crg = ColorRuleGroup.getTableColorRuleGroup(Tables.getInstance(), userTable.getAppName(), db, userTable.getTableId(), adminCols);
    } else if (crType == colorRuleType.COLUMN) {
      crg = ColorRuleGroup.getColumnColorRuleGroup(Tables.getInstance(), userTable.getAppName(), db, userTable.getTableId(), elementKey, adminCols);
    } else if (crType == colorRuleType.STATUS) {
      crg = ColorRuleGroup.getStatusColumnRuleGroup(Tables.getInstance(), userTable.getAppName(), db, userTable.getTableId(), adminCols);

    } else {
      return;
    }

    // Loop through the rows
    for (int i = 0; i < userTable.getNumberOfRows(); i++) {
      ColorGuide tcg = crg.getColorGuide(userTable.getColumnDefinitions(), userTable.getRowAtIndex(i));

      if (tcg != null) {
        String hexFgString = "#" + Integer.toHexString(0x00FFFFFF & tcg.getForeground());
        String hexBgString = "#" + Integer.toHexString(0x00FFFFFF & tcg.getBackground());
        RowColorObject rco = new RowColorObject(userTable.getRowAtIndex(i).getRowId(), i, hexFgString, hexBgString);
        colors.add(rco);
      }
    }
  }
}
