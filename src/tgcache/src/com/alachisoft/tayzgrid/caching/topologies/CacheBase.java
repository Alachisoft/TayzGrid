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

import com.alachisoft.tayzgrid.caching.AllowedOperationType;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.DataSourceUpdateOptions;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.EventContextFieldName;
import com.alachisoft.tayzgrid.caching.EventStatus;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.ShutDownServerInfo;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DSWriteBehindOperation;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.queries.DeleteQueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.PredicateHolder;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;

import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatistics;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.propagator.IAlertPropagator;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.util.ReaderWriterLock;
import com.alachisoft.tayzgrid.mapreduce.MapReduceOperation;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.NotSupportedException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessor;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessorResult;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.String;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base class for all cache implementations. Implements the ICache interface.
 */
public class CacheBase implements ICache, IDisposable {

    /**
     * Asynchronous notification dispatcher.
     */
    private static class AsyncLocalNotifyAdd implements AsyncProcessor.IAsyncTask {

        /**
         * The listener class
         */
        private ICacheEventsListener _listener;
        /**
         * Message to broadcast
         */
        private Object _key;
        private OperationContext _operationContext;
        private EventContext _eventContext;

        /**
         * Constructor
         *
         * @param listener
         * @param data
         */
        public AsyncLocalNotifyAdd(ICacheEventsListener listener, Object key, OperationContext operationContext, EventContext eventContext) {
            _listener = listener;
            _key = key;
            _operationContext = operationContext;
            _eventContext = eventContext;
        }

        /**
         * Implementation of message sending.
         */
        public void Process() {
            _listener.OnItemAdded(_key, _operationContext, _eventContext);
        }
    }

    /**
     * Asynchronous notification dispatcher.
     */
    private static class AsyncLocalNotifyCacheClear implements AsyncProcessor.IAsyncTask {

        /**
         * The listener class
         */
        private ICacheEventsListener _listener;
        private OperationContext _operationContext;
        private EventContext _eventContext;

        /**
         * Constructor
         *
         * @param listener
         * @param data
         */
        public AsyncLocalNotifyCacheClear(ICacheEventsListener listener, OperationContext operationContext, EventContext eventContext) {
            _listener = listener;
            _operationContext = operationContext;
            _eventContext = eventContext;
        }

        /**
         * Implementation of message sending.
         */
        public void Process() {
            _listener.OnCacheCleared(_operationContext, _eventContext);
        }
    }

    /**
     * Asynchronous notification dispatcher.
     */
    private static class AsyncLocalNotifyUpdate implements AsyncProcessor.IAsyncTask {

        /**
         * The listener class
         */
        private ICacheEventsListener _listener;
        /**
         * Message to broadcast
         */
        private Object _key;
        private OperationContext _operationContext;
        private EventContext _eventContext;

        /**
         * Constructor
         *
         * @param listener
         * @param data
         */
        public AsyncLocalNotifyUpdate(ICacheEventsListener listener, Object key, OperationContext operationContext, EventContext eventContext) {
            _listener = listener;
            _key = key;
            _operationContext = operationContext;
            _eventContext = eventContext;
        }

        /**
         * Implementation of message sending.
         */
        public void Process() {
            _listener.OnItemUpdated(_key, _operationContext, _eventContext);
        }
    }

    /**
     * Asynchronous notification dispatcher.
     */
    private static class AsyncLocalNotifyRemoval implements AsyncProcessor.IAsyncTask {

        /**
         * The listener class
         */
        private ICacheEventsListener _listener;
        /**
         * Message to broadcast
         */
        private Object _key, _value;
        private ItemRemoveReason _reason = ItemRemoveReason.Removed;
        private OperationContext _operationContext;
        private Object _eventContext;

        /**
         * Constructor
         *
         * @param listener
         * @param data
         */
        public AsyncLocalNotifyRemoval(ICacheEventsListener listener, Object key, Object value, ItemRemoveReason reason, OperationContext operationContext, Object eventContext) {
            _listener = listener;
            _key = key;
            _value = value;
            _reason = reason;
            _operationContext = operationContext;
            _eventContext = eventContext;
        }

        /**
         * Implementation of message sending.
         */
        @Override
        public void Process() throws OperationFailedException, java.io.IOException, CacheException, LockingException {
            if (_key instanceof Object[]) {
                _listener.OnItemsRemoved((Object[]) _key, (Object[]) _value, _reason, _operationContext, (EventContext[]) _eventContext);
            } else {
                _listener.OnItemRemoved(_key, _value, _reason, _operationContext, (EventContext) _eventContext);
            }
        }
    }

    /**
     * Asynchronous notification dispatcher.
     */
    private static class AsyncLocalNotifyCustomEvent implements AsyncProcessor.IAsyncTask {

        /**
         * The listener class
         */
        private ICacheEventsListener _listener;
        /**
         * Message to broadcast
         */
        private Object _notifId, _data;
        private OperationContext _operationContext;
        private EventContext _eventContext;

        /**
         * Constructor
         *
         * @param listener
         * @param data
         */
        public AsyncLocalNotifyCustomEvent(ICacheEventsListener listener, Object notifId, Object data, OperationContext operationContext, EventContext eventContext) {
            _listener = listener;
            _notifId = notifId;
            _data = data;
            _operationContext = operationContext;
            _eventContext = eventContext;
        }

        /**
         * Implementation of message sending.
         */
        public void Process() {
            {
                _listener.OnCustomEvent(_notifId, _data, _operationContext, _eventContext);
            }
        }
    }

    /**
     * Asynchronous notification dispatcher.
     */
    private static class AsyncLocalNotifyUpdateCallback implements AsyncProcessor.IAsyncTask {

        /**
         * The listener class
         */
        private ICacheEventsListener _listener;
        /**
         * Message to broadcast
         */
        private Object _key;
        private Object _entry;
        private OperationContext _operationContext;
        private EventContext _eventContext;

        /**
         * Constructor
         *
         * @param listener
         * @param data
         */
        public AsyncLocalNotifyUpdateCallback(ICacheEventsListener listener, Object key, Object entry, OperationContext operationContext, EventContext eventContext) {
            _listener = listener;
            _key = key;
            _entry = entry;
            _operationContext = operationContext;
            _eventContext = eventContext;
        }

