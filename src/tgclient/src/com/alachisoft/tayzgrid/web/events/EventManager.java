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

package com.alachisoft.tayzgrid.web.events;

import com.alachisoft.tayzgrid.common.ResourcePool;
import com.alachisoft.tayzgrid.common.logger.CacheLogger;
import com.alachisoft.tayzgrid.common.threading.ThreadPool;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.web.caching.CacheDataModificationListener;
import com.alachisoft.tayzgrid.web.caching.CacheItemRemovedReason;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceListener;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceResponse;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskCompletionStatus;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Has the responsibility of creating <see cref=" CacheEventDescriptor"/> and
 * registering it against a ResourcePool
 */
public class EventManager {

    public static final short REFSTART = -1;
    public static final short SELECTIVEREFSTARTRemove = 8000;
    public static final short SELECTIVEREFSTARTUpdate = 9000;
    
    public static short MAPREDUCELISTENER = 7000;
    
    private String _cacheName = null;
    private CacheLogger _logger;
    private Object sync_lock_selective = new Object();
    private Object sync_lock_general = new Object();
    private Object sync_mapReduce = new Object();
    
    private Cache _cache;
 
    private ResourcePool _addEventPool = null;
    private ResourcePool _cacheClearEventPool = null;
  
    private EventDataFilter _addDataFilter = EventDataFilter.None;
    private ResourcePool _removeEventPool = null;
   
    private EventDataFilter _removeDataFilter = EventDataFilter.None;
    private ResourcePool _updateEventPool = null;
   
    private EventDataFilter _updateDataFilter = EventDataFilter.None;
    private short _addEventRegistrationSequence = REFSTART; //Significant difference from old callback numbers
    private short _updateEventRegisrationSequenceId = REFSTART; //Significant difference from old callback numbers
    private short _removeEventRegistrationSequenceId = REFSTART; //Significant difference from old callback numbers
              
    private ResourcePool _selectiveRemoveEventPool = null;
    private ResourcePool _selectiveRemoveEventIDPool = null;
    private ResourcePool _mapReduceListenerPool = null;
    private ResourcePool _mapReduceListenerIDPool = null;
    private ResourcePool _oldSelectiveCallbackPool = new ResourcePool();
    private ResourcePool _oldSelectiveMappingCallbackPool = new ResourcePool();
    private short _selectveRemoveCallbackRef = SELECTIVEREFSTARTRemove;
    private ResourcePool _selectiveUpdateEventPool = null;
    private ResourcePool _selectiveUpdateEventIDPool = null;
    private short _selectiveUpdateCallbackRef = SELECTIVEREFSTARTUpdate;
    private EventDataFilter _generalAddDataFilter = EventDataFilter.None;
    private EventDataFilter _generalUpdateDataFilter = EventDataFilter.None;
    private EventDataFilter _generalRemoveDataFilter = EventDataFilter.None;

 
    public EventManager(String cacheName, CacheLogger logger, Cache cache) {
        _cacheName = cacheName;
        _logger = logger;
        _cache = cache;
    }

    public final short getAddSequenceNumber() {
        return _addEventRegistrationSequence;
    }

    public final short getUpdateSequenceNumber() {
        return _updateEventRegisrationSequenceId;
    }

    public final short getRemoveSequenceNumber() {
        return _removeEventRegistrationSequenceId;
    }

    public final Object getSyncLockGeneral() {
        return sync_lock_general;
    }

    public final Object getSyncLockSelective() {
        return sync_lock_selective;
    }

    /**
     * Provide
     *
     *
     * @param eventType
     * @return
     */
    public final short generalEventRefCountAgainstEvent(EventType eventType) {
        if ((eventType.getValue() & EventType.ItemAdded.getValue()) != 0) {
            return _addEventRegistrationSequence;
        }
        if ((eventType.getValue() & EventType.ItemRemoved.getValue()) != 0) {
            return _removeEventRegistrationSequenceId;
        }
        if ((eventType.getValue() & EventType.ItemUpdated.getValue()) != 0) {
            return _updateEventRegisrationSequenceId;
        }

        return -1;
    }

