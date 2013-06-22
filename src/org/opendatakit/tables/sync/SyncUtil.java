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
package org.opendatakit.tables.sync;

import org.opendatakit.aggregate.odktables.entity.Column;
import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColumnType;

import android.content.Context;
import android.util.Log;

/**
 * A utility class for common synchronization methods and definitions.
 */
public class SyncUtil {
  
  public static final String TAG = SyncUtil.class.getSimpleName();

  /**
   * <p>
   * Synchronization state.
   * </p>
   * <p>
   * Here is a brief overview of the rules for transitions between states on
   * basic write operations:
   * 
   * <pre>
   * insert: 
   *     state = INSERTING
   *   
   * update:
   *     if state == REST:
   *        state = UPDATING
   *     
   * delete:
   *     if state == REST or state == UPDATING:
   *        state = DELETING
   *        don't actually delete yet
   *     else if state == INSERTING:
   *        actually delete
   * </pre>
   * 
   * </p>
   * <p>
   * The {@link SyncProcessor} handles moving resources from the INSERTING,
   * UPDATING, or DELETING states back to the REST state. CONFLICTING is a
   * special state set by the SyncProcessor to signify conflicts between local
   * and remote updates to the same resource and is handled separately from the
   * basic write operations.
   * 
   */
  public class State {
    public static final int REST = 0;
    public static final int INSERTING = 1;
    public static final int UPDATING = 2;
    public static final int DELETING = 3;
    public static final int CONFLICTING = 4;

    private State() {
    }
  }

  public static boolean intToBool(int i) {
    return i != 0;
  }

  public static int boolToInt(boolean b) {
    return b ? 1 : 0;
  }
  
  public static boolean stringToBool(String bool) {
    return bool.equalsIgnoreCase("true");
  }
  
  public static org.opendatakit.tables.data.TableType transformServerTableType(
      org.opendatakit.aggregate.odktables.entity.api.TableType serverType) {
    org.opendatakit.tables.data.TableType phoneType = 
        org.opendatakit.tables.data.TableType.data;
    switch (serverType) {
    case DATA:
      phoneType = org.opendatakit.tables.data.TableType.data;
      break;
    case SHORTCUT:
      phoneType = org.opendatakit.tables.data.TableType.shortcut;
      break;
    case SECURITY:
      phoneType = org.opendatakit.tables.data.TableType.security;
      break;
    default:
      Log.e(TAG, "unrecognized serverType: " + serverType);
    }
    return phoneType;
  }
  
  public static org.opendatakit.aggregate.odktables.entity.api.TableType 
  transformClientTableType(org.opendatakit.tables.data.TableType clientType) {
    org.opendatakit.aggregate.odktables.entity.api.TableType serverType = 
        org.opendatakit.aggregate.odktables.entity.api.TableType.DATA;
    switch (clientType) {
    case data:
      serverType = 
        org.opendatakit.aggregate.odktables.entity.api.TableType.DATA;
      break;
    case shortcut:
      serverType = 
        org.opendatakit.aggregate.odktables.entity.api.TableType.SHORTCUT;
      break;
    case security:
      serverType = 
        org.opendatakit.aggregate.odktables.entity.api.TableType.SECURITY;
      break;
    default:
      Log.e(TAG, "unrecognized clientType: " + clientType);
    }
    return serverType;
  }
  
  /**
   * This should eventually map the the column type
   * on the server to the phone-side column type. It currently just returns
   * type none, losing any sort of type information from the server. 
   * TODO: make this method work once it's been updated from the server.
   * @param strColumn
   * @return
   */
  public static ColumnType getTablesColumnTypeFromServerColumnType(
      Column.ColumnType serverColumnType) {
    // TODO: Sort out the way column types are going to go back and forth b/w
    // the server and the device.
    return ColumnType.NONE;
  }
  
  /**
   * Gets the name of the {@link TableResult#Status}.
   * @param context
   * @param status
   * @return
   */
  public static String getLocalizedNameForTableResultStatus(Context context,
      TableResult.Status status) {
    String name;
    switch (status) {
    case EXCEPTION:
      name = context.getString(R.string.sync_table_result_exception);
      break;
    case FAILURE:
      name = context.getString(R.string.sync_table_result_failure);
      break;
    case SUCCESS:
      name = context.getString(R.string.sync_table_result_success);
      break;
    default:
      Log.e(TAG, "unrecognized TableResult status: " + status + ". Setting " +
      		"to failure.");
      name = context.getString(R.string.sync_table_result_failure);
    }
    return name;
  }
  
  /**
   * Get a string to display to the user based on the {@link TableResult} after
   * a sync. Handles the logic for generating an appropriate message.
   * <p>
   * Presents something along the lines of:
   * Your Table: Insert on the server--Success.
   * Your Table: Pulled data from server. Failed to push properties. Etc.
   * @param context
   * @param result
   * @return
   */
  public static String getMessageForTableResult(Context context, 
      TableResult result) {
    StringBuilder msg = new StringBuilder(); 
    msg.append(result.getTableDisplayName() + ": ");
    switch (result.getTableAction()) {
    case inserting:
      msg.append(
          context.getString(R.string.sync_action_message_insert) + "--");
      break;
    case deleting:
      msg.append(
          context.getString(R.string.sync_action_message_delete) + "--");
      break;
    }
    // Now add the result of the status.
    msg.append(getLocalizedNameForTableResultStatus(context, 
        result.getStatus()));
    if (result.getStatus() == TableResult.Status.EXCEPTION) {
      // We'll append the message as well.
      msg.append(result.getMessage());
    }
    msg.append("--");
    // Now we need to add some information about the individual actions that 
    // should have been performed.
    if (result.hadLocalDataChanges()) {
      if (result.pushedLocalData()) {
        msg.append("Pushed local data. ");
      } else {
        msg.append("Failed to push local data. ");
      }
    } else {
      msg.append("No local data changes. ");
    }
    
    if (result.hadLocalPropertiesChanges()) {
      if (result.pushedLocalProperties()) {
        msg.append("Pushed local properties. ");
      } else {
        msg.append("Failed to push local properties. ");
      }
    } else {
      msg.append("No local properties changes. ");
    }
    
    if (result.serverHadDataChanges()) {
      if (result.pulledServerData()) {
        msg.append("Pulled data from server. ");
      } else {
        msg.append("Failed to pull data from server. ");
      }
    } else {
      msg.append("No data to pull from server. ");
    }
    
    if (result.serverHadPropertiesChanges()) {
      if (result.pulledServerProperties()) {
        msg.append("Pulled properties from server. ");
      } else {
        msg.append("Failed to pull properties from server. ");
      }
    } else {
      msg.append("No properties to pull from server.");
    }
    
    return msg.toString();
  }
  
  
  
}