        /**
         * Implementation of message sending.
         */
        public void Process() {
            _listener.OnCustomUpdateCallback(_key, _entry, _operationContext, _eventContext);
        }
    }

    /**
     * Asynchronous notification dispatcher.
     */
    private static class AsyncLocalNotifyRemoveCallback implements AsyncProcessor.IAsyncTask {

        /**
         * The listener class
         */
        private ICacheEventsListener _listener;
        /**
         * Message to broadcast
         */
        private Object _key;
        /**
         * Callbackentry, having callback info.
         */
        private Object _entry;
        /**
         * reaon for item removal
         */
        private ItemRemoveReason _reason = ItemRemoveReason.values()[0];
        private OperationContext _operationContext;
        private EventContext _eventContext;

        /**
         * Constructor
         *
         * @param listener
         * @param data
         */
        public AsyncLocalNotifyRemoveCallback(ICacheEventsListener listener, Object key, Object entry, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {
            _listener = listener;
            _key = key;
            _entry = entry;
            _reason = reason;
            _operationContext = operationContext;
            _eventContext = eventContext;
        }

        /**
         * Implementation of message sending.
         */
        public void Process() throws OperationFailedException {
            _listener.OnCustomRemoveCallback(_key, _entry, _reason, _operationContext, _eventContext);
        }
    }

    /**
     * Asynchronous hashmap notification dispatcher
     */
    private static class AsyncLocalNotifyHashmapCallback implements AsyncProcessor.IAsyncTask {

        private ICacheEventsListener _listener;
        private NewHashmap _hashMap;
        private boolean _updateClientMap;

        public AsyncLocalNotifyHashmapCallback(ICacheEventsListener listener, long lastViewid, java.util.HashMap newmap, java.util.ArrayList members, boolean updateClientMap) {
            this._listener = listener;
            this._hashMap = new NewHashmap(lastViewid, newmap, members);
            this._updateClientMap = updateClientMap;

        }

        public void Process() {
            _listener.OnHashmapChanged(this._hashMap, this._updateClientMap);
        }
    }

    /**
     * Enumeration that defines flags for various notifications.
     */
    public enum Notification {

        None(0x0000),
        ItemAdd(0x0001),
        ItemUpdate(0x0002),
        ItemRemove(0x0004),
        CacheClear(0x0008),
        All(0x0001 | 0x0002 | 0x0004 | 0x0008);

        private int intValue;
        private int multipleVal = 0;
        private java.util.HashMap<Integer, Notification> mappings;

        private java.util.HashMap<Integer, Notification> getMappings() {
            if (mappings == null) {
                synchronized (Notification.class) {
                    if (mappings == null) {
                        mappings = new java.util.HashMap<Integer, Notification>();
                    }
                }
            }
            return mappings;
        }

        private Notification(int value) {
            intValue = value;
            this.getMappings().put(value, this);
        }

        public int getValue() {
            return intValue;
        }

        public int getMulValue() {
            return multipleVal;
        }

        public void setMulVal(int values) {
            multipleVal = values;
        }

        public static Notification forValue(int value) {
            return Notification.All.getMappings().get(value);
        }
    }

    public class NotificationWrapper {

        private EnumSet<Notification> notfications = EnumSet.of(Notification.None);

        public NotificationWrapper(Notification notif) {
            if (notif == Notification.None) {
                this.notfications.clear();
                this.notfications.add(notif);
            } else if (notif == Notification.All) {
                this.notfications.clear();
                this.notfications.add(Notification.CacheClear);
                this.notfications.add(Notification.ItemAdd);
                this.notfications.add(Notification.ItemRemove);
                this.notfications.add(Notification.ItemUpdate);
                this.notfications.add(Notification.All);
            } else {
                this.notfications.clear();
                this.notfications.add(notif);
            }
        }

        public boolean isItemAdd() {
            if (!this.notfications.contains(Notification.None)) {
                return this.notfications.contains(Notification.ItemAdd) | this.notfications.contains(Notification.All);
            }
            return false;
        }

        public boolean isItemRemove() {
            if (!this.notfications.contains(Notification.None)) {
                return this.notfications.contains(Notification.ItemRemove) | this.notfications.contains(Notification.All);
            }
            return false;
        }

        public boolean isItemUpdate() {
            if (!this.notfications.contains(Notification.None)) {
                return this.notfications.contains(Notification.ItemUpdate) | this.notfications.contains(Notification.All);
            }
            return false;
        }

        public boolean isCacheClear() {
            if (!this.notfications.contains(Notification.None)) {
                return this.notfications.contains(Notification.CacheClear) | this.notfications.contains(Notification.All);
            }
            return false;
        }

        public boolean isAll() {
            if (!this.notfications.contains(Notification.None)) {
                return this.notfications.contains(Notification.All);
            }
            return false;
        }

        public void add(Notification notif) {
            if (notif == Notification.None) {
                this.notfications.clear();
                this.notfications.add(notif);
            } else if (notif == Notification.All) {
                this.notfications.clear();
                this.notfications.add(Notification.CacheClear);
                this.notfications.add(Notification.ItemAdd);
                this.notfications.add(Notification.ItemRemove);
                this.notfications.add(Notification.ItemUpdate);
                this.notfications.add(Notification.All);
            } else {
                if (this.notfications.contains(Notification.None)) {
                    this.notfications.remove(Notification.None);
                }
                this.notfications.add(notif);
            }
        }

        public void remove(Notification notif) {
            this.notfications.remove(notif);
        }

        private NotificationWrapper() {
        }
    }

    /**
     * Name of the cache
     */
    private String _name = "";
    /**
     * listener of Cache events.
     */
    private ICacheEventsListener _listener;
    /**
     * Reader, writer lock to be used for synchronization.
     */
    protected ReaderWriterLock _syncObj;
    /**
     * The runtime context associated with the current cache.
     */
    protected CacheRuntimeContext _context;
    /**
     * Flag that controls notifications.
     */
    private NotificationWrapper _notifiers = new NotificationWrapper(Notification.None);
    private boolean _isInProc = false;
    private boolean _keepDeflattedObjects = false;
    public BufferedWriter writer;
    public IAlertPropagator alertPropagator;