    /**
     * Returns the filter type of the eventType
     *
     * @param eventType
     * @return
     */
    public final EventDataFilter maxFilterAgainstEvent(EventType eventType) {
        if ((eventType.getValue() & EventType.ItemAdded.getValue()) != 0) {
            return _addDataFilter;
        }
        if ((eventType.getValue() & EventType.ItemRemoved.getValue()) != 0) {
            return _removeDataFilter;
        }
        if ((eventType.getValue() & EventType.ItemUpdated.getValue()) != 0) {
            return _updateDataFilter;
        }

        return EventDataFilter.DataWithMetaData;
    }

    /**
     * Registers the callback separately and returns short values of
     * registeredCallbacks
     *
     * @param key
     * @param callback
     * @param eventType
     * @param datafilter
     * @return short array,<p>1st element is updated callbackRef</p><para>2st
     * element is removed callbackRef</para>
     */
    public final short[] registerSelectiveEvent(CacheDataModificationListener callback, EnumSet<EventType> enumTypeSet, EventDataFilter datafilter) {
        if (callback != null) {
            //Avoiding new ResourcePool(inside = new Hashtable) at constructor level
            if (_selectiveUpdateEventPool == null) {
                _selectiveUpdateEventPool = new ResourcePool();
                _selectiveUpdateEventIDPool = new ResourcePool();
            }
            if (_selectiveRemoveEventPool == null) {
                _selectiveRemoveEventPool = new ResourcePool();
                _selectiveRemoveEventIDPool = new ResourcePool();
            }

             
            return registerSelectiveDiscriptor(callback, enumTypeSet);
        } else {
            return null;
        }
    }

    
    // <editor-fold desc=" ---------- MAP REDUCE ---------- ">
    
    public final Object getSyncMapReduce()
    {
        return sync_mapReduce;
    }
    
    public short registerMapReduceListener(MapReduceListener listener, String taskId)
    {
        if(listener != null)
        {
            if(_mapReduceListenerPool == null) {
                _mapReduceListenerPool = new ResourcePool();
                _mapReduceListenerIDPool = new ResourcePool();
            }
            return registerMRListenerDescriptor(listener);
        }
        return 0;
    }
    
    private short registerMRListenerDescriptor(MapReduceListener listener)
    {
        if(listener == null)
            return 0;
        
        short returnValue = 0;
        
        synchronized(getSyncMapReduce())
        {
            ResourcePool pool = _mapReduceListenerPool;
            ResourcePool poolID = _mapReduceListenerIDPool;
            
            if(pool.GetResource(listener) == null)
            {
                returnValue = ++MAPREDUCELISTENER;
                pool.AddResource(listener, returnValue);
                poolID.AddResource(returnValue, listener);
            }
            else
            {
                short val = (Short)pool.GetResource(listener);
                if (val >= 0) {
                    //add it again into the table for updating ref count.
                    pool.AddResource(listener, val);
                    poolID.AddResource(val, listener);
                    returnValue = val;
                }
            }
        }
        
        return returnValue;
    }
    
