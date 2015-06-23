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

package com.alachisoft.tayzgrid.caching.topologies;

import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.caching.DataSourceUpdateOptions;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.queries.DeleteQueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.PredicateHolder;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;
import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.common.ResetableIterator;


import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.stats.HPTime;
import com.alachisoft.tayzgrid.mapreduce.MapReduceOperation;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import com.alachisoft.tayzgrid.caching.util.SerializationUtil;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Synchronized wrapper over cache. Provides internal as well as external thread
 * safety.
 */
public class CacheSyncWrapper extends CacheBase {

    /**
     * The en-wrapped instance of cache.
     */
    private CacheBase _cache;
    /**
     * Default constructor.
     */
    public CacheSyncWrapper(CacheBase cache) {
        if (cache == null) {
            throw new IllegalArgumentException("cache");
        }
        _cache = cache;
        _context = cache.getContext();
        _syncObj = _cache.getSync();
    }

    

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public void dispose() {
        if (_cache != null) {
            _cache.dispose();
            _cache = null;
        }
        super.dispose();
    }

    /**
     * get/set listener of Cache events. 'null' implies no listener.
     */
    public final CacheBase getInternal() {
        return _cache;
    }

    /**
     * Returns the cache local to the node, i.e., internal cache.
     */
    @Override
    public CacheBase getInternalCache() {
        return _cache.getInternalCache();
    }

    @Override
    public TypeInfoMap getTypeInfoMap() {
        return _cache.getTypeInfoMap();
    }

    /**
     * get/set the name of the cache.
     */
    @Override
    public String getName() {
        return getInternal().getName();
    }

    @Override
    public void setName(String value) {
        getInternal().setName(value);
    }

    /**
     * get/set listener of Cache events. 'null' implies no listener.
     */
    @Override
    public ICacheEventsListener getListener() {
        return getInternal().getListener();
    }

    @Override
    public void setListener(ICacheEventsListener value) {
        getInternal().setListener(value);
    }

    @Override
    public int GetItemSize(Object key) {
        return _cache.GetItemSize(key);
    }

    /**
     * Notifications are enabled.
     */
    @Override
    public NotificationWrapper getNotifiers() {
        return getInternal().getNotifiers();
    }

    @Override
    public void setNotifiers(NotificationWrapper value) {
        getInternal().setNotifiers(value);
    }

