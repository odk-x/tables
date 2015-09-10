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

/**
 * The types of ExecutorRequest actions.
 *
 * @author mitchellsundt@gmail.com
 */
public enum ExecutorRequestType {
    UPDATE_EXECUTOR_CONTEXT,
    RAW_QUERY,
    USER_TABLE_QUERY,
    USER_TABLE_UPDATE_ROW,
    USER_TABLE_DELETE_ROW,
    USER_TABLE_ADD_ROW,
    USER_TABLE_ADD_CHECKPOINT,
    USER_TABLE_SAVE_CHECKPOINT_AS_INCOMPLETE,
    USER_TABLE_SAVE_CHECKPOINT_AS_COMPLETE,
    USER_TABLE_DELETE_LAST_CHECKPOINT,
    CLOSE_TRANSACTION
}