    public void fireMapReduceCallback(String taskId, int taskStatus, short callbackId)
    {
        TaskCompletionStatus status = TaskCompletionStatus.Success;
        status.setValue(taskStatus);
        switch(taskStatus)
        {
            case 1:
                status = TaskCompletionStatus.Success;
                break;
            case 2:
                status = TaskCompletionStatus.Failure;
                break;
            case 3:
                status = TaskCompletionStatus.Cancelled;
                break;
        }
        MapReduceResponse mrResponse = new MapReduceResponse(status, taskId, callbackId);
        
        if(_mapReduceListenerIDPool != null)
        {
            ResourcePool poole = _mapReduceListenerIDPool;
            MapReduceListener callback = (MapReduceListener) poole.GetResource(callbackId);
            
            if(callback != null)
                callback.onTaskResult(mrResponse);
        }
    }
    // </editor-fold>
    
    
    public final CacheEventDescriptor registerGeneralEvents(CacheDataModificationListener callback, EnumSet<EventType> eventEnumSet, EventDataFilter datafilter) throws GeneralFailureException, ConfigurationException, ConnectionException, OperationFailedException, OperationFailedException, AggregateException, AggregateException {
        if (callback != null) {
            //Avoiding new ResourcePool(inside = new Hashtable) at constructor level
            if (_addEventPool == null) {
                _addEventPool = new ResourcePool();
                 
            }
            if (_removeEventPool == null) {
                _removeEventPool = new ResourcePool();
                 
            }
            if (_updateEventPool == null) {
                _updateEventPool = new ResourcePool();
                 
            }
            if (_cacheClearEventPool == null) {
                _cacheClearEventPool = new ResourcePool();
                 
            }
            
            Iterator<EventType> itor = eventEnumSet.iterator();

            CacheEventDescriptor discriptor = CacheEventDescriptor.CreateCacheDiscriptor(eventEnumSet, _cacheName, callback, datafilter);

            //Registers the handle
            if (!registerGeneralDiscriptor(discriptor, eventEnumSet)) {
                return null;
            }

            return discriptor;

        } else {
            return null;
        }
    }

    public final void unregisterAll() {
        //TODO
    }

    /**
     * TheadSafe and no locks internally
     *
     * @param key
     * @param eventType Should contain one type i.e. should not be used as a
     * flag. Every EventType should be executed from another thread
     * @param item
     * @param oldItem
     * @param reason
     * @param notifyAsync
     */
    public final void raiseGeneralCacheNotification(final Object key, EnumSet<EventType> eventEnumSet, EventCacheItem item, EventCacheItem oldItem, CacheItemRemovedReason reason, boolean notifyAsync) {
        try {
            Object[] registeredDiscriptors = null;

            EventType eventType = EventUtil.getEventType(eventEnumSet);
            ResourcePool eventPool = getEventPool(eventEnumSet);
            if (eventPool != null) {
                registeredDiscriptors = eventPool.GetAllResourceKeys();
            }

            if (registeredDiscriptors != null && registeredDiscriptors.length > 0) {
                for (int i = 0; i < registeredDiscriptors.length; i++) {
                    final CacheEventDescriptor discriptor = (CacheEventDescriptor) ((registeredDiscriptors[i] instanceof CacheEventDescriptor) ? registeredDiscriptors[i] : null);

                    if (discriptor == null) {
                        continue;
                    }

                    if(eventEnumSet.contains(EventType.CacheCleared)){
                        
                            if (notifyAsync) {
                            ThreadPool.executeTask((new Runnable() {
                                @Override
                                public void run() {
                                    discriptor.getCacheDataNotificationListener().cacheCleared();
                                }
                            }));
                        }  
                        else {
                            discriptor.getCacheDataNotificationListener().cacheCleared();
                        }
                    }else{
                    
                        final CacheEventArg arg = createCacheEventArgument(discriptor.getDataFilter(), key, _cacheName, eventType, item, oldItem, reason);
                        arg.setDescriptor(discriptor);


                        if (notifyAsync) {
                            ThreadPool.executeTask((new Runnable() {
                                @Override
                                public void run() {
                                    fire(new Object[]{discriptor.getCacheDataNotificationListener(), key, arg});
                                }
                            }));
                        }  
                        else {
                            discriptor.getCacheDataNotificationListener().cacheDataModified(key, arg);
                        }
                    }
                }
            }
        } catch (RuntimeException ex) {
            if (_logger != null && _logger.getIsErrorEnabled()) {
                _logger.CriticalInfo(ex.toString());
            }
        }
    }

