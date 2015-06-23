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
import com.alachisoft.tayzgrid.common.stats.HPTime;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackEnumerator;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackException;
import com.alachisoft.tayzgrid.common.datastructures.RedBlack;
import java.util.Iterator;
import java.util.Map;

/**
 * Logs the operation during state transfer
 */
public class OperationLogger {

    private LogMode _loggingMode = LogMode.values()[0];
    private int _bucketId;
    private java.util.HashMap _logTbl;
    private boolean _bucketTransfered = false;
    private RedBlack _opIndex;

    public OperationLogger(int bucketId, LogMode loggingMode) {
        _bucketId = bucketId;
        _opIndex = new RedBlack();
        _loggingMode = loggingMode;
    }

    public final LogMode getLoggingMode() {
        return _loggingMode;
    }

    public final void setLoggingMode(LogMode value) {
        _loggingMode = value;
    }

    public final boolean getBucketTransfered() {
        return _bucketTransfered;
    }

    public final void setBucketTransfered(boolean value) {
        _bucketTransfered = value;
    }

    public final java.util.HashMap getLoggedEnteries() {
        java.util.HashMap updatedKeys = null;
        java.util.HashMap removedKeys = null;

        if (_logTbl == null) {
            _logTbl = new java.util.HashMap();
        }

        _logTbl.put("updated", new java.util.HashMap());
        _logTbl.put("removed", new java.util.HashMap());

        if (_logTbl.containsKey("updated")) {
            updatedKeys = (java.util.HashMap) _logTbl.get("updated");
        }

        if (_logTbl.containsKey("removed")) {
            removedKeys = (java.util.HashMap) _logTbl.get("removed");
        }
        Iterator rbe = _opIndex.GetEnumerator();
        RedBlackEnumerator KeyValue1;

        while (rbe.hasNext()) {
            KeyValue1 = (RedBlackEnumerator) rbe;
            Object Value = KeyValue1.getValue();
            java.util.HashMap tbl = (java.util.HashMap) ((Value instanceof java.util.HashMap) ? Value : null);
            OperationInfo info = null;

            if (tbl != null) {
                Iterator ide = tbl.entrySet().iterator();
                Map.Entry KeyValue2;
                while (ide.hasNext()) {
                    KeyValue2 = (Map.Entry) ide.next();
                    Object Key2 = KeyValue2.getKey();
                    info = (OperationInfo) Key2;
                    break;
                }
            }

            OperationType opType = (OperationType) info.getOpType();
            switch (opType) {

                case Add:
                    removedKeys.remove(info.getKey());
                    updatedKeys.put(info.getKey(), info.getEntry());
                    break;

                case Insert:
                    removedKeys.remove(info.getKey());
                    updatedKeys.put(info.getKey(), info.getEntry());
                    break;

                case Delete:
                    updatedKeys.remove(info.getKey());
                    removedKeys.put(info.getKey(), info.getEntry());
                    break;
            }
        }
        return _logTbl;
    }

    public final java.util.HashMap getLoggedKeys() {
        java.util.ArrayList updatedKeys = null;
        java.util.ArrayList removedKeys = null;

        if (_logTbl == null) {
            _logTbl = new java.util.HashMap();
        }

        _logTbl.put("updated", new java.util.ArrayList());
        _logTbl.put("removed", new java.util.ArrayList());

        if (_logTbl.containsKey("updated")) {
            updatedKeys = (java.util.ArrayList) _logTbl.get("updated");
        }

        if (_logTbl.containsKey("removed")) {
            removedKeys = (java.util.ArrayList) _logTbl.get("removed");
        }

        Iterator rbe = _opIndex.GetEnumerator();
        RedBlackEnumerator KeyValue1;
        while (rbe.hasNext()) {
            KeyValue1 = (RedBlackEnumerator) rbe;
            Object Value = KeyValue1.getValue();
            java.util.HashMap tbl = (java.util.HashMap) ((Value instanceof java.util.HashMap) ? Value : null);
            OperationInfo info = null;

            if (tbl != null) {
                Iterator ide = tbl.entrySet().iterator();
                Map.Entry KeyValue2;
                while (ide.hasNext()) {
                    KeyValue2 = (Map.Entry) ide.next();
                    Object Key = KeyValue2.getKey();
                    info = (OperationInfo) Key;
                    break;
                }
            }
            OperationType opType = (OperationType) info.getOpType();
            switch (opType) {

                case Add:
                    removedKeys.remove(info.getKey());
                    updatedKeys.add(info.getKey());
                    break;

                case Insert:
                    removedKeys.remove(info.getKey());
                    updatedKeys.add(info.getKey());
                    break;

                case Delete:
                    updatedKeys.remove(info.getKey());
                    removedKeys.add(info.getKey());
                    break;
            }
        }
        return _logTbl;
    }

    public final void Clear() {
        if (_opIndex != null) {
            _opIndex.Clear();
        }
    }

    public final void LogOperation(Object key, CacheEntry entry, OperationType type) {
        if (_opIndex != null) {
            try {
                _opIndex.Add(HPTime.getNow(), new OperationInfo(key, entry, type));
            } catch (RedBlackException redBlackException) {

            }
        }
    }
}