    /**
     * Default constructor.
     */
    protected CacheBase() { 
    }

    /**
     * Overloaded constructor. Takes the listener as parameter.
     *
     * @param listener listener of Cache events.
     */
    public CacheBase(java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) {
        _context = context;
        alertPropagator = context.getEmailAlertNotifier();
        _listener = listener;
        _syncObj = new ReaderWriterLock();
        _isInProc = _context.getCacheRoot().getIsInProc();
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public void dispose() {
        try {
            if (writer != null) {
                synchronized (writer) {
                    writer.close();
                    writer = null;
                }
            }
            _listener = null;
            _syncObj = null;
        } catch (Exception Exception) {
        }
    }

    public void StopServices() throws java.lang.InterruptedException {
    }

    public final boolean getIsInProc() {
        return _isInProc;
    }

    public final void setIsInProc(boolean value) {
        _isInProc = value;
    }

    public final boolean getKeepDeflattedValues() {
        return _keepDeflattedObjects;
    }

    public final void setKeepDeflattedValues(boolean value) {
        _keepDeflattedObjects = value;
    }

    /**
     * get/set the name of the cache.
     */
    public String getName() {
        return _name;
    }

    public void setName(String value) {
        _name = value;
    }

    public final ILogger getCacheLog() {
        return _context.getCacheLog();
    }

    
    public boolean getVirtualUnlimitedSpace()
    {
        return false;
    }
    
    public void setVirtualUnlimitedSpace(boolean isVirtualUnlimitedSpace)
    {
        
    }
    
    /**
     * Gets the CacheRunTime context.
     */
    public final CacheRuntimeContext getContext() {
        return _context;
    }

    /**
     * get/set listener of Cache events. 'null' implies no listener.
     */
    public ICacheEventsListener getListener() {
        return _listener;
    }

    public void setListener(ICacheEventsListener value) {
        _listener = value;
    }

    /**
     * Notifications are enabled.
     */
    public NotificationWrapper getNotifiers() {
        return _notifiers;
    }

    public void setNotifiers(NotificationWrapper value) {
        _notifiers = value;
    }

    /**
     * get the synchronization object for this store.
     */
    public final ReaderWriterLock getSync() {
        return _syncObj;
    }

    /**
     * returns the number of objects contained in the cache.
     */
    public long getCount() throws GeneralFailureException, OperationFailedException, CacheException {
        return 0;
    }

    /**
     * Get the size of data in store, in bytes.
     */
    public long getSize() {
        return 0;
    }

    public long getSessionCount() {
        return 0;
    }

    public InetAddress getServerJustLeft() {
        return null;
    }

    public void setServerJustLeft(InetAddress value) {
        ;
    }

    public int getServersCount() {
        return 0;
    }

    public boolean IsServerNodeIp(Address clientAddress) {
        return false;
    }

    /**
     * returns the statistics of the Clustered Cache.
     * @return 
     */
    public CacheStatistics getStatistics() {
        return null;
    }

    public java.util.ArrayList<CacheNodeStatistics> GetCacheNodeStatistics() throws GeneralFailureException, OperationFailedException, CacheException {
        return null;
    }

    public CacheStatistics getActualStats() {
        return null;
    }

    public TypeInfoMap getTypeInfoMap() {
        return null;
    }

    public boolean getIsEvictionAllowed() {
        return true;
    }

    public void setIsEvictionAllowed(boolean value) {
    }

    /**
     * Returns true if cache is started as backup mirror cache, false otherwise.
     */
    public boolean getIsStartedAsMirror() {
        return false;
    }

    //: made public from protected so that it may be accessed outside the project
    /**
     * Returns the cache local to the node, i.e., internal cache.
     */
    public CacheBase getInternalCache() {
        return null;
    }

    public long getMaxSize() {
        return 0;
    }

    public void setMaxSize(long value) throws Exception {
    }

    public float getEvictRatio() {
        return 0;
    }

    public void setEvictRatio(float value) {
    }

    public void UpdateLockInfo(Object key, Object lockId, java.util.Date lockDate, LockExpiration lockExpiration, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
    }

    public boolean CanChangeCacheSize(long size) {
        
        if(getContext() != null && getContext().getCacheImpl() != null && getContext().getCacheImpl().getInternalCache() != null)
            return getContext().getCacheImpl().getInternalCache().CanChangeCacheSize(size);
        
        return false;
    }

    public void EnqueueForReplication(Object key, int opCode, Object data) {
    }

    public void EnqueueForReplication(Object key, int opCode, Object data, int size, Object[] userPayLoad, long payLoadSize) {
    }

    public boolean getRequiresReplication() {
        return false;
    }

    public boolean IsBucketFunctionalOnReplica(Object key) {
        return false;
    }

   

    public Object[] getKeys() {
        return null;
    }


    /**
     * Removes a bucket completely.
     *
     * @param bucket
     */
    public void RemoveBucket(int bucket) throws LockingException, StateTransferException, ClassNotFoundException, java.io.IOException, OperationFailedException, CacheException {
    }

    /**
     * Removes all the extra buckets that do not belong to this instance;
     * according to BucketOwnerShipMap.
     *
     * @param bucketIds
     */
    public void RemoveExtraBuckets(java.util.ArrayList bucketIds) throws LockingException, StateTransferException, OperationFailedException, CacheException {
    }

    public void DoWrite(String module, String message, OperationContext operationContext) {
        try {
            if (writer != null) {
                synchronized (writer) {
                    writer.write("[" + module + "]" + message + "\t" + Calendar.getInstance().getTime().toString() + "\n");
                }
            }
        } catch (IOException iOException) {
            this.getCacheLog().Error("CacheBase.DoWrite", iOException.getMessage());
        }
    }

    /**
     * Method that allows the object to initialize itself. Passes the property
     * map down the object hierarchy so that other objects may configure
     * themselves as well..
     *
     * @param cacheSchemes collection of cache schemes (config properties).
     * @param properties properties collection for this cache.
     */
    protected void Initialize(java.util.Map cacheClasses, java.util.Map properties) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {

            setName(String.valueOf(properties.get("id")));
            if (properties.containsKey("notifications")) {
                java.util.Map notifconfig = (java.util.Map) ((properties.get("notifications") instanceof java.util.Map) ? properties.get("notifications") : null);
                if (notifconfig.containsKey("item-add")) {
                    if ((Boolean.parseBoolean(notifconfig.get("item-add").toString()))) {
                        _notifiers.add(Notification.ItemAdd);
                    }
                }
                if (notifconfig.containsKey("item-update")) {
                    if ((Boolean.parseBoolean(notifconfig.get("item-update").toString()))) {
                        _notifiers.add(Notification.ItemUpdate);
                    }
                }
                if (notifconfig.containsKey("item-remove")) {
                    if ((Boolean.parseBoolean(notifconfig.get("item-remove").toString()))) {
                        _notifiers.add(Notification.ItemRemove);
                    }
                }
                if (notifconfig.containsKey("cache-clear")) {
                    if ((Boolean.parseBoolean(notifconfig.get("cache-clear").toString()))) {
                        _notifiers.add(Notification.CacheClear);
                    }
                }
            } else {
                _notifiers.add(Notification.All);
            }

        } 
        catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    public void Initialize(java.util.Map cacheClasses, java.util.Map properties, String userId, String password, boolean twoPhaseInitialization) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            setName(String.valueOf(properties.get("id")));

            if (properties.containsKey("notifications")) {
                java.util.Map notifconfig = (java.util.Map) ((properties.get("notifications") instanceof java.util.Map) ? properties.get("notifications") : null);
                if (notifconfig.containsKey("item-add")) {
                    if ((Boolean) (notifconfig.get("item-add"))) {
                        _notifiers.add(Notification.ItemAdd);
                    }
                }
                if (notifconfig.containsKey("item-update")) {
                    if ((Boolean) (notifconfig.get("item-update"))) {
                        _notifiers.add(Notification.ItemUpdate);
                    }
                }
                if (notifconfig.containsKey("item-remove")) {
                    if ((Boolean) (notifconfig.get("item-remove"))) {
                        _notifiers.add(Notification.ItemRemove);
                    }
                }
                if (notifconfig.containsKey("cache-clear")) {
                    if ((Boolean) (notifconfig.get("cache-clear"))) {
                        _notifiers.add(Notification.CacheClear);
                    }
                }
            } else {
                _notifiers.add(Notification.All);
            }

        }
        catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    
    public void ClientConnected(String client, boolean isInproc) throws OperationFailedException {

        CacheStatistics stats = getInternalCache().getStatistics();

        if (stats != null && stats.getConnectedClients() != null) {
            synchronized (stats.getConnectedClients()) {
                if (!stats.getConnectedClients().contains(client)) {
                    stats.getConnectedClients().add(client);
                }
            }
        }
    }