    private CacheEventArg createCacheEventArgument(EventDataFilter dataFilter, Object key, String cacheName, EventType eventType, EventCacheItem item, EventCacheItem oldItem, CacheItemRemovedReason removedReason) {
        EventCacheItem cloneItem = null;
        EventCacheItem cloneOldItem = null;

        if (dataFilter != EventDataFilter.None && item != null) {
            Object tempVar = item.clone();
            cloneItem = (EventCacheItem) ((tempVar instanceof EventCacheItem) ? tempVar : null);

            if (cloneItem != null) {
                if (dataFilter == EventDataFilter.Metadata) {
                    cloneItem.setValue(null);
                }
            }
        }

        if (dataFilter != EventDataFilter.None && oldItem != null) {
            Object tempVar2 = oldItem.clone();
            cloneOldItem = (EventCacheItem) ((tempVar2 instanceof EventCacheItem) ? tempVar2 : null);

            if (cloneOldItem != null) {
                if (dataFilter == EventDataFilter.Metadata) {
                    cloneOldItem.setValue(null);
                }
            }
        }

        CacheEventArg eventArg = new CacheEventArg(key, cacheName, eventType, cloneItem, null, removedReason);
        if (eventType == EventType.ItemUpdated) {
            eventArg.setOldItem(cloneOldItem);
        }

        return eventArg;
    }

    /**
     * TheadSafe and no locks internally
     *
     * @param key
     * @param eventType Should contain one type i.e. should not be used as a
     * flag. Every EventType should be executed from another thread
     * @param item
     * @param oldItem
     * @param reason
     * @param _notifyAsync
     * @param eventhandle
     */
    public final void raiseSelectiveCacheNotification(final Object key, EnumSet<EventType> eventEnumSet, EventCacheItem item, EventCacheItem oldItem, CacheItemRemovedReason reason, boolean _notifyAsync, EventHandle eventhandle, EventDataFilter dataFilter) {
        try {
            ResourcePool poolID = null;

            if (eventEnumSet.contains(EventType.ItemUpdated)) {
                poolID = _selectiveUpdateEventIDPool;
                
            } else if (eventEnumSet.contains(EventType.ItemRemoved)) {
                poolID = _selectiveRemoveEventIDPool;
                 
            }

             EventType eventType = EventUtil.getEventType(eventEnumSet);
            final CacheEventArg arg = createCacheEventArgument(dataFilter, key, _cacheName, eventType, item, oldItem, reason);

            if (poolID == null) {
                return;
            }

            Object tempVar = poolID.GetResource((short) eventhandle.getHandle());
            final CacheDataModificationListener callback = (CacheDataModificationListener) ((tempVar instanceof CacheDataModificationListener) ? tempVar : null);

            if (callback == null) //Can occur if Unregistered concurrently
            {
                return;
            }

            if (_notifyAsync) //callback.BeginInvoke(key, arg, asyn, null); 
            {                
                ThreadPool.executeTask(new Runnable() {
                    @Override
                    public void run() {
                        fire(new Object[]{callback, key, arg});
                    }
                });
                 
            } else {
                callback.cacheDataModified(key, arg);
            }
        } catch (RuntimeException ex) {
            if (_logger != null && _logger.getIsErrorEnabled()) {
                _logger.CriticalInfo(ex.toString());
            }
        }
    }

    private static void fire(Object obj) {
        try {
            Object[] objArray = (Object[]) obj;
            ((CacheDataModificationListener) objArray[0]).cacheDataModified(objArray[1], (CacheEventArg) objArray[2]);
        }catch (Exception e) {
        }
    }
  
