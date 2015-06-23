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
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.DataSourceUpdateOptions;
import com.alachisoft.tayzgrid.caching.EventCacheEntry;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.EventContextFieldName;
import com.alachisoft.tayzgrid.caching.EventId;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationID;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.queries.DeleteQueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.caching.util.MiscUtil;
import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.mapreduce.MapReduceOpCodes;
import com.alachisoft.tayzgrid.mapreduce.MapReduceOperation;
import com.alachisoft.tayzgrid.mapreduce.TaskExecutionStatus;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.persistence.EventType;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.NotSupportedException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class LocalCacheImpl extends CacheBase implements ICacheEventsListener {

    private CacheBase _cache;
    public LocalCacheImpl() 
    {
    }
    
    public LocalCacheImpl(CacheBase cache) {
        if (cache == null) {
            throw new IllegalArgumentException("cache");
        }
        _cache = cache;
        _context = cache.getInternalCache().getContext();
    }

    @Override
    public void dispose() {
        if (_cache != null) {
            _cache.dispose();
            _cache = null;
        }
        super.dispose();
    }

    public final CacheBase getInternal() {
        return _cache;
    }

    public final void setInternal(CacheBase value) {
        _cache = value;
        _context = value.getInternalCache().getContext();
    }

    @Override
    public CacheBase getInternalCache() {
        return _cache;
    }

    @Override
    public TypeInfoMap getTypeInfoMap() {
        return _cache.getTypeInfoMap();
    }

    @Override
    public String getName() {
        return getInternal().getName();
    }

    @Override
    public void setName(String value) {
        getInternal().setName(value);
    }

    @Override
    public ICacheEventsListener getListener() {
        return getInternal().getListener();
    }

    @Override
    public void setListener(ICacheEventsListener value) {
        getInternal().setListener(value);
    }

    @Override
    public NotificationWrapper getNotifiers() {
        return getInternal().getNotifiers();
    }

    @Override
    public void setNotifiers(NotificationWrapper value) {
        getInternal().setNotifiers(value);
    }

    @Override
    public long getCount() throws GeneralFailureException, OperationFailedException, CacheException {
        return getInternal().getCount();
    }

    @Override
    public long getSessionCount() {
        return getInternal().getSessionCount();
    }
    
    @Override
    public CacheStatistics getStatistics() {
        return getInternal().getStatistics();
    }

    @Override
    public CacheStatistics getActualStats() {
        return getInternal().getActualStats();
    }

    @Override
    public void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        getInternal().Clear(cbEntry, updateOptions, operationContext);
    }

    @Override
    public boolean Contains(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return getInternal().Contains(key, operationContext);
    }

    @Override
    public java.util.HashMap Contains(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return getInternal().Contains(keys, operationContext);
    }

    @Override
    public CacheEntry Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        CacheEntry entry = getInternal().Get(key, version, lockId, lockDate, lockExpiration, accessType, operationContext);
        if (entry != null && getKeepDeflattedValues()) {
            try {
                entry.KeepDeflattedValue(_context.getSerializationContext());
            } catch (Exception exception) {
                throw new OperationFailedException(exception);
            }
        }
        return entry;
    }

    @Override
    public java.util.HashMap GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return getInternal().GetTagData(tags, comparisonType, operationContext);
    }

    @Override
    public java.util.HashMap Remove(String[] tags, TagComparisonType tagComparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return getInternal().Remove(tags, tagComparisonType, notify, operationContext);
    }

    @Override
    public java.util.ArrayList GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return getInternal().GetTagKeys(tags, comparisonType, operationContext);
    }

    @Override
    public java.util.HashMap Get(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        java.util.HashMap data = getInternal().Get(keys, operationContext);
        if (data != null && getKeepDeflattedValues()) {
            Iterator ide = data.entrySet().iterator();
            CacheEntry entry;

            Map.Entry KeyValue;
            while (ide.hasNext()) {
                KeyValue = (Map.Entry) ide.next();
                Object Key = KeyValue.getKey();
                Object Value = KeyValue.getValue();
                entry = (CacheEntry) ((Value instanceof CacheEntry) ? Value : null);
                if (entry != null) {
                    try {
                        entry.KeepDeflattedValue(_context.getSerializationContext());
                    } catch (Exception exception) {
                        throw new OperationFailedException(exception);
                    }
                }
            }
        }
        return data;
    }

    @Override
    public java.util.ArrayList GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return getInternal().GetGroupKeys(group, subGroup, operationContext);
    }

    @Override
    public CacheEntry GetGroup(Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        CacheEntry entry = getInternal().GetGroup(key, group, subGroup, version, lockId, lockDate, lockExpiration, accessType, operationContext);
        if (entry != null && getKeepDeflattedValues()) {
            try {
                entry.KeepDeflattedValue(_context.getSerializationContext());
            } catch (Exception exception) {
                throw new OperationFailedException(exception);
            }
        }
        return entry;
    }

    @Override
    public com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return getInternal().GetGroupInfo(key, operationContext);
    }

    @Override
    public java.util.HashMap GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return getInternal().GetGroupInfoBulk(keys, operationContext);
    }

    @Override
    public java.util.HashMap GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        java.util.HashMap data = getInternal().GetGroupData(group, subGroup, operationContext);
        if (data != null && getKeepDeflattedValues()) {
            Iterator ide = data.entrySet().iterator();
            CacheEntry entry;
            Map.Entry KeyValue;
            while (ide.hasNext()) {
                KeyValue = (Map.Entry) ide.next();
                Object Value = KeyValue.getValue();
                entry = (CacheEntry) ((Value instanceof CacheEntry) ? Value : null);
                if (entry != null) {
                    try {
                        entry.KeepDeflattedValue(_context.getSerializationContext());
                    } catch (Exception exception) {
                        throw new OperationFailedException(exception);
                    }
                }
            }
        }
        return data;
    }

    @Override
    public java.util.ArrayList getDataGroupList() {
        return getInternal().getDataGroupList();
    }

    @Override
    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException {
        CacheAddResult result = CacheAddResult.Failure;
        if (getInternal() != null) {
            result = getInternal().Add(key, cacheEntry, notify, operationContext);         
        }
        return result;
    }

    @Override
    public boolean Add(Object key, ExpirationHint eh, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        boolean result = false;
        if (getInternal() != null) {
            CacheEntry cacheEntry = new CacheEntry();
            cacheEntry.setExpirationHint(eh);          
         
            result = getInternal().Add(key, eh, operationContext);
            
        }
        return result;
    }



    @Override
    public java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap table = new java.util.HashMap();

        java.util.ArrayList goodKeysList = new java.util.ArrayList();
        java.util.ArrayList badKeysList = new java.util.ArrayList();
        java.util.ArrayList goodEntriesList = new java.util.ArrayList();

        if (getInternal() != null) {
               
            table = getInternal().Add(keys, cacheEntries, notify, operationContext);
           
        }
        return table;
    }

    @Override
    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        CacheInsResultWithEntry retVal = new CacheInsResultWithEntry();
        if (getInternal() != null) {
        
            retVal = getInternal().Insert(key, cacheEntry, notify, lockId, version, accessType, operationContext);
           
        }
        return retVal;
    }

    @Override
    public java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap retVal = null;

        java.util.ArrayList goodKeysList = new java.util.ArrayList();
        java.util.ArrayList goodEntriesList = new java.util.ArrayList();
        java.util.ArrayList badKeysList = new java.util.ArrayList();

        if (getInternal() != null) {
                        
            retVal = getInternal().Insert(keys, cacheEntries, notify, operationContext);
        }
        return retVal;
    }

    @Override
    public Object RemoveSync(Object[] keys, ItemRemoveReason reason, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {
        java.util.ArrayList depenedentItemList = new java.util.ArrayList();
        try {

            java.util.HashMap totalRemovedItems = new java.util.HashMap();

            CacheEntry entry = null;
            Iterator ide = null;

            for (int i = 0; i < keys.length; i++) {
                try {
                    if (keys[i] != null) {
                        entry = getInternal().Remove(keys[i], reason, false, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                    }

                    if (entry != null) {
                        totalRemovedItems.put(keys[i], entry);
                       
                    }
                } catch (Exception ex) {
                }
            }

            ide = totalRemovedItems.entrySet().iterator();
            Map.Entry pair;
            while (ide.hasNext()) {
                Map.Entry idePair = (Map.Entry) ide.next();
                try {
                    entry = (CacheEntry) ((idePair.getValue() instanceof CacheEntry) ? idePair.getValue() : null);
                    if (entry != null) {
                        if (getIsItemRemoveNotifier()) {
                            EventId eventId = null;
                            OperationID opId = operationContext.getOperatoinID();
                            EventContext eventContext = null;

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
                            eventContext.setItem(CacheHelper.CreateCacheEventEntry(com.alachisoft.tayzgrid.runtime.events.EventDataFilter.DataWithMetaData, entry));

                            NotifyItemRemoved(idePair.getKey(), entry, reason, true, operationContext, eventContext);

                        }
                        if (entry.getValue() instanceof CallbackEntry) {
                            EventId eventId = null;
                            OperationID opId = operationContext.getOperatoinID();
                            CallbackEntry cbEtnry = (CallbackEntry) entry.getValue(); // e.DeflattedValue(_context.SerializationContext);
                            EventContext eventContext = null;

                            if (cbEtnry != null && cbEtnry.getItemRemoveCallbackListener() != null && cbEtnry.getItemRemoveCallbackListener().size() > 0) {
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
                                EventCacheEntry eventCacheEntry = CacheHelper.CreateCacheEventEntry(cbEtnry.getItemRemoveCallbackListener(), entry);
                                eventContext.setItem(eventCacheEntry);
                                eventContext.Add(EventContextFieldName.ItemRemoveCallbackList, new ArrayList(cbEtnry.getItemRemoveCallbackListener()));

                                //Will always reaise the whole entry for old clients
                                NotifyCustomRemoveCallback(idePair.getKey(), entry, reason, true, operationContext, eventContext);
                            }
                        }
                    }
                } catch (Exception ex) {
                }
            }

        } catch (Exception e) {
            throw new OperationFailedException(e);
        }

        return depenedentItemList;
    }

    @Override
    public CacheEntry Remove(Object key, ItemRemoveReason ir, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        CacheEntry retVal = getInternal().Remove(key, ir, notify, lockId, version, accessType, operationContext);
     
        return retVal;
    }

    @Override
    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason ir, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        java.util.HashMap retVal = getInternal().Remove(keys, ir, notify, operationContext);

        return retVal;
    }

    @Override
    public java.util.HashMap Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        java.util.ArrayList list = GetGroupKeys(group, subGroup, operationContext);
        if (list != null && list.size() > 0) {
            Object[] grpKeys = MiscUtil.GetArrayFromCollection(list);
            return Remove(grpKeys, ItemRemoveReason.Removed, notify, operationContext);
        }
        return null;
    }

    @Override
    public QueryResultSet Search(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return getInternal().Search(query, values, operationContext);
    }

    @Override
    public QueryResultSet SearchEntries(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        QueryResultSet resultSet = getInternal().SearchEntries(query, values, operationContext);

        if (resultSet.getSearchEntriesResult() != null && getKeepDeflattedValues()) {
            Iterator ide = resultSet.getSearchEntriesResult().entrySet().iterator();
            CacheEntry entry;
            Map.Entry KeyValue;
            while (ide.hasNext()) {
                KeyValue = (Map.Entry) ide.next();
                Object Key = KeyValue.getKey();
                Object Value = KeyValue.getValue();
                entry = (CacheEntry) ((Value instanceof CacheEntry) ? Value : null);
                if (entry != null) {
                    try {
                        entry.KeepDeflattedValue(_context.getSerializationContext());
                    } catch (Exception exception) {
                        throw new OperationFailedException(exception);
                    }
                }
            }
        }
        return resultSet;
    }

    @Override
    public DeleteQueryResultSet DeleteQuery(String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        return getInternal().DeleteQuery(query, values, notify, isUserOperation, ir, operationContext);
    }

    @Override
    public void SendNotification(Object notifId, Object data) {
        getInternal().SendNotification(notifId, data);
    }

    @Override
    public ResetableIterator GetEnumerator() throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return getInternal().GetEnumerator();
    }

    @Override
    public EnumerationDataChunk GetNextChunk(EnumerationPointer pointer, OperationContext operationContext) throws GeneralFailureException, TimeoutException, SuspectedException, CacheException {
        return getInternal().GetNextChunk(pointer, operationContext);
    }

    @Override
    public void RegisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException, TimeoutException {
        getInternal().RegisterKeyNotification(key, updateCallback, removeCallback, operationContext);
    }

    @Override
    public void RegisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException, TimeoutException {
        getInternal().RegisterKeyNotification(keys, updateCallback, removeCallback, operationContext);
    }

    @Override
    public void UnregisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        getInternal().UnregisterKeyNotification(key, updateCallback, removeCallback, operationContext);
    }

    @Override
    public void UnregisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        getInternal().UnregisterKeyNotification(keys, updateCallback, removeCallback, operationContext);
    }

    @Override
    public void UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        getInternal().UnLock(key, lockId, isPreemptive, operationContext);
    }

    @Override
    public LockOptions Lock(Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return getInternal().Lock(key, lockExpiration, lockId, lockDate, operationContext);
    }

    @Override
    public LockOptions IsLocked(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return getInternal().IsLocked(key, lockId, lockDate, operationContext);
    }

    @Override
    public void OnItemAdded(Object key, OperationContext operationContext, EventContext eventContext) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    @Override
    public void OnItemUpdated(Object key, OperationContext operationContext, EventContext eventContext) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    @Override
    public void OnItemRemoved(Object key, Object val, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    @Override
    public void OnItemsRemoved(Object[] keys, Object[] vals, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContext) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    @Override
    public void OnCacheCleared(OperationContext operationContext, EventContext eventContext) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    @Override
    public void OnCustomEvent(Object notifId, Object data, OperationContext operationContext, EventContext eventContext) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    @Override
    public void OnCustomUpdateCallback(Object key, Object value, OperationContext operationContext, EventContext eventContext) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    /**
     *
     * @param key
     * @param value
     * @param reason
     * @param operationContext
     * @param eventContext
     */
    @Override
    public void OnCustomRemoveCallback(Object key, Object value, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }
    @Override
    public void OnHashmapChanged(com.alachisoft.tayzgrid.common.datastructures.NewHashmap newHashmap, boolean updateClientMap) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }
    @Override
    public void OnWriteBehindOperationCompletedCallback(OpCode operationCode, Object result, CallbackEntry cbEntry) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }
    
    @Override
    public void submitMapReduceTask(MapReduceTask task, String taskId, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws NotSupportedException,GeneralFailureException, OperationFailedException
    {
        try 
        {
            MapReduceOperation op = new MapReduceOperation();
            op.setData(task);
            op.setCallbackInfo(callbackInfo);
            op.setOpCode(MapReduceOpCodes.RegisterTaskNotification);
            op.setTaskID(taskId);
           
            Object result=(Object)getInternal().TaskOperationRecieved(op);
            
            if((TaskExecutionStatus)result == TaskExecutionStatus.Failure)throw new Exception("Operation Failed");
            
        }catch (Exception inner) 
        {
            _context.getCacheLog().Error("LocalCacheImpl.SubmitMapReduceTask() ", inner.toString());
            throw new OperationFailedException("SubmitMapReduceTask failed. Error : " + inner.getMessage(), inner);
        }
    }
              
    /**
     * Registers the item update/remove or both callbacks with the specified
     * key. Keys should exist before the registration.
     *
     * @param taskID            
     * @param callbackInfo            
     * @param operationContext
     * @throws
     * OperationFailedException
     */
    @Override
    public final void RegisterTaskNotification(String taskID, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws
            OperationFailedException {
        try 
        {
            MapReduceOperation op = new MapReduceOperation();
            op.setCallbackInfo(callbackInfo);
            op.setOpCode(MapReduceOpCodes.RegisterTaskNotification);
            op.setTaskID(taskID);
           
            Object result=(Object)getInternal().TaskOperationRecieved(op);
            
            if((TaskExecutionStatus)result == TaskExecutionStatus.Failure)throw new Exception("Operation Failed");
            
        }catch (Exception inner) 
        {
            _context.getCacheLog().Error("ClusterCacheBase.RegisterTaskNotification() ", inner.toString());
            throw new OperationFailedException("RegisterTaskNotification failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Unregisters the item update/remove or both call backs with the specified
     * key.
     *
     * @param taskID
     * @param operationContext
     * @param callbackInfo
     * @throws OperationFailedException
     */
    @Override
    public final void UnregisterTaskNotification(String taskID, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws
            OperationFailedException {

        try 
        {
            MapReduceOperation op = new MapReduceOperation();
            op.setData(callbackInfo);
            op.setOpCode(MapReduceOpCodes.UnregisterTaskNotification);
            op.setTaskID(taskID);           
            
            Object result=(Object)getInternal().TaskOperationRecieved(op);
            
            if((TaskExecutionStatus)result == TaskExecutionStatus.Failure)throw new Exception("Operation Failed");
           
        }catch (Exception inner) 
        {
            _context.getCacheLog().Error("ClusterCacheBase.UnregisterTaskNotification() ", inner.toString());
            throw new OperationFailedException("UnregisterTaskNotification failed. Error : " + inner.getMessage(), inner);
        }
    }
    
      @Override
    public void OnTaskCallback(Object taskID, Object value, OperationContext operationContext, EventContext eventContext) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
