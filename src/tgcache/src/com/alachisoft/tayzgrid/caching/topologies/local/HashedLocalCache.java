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

import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.OperationType;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.statistics.BucketStatistics;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HashedLocalCache extends IndexedLocalCache {

    private int _bucketSize;
    /**
     * A map that contains key lists against each bucket id.
     */
    private java.util.HashMap _keyList;
    private int _stopLoggingThreshhold = 50;
    private OpLogManager _logMgr;

    public HashedLocalCache(java.util.Map cacheClasses, CacheBase parentCache, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context, boolean logEntries) throws ConfigurationException {
        super(cacheClasses, parentCache, properties, listener, context);
        _logMgr = new OpLogManager(logEntries, context);
        _stats.setLocalBuckets(new java.util.HashMap());
    }

    @Override
    public void setBucketSize(int value) {
        _bucketSize = value;
    }

    @Override
    public java.util.HashMap getLocalBuckets() {
        return _stats.getLocalBuckets();
    }

    @Override
    public void setLocalBuckets(java.util.HashMap value) {
        _stats.setLocalBuckets(value);
    }

    @Override
    public java.util.ArrayList GetKeyList(int bucketId, boolean startLogging) {
        if (startLogging) {
            _logMgr.StartLogging(bucketId, LogMode.LogBeforeAfterActualOperation);
        }

        if (_keyList != null) {
            if (_keyList.containsKey(bucketId)) {
                java.util.HashMap keyTbl = (java.util.HashMap) ((_keyList.get(bucketId) instanceof java.util.HashMap) ? _keyList.get(bucketId) : null);
                return new java.util.ArrayList(keyTbl.keySet());
                //return _keyList[bucketId] as ArrayList;
            }
            return null;
        }
        return new ArrayList();
    }

    /**
     * Starts logging on the specified bucket. All operation are logged untill
     * we explicitly stop logging. Logging operations helps synchronize the
     * state of the cluste during the state transfer of a bucket.
     *
     * @param bucketId
     */
    @Override
    public void StartLogging(int bucketId) {
        if (_logMgr != null) {
            _logMgr.StartLogging(bucketId, LogMode.LogBeforeActualOperation);
        }
    }

    @Override
    public java.util.HashMap GetLogTable(java.util.ArrayList bucketIds, tangible.RefObject<Boolean> isLoggingStopped) {
        java.util.HashMap result = null;
        int logCount = 0;
        java.util.Iterator ie = bucketIds.iterator();
        while (ie.hasNext()) {
            java.util.HashMap tmp = _logMgr.GetLogTable((Integer) ie.next());
            if (tmp != null) {
                if (result == null) {
                    result = tmp;
                } else {
                    java.util.ArrayList removed = (java.util.ArrayList) ((tmp.get("removed") instanceof java.util.ArrayList) ? tmp.get("removed") : null);
                    java.util.ArrayList updated = (java.util.ArrayList) ((tmp.get("updated") instanceof java.util.ArrayList) ? tmp.get("updated") : null);

                    if (removed != null) {
                        ((java.util.ArrayList) result.get("removed")).addAll(removed);
                        logCount += removed.size();
                    }

                    if (updated != null) {
                        ((java.util.ArrayList) result.get("updated")).addAll(updated);
                        logCount += updated.size();
                    }
                }
            }
        }
        if (logCount < _stopLoggingThreshhold) {
            isLoggingStopped.argvalue = true;
            _logMgr.StopLogging(bucketIds);
        } else {
            isLoggingStopped.argvalue = false;
        }

        return result;
    }

    private void IncrementBucketStats(Object key, int bucketId, long dataSize) {
        if (_stats.getLocalBuckets() != null && _stats.getLocalBuckets().containsKey(bucketId)) {
            ((BucketStatistics) _stats.getLocalBuckets().get(bucketId)).Increment(dataSize);
        }

        if (_keyList == null) {
            _keyList = new java.util.HashMap();
        }

        if(_keyList != null) {
            if(_keyList.containsKey(bucketId)){
                HashMap keys = (HashMap)_keyList.get(bucketId);
                keys. put(key, null);
            }
            else{
                HashMap keys = new HashMap();
                keys.put(key, null);
                _keyList.put(bucketId, keys);                
            }
                
        }
    }

    private void DecrementBucketStats(Object key, int bucketId, long dataSize) {
        if (_stats.getLocalBuckets() != null && _stats.getLocalBuckets().containsKey(bucketId)) {
            ((BucketStatistics) _stats.getLocalBuckets().get(bucketId)).Decrement(dataSize);
        }

        if (_keyList != null) {
            if (_keyList.containsKey(bucketId)) {
                java.util.HashMap keys = (java.util.HashMap) _keyList.get(bucketId);
                keys.remove(key);

                if (keys.isEmpty()) {
                    _keyList.remove(bucketId);
                }
            }
        }
    }

    private int GetBucketId(Object key) {
        int hashCode = AppUtil.hashCode(key);
        
        //+Numan Hanif: Fix for 7419
        //int bucketId = hashCode / _bucketSize;
        int bucketId = hashCode % AppUtil.TotalBuckets;
        //+Numan Hanif


        if (bucketId < 0) {
            bucketId *= -1;
        }
        return bucketId;
    }

    @Override
    public void RemoveFromLogTbl(int bucketId) {
        if (_logMgr != null) {
            _logMgr.RemoveLogger(bucketId);
        }
    }

    @Override
    public void AddLoggedData(java.util.ArrayList bucketIds) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (bucketIds != null) {
            java.util.Iterator ie = bucketIds.iterator();
            while (ie.hasNext()) {
                Object next = ie.next();
                if (_logMgr != null) {
                    java.util.HashMap loggedEnteries = _logMgr.GetLoggedEnteries((Integer) next);
                    if (loggedEnteries != null && loggedEnteries.size() > 0) {
                        java.util.HashMap removed = (java.util.HashMap) ((loggedEnteries.get("removed") instanceof java.util.HashMap) ? loggedEnteries.get("removed") : null);
                        java.util.HashMap updated = (java.util.HashMap) ((loggedEnteries.get("updated") instanceof java.util.HashMap) ? loggedEnteries.get("updated") : null);

                        if (removed != null && removed.size() > 0) {
                            Iterator ide = removed.entrySet().iterator();
                            Map.Entry KeyValue;
                            while (ide.hasNext()) {
                                KeyValue = (Map.Entry) ide.next();
                                Object Key = KeyValue.getKey();
                                Object Value = KeyValue.getValue();
                                Remove(Key, ItemRemoveReason.Removed, false, null, 0, LockAccessType.IGNORE_LOCK, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                            }
                        }

                        if (updated != null && updated.size() > 0) {
                            Iterator ide = updated.entrySet().iterator();
                            Map.Entry KeyValue;
                            while (ide.hasNext()) {
                                KeyValue = (Map.Entry) ide.next();
                                Object Key = KeyValue.getKey();
                                Object Value = KeyValue.getValue();
                                CacheEntry entry = (CacheEntry) ((Value instanceof CacheEntry) ? Value : null);
                                if (entry != null) {
                                    Add(Key, entry, false, false, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                                }
                            }
                        }
                    }
                    //disable logging for this bucket...
                    _logMgr.RemoveLogger((Integer) next);
                }
            }
        }
    }

    @Override
    public void RemoveBucketData(int bucketId) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.ArrayList keys = GetKeyList(bucketId, false);
        if (keys != null) {
            Object tempVar = keys.clone();
            keys = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
            java.util.Iterator ie = keys.iterator();
            while (ie.hasNext()) {
                Object obj = ie.next();
                if (obj != null) {
                    Remove(obj, ItemRemoveReason.Removed, false, false, null, 0, LockAccessType.IGNORE_LOCK, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                }
            }
            _context.PerfStatsColl.incrementStateTxfrPerSecStatsBy(keys.size());
        }
    }

    @Override
    public void RemoveBucket(int bucket) throws LockingException, StateTransferException, OperationFailedException, GeneralFailureException, CacheException {
        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("HashedCache.RemoveBucket", "removing bucket :" + bucket);
        }
        //Remove from stats
        if (getLocalBuckets() != null) {
            getLocalBuckets().remove(bucket);
        }
        //Remove actual data of the bucket
        RemoveBucketData(bucket);
        //remove operation logger for the bucket from log table if any exists.
        RemoveFromLogTbl(bucket);
    }

    /**
     *
     *
     * @param bucketIds
     */
    @Override
    public void RemoveExtraBuckets(java.util.ArrayList myBuckets) throws LockingException, StateTransferException, OperationFailedException, GeneralFailureException, CacheException {
        for (int i = 0; i < 1000; i++) {
            if (!myBuckets.contains(i)) {
                RemoveBucket(i);
            }
        }
    }

    @Override
    public void UpdateLocalBuckets(java.util.ArrayList bucketIds) {
        for (int i = 0; i < bucketIds.size(); i++) {
            if (getLocalBuckets() == null) {
                setLocalBuckets(new java.util.HashMap());
            }

            if (!getLocalBuckets().containsKey(bucketIds.get(i))) {
                getLocalBuckets().put(bucketIds.get(i), new BucketStatistics());
            }
        }
    }

    @Override
    public CacheAddResult AddInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation) throws StateTransferException, CacheException {
        int bucketId = GetBucketId(key);

        if (_logMgr.IsOperationAllowed(bucketId) && getLocalBuckets().containsKey(bucketId)) {
            if (_logMgr.IsLoggingEnbaled(bucketId, LogMode.LogBeforeActualOperation) && isUserOperation) {
                _logMgr.LogOperation(bucketId, key, cacheEntry, OperationType.Add);
                return CacheAddResult.Success;
            }

            CacheEntry clone = (CacheEntry) cacheEntry.clone();
            CacheAddResult result = super.AddInternal(key, cacheEntry, isUserOperation);

            if (result == CacheAddResult.Success || result == CacheAddResult.SuccessNearEviction) {
                IncrementBucketStats(key, bucketId, clone.getDataSize());
                if (isUserOperation) {
                    _logMgr.LogOperation(bucketId, key, clone, OperationType.Add);
                }
            }

            return result;
        }

        throw new StateTransferException("I am no more the owner of this bucket");
    }

    @Override
    public boolean AddInternal(Object key, com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint eh, OperationContext operationContext) throws StateTransferException {
        int bucketId = GetBucketId(key);

        if (_logMgr.IsOperationAllowed(bucketId) && getLocalBuckets().containsKey(bucketId)) {
            return super.AddInternal(key, eh, operationContext);
        }

        throw new StateTransferException("I am no more the owner of this bucket");
    }

    @Override
    public boolean RemoveInternal(Object key, com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint eh) throws StateTransferException, CacheException {
        int bucketId = GetBucketId(key);

        if (_logMgr.IsOperationAllowed(bucketId) && getLocalBuckets().containsKey(bucketId)) {
            return super.RemoveInternal(key, eh);
        }

        throw new StateTransferException("I am no more the owner of this bucket");
    }



    @Override
    public CacheInsResult InsertInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation, CacheEntry oldEntry, OperationContext operationContext) throws StateTransferException, CacheException {
        int bucketId = GetBucketId(key);
        OperationLogger opLogger = null;

        //fetch the operation logger...
        if (_logMgr.IsOperationAllowed(bucketId) && getLocalBuckets().containsKey(bucketId)) {
            long oldEntrysize = oldEntry == null ? 0 : oldEntry.getDataSize();

            if (_logMgr.IsLoggingEnbaled(bucketId, LogMode.LogBeforeActualOperation) && isUserOperation) {
                _logMgr.LogOperation(bucketId, key, cacheEntry, OperationType.Insert);
                return oldEntry != null ? CacheInsResult.SuccessOverwrite : CacheInsResult.Success;
            }
            CacheEntry clone = (CacheEntry) cacheEntry.clone();
            CacheInsResult result = super.InsertInternal(key, cacheEntry, isUserOperation, oldEntry, operationContext);

            switch (result) {
                case SuccessNearEvicition:
                case Success:
                    if (isUserOperation) {
                        _logMgr.LogOperation(bucketId, key, clone, OperationType.Insert);
                    }
                    IncrementBucketStats(key, bucketId, cacheEntry.getDataSize());
                    break;

                case SuccessOverwriteNearEviction:
                case SuccessOverwrite:
                    if (isUserOperation) {
                        _logMgr.LogOperation(bucketId, key, clone, OperationType.Insert);
                    }
                    DecrementBucketStats(key, bucketId, oldEntrysize);
                    IncrementBucketStats(key, bucketId, cacheEntry.getDataSize());
                    break;
            }

            return result;
        }

        throw new StateTransferException("I am no more the owner of this bucket");
    }

    @Override
    public CacheEntry RemoveInternal(Object key, ItemRemoveReason removalReason, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, CacheException {
        int bucketId = GetBucketId(key);

        if (isUserOperation) {
            if (!(_logMgr.IsOperationAllowed(bucketId) && getLocalBuckets().containsKey(bucketId))) {
                throw new StateTransferException("I am no more the owner of this bucket");
            }
        }
        if (_logMgr.IsLoggingEnbaled(bucketId, LogMode.LogBeforeActualOperation) && isUserOperation) {
            CacheEntry e = null;
            try {
                e = Get(key, operationContext);
            } catch (LockingException lockingException) {
                throw new OperationFailedException(lockingException);
            }
            _logMgr.LogOperation(bucketId, key, null, OperationType.Delete);
            return e;
        }

        CacheEntry entry = null;
        entry = super.RemoveInternal(key, removalReason, isUserOperation, operationContext);

        if (entry != null) {
            DecrementBucketStats(key, bucketId, entry.getDataSize());
            if (isUserOperation) {
                _logMgr.LogOperation(bucketId, key, null, OperationType.Delete);
            }
        }

        return entry;
    }

    @Override
    public CacheEntry GetInternal(Object key, boolean isUserOperation, OperationContext operationContext) throws StateTransferException {
        int bucketId = GetBucketId(key);
        if (isUserOperation) {
            if (!_logMgr.IsOperationAllowed(bucketId)) {
                throw new StateTransferException("I am no more the owner of this bucket");
            }
        }
        CacheEntry entry = super.GetInternal(key, isUserOperation, operationContext);

        if (entry == null && (isUserOperation && !getLocalBuckets().containsKey(bucketId))) {
            throw new StateTransferException("I am no more the owner of this bucket");
        }

        return entry;
    }

    @Override
    public boolean ContainsInternal(Object key) throws StateTransferException {
        int bucketId = GetBucketId(key);

        if (_logMgr.IsOperationAllowed(bucketId) && getLocalBuckets().containsKey(bucketId)) {
            return super.ContainsInternal(key);
        }

        throw new StateTransferException("I am no more the owner of this bucket");
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void ClearInternal() {
        super.ClearInternal();
        if (_keyList != null) {
            _keyList.clear();
        }
        if (_logMgr != null) { //it clears the operation loggers for each bucket
            _logMgr.dispose();
        }

        if (getLocalBuckets() == null) {
            return;
        }
        //clear the bucket stats
        Iterator ide = getLocalBuckets().entrySet().iterator();
        Map.Entry KeyValue;
        while (ide.hasNext()) {
            KeyValue = (Map.Entry) ide.next();
            Object Key = KeyValue.getKey();
            Object Value = KeyValue.getValue();
            BucketStatistics stats = (BucketStatistics) ((Value instanceof BucketStatistics) ? Value : null);
            if (stats != null) {
                stats.Clear();
            }
        }
    }

    @Override
    public void dispose() {
        if (_logMgr != null) {
            _logMgr.dispose();
        }
        if (_keyList != null) {
            _keyList.clear();
        }
        super.dispose();
    }
}