    /**
     * Returning Negative value means operation not successful
     *
     * @param discriptor
     * @param eventType
     * @return short array <p>1st value is Update callbackRef</p> <para>nd value
     * is removeRef</para>
     */
    private short[] registerSelectiveDiscriptor(CacheDataModificationListener callback, EnumSet<EventType> enumTypeSet) {
        if (callback == null) {
            return null; //FAIL CONDITION
        }
        short[] returnValue = new short[]{-1, -1}; //First value update callback ref & sencond is remove callbackref

        for (EventType type : EventType.values()) {
            if (type == EventType.ItemAdded) //ItemAdded not supported Yet
            {
                continue;
            }

            synchronized (getSyncLockSelective()) {
                ResourcePool pool = null;
                ResourcePool poolID = null;

 

                if (type.equals(EventType.ItemRemoved)
                        && enumTypeSet.contains(type)) {
                    pool = _selectiveRemoveEventPool;
                    poolID = _selectiveRemoveEventIDPool;
                } else if (type.equals(EventType.ItemUpdated)
                        && enumTypeSet.contains(type)) {
                    pool = _selectiveUpdateEventPool;
                    poolID = _selectiveUpdateEventIDPool;
                }

                if (pool == null) {
                    continue;
                }
 

                while (true) {
                    int i = type == EventType.ItemUpdated ? 0 : 1;
                    if (pool.GetResource(callback) == null) {

                        returnValue[i] = type == EventType.ItemUpdated ? ++_selectiveUpdateCallbackRef : ++_selectveRemoveCallbackRef;
                        pool.AddResource(callback, returnValue[i]);
                        poolID.AddResource(returnValue[i], callback);
                        break;
                    } else {
                        try {
                            if (pool.GetResource(callback) != null) {
                                short cref = (Short)pool.GetResource(callback);
                                if (cref < 0) {
                                    break; //FAIL CONDITION
                                }

                                //add it again into the table for updating ref count.
                                pool.AddResource(callback, cref);
                                poolID.AddResource(cref, callback);
                                returnValue[i] = cref;
                                break;
                            }

                        } catch (java.lang.NullPointerException e) {
                            //Legacy code: can create an infinite loop
                            //Recomendation of returning a negative number instead of continue
                            continue;
                        }
                    }
                }
            }
        }
        return returnValue;
    }

    private boolean registerGeneralDiscriptor(CacheEventDescriptor discriptor, EnumSet<EventType> eventEnumSet) throws GeneralFailureException, OperationFailedException, AggregateException, AggregateException,   ConfigurationException, ConfigurationException, ConnectionException {
        if (discriptor == null) {
            return false; //FAIL CONDITION
        }

        EventHandle handle = null;

        for (EventType type : EventType.values()) {
            ResourcePool pool = null;
            boolean registrationUpdated = false;

            if (eventEnumSet.contains(type)) {
                pool = getEventPool(EnumSet.of(type));
            }

            if (pool == null) {
                continue;
            }

            short registrationSequenceId = -1;

            synchronized (getSyncLockGeneral()) {
                pool.AddResource(discriptor, 1); // Everytime a new Discriptor is forcefully created

                //Keeps a sequence number

                switch (type) {
                    case ItemAdded:
                        
                        if (discriptor.getDataFilter().getValue() > _generalAddDataFilter.getValue()
                                || _addEventRegistrationSequence == REFSTART) {
                            registrationUpdated = true;
                            registrationSequenceId = ++_addEventRegistrationSequence;
                            _generalAddDataFilter = discriptor.getDataFilter();
                        } else {
                            registrationSequenceId = _addEventRegistrationSequence;
                        }
                        break;
                    case ItemRemoved:
                        if (discriptor.getDataFilter().getValue() > _generalRemoveDataFilter.getValue()
                                || _removeEventRegistrationSequenceId == REFSTART) {
                            registrationUpdated = true;
                            registrationSequenceId = ++_removeEventRegistrationSequenceId;
                            _generalRemoveDataFilter = discriptor.getDataFilter();
                        } else {
                            registrationSequenceId = _removeEventRegistrationSequenceId;
                        }
                        break;
                    case ItemUpdated:
                        if (discriptor.getDataFilter().getValue() > _generalUpdateDataFilter.getValue()
                                || _updateEventRegisrationSequenceId == REFSTART) {
                            registrationUpdated = true;
                            registrationSequenceId = ++_updateEventRegisrationSequenceId;
                            _generalUpdateDataFilter = discriptor.getDataFilter();
                        } else {
                            registrationSequenceId = _updateEventRegisrationSequenceId;
                        }
                        break;
                     case CacheCleared:
                       //just change from -1 to positive value. this id does not matter for cache clear callback.
                        registrationSequenceId = 1;
                        
                        break;
                }

                //Although the handle doesnt matter in general events
                if (handle == null) {
                    handle = new EventHandle(registrationSequenceId);
                }
            }

            if (_cache != null && registrationSequenceId != -1) {
                _cache.registerCacheNotificationDataFilterInternal(EnumSet.of(type), discriptor.getDataFilter(), registrationSequenceId);                
            }
        }

        discriptor.setIsRegistered(true);
        discriptor.setHandle(handle);
        return true;
    }