    /**
     * returns the number of objects contained in the cache.
     */
    @Override
    public long getCount() throws GeneralFailureException, OperationFailedException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Count_get", "enter");
        }
        try {
            return getInternal().getCount();
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Count_get", "exit");
            }
        }
    }

    @Override
    public long getSize() {
        return getInternal().getSize();
    }

    /**
     * returns the number of objects contained in the cache.
     */
    @Override
    public long getSessionCount() {
        if (!getSync().IsWriterLockHeld()) {
            getSync().AcquireReaderLock();
        }
        try {
            return getInternal().getSessionCount();
        } finally {
            if (!getSync().IsWriterLockHeld()) {
                getSync().ReleaseReaderLock();
            }
        }
    }

    /**
     * returns the statistics of the Clustered Cache.
     */
    @Override
    public CacheStatistics getActualStats() {
        if (!getSync().IsWriterLockHeld()) {
            getSync().AcquireReaderLock();
        }
        try {
            return getInternal().getActualStats();
        } finally {
            if (!getSync().IsWriterLockHeld()) {
                getSync().ReleaseReaderLock();
            }
        }
    }

    /**
     * returns the statistics of the Clustered Cache.
     */
    @Override
    public CacheStatistics getStatistics() {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Stats_get", "enter");
        }

        if (!getSync().IsWriterLockHeld()) {
            getSync().AcquireReaderLock();
        }
        try {
            return getInternal().getStatistics();
        } finally {
            if (!getSync().IsWriterLockHeld()) {
                getSync().ReleaseReaderLock();
            }
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Stats_get", "exit");
            }

        }
    }

    @Override
    public boolean getVirtualUnlimitedSpace() {

        getSync().AcquireReaderLock();
           try
           {
            return _cache.getVirtualUnlimitedSpace();
           }
           finally{
            getSync().ReleaseReaderLock();
        }
    }

    @Override
    public void setVirtualUnlimitedSpace(boolean isUnlimitSpace) {

        getSync().AcquireWriterLock();
        try {
            _cache.setVirtualUnlimitedSpace(isUnlimitSpace);
           }
           finally{
            getSync().ReleaseWriterLock();
        }
    }
    
    @Override
    public java.util.ArrayList GetKeyList(int bucketId, boolean startLogging) {
        getSync().AcquireReaderLock();
        try {
            return _cache.GetKeyList(bucketId, startLogging);
        } finally {
            getSync().ReleaseReaderLock();
        }
    }
    
        @Override
    //: Array -Object[]
    public Object[] getKeys() {
        if (getInternal()== null) {
            throw new UnsupportedOperationException();
        }
        return getInternal().getKeys();
    }

    @Override
    public void RemoveBucket(int bucket) throws LockingException, StateTransferException, ClassNotFoundException, java.io.IOException, OperationFailedException, CacheException {
        getSync().AcquireWriterLock();
        try {
            _cache.RemoveBucket(bucket);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void RemoveExtraBuckets(java.util.ArrayList bucketIds) throws LockingException, StateTransferException, OperationFailedException, CacheException {
        try {
            getSync().AcquireWriterLock();
            _cache.RemoveExtraBuckets(bucketIds);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public java.util.HashMap GetLogTable(java.util.ArrayList bucketIds, tangible.RefObject<Boolean> isLoggingStopped) {
        getSync().AcquireReaderLock();
        try {
            return _cache.GetLogTable(bucketIds, isLoggingStopped);
        } finally {
            getSync().ReleaseReaderLock();
        }
    }

    @Override
    public void setBucketSize(int value) {
        getSync().AcquireWriterLock();
        try {
            _cache.setBucketSize(value);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void RemoveBucketData(int bucketId) throws LockingException, StateTransferException, OperationFailedException, CacheException {
        getSync().AcquireWriterLock();
        try {
            _cache.RemoveBucketData(bucketId);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void AddLoggedData(java.util.ArrayList bucketIds) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        getSync().AcquireWriterLock();
        try {
            _cache.AddLoggedData(bucketIds);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void UpdateLocalBuckets(java.util.ArrayList bucketIds) {
        getSync().AcquireWriterLock();
        try {
            _cache.UpdateLocalBuckets(bucketIds);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void RemoveFromLogTbl(int bucketId) {
        getSync().AcquireWriterLock();
        try {
            _cache.RemoveFromLogTbl(bucketId);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void StartLogging(int bucketId) {
        getSync().AcquireWriterLock();
        try {
            _cache.StartLogging(bucketId);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public java.util.HashMap getLocalBuckets() {
        getSync().AcquireReaderLock();
        try {
            return _cache.getLocalBuckets();
        } finally {
            getSync().ReleaseReaderLock();
        }
    }

    @Override
    public void setLocalBuckets(java.util.HashMap value) {
        getSync().AcquireWriterLock();
        try {
            _cache.setLocalBuckets(value);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public float getEvictRatio() {
        return getInternal().getEvictRatio();
    }

    @Override
    public void setEvictRatio(float value) {
        getSync().AcquireWriterLock();
        try {
            getInternal().setEvictRatio(value);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public boolean CanChangeCacheSize(long size) {
        return getInternal().CanChangeCacheSize(size);
    }

    @Override
    public long getMaxSize() {
        getSync().AcquireReaderLock();
        try {
            return getInternal().getMaxSize();
        } finally {
            getSync().ReleaseReaderLock();
        }
    }

    @Override
    public void setMaxSize(long value) throws Exception {
        getSync().AcquireWriterLock();
        try {
            getInternal().setMaxSize(value);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void UpdateClientsList(java.util.HashMap list) {
        getSync().AcquireWriterLock();
        try {
            getInternal().UpdateClientsList(list);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public boolean AcceptClient(InetAddress clientAddress) {
        getSync().AcquireWriterLock();
        try {
            return getInternal().AcceptClient(clientAddress);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void DisconnectClient(InetAddress clientAddress) {
        getSync().AcquireWriterLock();
        try {
            getInternal().DisconnectClient(clientAddress);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        Clear(cbEntry, updateOptions, null, operationContext);
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, String taskId, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
       
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Clear", "enter");
        }
        getSync().AcquireWriterLock();
        try {
            getInternal().Clear(cbEntry, updateOptions, operationContext);
            if (_context.getCacheImpl().getRequiresReplication()) {
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.Clear.getValue(), new Object[]{
                        cbEntry, taskId, operationContext
                    });
                }
           
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Clear", "exit");
            }
        }
    }

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    @Override
    public boolean Contains(Object key, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Cont", "enter");
        }

        if (!getSync().IsWriterLockHeld()) {
            getSync().AcquireReaderLock();
        }
        try {
            return getInternal().Contains(key, operationContext);
        } finally {
            if (!getSync().IsWriterLockHeld()) {
                getSync().ReleaseReaderLock();
            }
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Cont", "exit");
            }
        }
    }

    /**
     * Determines whether the cache contains the specified keys.
     *
     * @param keys The keys to locate in the cache.
     * @return list of existing keys.
     */
    @Override
    public java.util.HashMap Contains(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.ContBlk", "enter");
        }

        if (!getSync().IsWriterLockHeld()) {
            getSync().AcquireReaderLock();
        }
        try {
            return getInternal().Contains(keys, operationContext);
        } finally {
            if (!getSync().IsWriterLockHeld()) {
                getSync().ReleaseReaderLock();
            }
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.ContBlk", "exit");
            }

        }
    }

    @Override
    public void UpdateLockInfo(Object key, Object lockId, java.util.Date lockDate, LockExpiration lockExpiration, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.UpdLock", "enter");
        }

        getSync().AcquireWriterLock();
        try {
            getInternal().UpdateLockInfo(key, lockId, lockDate, lockExpiration, operationContext);
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.UpdLock", "exit");
            }
        }
    }

    @Override
    public CacheEntry Get(Object key, boolean IsUserOperation, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        CacheEntry entry = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Get_2", "enter");
        }

        getSync().AcquireWriterLock();
        try {
            entry = getInternal().Get(key, IsUserOperation, version, lockId, lockDate, lockExpiration, accessType, operationContext);
            
                if (accessType == LockAccessType.ACQUIRE && entry != null && _context.getCacheImpl().getRequiresReplication()) {
                    String uniqueKey = UUID.randomUUID().toString() + key;
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.UpdateLockInfo.getValue(), new Object[]{
                        true, key, lockId.argvalue, lockDate.argvalue, lockExpiration, operationContext
                    });
                }
            
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Get_2", "exit");
            }

        }
        return ConditionalSerialize(entry, operationContext);
    }

    /**
     * Retrieve the object from the cache. A string key is passed as parameter.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    @Override
    public CacheEntry Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType access, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        CacheEntry entry = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Get_2", "enter");
        }

        getSync().AcquireReaderLock();
        try {
            entry = getInternal().Get(key, version, lockId, lockDate, lockExpiration, access, operationContext);

                if (access == LockAccessType.ACQUIRE && entry != null && _context.getCacheImpl().getRequiresReplication()) {
                    String uniqueKey = UUID.randomUUID().toString() + key;
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.UpdateLockInfo.getValue(), new Object[]{
                        true, key, lockId.argvalue, lockDate.argvalue, lockExpiration, operationContext
                    });
                }
            
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Get_2", "exit");
            }

        }
        return ConditionalSerialize(entry, operationContext);
    }

    @Override
    public java.util.HashMap GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        java.util.HashMap retVal = null;
        getSync().AcquireReaderLock();
        try {
            retVal = getInternal().GetTagData(tags, comparisonType, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
        }
        return ConditionalHashMapSerialize(retVal, operationContext);
    }

    @Override
    public java.util.HashMap Remove(String[] tags, TagComparisonType tagComparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap retVal = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.RemoveByTag", "enter");
        }

        getSync().AcquireWriterLock();

        try {
            retVal = getInternal().Remove(tags, tagComparisonType, notify, operationContext);
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.RemoveByTag", "exit");
            }

        }
        return ConditionalHashMapSerialize(retVal, operationContext);
    }

    @Override
    public java.util.ArrayList GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        java.util.ArrayList keys = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.GetTagInternal", "enter");
        }

        getSync().AcquireReaderLock();

        try {
            keys = getInternal().GetTagKeys(tags, comparisonType, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.GetTagInternal", "exit");
            }

        }
        return keys;
    }

    @Override
    public LockOptions IsLocked(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        LockOptions lockInfo = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.IsLock", "enter");
        }

        getSync().AcquireWriterLock();

        try {
            lockInfo = getInternal().IsLocked(key, lockId, lockDate, operationContext);
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.IsLock", "exit");
            }

        }
        return lockInfo;
    }

    @Override
    public LockOptions Lock(Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        LockOptions lockInfo = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Lock", "enter");
        }

        getSync().AcquireWriterLock();
        try {
            lockInfo = getInternal().Lock(key, lockExpiration, lockId, lockDate, operationContext);

            
                if (_context.getCacheImpl().getRequiresReplication()) {
                    String uniqueKey = UUID.randomUUID().toString() + key;
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.UpdateLockInfo.getValue(), new Object[]{
                        true, key, lockId.argvalue, lockDate.argvalue, lockExpiration, operationContext
                    });
                }
            
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Lock", "exit");
            }

        }
        return lockInfo;
    }

    @Override
    public void UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Unlock", "enter");
        }
        getSync().AcquireWriterLock();
        try {
            getInternal().UnLock(key, lockId, isPreemptive, operationContext);
            
            if (_context.getCacheImpl().getRequiresReplication()) {
                String uniqueKey = UUID.randomUUID().toString() + key;
                _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.UpdateLockInfo.getValue(), new Object[]{
                    false, key, lockId, new java.util.Date(), null, isPreemptive, operationContext
                });
            }
          
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Unlock", "exit");
            }
        }
    }

    /**
     * Retrieve the objects from the cache. An array of keys is passed as
     * parameter.
     *
     * @param key keys of the entries.
     * @return cache entries.
     */
    @Override
    public java.util.HashMap Get(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.GetBlk", "enter");
        }
        java.util.HashMap retVal=null;
        getSync().AcquireReaderLock();
        try {
            retVal = getInternal().Get(keys, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.GetBlk", "exit");
            }
        }
        return ConditionalHashMapSerialize(retVal, operationContext);
    }

    /**
     * Retrieve the keys from the cache.
     *
     * @return list of keys.
     */
    @Override
    public java.util.ArrayList GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrpKeys", "enter");
        }

        getSync().AcquireReaderLock();
        try {
            return getInternal().GetGroupKeys(group, subGroup, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrpKeys", "exit");
            }
        }
    }

    /**
     * Retrieve the keys from the cache.
     *
     * @return list of keys.
     */
    @Override
    public CacheEntry GetGroup(Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        CacheEntry entry = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrp", "enter");
        }

        getSync().AcquireReaderLock();
        try {
            entry = getInternal().GetGroup(key, group, subGroup, version, lockId, lockDate, lockExpiration, accessType, operationContext);

                if (accessType == LockAccessType.ACQUIRE && entry != null && _context.getCacheImpl().getRequiresReplication()) {
                    String uniqueKey = UUID.randomUUID().toString() + key;
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.UpdateLockInfo.getValue(), new Object[]{
                        true, key, lockId.argvalue, lockDate.argvalue, lockExpiration, operationContext
                    });
                }
            
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrp", "exit");
            }
        }
        return ConditionalSerialize(entry, operationContext);
    }

    /**
     * Gets the data group information of the item.
     *
     * @param key
     * @return
     */
    @Override
    public GroupInfo GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrpInfo", "enter");
        }

        getSync().AcquireReaderLock();
        try {
            return getInternal().GetGroupInfo(key, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrpInf", "exit");
            }
        }
    }

    /**
     * Gets the data groups of the items.
     *
     * @param keys Keys of the items
     * @return HashMap containing key of the item as 'key' and GroupInfo as
     * 'value'
     */
    @Override
    public java.util.HashMap GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrpInfBlk", "enter");
        }

        getSync().AcquireReaderLock();
        try {
            return getInternal().GetGroupInfoBulk(keys, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrpInfBlk", "exit");
            }
        }
    }

    /**
     * Retrieve the keys from the cache.
     *
     * @return key and value pairs.
     */
    @Override
    public java.util.HashMap GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrpData", "enter");
        }
        java.util.HashMap retVal =null;
        getSync().AcquireReaderLock();
        try 
        {
            retVal = getInternal().GetGroupData(group, subGroup, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.GetGrpData", "exit");
            }
        }
        return ConditionalHashMapSerialize(retVal, operationContext);
    }

    /**
     * Gets/sets the list of data groups contained in the cache.
     */
    @Override
    public java.util.ArrayList getDataGroupList() {
        getSync().AcquireWriterLock();
        try {
            return getInternal().getDataGroupList();
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    /**
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     */
    @Override
    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException, SuspectedException, TimeoutException {
//        CacheEntry clone = (CacheEntry)cacheEntry.clone();
//        if (_cache._context.getInMemoryDataFormat() == DataFormat.object) 
//        {
////            try{
////            entryClone.DeflattedValue(_cache.getName());
////            }
////            catch (Exception e)
////            {
////                throw new GeneralFailureException(e.getMessage(),e);
////            }
//            Deserialize(cacheEntry);
//        }
        return Add(key, cacheEntry, notify, null, operationContext);
    }

    /**
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     */
    @Override
    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, String taskId, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException, SuspectedException, TimeoutException {

        boolean requiresReplication = _context.getCacheImpl().getRequiresReplication();

        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Add_1", "enter");
        }

        CacheEntry clone = (CacheEntry) cacheEntry.clone();
        if (_cache._context.getInMemoryDataFormat() == DataFormat.Object)
        {

            String _serilizationContext=(_cache._context!=null&&_cache._context.getCacheRoot()!=null)?_cache._context.getCacheRoot().getName():null;
            SerializationUtil.Deserialize(cacheEntry,_serilizationContext);

        }
        getSync().AcquireWriterLock();
        try {
            CacheAddResult result = getInternal().Add(key, cacheEntry, notify, operationContext);
            if ( requiresReplication) {
                if (result == CacheAddResult.Success || result == CacheAddResult.SuccessNearEviction) {
                    CacheEntry tempVar2 = clone.CloneWithoutValue();
                    CacheEntry cloneWithoutvalue = (CacheEntry) ((tempVar2 instanceof CacheEntry) ? tempVar2 : null);
                    Object[] userPayLoad = clone.getUserData();
                    long payLoadSize = clone.getDataSize();

                    _context.getCacheImpl().EnqueueForReplication(key, ClusterCacheBase.OpCodes.Add.getValue(), new Object[]{
                        key, cloneWithoutvalue, taskId, operationContext
                    }, clone.getSize(), userPayLoad, payLoadSize);
                }
            }
            return result;
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Add_1", "exit");
            }

        }
    }

    /**
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     */
    @Override
    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, boolean IsUserOperation, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {

        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Add_2", "enter");
        }
        if (_cache._context.getInMemoryDataFormat() == DataFormat.Object)
        {
            String _serilizationContext=(_cache._context!=null&&_cache._context.getCacheRoot()!=null)?_cache._context.getCacheRoot().getName():null;
            SerializationUtil.Deserialize(cacheEntry, _serilizationContext);
        }
        getSync().AcquireWriterLock();

        try {
            CacheAddResult result = getInternal().Add(key, cacheEntry, notify, IsUserOperation, operationContext);
            return result;
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Add_2", "exit");
            }
        }
    }

    /**
     * Add ExpirationHint against the given key Key must already exists in the
     * cache
     *
     * @param key
     * @param eh
     * @return
     */
    @Override
    public boolean Add(Object key, ExpirationHint eh, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        boolean depAdded = false;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Add_3", "enter");
        }
        getSync().AcquireWriterLock();
        try {
            depAdded = getInternal().Add(key, eh, operationContext);
            if (depAdded) {
                if (_context.getCacheImpl().getRequiresReplication()) {
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.AddHint.getValue(), new Object[]{
                        key, eh, operationContext
                    });
                }
            }
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Add_3", "exit");
            }

        }
        return depAdded;
    }

    /**
     * Adds key and value pairs to the cache. Throws an exception or returns the
     * list of keys that already exists in the cache.
     *
     * @param keys key of the entry.
     * @param cacheEntries the cache entry.
     * @return List of keys that are added or that alredy exists in the cache
     * and their status
     */
    @Override
    public java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Add(keys, cacheEntries, notify, null, operationContext);
    }

    /**
     * Adds key and value pairs to the cache. Throws an exception or returns the
     * list of keys that already exists in the cache.
     *
     * @param keys key of the entry.
     * @param cacheEntries the cache entry.
     * @return List of keys that are added or that alredy exists in the cache
     * and their status
     */
    @Override
    public java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, String taskId, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap result = null;

        boolean requiresReplication = _context.getCacheImpl().getRequiresReplication();
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.AddBlk", "enter");
        }
        
        CacheEntry[] clone= new CacheEntry[cacheEntries.length];        
        for (int i = 0; i < cacheEntries.length; i++)
        {
            clone[i] = (CacheEntry) cacheEntries[i].clone();
        }
        if (_cache._context.getInMemoryDataFormat() == DataFormat.Object)
        {
            String _serilizationContext=(_cache._context!=null&&_cache._context.getCacheRoot()!=null)?_cache._context.getCacheRoot().getName():null;

            for (int i = 0; i < cacheEntries.length; i++)
            {
                SerializationUtil.Deserialize(cacheEntries[i], _serilizationContext);
            }
        }
        getSync().AcquireWriterLock();
        try {
            result = getInternal().Add(keys, cacheEntries, notify, operationContext);

           
                if ( requiresReplication) {
                    
                    java.util.ArrayList successfulKeys = new java.util.ArrayList();
                    java.util.ArrayList successfulEnteries = new java.util.ArrayList();
                    if (result != null && result.size() > 0) {
                        for (int i = 0; i < keys.length; i++) {
                            if (result.containsKey(keys[i])) {
                                if (result.get(keys[i]) instanceof CacheAddResult) {
                                    CacheAddResult addResult = (CacheAddResult) result.get(keys[i]);
                                    if (addResult == CacheAddResult.Success || addResult == CacheAddResult.SuccessNearEviction) {
                                        successfulKeys.add(keys[i]);
                                        if (clone != null) {
                                            successfulEnteries.add(clone[i]);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (successfulKeys.size() > 0) {
                        for (int i = 0; i < successfulKeys.size(); i++) {
                            CacheEntry entry = (CacheEntry) ((successfulEnteries.get(i) instanceof CacheEntry) ? successfulEnteries.get(i) : null);
                            CacheEntry tempVar = entry.CloneWithoutValue();
                            _context.getCacheImpl().EnqueueForReplication(successfulKeys.get(i), ClusterCacheBase.OpCodes.Add.getValue(), new Object[]{
                                successfulKeys.get(i), (CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null), (taskId != null) ? taskId + "-" + i : null, operationContext
                            }, entry.getSize(), entry.getUserData(), entry.getDataSize());
                        }
                    }
                }
            

        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.AddBlk", "exit");
            }
        }
        return result;
    }

    /**
     * Adds a pair of key and value to the cache. If the specified key already
     * exists in the cache; it is updated, otherwise a new item is added to the
     * cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     */
    @Override
    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Insert(key, cacheEntry, notify, null, lockId, version, accessType, operationContext);
    }

    @Override
    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, String taskId, Object lockId, long version, LockAccessType access, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        boolean requiresReplication = false;
       
        requiresReplication = _context.getCacheImpl().getRequiresReplication();
       

        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Insert_1", "enter");
        }
        CacheEntry clone = (CacheEntry) cacheEntry.clone();
        if (_cache._context.getInMemoryDataFormat() == DataFormat.Object)
        {

            String _serilizationContext=(_cache._context!=null&&_cache._context.getCacheRoot()!=null)?_cache._context.getCacheRoot().getName():null;
            
            SerializationUtil.Deserialize(cacheEntry, _serilizationContext);

            //--TODO-- change object state in insert params
            if(operationContext.Contains(OperationContextFieldName.InsertParams))
            {
                InsertParams insertParams = (InsertParams) operationContext.GetValueByField(OperationContextFieldName.InsertParams);
                if(insertParams.OldValue!= null)
                {
                    insertParams.OldValue = SerializationUtil.Deserialize(insertParams.OldValue, insertParams.OldValueFlag, _cache.getName());
                }
            }
        }
        getSync().AcquireWriterLock();

        try {
            CacheInsResultWithEntry result = getInternal().Insert(key, cacheEntry, notify, lockId, version, access, operationContext);

            
                if (requiresReplication) {
                    if (result.getResult() == CacheInsResult.Success || result.getResult() == CacheInsResult.SuccessNearEvicition || result.getResult()
                            == CacheInsResult.SuccessOverwrite || result.getResult() == CacheInsResult.SuccessOverwriteNearEviction) {
                        CacheEntry tempVar2 = cacheEntry.CloneWithoutValue();
                        CacheEntry CloneWithoutValue = (CacheEntry) ((tempVar2 instanceof CacheEntry) ? tempVar2 : null);

                        if (result.getResult() == CacheInsResult.SuccessOverwrite && access == LockAccessType.DONT_RELEASE) {
                            clone.CopyLock(result.getEntry().getLockId(), result.getEntry().getLockDate(), result.getEntry().getLockExpiration());
                        }
                        _context.getCacheImpl().EnqueueForReplication(key, ClusterCacheBase.OpCodes.Insert.getValue(), new Object[]{
                            key, CloneWithoutValue, taskId, operationContext
                        }, clone.getSize(), clone.getUserData(), clone.getDataSize());
                    }
                }
        
            
            result.setEntry(ConditionalSerialize(result.getEntry(), operationContext));
                      
            return result;
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Insert_1", "exit");
            }

        }
    }

    /**
     * Adds a pair of key and value to the cache. If the specified key already
     * exists in the cache; it is updated, otherwise a new item is added to the
     * cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     */
    @Override
    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, boolean IsUserOperation, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Insert_2", "enter");
        }
        if (_cache._context.getInMemoryDataFormat() == DataFormat.Object)
        {

            String _serilizationContext=(_cache._context!=null&&_cache._context.getCacheRoot()!=null)?_cache._context.getCacheRoot().getName():null;
            SerializationUtil.Deserialize(cacheEntry, _serilizationContext);
            //--TODO-- change object state in insert params
            if(operationContext.Contains(OperationContextFieldName.InsertParams))
            {
                InsertParams insertParams = (InsertParams) operationContext.GetValueByField(OperationContextFieldName.InsertParams);
                if(insertParams.OldValue!= null)
                {
                    insertParams.OldValue = SerializationUtil.Deserialize(insertParams.OldValue, insertParams.OldValueFlag, _cache.getName());
                }
            }
        }
        getSync().AcquireWriterLock();
        try {
            CacheInsResultWithEntry result = getInternal().Insert(key, cacheEntry, notify, IsUserOperation, lockId, version, accessType, operationContext);
            result.setEntry(ConditionalSerialize(result.getEntry(), operationContext));
            return result;
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Insert_2", "exit");
            }

        }
    }

    @Override
    public java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Insert(keys, cacheEntries, notify, null, operationContext);
    }

    /**
     * Adds key and value pairs to the cache. If any of the specified key
     * already exists in the cache; it is updated, otherwise a new item is added
     * to the cache.
     *
     * @param keys keys of the entries.
     * @param cacheEntries the cache entries.
     * @return returns the results for inserted keys
     */
    @Override
    public java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, String taskId, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap result = null;
        boolean requiresReplication = false;
   
        requiresReplication = _context.getCacheImpl().getRequiresReplication();
       
        CacheEntry[] clone = new CacheEntry[cacheEntries.length];
        for (int i = 0; i < cacheEntries.length; i++)
        {
            clone[i] = (CacheEntry) cacheEntries[i].clone();
        }
        if (_cache._context.getInMemoryDataFormat() == DataFormat.Object)
        {
            String _serilizationContext=(_cache._context!=null&&_cache._context.getCacheRoot()!=null)?_cache._context.getCacheRoot().getName():null;

            for (int i = 0; i < cacheEntries.length; i++)
            {
                SerializationUtil.Deserialize(cacheEntries[i], _serilizationContext);
            }
        }
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.InsertBlk", "enter");
        }

        getSync().AcquireWriterLock();
        try {
            result = getInternal().Insert(keys, cacheEntries, notify, operationContext);
            
                if ( requiresReplication) {
                    if (result != null && result.size() > 0) {
                        java.util.ArrayList successfulKeys = new java.util.ArrayList();
                        java.util.ArrayList successfulEnteries = new java.util.ArrayList();

                        for (int i = 0; i < keys.length; i++) {
                            if (result.containsKey(keys[i])) {
                                Object key = keys[i];
                                CacheInsResultWithEntry resultWithEntry = (CacheInsResultWithEntry) ((result.get(keys[i]) instanceof CacheInsResultWithEntry) ? result.get(keys[i]) : null);
                                if (resultWithEntry != null) {
                                    CacheInsResult insResult = resultWithEntry.getResult();
                                    if (insResult == CacheInsResult.Success || insResult == CacheInsResult.SuccessOverwrite) {
                                        successfulKeys.add(keys[i]);
                                        successfulEnteries.add(clone[i]);
                                    }
                                }
                            }
                        }


                        if (successfulKeys.size() > 0) {
                            for (int i = 0; i < successfulKeys.size(); i++) {
                                CacheEntry entry = (CacheEntry) ((successfulEnteries.get(i) instanceof CacheEntry) ? successfulEnteries.get(i) : null);
                                CacheEntry tempVar = entry.CloneWithoutValue();
                                _context.getCacheImpl().EnqueueForReplication(successfulKeys.get(i), ClusterCacheBase.OpCodes.Insert.getValue(), new Object[]{
                                    successfulKeys.get(i), (CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null), (taskId != null) ? taskId + "-" + i : null, operationContext
                                }, entry.getSize(), entry.getUserData(), entry.getDataSize());
                            }
                        }
                    }
                }
            
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.InsertBlk", "exit");
            }
        }
        
        return result;
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter. Moreover it take a removal reason and a boolean specifying if
     * a notification should be raised.
     *
     * @param key key of the entry.
     * @param notify boolean specifying to raise the event.
     * @return item value
     */
    @Override
    public CacheEntry Remove(Object key, ItemRemoveReason ir, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Remove(key, ir, notify, null, lockId, version, accessType, operationContext);
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter. Moreover it take a removal reason and a boolean specifying if
     * a notification should be raised.
     *
     * @param key key of the entry.
     * @param removalReason reason for the removal.
     * @param notify boolean specifying to raise the event.
     * @return item value
     */
    @Override
    public CacheEntry Remove(Object key, ItemRemoveReason ir, boolean notify, String taskId, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        CacheEntry entry = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Remove_1", "enter");
        }
        //--TODO-- change state of object in delete params
        if(_cache._context.getInMemoryDataFormat() == DataFormat.Object)
        {
            if(operationContext.Contains(OperationContextFieldName.DeleteParams))
            {
                DeleteParams deleteParams = (DeleteParams) operationContext.GetValueByField(OperationContextFieldName.DeleteParams);
                deleteParams.OldValue = SerializationUtil.Deserialize(deleteParams.OldValue, deleteParams.OldValueFlag, _cache.getName());
            }
        }

        getSync().AcquireWriterLock();
        try {
            Object removeOnReplica = operationContext.GetValueByField(OperationContextFieldName.RemoveOnReplica);

            entry = getInternal().Remove(key, ir, notify, lockId, version, accessType, operationContext);

           
                if (entry != null) {
                    if (_context.getCacheImpl().getRequiresReplication()) {
                        _context.getCacheImpl().EnqueueForReplication(key, ClusterCacheBase.OpCodes.Remove.getValue(), new Object[]{
                            key, taskId, operationContext
                        });
                    }
                } 
                else if (removeOnReplica != null) {
                    getContext().getCacheLog().Error("CacheSync Remove on Replica Key : " + key);
                    if (getContext().getCacheImpl().getRequiresReplication()) {
                        getContext().getCacheImpl().EnqueueForReplication(key, ClusterCacheBase.OpCodes.Remove.getValue(), new Object[]{key, taskId, operationContext});
                    }
                }
            
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Remove_1", "exit");
            }

        }
        return ConditionalSerialize(entry, operationContext);
    }

    @Override
    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason ir, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Remove(keys, ir, notify, null, operationContext);
    }

    /**
     * Removes the key and pairs from the cache. The keys are specified as
     * parameter. Moreover it take a removal reason and a boolean specifying if
     * a notification should be raised.
     *
     * @param keys key of the entries.
     * @param notify boolean specifying to raise the event.
     * @return removed keys list
     */
    @Override
    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason ir, boolean notify, String taskId, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.RemoveBlk", "enter");
        }
        java.util.HashMap result = null;
        Object[] successfulKeys = null;
        boolean requiresReplication = false;
       
        requiresReplication = _context.getCacheImpl().getRequiresReplication();
        
        getSync().AcquireWriterLock();

        try {

            result = getInternal().Remove(keys, ir, notify, operationContext);
           
                if ( requiresReplication) {
                    if (result != null && result.size() > 0) {
                        successfulKeys = new Object[result.size()];
                        int j = 0;
                        for (int i = 0; i < keys.length && j < successfulKeys.length; i++) {
                            if (result.containsKey(keys[i])) {
                                successfulKeys[j] = keys[i];
                                j++;
                            }
                        }
                        //: we generate a unique key to be passed to async replicator because
                        //it is required by the replicator and we do not want this operation 2 be overriden
                        //in optimized queue.
                        String uniqueKey = UUID.randomUUID().toString() + keys[0];
                        _context.getCacheImpl().EnqueueForReplication(null, com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase.OpCodes.RemoveRange.getValue(), new Object[]{
                            keys, ir, taskId, operationContext
                        });
                    }
                }
           
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.RemoveBlk", "exit");
            }

        }
        return ConditionalHashMapSerialize(result, operationContext);
    }



    @Override
    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason ir, boolean notify, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap result = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Remove_4", "enter");
        }
        getSync().AcquireWriterLock();
        try {
            result = getInternal().Remove(keys, ir, notify, isUserOperation, operationContext);
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Remove_4", "exit");
            }

        }
        return ConditionalHashMapSerialize(result, operationContext);
    }

    @Override
    public Object RemoveSync(Object[] keys, ItemRemoveReason reason, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return getInternal().RemoveSync(keys, reason, notify, operationContext);
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter. Moreover it take a removal reason and a boolean specifying if
     * a notification should be raised.
     *
     * @param key key of the entry.
     * @param removalReason reason for the removal.
     * @param notify boolean specifying to raise the event.
     * @return item value
     */
    @Override
    public CacheEntry Remove(Object key, ItemRemoveReason ir, boolean notify, boolean isUserOperation, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        CacheEntry entry = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Remove_5", "enter");
        }
        
        if(_cache._context.getInMemoryDataFormat() == DataFormat.Object)
        {
            if(operationContext.Contains(OperationContextFieldName.DeleteParams))
            {
                DeleteParams deleteParams = (DeleteParams) operationContext.GetValueByField(OperationContextFieldName.DeleteParams);
                deleteParams.OldValue = SerializationUtil.Deserialize(deleteParams.OldValue, deleteParams.OldValueFlag, _cache.getName());
            }
        }
        getSync().AcquireWriterLock();
        try {
            entry = getInternal().Remove(key, ir, notify, isUserOperation, lockId, version, accessType, operationContext);
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Remove_5", "exit");
            }

        }
        return ConditionalSerialize(entry, operationContext);
    }

    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     * @param notify boolean specifying to raise the event.
     */
    @Override
    public java.util.HashMap Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap result = null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.RemoveGrp", "enter");
        }
        getSync().AcquireWriterLock();
        try {
            result = getInternal().Remove(group, subGroup, notify, operationContext);

           
                if (_context.getCacheImpl().getRequiresReplication()) {
                    //: we generate a unique key to be passed to async replicator because
                    //it is required by the replicator and we do not want this operation 2 be overriden
                    //in optimized queue.
                    String uniqueKey = UUID.randomUUID().toString() + group;
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.RemoveGroup.getValue(), new Object[]{
                        group, subGroup, operationContext
                    });
                }
            
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.RemoveGrp", "exit");
            }

        }
        return ConditionalHashMapSerialize(result, operationContext);
    }

    @Override
    public QueryResultSet Search(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.Search", "enter");
        }
        QueryResultSet result=null;
        getSync().AcquireReaderLock();
        try {
             result = getInternal().Search(query, values, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.Search", "exit");
            }

        }
        //no need to conditional hashmap serialize as it does not return values
        return result;
    }

    @Override
    public QueryResultSet SearchEntries(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.SearchEnt", "enter");
        }
        QueryResultSet result = null;
        getSync().AcquireReaderLock();
        try {
            result = getInternal().SearchEntries(query, values, operationContext);
        } finally {
            getSync().ReleaseReaderLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.SearchEnt", "exit");
            }
        }
        result.setSearchEntriesResult(ConditionalHashMapSerialize(result.getSearchEntriesResult(), operationContext));
        return result;
    }

    @Override
    public DeleteQueryResultSet DeleteQuery(String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason reason, OperationContext operationContext)
            throws Exception {
        DeleteQueryResultSet result = new DeleteQueryResultSet();

        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.DeleteQueryEnt", "enter");
        }

        getSync().AcquireWriterLock();

        try {
            result = getInternal().DeleteQuery(query, values, notify, isUserOperation, reason, operationContext);

            if (_context.getCacheImpl().getRequiresReplication()) {
                _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.RemoveRange.getValue(), new Object[]{result.getRemoveKeys(), reason, null, operationContext});
            }

            // We make it Null, because it is no more needed and can add up network cost for clustered
            //responses.
            result.setKeysEffected(null);
        } finally {
            getSync().ReleaseWriterLock();
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.DeleteQueryEnt", "exit");
            }
        }
        return result;
    }

    /**
     * Broadcasts a user-defined event across the cluster.
     *
     * @param notifId
     * @param data
     * @param async
     */
    @Override
    public void SendNotification(Object notifId, Object data) {
        getInternal().SendNotification(notifId, data);
    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     *
     * @return Iterator enumerator.
     */
    @Override
    public ResetableIterator GetEnumerator() throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        getSync().AcquireWriterLock();
        try {
            return getInternal().GetEnumerator();
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public EnumerationDataChunk GetNextChunk(EnumerationPointer pointer, OperationContext operationContext) throws GeneralFailureException, TimeoutException, SuspectedException, CacheException {
        getSync().AcquireWriterLock();
        try {
            return getInternal().GetNextChunk(pointer, operationContext);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public boolean HasEnumerationPointer(EnumerationPointer pointer) {
        getSync().AcquireWriterLock();
        try {
            return getInternal().HasEnumerationPointer(pointer);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void RegisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, SuspectedException, TimeoutException {
        getSync().AcquireWriterLock();
        try {
            getInternal().RegisterKeyNotification(key, updateCallback, removeCallback, operationContext);
            
                if (_context.getCacheImpl().getRequiresReplication()) {
                    String uniqueKey = UUID.randomUUID().toString() + key;
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.RegisterKeyNotification.getValue(), new Object[]{
                        key, updateCallback, removeCallback, operationContext
                    });
                }

            
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void RegisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, SuspectedException, TimeoutException {
        boolean mirrorReplication = _context.getCacheImpl().getRequiresReplication();
        getSync().AcquireWriterLock();
        try {

            getInternal().RegisterKeyNotification(keys, updateCallback, removeCallback, operationContext);

           
                if ( mirrorReplication) {
                    for (int i = 0; i < keys.length; i++) {
                        String uniqueKey = UUID.randomUUID().toString() + keys[i];
                        _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.RegisterKeyNotification.getValue(), new Object[]{
                            keys[i], updateCallback, removeCallback, operationContext
                        });
                    }
                    
                }
            
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void UnregisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {

        getSync().AcquireWriterLock();
        try {
            getInternal().UnregisterKeyNotification(key, updateCallback, removeCallback, operationContext);
            if (_context.getCacheImpl().getRequiresReplication()) {
                String uniqueKey = UUID.randomUUID().toString() + key;
                _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.UnregisterKeyNotification.getValue(), new Object[]{
                    key, updateCallback, removeCallback, operationContext
                });
            }
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void UnregisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        boolean mirrorReplication = _context.getCacheImpl().getRequiresReplication();
        getSync().AcquireWriterLock();
        try {
            getInternal().UnregisterKeyNotification(keys, updateCallback, removeCallback, operationContext);
            if ( mirrorReplication) {
                for (int i = 0; i < keys.length; i++) {
                    String uniqueKey = UUID.randomUUID().toString() + keys[i];
                    _context.getCacheImpl().EnqueueForReplication(null, ClusterCacheBase.OpCodes.UnregisterKeyNotification.getValue(), new Object[]{
                        keys[i], updateCallback, removeCallback, operationContext
                    });
                }
            }
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

   
    

    @Override
    public void SetStateTransferKeyList(java.util.HashMap keylist) {
        getSync().AcquireWriterLock();
        try {
            _cache.SetStateTransferKeyList(keylist);
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    @Override
    public void UnSetStateTransferKeyList() {
        getSync().AcquireWriterLock();
        try {
            _cache.UnSetStateTransferKeyList();
        } finally {
            getSync().ReleaseWriterLock();
        }
    }


    @Override
    public Object TaskOperationRecieved(MapReduceOperation operation) throws OperationFailedException
    {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CacheSyncWrp.MapReduceOperationRecieved", "enter");
        }        

        try {
            return getInternal().TaskOperationRecieved(operation);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("CacheSyncWrp.MapReduceOperationRecieved", "exit");
            }
        }
    }
    
    @Override
    public void declaredDeadClients(ArrayList clients)
    {
        getInternalCache().declaredDeadClients(clients);
	}
    
    
    private CacheEntry ConditionalSerialize(CacheEntry entry, OperationContext operationContext) throws GeneralFailureException{
        if (entry != null && _cache._context.getInMemoryDataFormat() == DataFormat.Object) 
        {
            String _serilizationContext=(_cache._context!=null&&_cache._context.getCacheRoot()!=null)?_cache._context.getCacheRoot().getName():null;

            if (operationContext.GetValueByField(OperationContextFieldName.DataFormat) != null) 
            {
                if (operationContext.GetValueByField(OperationContextFieldName.DataFormat) == DataFormat.Binary
                        || operationContext.Contains(OperationContextFieldName.IsClusteredCall)) {
                   
                    entry = (CacheEntry)entry.clone();
                    SerializationUtil.Serialize(entry,_serilizationContext);

                }
            } 
            else 
            {
                if(_context.getCacheRoot().getIsInProc()&&!operationContext.Contains(OperationContextFieldName.IsClusteredCall))
                    return entry;
                entry = (CacheEntry)entry.clone();
                SerializationUtil.Serialize(entry, _serilizationContext);

            }
            
        }
        return entry;
        
    }
    
    private HashMap ConditionalHashMapSerialize(HashMap hMap, OperationContext operationContext) throws GeneralFailureException{
        if (hMap != null && _cache._context.getInMemoryDataFormat() == DataFormat.Object) 
        {
            String _serilizationContext=(_cache._context!=null&&_cache._context.getCacheRoot()!=null)?_cache._context.getCacheRoot().getName():null;
            
            if (operationContext.GetValueByField(OperationContextFieldName.DataFormat) != null) 
            {
                if (operationContext.GetValueByField(OperationContextFieldName.DataFormat) == DataFormat.Binary) {
                    if(_context.getCacheRoot().getIsInProc()&&!operationContext.Contains(OperationContextFieldName.IsClusteredCall))
                    return hMap;
                    SerializationUtil.SerializeHashMap(hMap,_serilizationContext);
                }
            } 
            else 
            {
                if(_context.getCacheRoot().getIsInProc()&&!operationContext.Contains(OperationContextFieldName.IsClusteredCall))
                    return hMap;
                SerializationUtil.SerializeHashMap(hMap, _serilizationContext);
            }
            
        }
        return hMap;
    }
}

