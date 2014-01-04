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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.tables.R;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * A utility class for common synchronization methods and definitions.
 */
public class SyncUtil {

  public static final String TAG = SyncUtil.class.getSimpleName();

  private static final String FORWARD_SLASH = "/";

  /**
   * Get the path to the file server. Should be appended to the uri of the
   * aggregate uri. Begins and ends with "/".
   * @return
   */
  public static String getFileServerPath() {
    return "/odktables/files/";
  }

  public static String getFileManifestServerPath() {
    return "/odktables/filemanifest";
  }

  /**
   * Format a file path to be pushed up to aggregate. Essentially escapes the
   * string as for an html url, but leaves forward slashes. The path must begin
   * with a forward slash, as if starting at the root directory.
   * @return a properly escaped url, with forward slashes remaining.
   */
  public static String formatPathForAggregate(String path) {
    String escaped = Uri.encode(path, "/");
    return escaped;
  }

  /**
   * Escape a list of paths for aggregate, leaving forward slashes. This
   * utility method is equivalent to calling
   * {@link SyncUtil#formatPathForAggregate(String)} on every element of the
   * list.
   * @param paths
   * @return
   */
  public static List<String> formatPathsForAggregate(List<String> paths) {
    List<String> escapedPaths = new ArrayList<String>();
    for (String path : paths) {
      escapedPaths.add(SyncUtil.formatPathForAggregate(path));
    }
    return escapedPaths;
  }

  public static boolean intToBool(int i) {
    return i != 0;
  }

  public static int boolToInt(boolean b) {
    return b ? 1 : 0;
  }

  public static boolean stringToBool(String bool) {
    return (bool == null) ? true : bool.equalsIgnoreCase("true");
  }

  public static org.opendatakit.tables.data.TableType transformServerTableType(
      org.opendatakit.aggregate.odktables.rest.entity.TableType serverType) {
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

  public static org.opendatakit.aggregate.odktables.rest.entity.TableType
  transformClientTableType(org.opendatakit.tables.data.TableType clientType) {
    org.opendatakit.aggregate.odktables.rest.entity.TableType serverType =
        org.opendatakit.aggregate.odktables.rest.entity.TableType.DATA;
    switch (clientType) {
    case data:
      serverType =
          org.opendatakit.aggregate.odktables.rest.entity.TableType.DATA;
      break;
    case shortcut:
      serverType =
          org.opendatakit.aggregate.odktables.rest.entity.TableType.SHORTCUT;
      break;
    case security:
      serverType =
          org.opendatakit.aggregate.odktables.rest.entity.TableType.SECURITY;
      break;
    default:
      Log.e(TAG, "unrecognized clientType: " + clientType);
    }
    return serverType;
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
      msg.append("Data not re-fetched from server. ");
    }

    if (result.serverHadPropertiesChanges()) {
      if (result.pulledServerProperties()) {
        msg.append("Pulled properties from server. ");
      } else {
        msg.append("Failed to pull properties from server. ");
      }
    } else {
      msg.append("Properties not re-fetched from server.");
    }

    if (result.serverHadSchemaChanges()) {
      if (result.pulledServerSchema()) {
        msg.append("Pulled schema from server. ");
      } else {
        msg.append("Failed to pull schema from server. ");
      }
    } else {
      msg.append("Schema not re-fetched from server.");
    }

    return msg.toString();
  }

  /**
   * Get a {@link RestTemplate} for synchronizing files.
   * @return
   */
  public static RestTemplate getRestTemplateForFiles() {
    // Thanks to this guy for the snippet:
    // https://github.com/barryku/SpringCloud/blob/master/BoxApp/BoxNetApp/src/com/barryku/android/boxnet/RestUtil.java
    RestTemplate rt = new RestTemplate();
    ResourceHttpMessageConverter fileConverter = new ResourceHttpMessageConverter();
    rt.getMessageConverters().add(fileConverter);
//    rt.setErrorHandler(new ResponseErrorHandler() {
//
//      @Override
//      public boolean hasError(ClientHttpResponse resp) throws IOException {
//        HttpStatus status = resp.getStatusCode();
//        if (HttpStatus.CREATED.equals(status)
//            || HttpStatus.OK.equals(status)) {
//         return false;
//        } else {
//          Log.e(TAG, "[hasError] response: " + resp.getBody());
//          return true;
//        }
//      }
//
//      @Override
//      public void handleError(ClientHttpResponse resp) throws IOException {
//        Log.e(TAG, "[handleError] response body: " + resp.getBody());
//        throw new HttpClientErrorException(resp.getStatusCode());
//      }
//    });
    return rt;
  }

  public static RestTemplate getRestTemplateForString() {
    RestTemplate rt = new RestTemplate();
    rt.getMessageConverters().add(new StringHttpMessageConverter());
    return rt;
  }


  /**
   * Compare the two {@link OdkTablesKeyValueStoreEntry} objects based on
   * their partition, aspect, and key, in that order. Must be from the same
   * table (i.e. have the same tableId) to have any meaning.
   * @author sudar.sam@gmail.com
   *
   */
  public static class KVSEntryComparator implements
      Comparator<OdkTablesKeyValueStoreEntry> {

    @Override
    public int compare(OdkTablesKeyValueStoreEntry lhs,
        OdkTablesKeyValueStoreEntry rhs) {
      int partitionComparison = lhs.partition.compareTo(rhs.partition);
      if (partitionComparison != 0) {
        return partitionComparison;
      }
      int aspectComparison = lhs.aspect.compareTo(rhs.aspect);
      if (aspectComparison != 0) {
        return aspectComparison;
      }
      // Otherwise, we'll just return the value of the key, b/c if the key
      // is also the same, we're equal.
      int keyComparison = lhs.key.compareTo(rhs.key);
      return keyComparison;
    }

  }



}