    /**
     * Unregisters CacheDataNotificationCallback
     * <p>Flag based unregistration</p>
     *
     * @param callback
     * @param key
     * @param eventType
     */
    public final short[] unRegisterSelectiveNotification(CacheDataModificationListener callback, EnumSet<EventType> eventEnumSet) {

        if (callback == null) {
            return null;
        }

        short[] returnValue = new short[]{-1, -1}; //First value update callback ref & sencond is remove callbackref


        for (EventType type : EventType.values()) {
            if (type == EventType.ItemAdded) //ItemAdded not supported Yet
            {
                continue;
            }

            Object id = -1;

            synchronized (getSyncLockSelective()) {
                ResourcePool pool = null;
                ResourcePool poolID = null;

                if (type.equals(EventType.ItemRemoved)
                        && eventEnumSet.contains(type)) {
                    pool = _selectiveRemoveEventPool;
                    poolID = _selectiveRemoveEventIDPool;
                } else if (type.equals(EventType.ItemUpdated)
                        && eventEnumSet.contains(type)) {
                    pool = _selectiveUpdateEventPool;
                    poolID = _selectiveUpdateEventIDPool;
                }

                if (pool == null) {
                    continue;
                }
 

                // : For selective callback, we dont remove the callback as it can create chaos if user try to unregister
                //a callback more then one time or against wrong items.

                
                int i = type == EventType.ItemUpdated ? 0 : 1;
                id = pool.GetResource(callback);
                if (id instanceof Short) {
                  
                    returnValue[i] = (Short) id;
                }
            }
        }
        return returnValue;
    }

    public final EventHandle unRegisterDiscriptor(CacheEventDescriptor discriptor) throws GeneralFailureException,   OperationFailedException, OperationFailedException, ConnectionException, ConnectionException, AggregateException, ConfigurationException {
        if (discriptor == null || !discriptor.getIsRegistered()) {
            return null;
        }

        for (EventType type : EventType.values()) {
            ResourcePool pool = null;
 

            if (discriptor.getRegisteredAgainst().contains(type)) {
                pool = getEventPool(EnumSet.of(type));
            }

            if (pool == null) {
                continue;
            }
 

            short registrationSequenceId = -1;
            boolean unregisterNotification = false;
            EventDataFilter maxDataFilter = EventDataFilter.None;

            synchronized (getSyncLockGeneral()) {
                Object retVal = pool.RemoveResource(discriptor);

                if (retVal == null) {
                    continue;
                }
                unregisterNotification = pool.getCount() == 0;

                if (!unregisterNotification && type != EventType.CacheCleared) {
                    Object[] pooledDescriptors = pool.GetAllResourceKeys();

                    if (pooledDescriptors != null) {
                        for (int i = 0; i < pooledDescriptors.length; i++) {
                            CacheEventDescriptor pooledDescriptor = (CacheEventDescriptor) ((pooledDescriptors[i] instanceof CacheEventDescriptor) ? pooledDescriptors[i] : null);

                            if (pooledDescriptor != null && pooledDescriptor.getDataFilter().getValue() > maxDataFilter.getValue()) {
                                maxDataFilter = pooledDescriptor.getDataFilter();
                            }

                            if (maxDataFilter.getValue() == EventDataFilter.DataWithMetaData.getValue()) {
                                break;
                            }
                        }
                    }
                }


                discriptor.setIsRegistered(false);

                //keeps a sequence number
                switch (type) {
                    case ItemAdded:
                        //Data filter is being updated
                        if (maxDataFilter != _generalAddDataFilter) {
                            _generalAddDataFilter = maxDataFilter;
                            registrationSequenceId = ++_addEventRegistrationSequence;
                        }
                        if (unregisterNotification) {
                            _generalAddDataFilter = EventDataFilter.None;
                        }
                        break;
                    case ItemRemoved:
                        if (maxDataFilter != _generalRemoveDataFilter) {
                            _generalRemoveDataFilter = maxDataFilter;
                            registrationSequenceId = ++_removeEventRegistrationSequenceId;
                        }
                        if (unregisterNotification) {
                            _generalAddDataFilter = EventDataFilter.None;
                        }
                        break;
                    case ItemUpdated:
                        if (maxDataFilter != _generalUpdateDataFilter) {
                            _generalUpdateDataFilter = maxDataFilter;
                            registrationSequenceId = ++_updateEventRegisrationSequenceId;
                        }
                        if (unregisterNotification) {
                            _generalAddDataFilter = EventDataFilter.None;
                        }
                        break;
                }
            }

            if (_cache != null) {
                if (unregisterNotification) {
                    //client is no more interested in event, therefore unregister it from server
                    _cache.unregisterGeneralCacheNotificationInternal(EnumSet.of(type));
                } else if (registrationSequenceId != -1) {
                    //only caused update of data filter either upgrade or downgrade
                    _cache.registerCacheNotificationDataFilterInternal(EnumSet.of(type), maxDataFilter, registrationSequenceId);                    
                }
            }
        }
        return null;

    }

