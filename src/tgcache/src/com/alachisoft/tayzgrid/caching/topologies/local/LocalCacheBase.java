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

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.DataSourceUpdateOptions;
import com.alachisoft.tayzgrid.caching.EventCacheEntry;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.EventContextFieldName;
import com.alachisoft.tayzgrid.caching.EventId;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.MetaInformation;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;

import com.alachisoft.tayzgrid.caching.OperationID;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;

import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.queries.DeleteQueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.QueryType;
import com.alachisoft.tayzgrid.caching.queries.filters.Predicate;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.CacheSyncWrapper;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.caching.util.ParserHelper;
import com.alachisoft.tayzgrid.caching.util.QueryIdentifier;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.IDisposable;

import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.locking.LockMode;
import com.alachisoft.tayzgrid.common.stats.HPTime;

import com.alachisoft.tayzgrid.mapreduce.MapReduceOperation;
import com.alachisoft.tayzgrid.mapreduce.TaskManager;

import com.alachisoft.tayzgrid.parser.ParseMessage;
import com.alachisoft.tayzgrid.parser.Reduction;
import com.alachisoft.tayzgrid.persistence.EventType;


import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;

import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.ParserException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class LocalCacheBase extends CacheBase {

    protected CacheStatistics _stats;
    public boolean _allowAsyncEviction = true;
    protected CacheBase _parentCache;
    private java.util.HashMap _preparedQueryTable = new java.util.HashMap();
    private int _preparedQueryTableSize = 1000;
    private int _preparedQueryEvictionPercentage = 10;
    private java.util.HashMap _stateTransferKeyList;
    private com.alachisoft.tayzgrid.mapreduce.TaskManager taskManager;
    int insertCounter = 0;
    int removeCounter = 0;

    public LocalCacheBase(java.util.Map properties, CacheBase parentCache, ICacheEventsListener listener, CacheRuntimeContext context) {
        super(properties, listener, context);

        if (ServicePropValues.preparedQueryTableSize != null) {
            _preparedQueryTableSize = Integer.decode(ServicePropValues.preparedQueryTableSize);
        }
        if (ServicePropValues.preparedQueryEvictionPercentage != null) {
            _preparedQueryEvictionPercentage = Integer.decode(ServicePropValues.preparedQueryEvictionPercentage);
        }
        _stats = new CacheStatistics();
        _parentCache = parentCache;
        taskManager = new TaskManager(properties, context);
    }

    public LocalCacheBase() {
    }

    @Override
    public void dispose() {
        _stats = null;
        super.dispose();
    }

    @Override
    public CacheStatistics getStatistics() {
        Object tempVar = _stats.clone();
        return (CacheStatistics) ((tempVar instanceof CacheStatistics) ? tempVar : null);
    }

    @Override
    public CacheStatistics getActualStats() {
        return _stats;
    }

    @Override
    public CacheBase getInternalCache() {
        return this;
    }

    protected final boolean getIsSelfInternal() {
        if (_context.getCacheInternal() instanceof CacheSyncWrapper) {
            return this.equals(((CacheSyncWrapper) _context.getCacheInternal()).getInternal());
        }
        return this.equals(_context.getCacheInternal());
    }

    public final java.util.HashMap getPreparedQueryTable() {
        return _preparedQueryTable;
    }

    @Override
    public final void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        ClearInternal();
        
        // generate event id
        EventContext eventContext = null;
        EventId eventId = null;

        if (getIsSelfInternal()) {
            _context.ExpiryMgr.Clear();

            if (_context.PerfStatsColl != null) {
                _context.PerfStatsColl.SetExpirationIndexSize(_context.ExpiryMgr.getIndexInMemorySize());
            }

            _context.PerfStatsColl.setCountStats((long) getCount());
            _context.PerfStatsColl.setCacheSizeStats((long) getSize());
        }

        _stats.UpdateCount(this.getCount());

        OperationID opId = operationContext.getOperatoinID();
        eventContext = new EventContext();
        eventId = EventId.CreateEventId(opId);
        eventId.setEventType(EventType.CACHE_CLEARED_EVENT);
        eventContext.Add(EventContextFieldName.EventID, eventId);

        if (_context.PerfStatsColl != null) {
            _context.PerfStatsColl.setCacheSizeStats(getSize());
        }

        NotifyCacheCleared(false, operationContext, eventContext);
    }

    @Override
    public final boolean Contains(Object key, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        try {
            if (ContainsInternal(key)) {
                CacheEntry e = GetInternal(key, true, operationContext);
                if (e == null) {
                    return false;
                }
                if (e.getExpirationHint() != null && e.getExpirationHint().CheckExpired(_context)) {
                    Remove(key, ItemRemoveReason.Expired, true, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                    return false;
                }
                return true;
            }
            return false;
        } catch (StateTransferException se) {
            throw new OperationFailedException(se);
        }
    }

    @Override
    public LockOptions Lock(Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        LockOptions lockInfo = new LockOptions();

        CacheEntry e = null;

        e = GetInternal(key, false, operationContext);

        if (e != null) {
            e.Lock(lockExpiration, lockId, lockDate);
            lockInfo.setLockDate(lockDate.argvalue);
            lockInfo.setLockId(lockId.argvalue);
            return lockInfo;
        } else {
            lockInfo.setLockId(lockId.argvalue = null);
            return lockInfo;
        }
    }

    @Override
    public LockOptions IsLocked(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        LockOptions lockInfo = new LockOptions();
        CacheEntry e = null;

        e = GetInternal(key, false, operationContext);

        if (e != null) {
            e.IsLocked(lockId, lockDate);
            lockInfo.setLockDate(lockDate.argvalue);
            lockInfo.setLockId(lockId.argvalue);
            return lockInfo;
        } else {
            lockId.argvalue = null;
            return lockInfo;
        }
    }

    @Override
    public void UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        Object tmpLockId = null;
        java.util.Date tmpLockDate = new java.util.Date();
        CacheEntry e = null;

        e = GetInternal(key, false, operationContext);

        if (e != null) {
            if (isPreemptive) {
                e.ReleaseLock();
            } else {
                if (e.CompareLock(lockId)) {
                    e.ReleaseLock();
                }
            }
        }
    }

    @Override
    public void UpdateLockInfo(Object key, Object lockId, java.util.Date lockDate, LockExpiration lockExpiration, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        CacheEntry entry = null;

        entry = GetInternal(key, false, operationContext);

        if (entry != null) {
            entry.CopyLock(lockId, lockDate, lockExpiration);
        }
    }

    @Override
    public final CacheEntry Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Get(key, true, version, lockId, lockDate, lockExpiration, accessType, operationContext);
    }

    @Override
    public java.util.HashMap GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, GeneralFailureException, StateTransferException, CacheException, LockingException, GeneralFailureException {
        return GetFromCache(GetGroupKeys(group, subGroup, operationContext), operationContext);
    }

    @Override
    public java.util.HashMap GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException, LockingException, GeneralFailureException {
        return GetFromCache(GetTagKeys(tags, comparisonType, operationContext), operationContext);
    }

    private java.util.HashMap GetFromCache(java.util.ArrayList keys, OperationContext operationContext) throws OperationFailedException {
        if (keys == null) {
            return null;
        }
        return GetEntries(keys.toArray(new Object[0]), operationContext);
    }

    @Override
    public java.util.HashMap Remove(String[] tags, TagComparisonType tagComparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return RemoveFromCache(GetTagKeys(tags, tagComparisonType, operationContext), notify, operationContext);
    }

    private java.util.HashMap RemoveFromCache(java.util.ArrayList keys, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (keys == null) {
            return null;
        }

        return Remove(keys.toArray(new Object[0]), ItemRemoveReason.Removed, notify, operationContext);

    }

    @Override
    public final CacheEntry Get(Object key, boolean isUserOperation, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws LockingException, OperationFailedException, GeneralFailureException, CacheException {
        CacheEntry e = null;

        e = GetInternal(key, isUserOperation, operationContext);

        if (accessType != LockAccessType.IGNORE_LOCK) {
            if (e != null) {
                if (accessType == LockAccessType.DONT_ACQUIRE) {
                    boolean success = e.CompareLock(lockId.argvalue);
                    if (success) {
                        //explicitly set the lockdate incase of compare lock.
                        //compare lock does not set the lockdate.
                        lockDate.argvalue = e.getLockDate();
                    } else {
                        success = !e.IsLocked(lockId, lockDate);
                    }

                    if (!success) {
                        e = null;
                    }

                } else if (accessType == LockAccessType.ACQUIRE && !e.Lock(lockExpiration, lockId, lockDate)) //internally sets the out parameters
                {
                    e = null;
                } else if (accessType == LockAccessType.GET_VERSION) {
                    version.argvalue = e.getVersion();
                } else if (accessType == LockAccessType.COMPARE_VERSION) {
                    if (e.IsNewer(version.argvalue)) {
                        version.argvalue = e.getVersion();
                    } else {
                        version.argvalue = 0L;
                        e = null;
                    }
                } else if (accessType == LockAccessType.MATCH_VERSION) {
                    if (!e.CompareVersion(version.argvalue)) {
                        e = null;
                    }
                }
            } else {
                lockId.argvalue = null;
            }
        }

        ExpirationHint exh = (e == null ? null : e.getExpirationHint());

        if (exh != null) {
            if (exh.CheckExpired(_context)) {
                // If cache forward is set we skip the expiration.
                if (!exh.getNeedsReSync()) {
                    Remove(key, ItemRemoveReason.Expired, true, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                    e = null;
                }
            }

            if (exh.getIsVariant() && isUserOperation) {
                try {
                    _context.ExpiryMgr.ResetVariant(exh);
                } catch (Exception ex) {
                    RemoveInternal(key, ItemRemoveReason.Removed, false, operationContext);
                    throw new OperationFailedException(ex);
                }
            }
        }

        _stats.UpdateCount(this.getCount());
        if (e != null) {
            _stats.BumpHitCount();
        } else {
            _stats.BumpMissCount();
        }

        return e;
    }

    @Override
    public int GetItemSize(Object key) {
        return 0;
    }

    @Override
    public final CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Add(key, cacheEntry, notify, true, operationContext);
    }

    @Override
    public final CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException, StateTransferException {
        CacheAddResult result = CacheAddResult.Failure;

        try {
            if (_stateTransferKeyList != null && _stateTransferKeyList.containsKey(key) && notify) {
                return CacheAddResult.KeyExists;
            }

           

           

            result = AddInternal(key, cacheEntry, isUserOperation);

           

            // Not enough space, evict and try again.
            if (result == CacheAddResult.NeedsEviction || result == CacheAddResult.SuccessNearEviction) {
                
                    Evict();
             

                if (result == CacheAddResult.SuccessNearEviction) {
                    result = CacheAddResult.Success;
                }
            }

            //  This code should be added to allow the user
            // to add a key value pair that has expired.
            if (result == CacheAddResult.KeyExists) {
                
                CacheEntry e = null;

                e = GetInternal(key, isUserOperation, operationContext);

                if (e.getExpirationHint() != null && e.getExpirationHint().CheckExpired(_context)) {
                    Remove(key, ItemRemoveReason.Expired, true, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                }
            }

            // Operation completed!
            if (result == CacheAddResult.Success) {
                if (cacheEntry.getExpirationHint() != null) {
                    cacheEntry.getExpirationHint().setCacheKey(key);

                    try {
                        _context.ExpiryMgr.ResetHint(null, cacheEntry.getExpirationHint()); //:muds
                    } catch (Exception e) {
                        RemoveInternal(key, ItemRemoveReason.Removed, false, operationContext);
                        throw new OperationFailedException(e);
                    }
                    _context.ExpiryMgr.UpdateIndex(key, cacheEntry);
                }

                if (getIsSelfInternal()) {
                    _context.PerfStatsColl.setCountStats((long) getCount());
                    _context.PerfStatsColl.setCacheSizeStats((long) getSize());
                }

                EventId eventId = null;
                EventContext eventContext = null;
                OperationID opId = operationContext.getOperatoinID();

                if (notify) {
                    //generate event id
                    if (operationContext != null) {
                        if (!operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
                        {
                            eventId = EventId.CreateEventId(opId);
                        } else //for bulk
                        {
                            eventId = ((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID();
                        }

                        eventId.setEventType(EventType.ITEM_ADDED_EVENT);
                        eventContext = new EventContext();
                        eventContext.Add(EventContextFieldName.EventID, eventId);
                        eventContext.setItem(CacheHelper.CreateCacheEventEntry(EventDataFilter.DataWithMetaData, cacheEntry));
                    }

                    NotifyItemAdded(key, false, (OperationContext) operationContext.clone(), eventContext);
                }
            }

            _stats.UpdateCount(this.getCount());

            
        } finally {
            cacheEntry.setMetaInformation(null);
        }
        if (_context.PerfStatsColl != null) {
            _context.PerfStatsColl.setCacheSizeStats(getSize());
        }

        return result;
    }

    @Override
    public final boolean Add(Object key, ExpirationHint eh, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException, StateTransferException {
        if (eh == null) {
            return false;
        }
        boolean result = false;
        CacheEntry entry = null;

        entry = GetInternal(key, false, operationContext);
        try {
            result = AddInternal(key, eh, operationContext);
        } catch (ClassNotFoundException e) {
            throw new OperationFailedException(e);
        }

        if (result) {
            eh.setCacheKey(key);
            if (!eh.Reset(_context)) {
                RemoveInternal(key, eh);
                throw new OperationFailedException("Unable to initialize expiration hint");
            }
           
            _context.ExpiryMgr.UpdateIndex(key, entry.getExpirationHint());
        }

        if (_context.PerfStatsColl != null) {
            _context.PerfStatsColl.setCacheSizeStats(getSize());
        }

        return result;
    }

   

    @Override
    public final CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Insert(key, cacheEntry, notify, true, lockId, version, accessType, operationContext);
    }

    @Override
    public void SetStateTransferKeyList(java.util.HashMap keylist) {
        _stateTransferKeyList = keylist;
    }

    @Override
    public void UnSetStateTransferKeyList() {
        _stateTransferKeyList = null;
    }

    @Override
    public final CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, boolean isUserOperation, Object lockId, long version, LockAccessType access, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException, StateTransferException {
        CacheInsResultWithEntry result = new CacheInsResultWithEntry();
        boolean extendExpiry = false;
        try {
            int maxEvict = 3;
            CacheEntry pe = null;
            CallbackEntry cbEtnry = null;
            OperationID opId = operationContext.getOperatoinID();
            EventId eventId = null;
            EventContext eventContext = null;

            {

                pe = GetInternal(key, false, operationContext);

                result.setEntry(pe);

               

                if (pe != null && access != LockAccessType.IGNORE_LOCK) {
                    if (access == LockAccessType.COMPARE_VERSION) {
                        if (!pe.CompareVersion(version)) {
                            result.setResult(CacheInsResult.VersionMismatch);
                            result.setEntry(null);
                            return result;
                        }
                    } else {
                        if (access == LockAccessType.RELEASE || access == LockAccessType.DONT_RELEASE) {
                            if (pe.IsItemLocked() && !pe.CompareLock(lockId)) {
                                result.setResult(CacheInsResult.ItemLocked);
                                result.setEntry(null);
                                return result;
                            }
                        }
                        if (access == LockAccessType.DONT_RELEASE) {
                            cacheEntry.CopyLock(pe.getLockId(), pe.getLockDate(), pe.getLockExpiration());
                        } else {
                            cacheEntry.ReleaseLock();
                        }
                    }
                }
                ExpirationHint peExh = pe == null ? null : pe.getExpirationHint();

                if (pe != null && pe.getValue() instanceof CallbackEntry) {
                    Object tempVar = pe.getValue();
                    cbEtnry = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);
                    cacheEntry = CacheHelper.MergeEntries(pe, cacheEntry);
                }

              

                if (access == LockAccessType.PRESERVE_VERSION) {
                    cacheEntry.setVersion(version);
                    isUserOperation = false;
                }

                try {
                    result.setResult(InsertInternal(key, cacheEntry, isUserOperation, pe, operationContext));

                    if ((result.getResult() == CacheInsResult.Success || result.getResult() == CacheInsResult.SuccessNearEvicition) && _stateTransferKeyList != null
                            && _stateTransferKeyList.containsKey(key)) {
                        result.setResult(result.getResult() == CacheInsResult.Success ? CacheInsResult.SuccessOverwrite : CacheInsResult.SuccessOverwriteNearEviction);
                    }
                } catch (ClassNotFoundException classNotFoundExcepton) {
                    throw new OperationFailedException(classNotFoundExcepton);
                }

                // There was a failure so we must stop further operation
                if (result.getResult() == CacheInsResult.Failure || result.getResult() == CacheInsResult.IncompatibleGroup) {
                   
                }

                // Not enough space, evict and try again.
                if (result.getResult() == CacheInsResult.NeedsEviction || result.getResult() == CacheInsResult.SuccessNearEvicition || result.getResult()
                        == CacheInsResult.SuccessOverwriteNearEviction) {
                    Evict();
                    if (result.getResult() == CacheInsResult.SuccessNearEvicition) {
                        result.setResult(CacheInsResult.Success);
                    }
                    if (result.getResult() == CacheInsResult.SuccessOverwriteNearEviction) {
                        result.setResult(CacheInsResult.SuccessOverwrite);
                    }

                }
                if (operationContext.Contains(OperationContextFieldName.ExtendExpiry) && operationContext.GetValueByField(OperationContextFieldName.ExtendExpiry) instanceof Boolean) {
                    extendExpiry = (Boolean) operationContext.GetValueByField(OperationContextFieldName.ExtendExpiry);
                }
                // Operation completed!
                if (result.getResult() == CacheInsResult.Success || result.getResult() == CacheInsResult.SuccessOverwrite || extendExpiry) {
                    //remove the old hint from expiry index.
                    if (peExh != null) {
                        _context.ExpiryMgr.RemoveFromIndex(key);
                        //to dispose old hint in case item is resync without dependency
                        peExh.dispose();
                    }
                    if (cacheEntry.getExpirationHint() != null) {
                        cacheEntry.getExpirationHint().setCacheKey(key);
                        if (isUserOperation) {
                            try {
                                _context.ExpiryMgr.ResetHint(peExh, cacheEntry.getExpirationHint());
                            } catch (Exception e) {

                                RemoveInternal(key, ItemRemoveReason.Removed, false, operationContext);
                                throw new OperationFailedException(e);

                            }
                        } else {
                            cacheEntry.getExpirationHint().ReInitializeHint(getContext());
                        }

                        _context.ExpiryMgr.UpdateIndex(key, cacheEntry);
                    }
                    if (!extendExpiry) {
                        if (getIsSelfInternal()) {
                            _context.PerfStatsColl.setCountStats((long) getCount());
                            _context.PerfStatsColl.setCacheSizeStats((long) getSize());
                        }
                    }
                }
            }

            if (result.getResult() == CacheInsResult.NeedsEviction || result.getResult() == CacheInsResult.NeedsEvictionNotRemove) {
              
            }
            EventCacheEntry eventCacheEntry = null;
            EventCacheEntry oldEventCacheEntry = null;
            _stats.UpdateCount(this.getCount());

            switch (result.getResult()) {
                case Success:
                    if (notify) {
                        //generate event id
                        if (!operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
                        {
                            eventId = EventId.CreateEventId(opId);
                            eventContext = new EventContext();
                        } else //for bulk
                        {
                            eventId = ((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID();
                        }

                        eventContext = new EventContext();
                        eventId.setEventType(EventType.ITEM_ADDED_EVENT);
                        eventContext.Add(EventContextFieldName.EventID, eventId);
                        eventContext.setItem(CacheHelper.CreateCacheEventEntry(EventDataFilter.DataWithMetaData, cacheEntry));
                        NotifyItemAdded(key, false, (OperationContext) operationContext.clone(), eventContext);
                    }
                    break;
                case SuccessOverwrite:
                    if (notify) {

                        eventCacheEntry = CacheHelper.CreateCacheEventEntry(EventDataFilter.DataWithMetaData, cacheEntry);
                        oldEventCacheEntry = CacheHelper.CreateCacheEventEntry(EventDataFilter.DataWithMetaData, pe);

                        if (cbEtnry != null) {
                            if (cbEtnry.getItemUpdateCallbackListener() != null && cbEtnry.getItemUpdateCallbackListener().size() > 0) {
                                if (!operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
                                {
                                    eventId = EventId.CreateEventId(opId);
                                    eventContext = new EventContext();
                                } else //for bulk
                                {
                                    eventId = ((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID();
                                }

                                eventContext = new EventContext();
                                eventId.setEventType(EventType.ITEM_UPDATED_CALLBACK);
                                eventContext.Add(EventContextFieldName.EventID, eventId);
                                eventContext.setItem(eventCacheEntry);
                                eventContext.setOldItem(oldEventCacheEntry);

                                NotifyCustomUpdateCallback(key, cbEtnry.getItemUpdateCallbackListener(), false, (OperationContext) operationContext.clone(), eventContext);
                            }
                        }

                        //generate event id
                        if (!operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
                        {
                            eventId = EventId.CreateEventId(opId);
                            eventContext = new EventContext();
                        } else //for bulk
                        {
                            eventId = ((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID();
                        }

                        eventContext = new EventContext();
                        eventId.setEventType(EventType.ITEM_UPDATED_EVENT);
                        eventContext.Add(EventContextFieldName.EventID, eventId);
                        eventContext.setItem(eventCacheEntry);
                        eventContext.setOldItem(oldEventCacheEntry);

                        NotifyItemUpdated(key, false, (OperationContext) operationContext.clone(), eventContext);
                    }
                    break;
            }
        } finally {
            cacheEntry.setMetaInformation(null);
        }

        if (_context.PerfStatsColl != null) {
            _context.PerfStatsColl.setCacheSizeStats(getSize());
        }

        return result;
    }

    @Override
    public final CacheEntry Remove(Object key, ItemRemoveReason removalReason, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Remove(key, removalReason, notify, true, lockId, version, accessType, operationContext);
    }

    @Override
    public CacheEntry Remove(Object key, ItemRemoveReason removalReason, boolean notify, boolean isUserOperation, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        CacheEntry e = null;
        CacheEntry pe = null;
        CacheEntry eResetExp = null;
        boolean resetExpiry = false;
        {
            Object actualKey = key;
            if (key instanceof Object[]) {
                actualKey = ((Object[]) key)[0];
            }

            if (accessType == LockAccessType.COMPARE_VERSION) {
                pe = GetInternal(actualKey, false, operationContext);

                if (pe != null) {
                    if (!pe.CompareVersion(version)) {
                        throw new LockingException("Item in the cache does not exist at the specified version.");
                    }
                }
            } else if (accessType != LockAccessType.IGNORE_LOCK) {
                pe = GetInternal(actualKey, false, operationContext);

                if (pe != null) {
                    if (pe.IsItemLocked() && !pe.CompareLock(lockId)) {
                        throw new LockingException("Item is locked.");
                    }
                }
            }
          

            e = RemoveInternal(actualKey, removalReason, isUserOperation, operationContext);

            if (operationContext.Contains(OperationContextFieldName.ExtendExpiry) && operationContext.GetValueByField(OperationContextFieldName.ExtendExpiry) instanceof CacheEntry) {
                eResetExp = (CacheEntry) operationContext.GetValueByField(OperationContextFieldName.ExtendExpiry);
                _context.ExpiryMgr.ResetHint(eResetExp.getExpirationHint(), eResetExp.getExpirationHint());
//                if (eResetExp.getExpirationHint() != null) {
//                    _context.ExpiryMgr.RemoveFromIndex(key);
//                }

            }

                boolean resyncExpiredItems = false;


                if (e != null && e.getExpirationHint() != null) {
                    resyncExpiredItems = e.getExpirationHint().getNeedsReSync();
                }

                EventId eventId = null;
                EventContext eventContext = null;
                OperationID opId = operationContext.getOperatoinID();

                if (e != null) {
                        if (_stateTransferKeyList != null && _stateTransferKeyList.containsKey(key)) {
                            _stateTransferKeyList.remove(key);
                        }

                    try {
                            _context.ExpiryMgr.ResetHint(e.getExpirationHint(), null);
                            if (e.getExpirationHint() != null)
                                _context.ExpiryMgr.RemoveFromIndex(key);

                    } catch (Exception ex) {
                        getCacheLog().Error("LocalCacheBase.Remove(object, ItemRemovedReason, bool):", ex.toString());
                    }
                       

                        if (getIsSelfInternal()) {
                            // Disposed the one and only cache entry.
                            ((IDisposable) e).dispose();

                            if (!_context.getIsDbSyncCoordinator() && (removalReason == ItemRemoveReason.Expired )) {
                                _context.PerfStatsColl.incrementExpiryPerSecStats();
                            } else if (!_context.getCacheImpl().getIsEvictionAllowed() && removalReason == ItemRemoveReason.Underused) {
                                _context.PerfStatsColl.incrementEvictPerSecStats();
                            }
                            _context.PerfStatsColl.setCountStats((long) getCount());
                            _context.PerfStatsColl.setCacheSizeStats((long) getSize());
                        }

                        if (notify) {
                            Object tempVar = e.getValue();
                            CallbackEntry cbEtnry = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null); // e.DeflattedValue(_context.SerializationContext);

                            if (cbEtnry != null && cbEtnry.getItemRemoveCallbackListener() != null && cbEtnry.getItemRemoveCallbackListener().size() > 0) {
                                try {
                                    //generate event id
                                    if (!operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
                                    {
                                        eventId = EventId.CreateEventId(opId);
                                    } else //for bulk
                                    {
                                        eventId = ((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID();
                                    }

                                    eventId.setEventType(EventType.ITEM_REMOVED_CALLBACK);
                                    eventContext = new EventContext();
                                    eventContext.Add(EventContextFieldName.EventID, eventId);
                                    EventCacheEntry eventCacheEntry = CacheHelper.CreateCacheEventEntry(cbEtnry.getItemRemoveCallbackListener(), e);
                                    if (eventCacheEntry != null) {
                                        eventCacheEntry.setReSyncExpiredItems(resyncExpiredItems);
                                    }
                                    eventContext.setItem(eventCacheEntry);
                                    eventContext.Add(EventContextFieldName.ItemRemoveCallbackList, new ArrayList(cbEtnry.getItemRemoveCallbackListener()));

                                    //Will always reaise the whole entry for old clients
                                    NotifyCustomRemoveCallback(actualKey, e, removalReason, false, (OperationContext) operationContext.clone(), eventContext);

                                } catch (ClassNotFoundException classNotFoundException) {
                                    throw new OperationFailedException(classNotFoundException);
                                }
                            }

                            //generate event id
                            if (!operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
                            {
                                eventId = EventId.CreateEventId(opId);
                            } else //for bulk
                            {
                                eventId = ((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID();
                            }

                            eventId.setEventType(EventType.ITEM_REMOVED_EVENT);
                            eventContext = new EventContext();
                            eventContext.Add(EventContextFieldName.EventID, eventId);
                            eventContext.Add(EventContextFieldName.EventID, eventId);
                            eventContext.setItem(CacheHelper.CreateCacheEventEntry(EventDataFilter.DataWithMetaData, e));
                            //Will always reaise the whole entry for old clients
                            NotifyItemRemoved(actualKey, e, removalReason, false, (OperationContext) operationContext.clone(), eventContext);
                        }

                        //Empty MetaInformation is passed with Type because in Remove only Type is needed.
                        MetaInformation metaInfo = new MetaInformation(null);
                        metaInfo.setType(e.getObjectType());
                } else if (_stateTransferKeyList != null && _stateTransferKeyList.containsKey(key)) {
                    try {
                        _stateTransferKeyList.remove(key);
                    } catch (Exception exception) {
                        _context.getCacheLog().Error("LocalCacheBase.Add", exception.getMessage());
                    }
                }

            }
            _stats.UpdateCount(this.getCount());

            if (_context.PerfStatsColl != null) {
                _context.PerfStatsColl.setCacheSizeStats(getSize());
            }

            return e;
        }

        @Override
        public Object RemoveSync
        (Object[] keys, ItemRemoveReason reason
        , boolean notify, OperationContext operationContext
        ) throws OperationFailedException
        , LockingException
        , GeneralFailureException
        , CacheException {
            if (_parentCache != null) {
                return _parentCache.RemoveSync(keys, reason, notify, operationContext);
            }
            return null;
        }

        @Override
        public final void SendNotification
        (Object notifId, Object data
        
            ) {
        super.NotifyCustomEvent(notifId, data, false, null, null);
        }


        @Override
        public final QueryResultSet Search
        (String query, java
        .util.Map values, OperationContext operationContext
        ) throws OperationFailedException
        , LockingException
        , GeneralFailureException
        , StateTransferException {
            try {

                _context.PerfStatsColl.MsecPerQueryExecutionTimeBeginSample();

                QueryContext queryContext = PrepareSearch(query, values);
                switch (queryContext.getResultSet().getType()) {
                    case AggregateFunction:
                        break;

                    default:
                        queryContext.getTree().Reduce();
                        queryContext.setCacheContext(_context.getSerializationContext());
                        queryContext.getResultSet().setSearchKeysResult(queryContext.getTree().getLeftList());
                        break;
                }

                _context.PerfStatsColl.MsecPerQueryExecutionTimeEndSample();

                if (queryContext.getResultSet() != null) {
                    long totalRowReturn = 0;
                    if (queryContext.getResultSet().getSearchEntriesResult() != null) {
                        totalRowReturn = queryContext.getResultSet().getSearchEntriesResult().size();
                    } else if (queryContext.getResultSet().getSearchKeysResult() != null) {
                        totalRowReturn = queryContext.getResultSet().getSearchKeysResult().size();
                    }

                    _context.PerfStatsColl.IncrementAvgQuerySize(totalRowReturn);
                }

                return queryContext.getResultSet();
            } catch (ParserException pe) {
                RemoveReduction(query);
                throw new OperationFailedException(pe.getMessage(), pe);
            } catch (Exception ex) {
                throw new OperationFailedException(ex);
            }
        }

        @Override
        public final QueryResultSet SearchEntries
        (String query, java
        .util.Map values, OperationContext operationContext
        ) throws OperationFailedException
        , LockingException
        , GeneralFailureException
        , StateTransferException {
            try {
                _context.PerfStatsColl.MsecPerQueryExecutionTimeBeginSample();

                QueryContext queryContext = PrepareSearch(query, values);
                switch (queryContext.getResultSet().getType()) {
                    case AggregateFunction:
                        break;
                    case GroupByAggregateFunction:
                        break;

                    default:
                        java.util.HashMap result = new java.util.HashMap();
                        java.util.Collection keyList = null;
                        ArrayList updatekeys = null;
                        queryContext.getTree().Reduce();
                        queryContext.setCacheContext(_context.getSerializationContext());
                        if (queryContext.getTree().getLeftList().size() > 0) {
                            keyList = queryContext.getTree().getLeftList();
                        }

                        if (keyList != null && keyList.size() > 0) {

                            Object[] keys = new Object[keyList.size()];
                            keyList.toArray(keys);

                            java.util.Map tmp = GetEntries(keys, operationContext);
                            Iterator ide = tmp.entrySet().iterator();

                            CompressedValueEntry cmpEntry = null;

                            Map.Entry KeyValue;
                            while (ide.hasNext()) {
                                KeyValue = (Map.Entry) ide.next();
                                Object Key = KeyValue.getKey();
                                Object Value = KeyValue.getValue();
                                CacheEntry entry = (CacheEntry) ((Value instanceof CacheEntry) ? Value : null);
                                if (entry != null) {
                                    cmpEntry = new CompressedValueEntry();
                                    cmpEntry.Value = entry.getValue();
                                    if (cmpEntry.Value instanceof CallbackEntry) {
                                        cmpEntry.Value = ((CallbackEntry) cmpEntry.Value).getValue();
                                    }

                                    cmpEntry.Flag = (BitSet)((CacheEntry) Value).getFlag().Clone();
                                    result.put(Key, cmpEntry);
                                    if ((entry.getExpirationHint() != null && entry.getExpirationHint().getIsVariant())) {
                                        if (updatekeys == null) {
                                            updatekeys = new ArrayList();
                                        }
                                        updatekeys.add(Key);
                                    }
                                }
                            }
                        }

                        queryContext.getResultSet().setType(QueryType.SearchEntries);
                        queryContext.getResultSet().setSearchEntriesResult(result);
                        queryContext.getResultSet().setUpdateIndicesKeys(updatekeys);

                        break;
                }

                _context.PerfStatsColl.MsecPerQueryExecutionTimeEndSample();

                if (queryContext.getResultSet() != null) {

                    long totalRowReturn = 0;

                    if (queryContext.getResultSet().getSearchEntriesResult() != null) {
                        totalRowReturn = queryContext.getResultSet().getSearchEntriesResult().size();
                    } else if (queryContext.getResultSet().getSearchKeysResult() != null) {
                        totalRowReturn = queryContext.getResultSet().getSearchKeysResult().size();
                    }

                    _context.PerfStatsColl.IncrementAvgQuerySize(totalRowReturn);
                }
                _context.PerfStatsColl.IncrementQueryPerSec();

                return queryContext.getResultSet();
            } catch (com.alachisoft.tayzgrid.runtime.exceptions.ParserException pe) {
                RemoveReduction(query);
                throw new OperationFailedException(pe.getMessage(), pe);
            } catch (Exception ex) {
                throw new OperationFailedException(ex);
            }
        }

    private QueryContext PrepareSearch(String query, java.util.Map values) throws java.lang.Exception {
        Reduction currentQueryReduction = null;

        try {
            currentQueryReduction = GetPreparedReduction(query);

            ArrayList temp = currentQueryReduction.getTokens();
            String val = temp.get(0).toString();
            String v = val.toLowerCase();
            val = v.trim();
            // character is saved in two byes out of which one has null value
            // in order to comapir the two we have to remove null characters.
            val = RemoveNullCharacters(val);
            if (!val.equals("select")) {
                throw new ParserException("Only select query is supported");
            }

            Object tempVar = currentQueryReduction.getTag();
            return SearchInternal((Predicate) ((tempVar instanceof Predicate) ? tempVar : null), values);
        } catch (com.alachisoft.tayzgrid.runtime.exceptions.ParserException pe) {
            RemoveReduction(query);
            throw new com.alachisoft.tayzgrid.runtime.exceptions.ParserException(pe.getMessage(), pe);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private String RemoveNullCharacters(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != 0) {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    @Override
    public final DeleteQueryResultSet DeleteQuery(String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception, ParserException {
        java.util.HashMap result = new java.util.HashMap();
        java.util.ArrayList keysToBeRemoved;

        try {
            QueryContext queryContext = PrepareDeleteQuery(query, values);
            queryContext.getTree().Reduce();
            queryContext.setCacheContext(_context.getSerializationContext());

            keysToBeRemoved = queryContext.getTree().getLeftList();

            result = Remove(keysToBeRemoved.toArray(new Object[0]), ir, notify, isUserOperation, operationContext);

            DeleteQueryResultSet resultSet = new DeleteQueryResultSet();
            resultSet.setKeysEffectedCount(result.size());
            resultSet.setKeysEffected(result);

            return resultSet;
        } catch (ParserException pe) {
            RemoveReduction(query);
            throw new ParserException(pe.getMessage(), pe);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private QueryContext PrepareDeleteQuery(String query, java.util.Map values)
            throws ParserException, Exception {
        Reduction currentQueryReduction = null;

        try {
            currentQueryReduction = GetPreparedReduction(query);
            Object tempVar = currentQueryReduction.getTag();

            ArrayList temp = currentQueryReduction.getTokens();
            String val = temp.get(0).toString();
            String v = val.toLowerCase();
            val = v.trim();
            // character is saved in two byes out of which one has null value
            // in order to comapir the two we have to remove null characters.
            val = RemoveNullCharacters(val);

            if (!val.toLowerCase().equals("delete")) {
                throw new ParserException("Execute Non Query only supports delete query");
            }

            return DeleteQueryInternal((Predicate) ((tempVar instanceof Predicate) ? tempVar : null), values);
        } catch (ParserException pe) {
            RemoveReduction(query);
            throw new ParserException(pe.getMessage(), pe);
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    

    @Override
    public final java.util.HashMap Contains(Object[] keys, OperationContext operationContext) throws LockingException, OperationFailedException, CacheException, GeneralFailureException {
        java.util.HashMap tbl = new java.util.HashMap();
        java.util.ArrayList successfulKeys = new java.util.ArrayList();
        java.util.ArrayList failedKeys = new java.util.ArrayList();

        for (int i = 0; i < keys.length; i++) {
            try {
                boolean result = Contains(keys[i], operationContext);
                if (result) {
                    successfulKeys.add(keys[i]);
                }
            } catch (StateTransferException se) {
                failedKeys.add(keys[i]);
            }
        }

        if (successfulKeys.size() > 0) {
            tbl.put("items-found", successfulKeys);
        }
        if (failedKeys.size() > 0) {
            tbl.put("items-transfered", failedKeys);
        }

        return tbl;
    }

    @Override
    public final java.util.HashMap Get(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap entries = new java.util.HashMap();
        CacheEntry e = null;
        boolean isJCacheLoader = false;
        for (int i = 0; i < keys.length; i++) {
            try {
                if (operationContext != null) {
                    operationContext.RemoveValueByField(OperationContextFieldName.EventContext);
                    OperationID opId = operationContext.getOperatoinID();
                    //generate EventId
                    EventId eventId = EventId.CreateEventId(opId);
                    eventId.setEventUniqueID(opId.getOperationId());
                    eventId.setOperationCounter(opId.getOpCounter());
                    eventId.setEventCounter(i);
                    EventContext eventContext = new EventContext();
                    eventContext.Add(EventContextFieldName.EventID, eventId);
                    operationContext.Add(OperationContextFieldName.EventContext, eventContext);
                    if (operationContext.Contains(OperationContextFieldName.JCacheLoader)) {
                        isJCacheLoader = (Boolean) operationContext.GetValueByField(OperationContextFieldName.JCacheLoader);
                    }
                }

                e = Get(keys[i], operationContext);
                if (e != null) {
                    if (isJCacheLoader) {
                        entries.put(keys[i], null);
                    } else {
                        entries.put(keys[i], e);
                    }
                }
            } catch (StateTransferException se) {
                entries.put(keys[i], se);
            }
        }
        return entries;
    }

    private java.util.HashMap GetEntries(Object[] keys, OperationContext operationContext) throws OperationFailedException {
        java.util.HashMap entries = new java.util.HashMap();
        CacheEntry e = null;

        for (int i = 0; i < keys.length; i++) {
            try {
                e = GetEntryInternal(keys[i], true);
                if (e != null) {
                    ExpirationHint exh = e.getExpirationHint();
                    if (exh != null) {
                        if (exh.CheckExpired(_context)) {
                            // If cache forward is set we skip the expiration.
                            if (!exh.getNeedsReSync()) {
                                Remove(keys[i], ItemRemoveReason.Expired, true, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                                e = null;
                            }
                        }

                        if (exh.getIsVariant()) {
                            try {
                                _context.ExpiryMgr.ResetVariant(exh);
                            } catch (Exception ex) {
                                RemoveInternal(keys[i], ItemRemoveReason.Removed, false, operationContext);
                                throw ex;
                            }
                        }
                    }
                    entries.put(keys[i], e);
                }
            } catch (Exception ex) {
                entries.put(keys[i], ex);
            }
        }

        return entries;
    }

    @Override
    public final java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {
        java.util.HashMap table = new java.util.HashMap();
        EventContext eventContext = null;
        EventId eventId = null;
        OperationID opId = operationContext.getOperatoinID();

        for (int i = 0; i < keys.length; i++) {
            try {
               
                operationContext.RemoveValueByField(OperationContextFieldName.EventContext);
                if (notify) {
                    //generate EventId
                    eventId = new EventId();
                    eventId.setEventUniqueID(opId.getOperationId());
                    eventId.setOperationCounter(opId.getOpCounter());
                    eventId.setEventCounter(i);
                    eventContext = new EventContext();
                    eventContext.Add(EventContextFieldName.EventID, eventId);
                    operationContext.Add(OperationContextFieldName.EventContext, eventContext);
                }

                CacheAddResult result = Add(keys[i], cacheEntries[i], notify, operationContext);
                table.put(keys[i], result);
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                table.put(keys[i], se);
            } catch (Exception inner) {
                table.put(keys[i], new OperationFailedException(inner.getMessage(), inner));
            } finally {
                operationContext.RemoveValueByField(OperationContextFieldName.EventContext);
            }
        }

        if (_context.PerfStatsColl != null) {
            _context.PerfStatsColl.setCacheSizeStats(getSize());
        }

        return table;
    }

    @Override
    public final java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {
        java.util.HashMap table = new java.util.HashMap();

        EventContext eventContext = null;
        EventId eventId = null;
        OperationID opId = operationContext.getOperatoinID();

        for (int i = 0; i < keys.length; i++) {
            try {
               
                operationContext.RemoveValueByField(OperationContextFieldName.EventContext);
                if (notify) {
                    //generate EventId
                    eventId = new EventId();
                    eventId.setEventUniqueID(opId.getOperationId());
                    eventId.setOperationCounter(opId.getOpCounter());
                    eventId.setEventCounter(i);
                    eventContext = new EventContext();
                    eventContext.Add(EventContextFieldName.EventID, eventId);
                    operationContext.Add(OperationContextFieldName.EventContext, eventContext);
                }

                CacheInsResultWithEntry result = Insert(keys[i], cacheEntries[i], notify, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                table.put(keys[i], result);
            } catch (Exception e) {
                table.put(keys[i], e);
            } finally {
                operationContext.RemoveValueByField(OperationContextFieldName.EventContext);
            }
        }

        if (_context.PerfStatsColl != null) {
            _context.PerfStatsColl.setCacheSizeStats(getSize());
        }

        return table;
    }

    @Override
    public final java.util.HashMap Remove(Object[] keys, ItemRemoveReason removalReason, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        return Remove(keys, removalReason, notify, true, operationContext);
    }

    @Override
    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason removalReason, boolean notify, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap table = new java.util.HashMap();

        EventContext eventContext = null;
        EventId eventId = null;
        OperationID opId = operationContext.getOperatoinID();

        for (int i = 0; i < keys.length; i++) {
            try {
                operationContext.RemoveValueByField(OperationContextFieldName.EventContext);
                if (notify) {
                    //generate EventId
                    eventId = new EventId();
                    eventId.setEventUniqueID(opId.getOperationId());
                    eventId.setOperationCounter(opId.getOpCounter());
                    eventId.setEventCounter(i);
                    eventContext = new EventContext();
                    eventContext.Add(EventContextFieldName.EventID, eventId);
                    operationContext.Add(OperationContextFieldName.EventContext, eventContext);
                }
                CacheEntry e = Remove(keys[i], removalReason, notify, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                if (e != null) {
                    table.put(keys[i], e);
                }

            } catch (StateTransferException e) {
                table.put(keys[i], e);
            } finally {
                operationContext.RemoveValueByField(OperationContextFieldName.EventContext);
            }
        }

        if (_context.PerfStatsColl != null) {
            _context.PerfStatsColl.setCacheSizeStats(getSize());
        }

        return table;
    }

    public void ClearInternal() {
    }

    public boolean ContainsInternal(Object key) throws CacheException {
        return false;
    }

    public CacheEntry GetInternal(Object key, boolean isUserOperation, OperationContext operationContext) throws CacheException, OperationFailedException, LockingException {
        return null;
    }

    public CacheAddResult AddInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation) throws CacheException, CacheException {
        return CacheAddResult.Failure;
    }

    public boolean AddInternal(Object key, ExpirationHint eh, OperationContext operationContext) throws CacheException, LockingException, ClassNotFoundException, OperationFailedException {
        return false;
    }

    public boolean RemoveInternal(Object key, ExpirationHint eh) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return false;
    }

  

    public CacheInsResult InsertInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation, CacheEntry oldEntry, OperationContext operationContext) throws CacheException, LockingException, ClassNotFoundException, OperationFailedException, CacheException {
        return CacheInsResult.Failure;
    }

    public CacheEntry RemoveInternal(Object key, ItemRemoveReason removalReason, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    public QueryContext SearchInternal(Predicate pred, java.util.Map values) throws CacheException {
        return null;
    }

    public java.util.Map SearchEntriesInternal(Predicate pred, java.util.Map values) {
        return null;
    }

    public QueryContext DeleteQueryInternal(Predicate pred, java.util.Map values) throws Exception {
        return null;
    }

    public CacheEntry GetEntryInternal(Object key, boolean isUserOperation) throws CacheException {
        return null;
    }

    public void Evict() {
    }

    @Override
    public void RegisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        if (keys != null) {
            for (Object key : keys) {
                if (key != null) {
                    RegisterKeyNotification(key, updateCallback, removeCallback, operationContext);
                }
            }
        }
    }

    @Override
    public void RegisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        CacheEntry entry = Get(key, operationContext);

        if (entry != null) {
           
            entry.AddCallbackInfo(updateCallback, removeCallback);
        }
    }

    @Override
    public void UnregisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (keys != null) {
            for (Object key : keys) {
                UnregisterKeyNotification(key, updateCallback, removeCallback, operationContext);
            }
        }
    }

    @Override
    public void UnregisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        try {

            CacheEntry entry = Get(key, operationContext);
            if (entry != null) {
               
                entry.RemoveCallbackInfo(updateCallback, removeCallback);
            }
        } catch (StateTransferException e) {
            //ignore state transfer expcetion
        }
    }

    private Reduction GetPreparedReduction(String query) throws com.alachisoft.tayzgrid.runtime.exceptions.ParserException {
        Reduction reduction = null;
        synchronized (_preparedQueryTable) {
            if (!_preparedQueryTable.containsKey(query)) {
                ParserHelper parser = new ParserHelper(getInternalCache().getCacheLog());
                if (parser.Parse(query) == ParseMessage.Accept) {
                    reduction = parser.getCurrentReduction();
                    AddPreparedReduction(query, reduction);
                } else {
                    throw new com.alachisoft.tayzgrid.runtime.exceptions.ParserException("Incorrect query format");
                }
            } else {
                reduction = (Reduction) _preparedQueryTable.get(query);
            }
        }
        return reduction;
    }

    private void RemoveReduction(String query) {
        synchronized (_preparedQueryTable) {
            _preparedQueryTable.remove(query);
        }
    }

    private void AddPreparedReduction(String query, Reduction currentReduction) {
        _preparedQueryTable.put(new QueryIdentifier(query), currentReduction);
        if (_preparedQueryTable.size() > _preparedQueryTableSize) {
            java.util.ArrayList list = new java.util.ArrayList(_preparedQueryTable.keySet());

            Collections.sort(list);
            int evictCount = (_preparedQueryTable.size() * _preparedQueryEvictionPercentage) / 100;
            for (int i = 0; i < evictCount; i++) {
                _preparedQueryTable.remove(list.get(i));
            }
        }
    }




    @Override
    public Object TaskOperationRecieved(MapReduceOperation operation) throws OperationFailedException
    {
        if(taskManager!=null)
        {
            return taskManager.TaskOperationRecieved(operation);
        }
        return null;
    }

    @Override
    public void declaredDeadClients(ArrayList clients) {
        if (taskManager != null) {
            taskManager.DeadClients(clients);
        }
    }
}