    public void ClientDisconnected(String client, boolean isInproc) throws OperationFailedException {
        CacheStatistics stats = getInternalCache().getStatistics();
        if (stats != null && stats.getConnectedClients() != null) {
            synchronized (stats.getConnectedClients()) {
                if (stats.getConnectedClients().contains(client)) {
                    stats.getConnectedClients().remove(client);
                }
            }
        }
    }
    
   public void RegisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException, TimeoutException {

    }

    public void RegisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException, TimeoutException {
    }

    public void UnregisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
    }

    public void UnregisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
    }

    public void SetStateTransferKeyList(java.util.HashMap keylist) {
    }

    public void UnSetStateTransferKeyList() {
    }

    public java.util.ArrayList<com.alachisoft.tayzgrid.persistence.Event> GetFilteredEvents(String clientID, java.util.HashMap events, EventStatus registeredEventStatus) throws GeneralFailureException, Exception {
        return null;
    }

    public void RemoveBucketData(int bucketId) throws LockingException, StateTransferException, OperationFailedException, CacheException {
    }

    public void RemoveBucketData(java.util.ArrayList bucketIds) throws LockingException, StateTransferException, OperationFailedException, CacheException {
        if (bucketIds != null && bucketIds.size() > 0) {
            for (int i = 0; i < bucketIds.size(); i++) {
                RemoveBucketData((Integer) bucketIds.get(i));
            }
        }
    }

    public java.util.ArrayList GetKeyList(int bucketId, boolean startLogging) {
        return null;
    }

    public java.util.HashMap GetLogTable(java.util.ArrayList bucketIds, tangible.RefObject<Boolean> isLoggingStopped) {
        return null;
    }

    public void RemoveFromLogTbl(int bucketId) {
    }

    public void StartLogging(int bucketId) {
    }

    public void AddLoggedData(java.util.ArrayList bucketIds) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
    }

    public void UpdateLocalBuckets(java.util.ArrayList bucketIds) {
    }

    public java.util.HashMap getLocalBuckets() {
        return null;
    }

    public void setLocalBuckets(java.util.HashMap value) {
    }

    public void setBucketSize(int value) {
    }

    public long getCurrentViewId() {
        return 0;
    }

    public long getOperationSequenceId() {
        return 0;
    }

    public NewHashmap GetOwnerHashMapTable(tangible.RefObject<Integer> bucketSize) {
        bucketSize.argvalue = 0;
        return null;
    }

    public void UpdateClientsList(java.util.HashMap list) {
    }

    public boolean AcceptClient(InetAddress clientAddress) {
        return false;
    }

    public void DisconnectClient(InetAddress clientAddress) {
    }

    /**
     * Removes all entries from the store.
     */
    public void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
    }

    /**
     * Removes all entries from the store.
     */
    public void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, String taskId, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
    }

    public boolean Contains(Object key, String group, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return false;
    }

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    @Override
    public boolean Contains(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return Contains(key, null, operationContext);
    }

    public CacheEntry Get(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        Object lockId = null;
        java.util.Date lockDate = new java.util.Date();
        long version = 0;
        tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        CacheEntry tempVar = Get(key, tempRef_version, tempRef_lockId, tempRef_lockDate, null, LockAccessType.IGNORE_LOCK, operationContext);
        version = tempRef_version.argvalue;
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        return tempVar;
    }

    public CacheEntry Get(Object key, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        Object lockId = null;
        java.util.Date lockDate = new java.util.Date();
        long version = 0;

        tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        CacheEntry tempVar = Get(key, isUserOperation, tempRef_version, tempRef_lockId, tempRef_lockDate, null, LockAccessType.IGNORE_LOCK, operationContext);
        version = tempRef_version.argvalue;
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        return tempVar;
    }

    public CacheEntry Get(Object key, boolean isUserOperation, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Retrieve the object from the cache. A string key is passed as parameter.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    @Override
    public CacheEntry Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        lockId.argvalue = null;
        lockDate.argvalue = new java.util.Date();
        return null;
    }

    public java.util.HashMap GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    public java.util.ArrayList GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    public java.util.HashMap Remove(String[] tags, TagComparisonType tagComparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    public LockOptions Lock(Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        lockId.argvalue = null;
        lockDate.argvalue = new java.util.Date();
        return null;
    }

    public LockOptions IsLocked(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        lockId.argvalue = null;
        lockDate.argvalue = new java.util.Date();
        return null;
    }

    public void UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
    }

    /**
     * Get the item size stored in cache
     *
     * @param key key
     * @return item size
     */
    public int GetItemSize(Object key) {
        return 0;
    }

    /**
     * Returns the list of keys in the group or sub group
     *
     * @param group group for which keys are required
     * @param subGroup sub group within the group
     * @return list of keys in the group
     */
    public java.util.ArrayList GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    /**
     * Checks whether data group exist or not.
     *
     * @param group
     * @return
     */
    public boolean GroupExists(String group) {
        return false;
    }

    /**
     * Gets the group information of the item.
     *
     * @param key Key of the item
     * @return GroupInfo for the item.
     */
    public GroupInfo GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    /**
     * Gets the group information of the items.
     *
     * @return GroupInfo for the item.
     */
    public java.util.HashMap GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    /**
     * Gets list of data groups.
     */
    public java.util.ArrayList getDataGroupList() {
        return null;
    }

    /**
     * Returns the list of key and value pairs in the group or sub group
     *
     * @param group group for which data is required
     * @param subGroup sub group within the group
     * @return list of key and value pairs in the group
     */
    public java.util.HashMap GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    public CacheEntry GetGroup(Object key, String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        Object lockId = null;
        java.util.Date lockDate = new java.util.Date();
        long version = 0;
        tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        CacheEntry tempVar = GetGroup(key, group, subGroup, tempRef_version, tempRef_lockId, tempRef_lockDate, null, LockAccessType.IGNORE_LOCK, operationContext);
        version = tempRef_version.argvalue;
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        return tempVar;
    }

    public CacheEntry GetGroup(Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    /**
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @param notify boolean specifying to raise the event.
     * @param operationContext
     * @return returns the result of operation.
     * @throws OperationFailedException, CacheException
     */
    @Override
    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException, SuspectedException, TimeoutException {
        return CacheAddResult.Failure;
    }

    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, String taskId, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, TimeoutException, SuspectedException {
        return CacheAddResult.Failure;
    }

    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return CacheAddResult.Failure;
    }

    /**
     * Add an ExpirationHint against a given key Key must already exist in the
     * cache
     *
     * @param key
     * @param eh
     */
    public boolean Add(Object key, ExpirationHint eh, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return Add(key, null, eh, operationContext);
    }

    public boolean Add(Object key, String group, ExpirationHint eh, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return false;
    }
 

    public boolean Add(Object key, String group, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return false;
    }

    /**
     * Adds a pair of key and value to the cache. If the specified key already
     * exists in the cache; it is updated, otherwise a new item is added to the
     * cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @param notify boolean specifying to raise the event.
     * @param accessType
     * @return returns the result of operation.
     */
    @Override
    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return new CacheInsResultWithEntry();
    }

    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, String taskId, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return new CacheInsResultWithEntry();
    }

    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, boolean isUserOperation, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return new CacheInsResultWithEntry();
    }

    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     * @param notify boolean specifying to raise the event.
     */
    public java.util.HashMap Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
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
    public CacheEntry Remove(Object key, ItemRemoveReason removalReason, boolean notify, String taskId, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    @Override
    public CacheEntry Remove(Object key, ItemRemoveReason removalReason, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return Remove(key, null, removalReason, notify, lockId, version, accessType, operationContext);
    }

    public CacheEntry Remove(Object key, ItemRemoveReason removalReason, boolean notify, boolean isUserOperation, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    public CacheEntry Remove(Object key, String group, ItemRemoveReason removalReason, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Remove item from the cluster to synchronize the replicated nodes.
     * [WARNING]This method should be only called while removing items from the
     * cluster in order to synchronize them.[]
     *
     * @param keys
     * @param reason
     * @param notify
     * @return
     */
    public Object RemoveSync(Object[] keys, ItemRemoveReason reason, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Broadcasts a user-defined event across the cluster.
     *
     * @param notifId
     * @param data
     * @param async
     */
    public void SendNotification(Object notifId, Object data) {
    }

    public QueryResultSet Search(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    public QueryResultSet SearchEntries(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    public DeleteQueryResultSet DeleteQuery(String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        return null;
    }

    public java.util.HashMap Contains(Object[] keys, String group, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param keys The keys to locate in the cache.
     * @param operationContext
     * @return List of keys that are not found in the cache.
     * @throws OperationFailedException, CacheException
     * @throws LockingException
     * @throws GeneralFailureException
     */
    @Override
    public java.util.HashMap Contains(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return Contains(keys, null, operationContext);
    }

    /**
     * Retrieve the objects from the cache. An array of keys is passed as
     * parameter.
     *
     * @param keys keys of the entries.
     * @return key and value pairs
     */
    @Override
    public java.util.HashMap Get(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return Get(keys, operationContext);
    }

    public java.util.HashMap GetGroup(Object[] keys, String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return null;
    }

    /**
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @param notify boolean specifying to raise the event.
     * @return List of keys that are added or that alredy exists in the cache
     * and their status
     */
    @Override
    public java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    public java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, String taskId, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Adds key and value pairs to the cache. If any of the specified key
     * already exists in the cache; it is updated, otherwise a new item is added
     * to the cache.
     *
     * @param keys keys of the entries.
     * @param cacheEntries the cache entries.
     * @param notify boolean specifying to raise the event.
     * @return returns successful keys and thier status.
     */
    @Override
    public java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    public java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, String taskId, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Removes key and value pairs from the cache. The keys are specified as
     * parameter. Moreover it take a removal reason and a boolean specifying if
     * a notification should be raised.
     *
     * @param keys keys of the entries.
     * @param removalReason reason for the removal.
     * @param notify boolean specifying to raise the event.
     * @return List of keys and values that are removed from cache
     */
    @Override
    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason removalReason, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return Remove(keys, null, removalReason, notify, operationContext);
    }

    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason removalReason, boolean notify, String taskId, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    public java.util.HashMap Remove(Object[] keys, String group, ItemRemoveReason removalReason, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason removalReason, boolean notify, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     *
     * @return Iterator enumerator.
     */
    public ResetableIterator GetEnumerator() throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return GetEnumerator(null);
    }

    public ResetableIterator GetEnumerator(String group) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    public EnumerationDataChunk GetNextChunk(EnumerationPointer pointer, OperationContext operationContext) throws GeneralFailureException, TimeoutException, SuspectedException, CacheException {
        return null;
    }

    public boolean HasEnumerationPointer(EnumerationPointer pointer) {
        return false;
    }

    /**
     * Notifications are enabled.
     */
    protected final boolean getIsItemAddNotifier() {
        return getNotifiers().isItemAdd();
    }

    protected final boolean getIsItemUpdateNotifier() {
        return getNotifiers().isItemUpdate();
    }

    protected final boolean getIsItemRemoveNotifier() {
        return getNotifiers().isItemRemove();
    }

    protected final boolean getIsCacheClearNotifier() {
        return getNotifiers().isCacheClear();
    }

    /**
     * Notify the listener that an item is added to the cache.
     *
     * @param key key of the cache item
     * @param async flag indicating that the nofitication is asynchronous
     */
    protected void NotifyItemAdded(Object key, boolean async, OperationContext operationContext, EventContext eventContext) {
        if ((getListener() != null) && getIsItemAddNotifier()) {
            if (!async) {
                getListener().OnItemAdded(key, operationContext, eventContext);
            } else {
                _context.AsyncProc.Enqueue(new AsyncLocalNotifyAdd(getListener(), key, operationContext, eventContext));
            }
        }
    }

    /**
     * Notifies the listener that an item is updated which has an item update
     * callback.
     *
     * @param key key of cache item
     * @param entry Callback entry which contains the item update call back.
     * @param async flag indicating that the nofitication is asynchronous
     */
    protected void NotifyCustomUpdateCallback(Object key, Object value, boolean async, OperationContext operationContext, EventContext eventContext) {
        if ((getListener() != null)) {
            if (!async) {
                getListener().OnCustomUpdateCallback(key, value, operationContext, eventContext);
            } else {
                _context.AsyncProc.Enqueue(new AsyncLocalNotifyUpdateCallback(getListener(), key, value, operationContext, eventContext));
            }
        }
    }

    /**
     * Notifies the listener that an item is removed which has an item removed
     * callback.
     *
     * @param key key of cache item
     * @param value Callback entry which contains the item remove call back.
     * @param async flag indicating that the nofitication is asynchronous
     */
    public void NotifyCustomRemoveCallback(Object key, Object value, ItemRemoveReason reason, boolean async, OperationContext operationContext, EventContext eventContext) throws ClassNotFoundException, OperationFailedException {
        if ((getListener() != null)) {
            if (!async) {
                getListener().OnCustomRemoveCallback(key, value, reason, operationContext, eventContext);
            } else {
                boolean notify = true;

                if (reason == ItemRemoveReason.Expired) {

                    int notifyOnExpirationCount = 0;

                    if (eventContext != null) {
                        Object tempVar = eventContext.GetValueByField(EventContextFieldName.ItemRemoveCallbackList);
                        java.util.ArrayList removedListener = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                        if (removedListener != null) {
                            for (int i = 0; i < removedListener.size(); i++) {
                                CallbackInfo removeCallbackInfo = (CallbackInfo) removedListener.get(i);
                                if (removeCallbackInfo != null && removeCallbackInfo.getNotifyOnExpiration()) {
                                    notifyOnExpirationCount++;
                                }
                            }
                        }
                    }

                    if (notifyOnExpirationCount <= 0) {
                        notify = false;
                    }
                }

                if (notify) {
                    _context.AsyncProc.Enqueue(new AsyncLocalNotifyRemoveCallback(getListener(), key, value, reason, operationContext, eventContext));
                }
            }
        }
    }

    /**
     * Notify the listener that an item is updated in the cache.
     *
     * @param key key of the cache item
     * @param async flag indicating that the nofitication is asynchronous
     */
    protected void NotifyItemUpdated(Object key, boolean async, OperationContext operationContext, EventContext eventContext) {
        if ((getListener() != null) && getIsItemUpdateNotifier()) {
            if (!async) {
                getListener().OnItemUpdated(key, operationContext, eventContext);
            } else {
                _context.AsyncProc.Enqueue(new AsyncLocalNotifyUpdate(getListener(), key, operationContext, eventContext));
            }
        }
    }

    /**
     * Notify the listener that an item is removed from the cache.
     *
     * @param key key of the cache item
     * @param val item itself
     * @param reason reason the item was removed
     * @param async flag indicating that the nofitication is asynchronous
     */
    protected void NotifyItemRemoved(Object key, Object val, ItemRemoveReason reason, boolean async, OperationContext operationContext, EventContext eventContext) throws OperationFailedException, CacheException, StateTransferException, LockingException {
        if ((getListener() != null) && getIsItemRemoveNotifier()) {
            if (!async) {
                getListener().OnItemRemoved(key, val, reason, operationContext, eventContext);
            } else {
                _context.AsyncProc.Enqueue(new AsyncLocalNotifyRemoval(getListener(), key, val, reason, operationContext, eventContext));
            }
        }
    }

    /**
     * Notify the listener that items are removed from the cache.
     *
     * @param reason reason the item was removed
     * @param async flag indicating that the nofitication is asynchronous
     */
    public void NotifyItemsRemoved(Object[] keys, Object[] vals, ItemRemoveReason reason, boolean async, OperationContext operationContext, EventContext[] eventContext) throws OperationFailedException, CacheException, StateTransferException, LockingException {
        if ((getListener() != null) && getIsItemRemoveNotifier()) {
            if (!async) {
                getListener().OnItemsRemoved(keys, vals, reason, operationContext, eventContext);
            } else {
                _context.AsyncProc.Enqueue(new AsyncLocalNotifyRemoval(getListener(), keys, vals, reason, operationContext, eventContext));
            }
        }
    }

    /**
     * Fire when the cache is cleared.
     *
     * @param async flag indicating that the nofitication is asynchronous
     */
    protected void NotifyCacheCleared(boolean async, OperationContext operationContext, EventContext eventContext) {
        if ((getListener() != null) && getIsCacheClearNotifier()) {
            if (!async) {
                getListener().OnCacheCleared(operationContext, eventContext);
            } else {
                _context.AsyncProc.Enqueue(new AsyncLocalNotifyCacheClear(getListener(), operationContext, eventContext));
            }
        }
    }

    /**
     * Fire when user wishes.
     *
     * @param notifId
     * @param data
     * @param async
     */
    public void NotifyCustomEvent(Object notifId, Object data, boolean async, OperationContext operationContext, EventContext eventContext) {
        if ((getListener() != null)) {
            if (!async) {
                getListener().OnCustomEvent(notifId, data, operationContext, eventContext);
            } else {
                _context.AsyncProc.Enqueue(new AsyncLocalNotifyCustomEvent(getListener(), notifId, data, operationContext, eventContext));
            }
        }
    }

    /**
     * Notify all connected clients that hashmap has changed
     *
     * @param viewId View id
     * @param newmap New hashmap
     * @param members Current members list (contains Address)
     * @param async
     */
    protected void NotifyHashmapChanged(long viewId, java.util.HashMap newmap, java.util.ArrayList members, boolean async, boolean updateClientMap) {
        if (getListener() != null) {
            _context.AsyncProc.Enqueue(new AsyncLocalNotifyHashmapCallback(getListener(), viewId, newmap, members, updateClientMap));
        }
    }

    /**
     *
     *
     * @param operationCode
     * @param result
     * @param cbEntry
     */
    protected void NotifyWriteBehindTaskCompleted(OpCode operationCode, java.util.HashMap result, CallbackEntry cbEntry, OperationContext operationContext) {
        if (getListener() != null) {
            getInternalCache().DoWrite("CacheBase.NotifyWriteBehindTaskCompleted", "", operationContext);
            getListener().OnWriteBehindOperationCompletedCallback(operationCode, result, cbEntry);
        }
    }

    public void ReplicateConnectionString(String connString, boolean isSql) throws GeneralFailureException, CacheException {
    }

    public void ReplicateOperations(Object[] keys, Object[] cacheEntries, Object[] userPayloads, java.util.ArrayList compilationInfo, long seqId, long viewId) throws GeneralFailureException, CacheException {
    }

    public void ValidateItems(java.util.ArrayList keys, java.util.ArrayList userPayloads) {
    }

    public void ValidateItems(Object key, Object userPayloads) {
    }




  
  



 

    /**
     * Removes any occurances of old keys in the new Keys table.
     *
     * @param pKeys Contains old Depending Keys.
     * @param nKeys Contains new Depending Keys.
     * @return
     */
    public final java.util.HashMap GetFinalKeysList(Object[] pKeys, Object[] nKeys) {
        java.util.HashMap table = new java.util.HashMap();

        if (pKeys == null || nKeys == null) {
            table.put("oldKeys", new Object[0]);
            table.put("newKeys", new Object[0]);
        } else if (pKeys != null && nKeys != null) {

            java.util.ArrayList oldKeys = new java.util.ArrayList(Arrays.asList(pKeys));
            java.util.ArrayList newKeys = new java.util.ArrayList(Arrays.asList(nKeys));

            for (int i = 0; i < pKeys.length; i++) {
                for (int j = 0; j < nKeys.length; j++) {
                    if (pKeys[i] == nKeys[j]) {

                        oldKeys.remove(pKeys[i]);
                        newKeys.remove(nKeys[j]);
                        break;
                    }
                }
            }
            table.put("oldKeys", oldKeys.toArray(new Object[0]));
            table.put("newKeys", newKeys.toArray(new Object[0]));
        }
        return table;
    }

    /**
     * Create a key table that will then be used to remove old dependencies or
     * create new dependencies
     *
     * @param key key associated with entry
     * @param keys Array of keys
     * @return Table containing keys in order
     */
    protected final java.util.HashMap GetKeysTable(Object key, Object[] keys) {
        if (keys == null) {
            return null;
        }

        java.util.HashMap keyTable = new java.util.HashMap(keys.length);
        for (int i = 0; i < keys.length; i++) {
            if (!keyTable.containsKey(keys[i])) {
                keyTable.put(keys[i], new java.util.ArrayList());
            }
            ((java.util.ArrayList) keyTable.get(keys[i])).add(key);
        }
        return keyTable;
    }

    /**
     *
     *
     * @param operationCode
     * @param result
     * @param entry
     * @param taskId
     */
    public void NotifyWriteBehindTaskStatus(OpCode operationCode, java.util.HashMap result, CallbackEntry cbEntry, String taskId, String providerName, OperationContext operationContext) throws OperationFailedException {
        if (cbEntry != null && cbEntry.getWriteBehindOperationCompletedCallback() != null) {
            NotifyWriteBehindTaskCompleted(operationCode, result, cbEntry, operationContext);
        }
    }

    /**
     * Returns the thread safe synchronized wrapper over cache.
     *
     * @param cacheStore
     * @return
     */
    public static CacheBase Synchronized(CacheBase cache) {
        return new CacheSyncWrapper(cache);
    }

    public void BalanceDataLoad() throws SuspectedException, TimeoutException, GeneralFailureException {
    }



    public void EnqueueDSOperation(DSWriteBehindOperation operation) throws Exception {
        if (operation.getTaskId() == null) {
            operation.setTaskId(new com.alachisoft.tayzgrid.caching.util.GUID().toString());
        }
        _context.getDsMgr().WriteBehind(operation);
    }

    public void NotifyWriteBehindTaskStatus(HashMap opResult, String[] taskIds, String provider, OperationContext context) throws OperationFailedException {
        CallbackEntry cbEntry = null;
        HashMap status = new HashMap();
        if (opResult != null) {
            for (Object keyValue : opResult.entrySet()) {
                Map.Entry entry = (Map.Entry) keyValue;
                DSWriteBehindOperation dsOperation = entry.getValue() instanceof DSWriteBehindOperation ? (DSWriteBehindOperation) entry.getValue() : null;
                if (dsOperation == null) {
                    continue;
                }
                cbEntry = dsOperation.getEntry().getValue() instanceof CallbackEntry ? (CallbackEntry) dsOperation.getEntry().getValue() : null;
                if (cbEntry != null && cbEntry.getWriteBehindOperationCompletedCallback() != null) {
                    if (dsOperation.getException() != null) {
                        status.put(dsOperation.getKey(), dsOperation.getException());
                    } else {
                        status.put(dsOperation.getKey(), dsOperation.getDSOpState());
                    }
                    NotifyWriteBehindTaskCompleted(dsOperation.getOperationCode(), status, cbEntry, context);
                }
            }
        }
    }

    public void EnqueueDSOperation(ArrayList operations) throws Exception {
        _context.getDsMgr().WriteBehind(operations);
    }

    public void NotifyBlockActivity(String uniqueId, long interval) {

    }

    public void NotifyCacheLoaderExecution() {

    }


    public void WindUpReplicatorTask() {

    }

    public void WaitForReplicatorTask(long interval) {

    }

    public java.util.ArrayList<ShutDownServerInfo> GetShutDownServers() {
        return null;
    }

    public boolean IsShutdownServer(Address server) {
        return false;
    }

    public void NotifyUnBlockActivity(String uniqueId) {
    }

    public boolean IsOperationAllowed(Object key, AllowedOperationType opType) {
        return true;
    }

    public boolean IsOperationAllowed(Object[] key, AllowedOperationType opType, OperationContext operationContext) {
        return true;
    }

    public boolean IsOperationAllowed(AllowedOperationType opType, OperationContext operationContext) {
        return true;
    }
           
    public void submitMapReduceTask(MapReduceTask task, String taskId, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws NotSupportedException,GeneralFailureException,OperationFailedException

    {
    }
    
    public Object TaskOperationRecieved(MapReduceOperation operation) throws OperationFailedException
    {
        return null;
    }
        
    public void RegisterTaskNotification(String taskID, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws OperationFailedException{        
    }
    
    
    public void UnregisterTaskNotification(String taskID, TaskCallbackInfo callbackInfo, OperationContext operationContext)throws OperationFailedException {     
    }

    public void cancelMapReduceTask(String taskId, boolean cancelAll) throws OperationFailedException
    {}

    public List<TaskEnumeratorResult> getTaskEnumerator(TaskEnumeratorPointer pointer, OperationContext operationContext) throws Exception 
    {
        return null;
    }
    
    public TaskEnumeratorResult getTaskNextRecord(TaskEnumeratorPointer pointer, OperationContext operationContext) throws Exception 
    {
        return null;
    }
    
    public java.util.ArrayList getRunningTasks() throws GeneralFailureException
    { return null; }
    
    public TaskStatus getTaskProgress(String taskId) throws GeneralFailureException
    { return null; }
    

    
    public ArrayList determineClientConnectivity(ArrayList clients) {
        if (clients == null) {
            return null;
        }
        try {
            java.util.ArrayList result = new java.util.ArrayList();
            CacheStatistics stats = (CacheStatistics) ((getInternalCache().getStatistics() instanceof CacheStatistics) ? getInternalCache().getStatistics() : null);
            for (Iterator it = clients.iterator(); it.hasNext();) {
                String client = (String) it.next();
                if (!stats.getConnectedClients().contains(client)) {
                    result.add(client);
                }
            }
            return result;
        } catch (RuntimeException e) 
        {
            _context.getCacheLog().Error("Client-Death-Detection.DetermineClientConnectivity()", e.toString());
        } 
        finally {           
            _context.getCacheLog().Info("Client-Death-Detection.DetermineClientConnectivity()", "determining client connectivity completed");
            }
        return null;                
    }
    
    
    public void declaredDeadClients(ArrayList deadClients) 
    {
        getInternalCache().declaredDeadClients(deadClients);
    }
    
    /**
     * Notifies the listener that an item is updated which has an item update
     * callback.
     *
     * @param taskID
     * @param value
     * @param operationContext
     * @param eventContext
     * @param async flag indicating that the nofitication is asynchronous
     */
    public void NotifyTaskCallback(Object taskID, java.util.List listenerList, boolean async, OperationContext operationContext, EventContext eventContext) {
        if ((getListener() != null)) {
            if (!async) {
                getListener().OnTaskCallback(taskID, listenerList, operationContext, eventContext);
            } else {
                _context.AsyncProc.Enqueue(new AsyncLocalNotifyTaskCallback(getListener(), taskID, listenerList, operationContext, eventContext));
            }
        }
    }
    
    
    public java.util.Map<Object,EntryProcessorResult> invokeEntryProcessor(Object[] keys,EntryProcessor entryProcessor, Object[] arguments,String defaultReadThru,String defaultWriteThru,OperationContext operationContext)
    {        
        return null;        
    }
    
    
     /**
     * Asynchronous notification dispatcher.
     */
    private static class AsyncLocalNotifyTaskCallback implements AsyncProcessor.IAsyncTask {

        /**
         * The listener class
         */
        private ICacheEventsListener _listener;
        /**
         * Message to broadcast
         */
        private Object _taskID;
        private Object _entry;
        private OperationContext _operationContext;
        private EventContext _eventContext;

        /**
         * Constructor
         *
         * @param listener
         * @param data
         */
        public AsyncLocalNotifyTaskCallback(ICacheEventsListener listener, Object taskID, Object entry, OperationContext operationContext, EventContext eventContext) {
            _listener = listener;
            _taskID = taskID;
            _entry = entry;
            _operationContext = operationContext;
            _eventContext = eventContext;
        }

        /**
         * Implementation of message sending.
         */
        public void Process() {
            _listener.OnTaskCallback(_taskID, _entry, _operationContext, _eventContext);
        }
    }
}