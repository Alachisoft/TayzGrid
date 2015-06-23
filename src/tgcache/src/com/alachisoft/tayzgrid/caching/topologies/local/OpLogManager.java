/*
 * Copyright (c) 2015, Alachisoft. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alachisoft.tayzgrid.caching.topologies.local;

import com.alachisoft.tayzgrid.caching.topologies.OperationType;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.IDisposable;

public class OpLogManager implements IDisposable {

    private java.util.HashMap _loggers = new java.util.HashMap();
    private boolean _logEnteries;
    private boolean _allowOPAfterBuckeTxfrd = true;
    private ILogger _ncacheLog;

    private ILogger getCacheLog() {
        return _ncacheLog;
    }

    public OpLogManager(boolean logEnteries, CacheRuntimeContext context) {
        _logEnteries = logEnteries;
        _ncacheLog = context.getCacheLog();

    }

    public final void StartLogging(int bucket, LogMode loggingMode) {
        if (!_loggers.containsKey(bucket)) {
            _loggers.put(bucket, new OperationLogger(bucket, loggingMode));
        } else {
            OperationLogger logger = (OperationLogger) ((_loggers.get(bucket) instanceof OperationLogger) ? _loggers.get(bucket) : null);
            logger.setLoggingMode(loggingMode);
            logger.setBucketTransfered(false);
            logger.Clear();
        }
    }

    public final boolean IsLoggingEnbaled(int bucket, LogMode logMode) {
        if (_loggers.containsKey(bucket)) {
            OperationLogger logger = (OperationLogger) ((_loggers.get(bucket) instanceof OperationLogger) ? _loggers.get(bucket) : null);
            return logger.getLoggingMode() == logMode;
        }
        return false;
    }

    public final void StopLogging(int bucket) {
        if (_loggers.containsKey(bucket)) {
            OperationLogger logger = (OperationLogger) ((_loggers.get(bucket) instanceof OperationLogger) ? _loggers.get(bucket) : null);
            logger.setBucketTransfered(_allowOPAfterBuckeTxfrd);
            ;
            logger.Clear();
        }
    }

    public final void StopLogging(java.util.ArrayList buckets) {
        if (buckets != null) {
            for (Object bucket : buckets) {
                StopLogging((Integer) bucket);
            }
        }
    }

    public final void RemoveLogger(int bucket) {
        _loggers.remove(bucket);
    }

    /**
     * Logs the operation
     *
     * @param bucket
     * @param key
     * @param entry
     * @param type
     * @param logMode
     * @return True, in case operation is logged otherwise false
     */
    public final boolean LogOperation(int bucket, Object key, CacheEntry entry, OperationType type) {
        if (_loggers.containsKey(bucket)) {
            OperationLogger logger = (OperationLogger) ((_loggers.get(bucket) instanceof OperationLogger) ? _loggers.get(bucket) : null);
            if (_logEnteries) {
                logger.LogOperation(key, entry, type);
            } else {
                logger.LogOperation(key, null, type);
            }
            return true;
        }
        return false;
    }

    /**
     * if bucket has been transfered to an another node than operation are not
     * allowed.
     *
     * @param bucket
     * @return
     */
    public final boolean IsOperationAllowed(int bucket) {
        if (_loggers.containsKey(bucket)) {
            OperationLogger logger = (OperationLogger) ((_loggers.get(bucket) instanceof OperationLogger) ? _loggers.get(bucket) : null);
            return !logger.getBucketTransfered();
        }
        return true;
    }

    public final java.util.HashMap GetLogTable(int bucket) {
        java.util.HashMap result = null;
        if (_loggers.containsKey(bucket)) {
            OperationLogger opLogger = (OperationLogger) _loggers.get(bucket);

            Object tempVar = opLogger.getLoggedKeys().clone();
            result = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
            opLogger.Clear();
        }
        return result;
    }

    public final java.util.HashMap GetLoggedEnteries(int bucket) {
        java.util.HashMap result = null;
        if (_loggers.containsKey(bucket)) {
            OperationLogger opLogger = (OperationLogger) _loggers.get(bucket);

            Object tempVar = opLogger.getLoggedEnteries().clone();
            result = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
            opLogger.Clear();
        }
        return result;
    }

    public final void dispose() {
        if (_loggers != null) {
            for (Object logObject : _loggers.values()) {
                OperationLogger logger = (OperationLogger) logObject;
                logger.Clear();
            }
            _loggers.clear();
        }
    }
}