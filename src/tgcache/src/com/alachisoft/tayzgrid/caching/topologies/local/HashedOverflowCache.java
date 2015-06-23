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
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.statistics.BucketStatistics;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.Iterator;
import java.util.Map;

public class HashedOverflowCache extends IndexedOverflowCache
{

    private int _bucketSize;
    /**
     * A map that contains key lists against each bucket id.
     */
    private java.util.HashMap _keyList;
    /**
     * A map of operation loggers against each bucketId
     */
    private java.util.HashMap _operationLoggers;
    private OpLogManager _logMgr;
    private int _stopLoggingThreshhold = 50;
    private boolean _logEntries;

    public HashedOverflowCache(java.util.Map cacheClasses, CacheBase parentCache, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context, boolean logEntries)throws ConfigurationException
    {
        super(cacheClasses, parentCache, properties, listener, context);
        _logMgr = new OpLogManager(logEntries, context);
        _logEntries = logEntries;
    }

    @Override
    public void setBucketSize(int value)
    {
        _bucketSize = value;
    }

    @Override
    public java.util.ArrayList GetKeyList(int bucketId, boolean startLogging)
    {
        if (startLogging)
        {
            _logMgr.StartLogging(bucketId, LogMode.LogBeforeAfterActualOperation);
        }

        if (_keyList != null)
        {
            if (_keyList.containsKey(bucketId))
            {
                java.util.HashMap keyTbl = (java.util.HashMap) ((_keyList.get(bucketId) instanceof java.util.HashMap) ? _keyList.get(bucketId) : null);
                return new java.util.ArrayList(keyTbl.keySet());
            }
            return null;
        }
        return null;
    }

    @Override
    public java.util.HashMap GetLogTable(java.util.ArrayList bucketIds, tangible.RefObject<Boolean> isLoggingStopped)
    {
        java.util.HashMap result = null;
        int logCount = 0;
        java.util.Iterator ie = bucketIds.iterator();
        while (ie.hasNext())
        {
            java.util.HashMap tmp = _logMgr.GetLogTable((Integer) ie.next());
            if (tmp != null)
            {
                if (result == null)
                {
                    result = tmp;
                }
                else
                {
                    java.util.ArrayList removed = (java.util.ArrayList) ((tmp.get("removed") instanceof java.util.ArrayList) ? tmp.get("removed") : null);
                    java.util.ArrayList updated = (java.util.ArrayList) ((tmp.get("updated") instanceof java.util.ArrayList) ? tmp.get("updated") : null);

                    if (removed != null)
                    {
                        ((java.util.ArrayList) result.get("removed")).addAll(removed);
                        logCount += removed.size();
                    }

                    if (updated != null)
                    {
                        ((java.util.ArrayList) result.get("updated")).addAll(updated);
                        logCount += updated.size();
                    }
                }
            }
        }
        if (logCount < _stopLoggingThreshhold)
        {
            isLoggingStopped.argvalue = true;
            _logMgr.StopLogging(bucketIds);
        }
        else
        {
            isLoggingStopped.argvalue = false;
        }

        return result;
    }

    @Override
    public void RemoveFromLogTbl(int bucketId)
    {
        if (_logMgr != null)
        {
            _logMgr.RemoveLogger(bucketId);
        }
    }

    @Override
    public void RemoveBucket(int bucket)throws LockingException,StateTransferException,ClassNotFoundException, java.io.IOException,OperationFailedException
    {

        if (getContext().getCacheLog().getIsInfoEnabled())
        {
            getContext().getCacheLog().Info("HashedCache.RemoveBucket", "removing bucket :" + bucket);
        }
        //Remove from stats
        getLocalBuckets().remove(bucket);
        //Remove actual data of the bucket
        RemoveBucketData(bucket);
        //remove operation logger for the bucket from log table if any exists.
        RemoveFromLogTbl(bucket);
    }

    @Override
    public void RemoveBucketData(int bucketId)throws LockingException,StateTransferException,OperationFailedException
    {
    }

    @Override
    public void UpdateLocalBuckets(java.util.ArrayList bucketIds)
    {
        java.util.Iterator ie = bucketIds.iterator();
        while (ie.hasNext())
        {
            if (getLocalBuckets() == null)
            {
                setLocalBuckets(new java.util.HashMap());
            }

            if (!getLocalBuckets().containsKey(ie.next()))
            {
                getLocalBuckets().put(ie.next(), new BucketStatistics());
            }
        }
    }

    @Override
    public void StartLogging(int bucketId)
    {
        if (_logMgr != null)
        {
            _logMgr.StartLogging(bucketId, LogMode.LogBeforeActualOperation);
        }
    }