    public final EventRegistrationInfo[] getEventRegistrationInfo() {
        java.util.ArrayList<EventRegistrationInfo> registeredEvents = new java.util.ArrayList<EventRegistrationInfo>();

        synchronized (getSyncLockGeneral()) {
            if (_addEventPool != null && _addEventPool.getCount() > 0) {
                registeredEvents.add(new EventRegistrationInfo(EventType.ItemAdded, _generalAddDataFilter, _addEventRegistrationSequence));
            }
            if (_updateEventPool != null && _updateEventPool.getCount() > 0) {
                registeredEvents.add(new EventRegistrationInfo(EventType.ItemUpdated, _generalUpdateDataFilter, _updateEventRegisrationSequenceId));
            }
            if (_removeEventPool != null && _removeEventPool.getCount() > 0) {
                registeredEvents.add(new EventRegistrationInfo(EventType.ItemRemoved, _generalRemoveDataFilter, _removeEventRegistrationSequenceId));
            }
            if (_cacheClearEventPool != null && _cacheClearEventPool.getCount() > 0) {
                registeredEvents.add(new EventRegistrationInfo(EventType.CacheCleared, EventDataFilter.None,(short) -1));
            }
        }
        return registeredEvents.toArray(new EventRegistrationInfo[0]);
    }

    private ResourcePool getEventPool(EnumSet<EventType> eventEnumSet) {
        ResourcePool pool = null;

        if (eventEnumSet.contains(EventType.ItemAdded)) {
            pool = _addEventPool;
        } else if (eventEnumSet.contains(EventType.ItemRemoved)) {
            pool = _removeEventPool;
        } else if (eventEnumSet.contains(EventType.ItemUpdated)) {
            pool = _updateEventPool;
        }else if (eventEnumSet.contains(EventType.CacheCleared)) {
            pool = _cacheClearEventPool;
        }

        return pool;
    }
    
  
 

    public static class EventRegistrationInfo {

        private EventType _eventType;
        private EventDataFilter _filter;
        private short _registrationSequence;

        public EventRegistrationInfo() {
        }

        public EventRegistrationInfo(EventType eventTYpe, EventDataFilter filter, short sequenceId) {
            _eventType = eventTYpe;
            _filter = filter;
            _registrationSequence = sequenceId;
        }

        public final EventType getEventTYpe() {
            return _eventType;
        }

        public final void setEventTYpe(EventType value) {
            _eventType = value;
        }

        public final EventDataFilter getDataFilter() {
            return _filter;
        }

        public final void setDataFilter(EventDataFilter value) {
            _filter = value;
        }

        public final short getRegistrationSequence() {
            return _registrationSequence;
        }

        public final void setRegistrationSequence(short value) {
            _registrationSequence = value;
        }
    }
 
}
