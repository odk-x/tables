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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.math3.geometry.partitioning.BSPTreeVisitor;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.listener.DatabaseConnectionListener;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.fragments.AbsBaseFragment;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author mitchellsundt@gmail.com
 */
public class ExecutorContext implements DatabaseConnectionListener {
    private static ExecutorContext currentContext = null;

    private static void updateCurrentContext(ExecutorContext ctxt) {
        if ( currentContext != null ) {
            ctxt.queueRequest(new ExecutorRequest(currentContext));
        }
        currentContext = ctxt;
        // register for database connection status changes
        ctxt.fragment.registerDatabaseConnectionBackgroundListener(ctxt);
    }

    /**
     * The fragment containing the web view.
     * Specifically, the API we need to access.
     */
    private final ICallbackFragment fragment;
    /**
     * Our use of an executor is a bit odd:
     *
     * We need to handle database service disconnections.
     *
     * That requires direct management of the work queue.
     *
     * We still queue actions, but those actions need to pull
     * the request definitions off a work queue that is explicitly
     * managed by the ExecutorContext.
     *
     * The processors effectively record that there is (likely) work
     * to be processed. The work is held here.
     */
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final LinkedList<ExecutorRequest> workQueue = new LinkedList<ExecutorRequest>();

    private Map<String, OdkDbHandle> activeConnections = new HashMap<String, OdkDbHandle>();
    private Map<String, OrderedColumns> mCachedOrderedDefns = new HashMap<String, OrderedColumns>();

    ExecutorContext(ICallbackFragment fragment) {
        this.fragment = fragment;
        updateCurrentContext(this);
    }

    public synchronized void queueRequest(ExecutorRequest request) {
        // push the request
        workQueue.push(request);
        // signal executor that there is work
        worker.execute(new ExecutorProcessor(this));
    }

    public synchronized ExecutorRequest peekRequest() {
        if ( workQueue.isEmpty() ) {
            return null;
        } else {
            return workQueue.peekFirst();
        }
    }

    public synchronized void popRequest() {
        if ( !workQueue.isEmpty() ) {
            workQueue.removeFirst();
            // signal that we have work...
            worker.execute(new ExecutorProcessor(this));
        }
    }

    public OdkDbInterface getDatabase() {
        return fragment.getDatabase();
    }

    public String getAppName() {
        return fragment.getAppName();
    }

    public void releaseResources(String reason) {
        // TODO: rollback any transactions and close connections
        worker.shutdown();
        for ( OdkDbHandle dbh : activeConnections.values() ) {
            // close connection
        }
        while (!workQueue.isEmpty()) {
            ExecutorRequest req = workQueue.peekFirst();
            reportError(req.callbackJSON, null, "shutting down worker (" + reason + ") -- rolling back all transactions and releasing all connections");
            workQueue.pop();
        }
    }

    public void reportError(String callbackJSON, String transId, String errorMessage) {
        Map<String,Object> response = new HashMap<String,Object>();
        response.put("callbackJSON", callbackJSON);
        response.put("error", errorMessage);
        if ( transId != null ) {
            response.put("transId", transId);
        }
        String responseStr = null;
        try {
            responseStr = ODKFileUtils.mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalStateException("should never have a conversion error");
        }
        fragment.signalResponseAvailable(responseStr);
    }


    public void reportSuccess(String callbackJSON, String transId, ArrayList<List<Object>> data, Map<String,Object> metadata) {
        Map<String,Object> response = new HashMap<String,Object>();
        response.put("callbackJSON", callbackJSON);
        if ( transId != null ) {
            response.put("transId", transId);
        }
        if ( data != null ) {
            response.put("data", data);
        }
        if ( metadata != null ) {
            response.put("metadata", metadata);
        }
        String responseStr = null;
        try {
            responseStr = ODKFileUtils.mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalStateException("should never have a conversion error");
        }
        fragment.signalResponseAvailable(responseStr);
    }

    /**
     * Get the connection on which this transaction is active.
     *
     * @param transId
     * @return OdkDbHandle
     */
    public OdkDbHandle getActiveConnection(String transId) {
        return activeConnections.get(transId);
    }

    public void registerActiveConnection(String transId, OdkDbHandle dbHandle) {
        if ( activeConnections.containsKey(transId) ) {
            throw new IllegalArgumentException("transaction id already registered!");
        }
        activeConnections.put(transId, dbHandle);
    }

    public void removeActiveConnection(String transId) {
        activeConnections.remove(transId);
    }

    public OrderedColumns getOrderedColumns(String tableId) {
        return mCachedOrderedDefns.get(tableId);
    }

    public void putOrderedColumns(String tableId, OrderedColumns orderedColumns) {
        mCachedOrderedDefns.put(tableId, orderedColumns);
    }

    @Override
    public void databaseAvailable() {
        // we might have drained the queue -- or not.
        worker.execute(new ExecutorProcessor(this));
    }

    @Override
    public void databaseUnavailable() {
        new ExecutorContext(fragment);
    }
}