    private void IncrementBucketStats(Object key, int bucketId, long dataSize)
    {
        if (_stats.getLocalBuckets().containsKey(bucketId))
        {
            ((BucketStatistics) _stats.getLocalBuckets().get(bucketId)).Increment(dataSize);
        }

        if (_keyList == null)
        {
            _keyList = new java.util.HashMap();
        }

        if (_keyList.containsKey(bucketId))
        {
            java.util.HashMap keys = (java.util.HashMap) _keyList.get(bucketId);
            keys.put(key, null);
        }
        else
        {
            java.util.HashMap keys = new java.util.HashMap();
            keys.put(key, null);
            _keyList.put(bucketId, keys);
        }
    }

    private void DecrementBucketStats(Object key, int bucketId, long dataSize)
    {
        if (_stats.getLocalBuckets().containsKey(bucketId))
        {
            ((BucketStatistics) _stats.getLocalBuckets().get(bucketId)).Decrement(dataSize);
        }

        if (_keyList != null)
        {
            if (_keyList.containsKey(bucketId))
            {
                java.util.HashMap keys = (java.util.HashMap) _keyList.get(bucketId);
                keys.remove(key);

                if (keys.isEmpty())
                {
                    _keyList.remove(bucketId);
                }
            }
        }
    }

    private boolean IsBucketTransfered(int bucketId)
    {
        return !_logMgr.IsOperationAllowed(bucketId);
    }

    private int GetBucketId(Object key)
    {
        int hashCode = AppUtil.hashCode(key);

        //+Numan Hanif: Fix for 7419
        //int bucketId = hashCode / _bucketSize;
        int bucketId = hashCode % AppUtil.TotalBuckets;
        //+Numan Hanif

        if (bucketId < 0)
        {
            bucketId *= -1;
        }
        return bucketId;
    }

    @Override
    public java.util.HashMap getLocalBuckets()
    {
        return _stats.getLocalBuckets();
    }

    @Override
    public void setLocalBuckets(java.util.HashMap value)
    {
        _stats.setLocalBuckets(value);
    }

    @Override
    public void AddLoggedData(java.util.ArrayList bucketIds)
    {
    }

    @Override
    public void dispose()
    {
        if (_logMgr != null)
        {
            _logMgr.dispose();
        }
        if (_keyList != null)
        {
            _keyList.clear();
        }
        super.dispose();
    }

    @Override
    public CacheAddResult AddInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation) throws StateTransferException, CacheException
    {
        int bucketId = GetBucketId(key);

        if (IsBucketTransfered(bucketId))
        {
            throw new StateTransferException("I am no more the owner of this bucket");
        }

        if (_logMgr.IsLoggingEnbaled(bucketId, LogMode.LogBeforeActualOperation) && isUserOperation)
        {
            _logMgr.LogOperation(bucketId, key, cacheEntry, OperationType.Add);
            return CacheAddResult.Success;
        }

        CacheAddResult result = super.AddInternal(key, cacheEntry, isUserOperation);
        if (result == CacheAddResult.Success || result == CacheAddResult.SuccessNearEviction)
        {
            IncrementBucketStats(key, bucketId, cacheEntry.getDataSize());
            if (isUserOperation)
            {
                _logMgr.LogOperation(bucketId, key, cacheEntry, OperationType.Add);
            }

        }
        return result;
    }

    @Override
    public boolean AddInternal(Object key, com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint eh, OperationContext operationContext)
    {
        return super.AddInternal(key, eh, operationContext);
    }

 

    /**
     * Removes all entries from the store.
     */
    @Override
    public void ClearInternal()
    {
        super.ClearInternal();
        if (_keyList != null)
        {
            _keyList.clear();
        }
        if (_logMgr != null)
        { //it clears the operation loggers for each bucket
            _logMgr.dispose();
        }

        //clear the bucket stats
        Iterator ide = getLocalBuckets().entrySet().iterator();
       Map.Entry KeyValue;
        while (ide.hasNext()) {
            KeyValue = (Map.Entry) ide.next();
            Object Value = KeyValue.getValue();
            BucketStatistics stats = (BucketStatistics) ((Value instanceof BucketStatistics) ? Value : null);
            if (stats != null)
            {
                stats.Clear();
            }
        }
    }

    @Override
    public CacheInsResult InsertInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation, CacheEntry oldEntry, OperationContext operationContext)
    {
        return null;
    }

    @Override
    public CacheEntry RemoveInternal(Object key, ItemRemoveReason removalReason, boolean isUserOperation, OperationContext operationContext)
    {
        return null;
    }

    @Override
    public CacheEntry GetInternal(Object key, boolean isUserOperation, OperationContext operationContext)
    {
        return null;
    }

    @Override
    public boolean ContainsInternal(Object key)throws StateTransferException, CacheException
    {
        int bucketId = GetBucketId(key);
        if (IsBucketTransfered(bucketId))
        {
            throw new StateTransferException("I am no more the owner of this bucket");
        }

        return super.ContainsInternal(key);
    }
}
