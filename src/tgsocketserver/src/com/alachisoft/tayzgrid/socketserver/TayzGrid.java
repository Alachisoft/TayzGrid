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
package com.alachisoft.tayzgrid.socketserver;

import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.EventStatus;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.SocketServerStats;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.caching.util.HotConfig;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationNotSupportedException;
import com.alachisoft.tayzgrid.socketserver.callbacktasks.AsyncOpCompletedCallback;
import com.alachisoft.tayzgrid.socketserver.callbacktasks.DataSourceUpdatedCallbackTask;
import com.alachisoft.tayzgrid.socketserver.callbacktasks.ItemRemoveCallback;
import com.alachisoft.tayzgrid.socketserver.callbacktasks.ItemUpdateCallback;
import com.alachisoft.tayzgrid.socketserver.callbacktasks.TaskCallback;
import com.alachisoft.tayzgrid.socketserver.eventtask.BlockActivityEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.CacheClearedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.CacheStoppedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.ConfigModifiedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.CustomEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.HashmapChangedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.ItemAddedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.ItemRemovedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.ItemUpdatedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.LoggingInfoModifiedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.NodeJoinedEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.NodeLeftEvent;
import com.alachisoft.tayzgrid.socketserver.eventtask.UnBlockActivityEvent;
import java.net.SocketException;

public final class TayzGrid implements ICommandExecuter {

    private ClientManager _client;
    private boolean _isDotNetClient;
    private boolean _isBridgeClient;
    private Cache _cache = null;
    private String _cacheId = null;
    private String _licenceCode = "";
    private NEventStart _onItemUpdatedCallback = null;
    private NEventStart _onItemRemoveCallback = null;
    private NEventStart _asyncOperationCallback = null;
    private NEventStart _itemAdded = null;
    private NEventStart _itemUpdated = null;
    private NEventStart _itemRemoved = null;
    private NEventStart _cacheCleared = null;
    private NEventStart _customNotif = null;
    private NEventStart _dsUpdatedCallback = null;
    private NEventStart _configModified = null;
    private NEventStart _activeQuery = null;
    private NEventStart _nodeJoined = null;
    private NEventStart _nodeLeft = null;
    private NEventStart _cacheStopped = null;
    private NEventStart _cacheBecomeActive = null;
    private NEventStart _hashmapChanged = null;
    private NEventStart _blockClientActivity = null;
    private NEventStart _unblockClientActivity = null;
    private NEventStart _mrTaskCompletedCallback = null;
    private EventDataFilter _addDataFilter = EventDataFilter.None;
    private EventDataFilter _updateDataFilter = EventDataFilter.None;
    private EventDataFilter _removeDataFilter = EventDataFilter.None;
    private short _addSeq = -1;
    private short _removeSeq = -1;
    private short _updateSeq = -1;

    private Object sync_lock_AddDataFilter = new Object();
    private Object sync_lock_UpdateDataFilter = new Object();
    private Object sync_lock_RemoveDataFilter = new Object();

    /**
     * flag to determine if client has registered to cache stopped event. if so,
     * client will be notified about cache stop. We cannot unregister cache
     * stopped event upon client request because it is used to dispose client
     * too.
     */
    private boolean _cacheStoppedEventRegistered = false;

    public TayzGrid(String cacheId, boolean isDonNetClient, ClientManager client) throws Exception {

        this(cacheId, isDonNetClient, client, "", null, null);

    }

    /**
     * Initialize the cache instance.
     */

    public TayzGrid(String cacheId, boolean isDotNetClient, ClientManager client, String licenceInfo,  byte[] userIdBinary, byte[] paswordBinary) throws Exception {

        this._cacheId = cacheId;
        this._isDotNetClient = isDotNetClient;
        this._client = client;
        this._licenceCode = licenceInfo;
      

        try {
            _cache = CacheProvider.getProvider().GetCacheInstanceIgnoreReplica(cacheId, userIdBinary, paswordBinary);
        } catch (Exception e2) {
            throw e2;
        }
        if (_cache == null) {
            throw new Exception("Cache is not registered");
        }
        if (!_cache.getIsRunning()) {
            throw new Exception("Cache is not running");
        }

        _cache.addCustomUpdateNotifListner(new NEventStart() {
            @Override
            public Object hanleEvent(Object... obj) throws SocketException, Exception {

                CustomUpdate(obj[0], obj[1], (EventContext) obj[2]);
                return null;

            }
        }, null);

        _cache.addCustomRemoveNotifListner(new NEventStart() {
            @Override
            public Object hanleEvent(Object... obj) throws SocketException, Exception {

                CustomRemove(obj[0], obj[1], (ItemRemoveReason) obj[2], (BitSet) obj[3], (EventContext) obj[4]);
                return null;
            }
        }, null);

        if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
            SocketServer.getLogger().getCacheLog().Error("NCache.ctor", "Registering cache stopped event for " + _client.getClientID());
        }

        _cache.addCacheStoppedListner(new NEventStart() {
            @Override
            public Object hanleEvent(Object... obj) throws SocketException, Exception {

                OnCacheStopped((String) obj[0], (EventContext) obj[1]);
                return null;
            }
        }, null);

        if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
            SocketServer.getLogger().getCacheLog().Error("NCache.ctor", "Cache stopped event registered for " + _client.getClientID());
        }

        _cache.addAsyncOperationCallbackListner(new NEventStart() {
            @Override
            public Object hanleEvent(Object... obj) throws SocketException, Exception {

                AsyncOperationCompleted(obj[0], obj[1], (EventContext) obj[2]);
                return null;
            }
        }, null);

        _cache.addDsUpdatedListner(new NEventStart() {
            @Override
            public Object hanleEvent(Object... obj) throws SocketException, Exception {

                DataSourceUpdated(obj[0], (CallbackEntry) obj[1], (OpCode) obj[2]);
                return null;
            }
        }, null);

        _cache.addBlockActivity(new NEventStart() {
            @Override
            public Object hanleEvent(Object... obj) throws SocketException, Exception {

                BlockClientActivity((String) obj[0], (String) obj[1], (Long) obj[2], (Integer) obj[3]);
                return null;
            }
        }, null);

        _cache.addUnBlockActivity(new NEventStart() {
            @Override
            public Object hanleEvent(Object... obj) throws SocketException, Exception {

                UnBlockClientActivity((String) obj[0], (String) obj[1], (Integer) obj[2]);
                return null;
            }
        }, null);

        _cache.addTaskNotifListener(new NEventStart() {

            @Override
            public Object hanleEvent(Object... obj) throws SocketException, Exception {                
                TaskCallbackHandler(obj[0], (TaskCallbackInfo)obj[1], (EventContext) obj[2]);
                return null;
            }
        }, null);
        
        _cache.OnClientConnected(client.getClientID(), cacheId);       
        
    }

    /**
     * Get the cacheId
     */
    public String getCacheId() {
        return _cacheId;
    }

    /**
     * Instance of cache You must obtain lock before using cache
     */
    public Cache getCache() {
        return _cache;
    }

    public NEventStart getAsyncOperationCallback() {
        return _asyncOperationCallback;
    }

    public NEventStart getRemoveCallback() {
        return _onItemRemoveCallback;
    }

    public NEventStart getUpdateCallback() {
        return _onItemUpdatedCallback;
    }

    /**
     * Determine whether the client connected is a .net client
     */
    public boolean getIsDotnetClient() {
        return this._isDotNetClient;
    }

    /**
     * This function is called by CacheStoppedCallback
     */
    public void OnCacheStopped(String cacheId, EventContext eventContext) {
        //first of all fire the CacheStoppedCallback for the remote client.
        try {
            if (this._cacheStoppedEventRegistered) {
                CacheStopped();
            }
        } catch (RuntimeException e) {
            if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
                SocketServer.getLogger().getCacheLog().Error("NCache.OnCacheStopped", e.toString());
            }
        }

        //now break the connection of the socket server with the client.
        if (_client != null) {
            _client.OnCacheStopped(cacheId);
        }
    }

     /**
     * Dispose the cahce and Unregister the callbacks being registered with that
     * cache
     */
    public void dispose() {
        if (_cache != null) {

            UnRegisterNotifications();

            _client = null;
        }
    }

    public void DisposeEnumerator(EnumerationPointer pointer) throws OperationFailedException {
        if (_cache != null) {
            //Just a dummy call to dispose enumerator
            pointer.setDisposable(true);
            pointer.setSocketServerDispose(true);
            _cache.GetNextChunk(pointer, new OperationContext());
        }
    }

    /**
     * Unregister the callbacks registered with cache
     */
    private void UnRegisterNotifications() {

        if (_onItemUpdatedCallback != null) {
            _cache.removeCustomUpdateNotifListner(_onItemUpdatedCallback);
            _onItemUpdatedCallback = null;
        }
        if (_onItemRemoveCallback != null) {
            _cache.removeCustomRemoveNotifListner(_onItemRemoveCallback);
            _onItemRemoveCallback = null;
        }

        if (_cacheCleared != null) {
            _cache.removeCacheClearedListner(_cacheCleared);
            _cacheCleared = null;
        }
        if (_cacheStopped != null) {
            _cache.removeCacheStoppedListner(_cacheStopped);
            _cacheStopped = null;
        }

        if (_asyncOperationCallback != null) {
            _cache.removeAsyncOperationCallbackListner(_asyncOperationCallback);
            _asyncOperationCallback = null;
        }
        if (_dsUpdatedCallback != null) {
            _cache.removeDsUpdatedListner(_dsUpdatedCallback);
            _dsUpdatedCallback = null;
        }

        if (_itemAdded != null) {
            _cache.removeItemAddListner(_itemAdded);
            _itemAdded = null;
            synchronized (sync_lock_AddDataFilter) {
                _addDataFilter = EventDataFilter.None;
                _addSeq = -1;
            }
        }
        if (_itemUpdated != null) {
            _cache.removeItemUpdatedListner(_itemUpdated);
            _itemUpdated = null;
            synchronized (sync_lock_UpdateDataFilter) {
                _updateDataFilter = EventDataFilter.None;
                _updateSeq = -1;
            }
        }
        if (_itemRemoved != null) {
            _cache.removeItemRemovedListner(_itemRemoved);
            _itemRemoved = null;
            synchronized (sync_lock_RemoveDataFilter) {
                _removeDataFilter = EventDataFilter.None;
                _removeSeq = -1;
            }
        }

        if (_customNotif != null) {
            _cache.removeCustomNotifListner(_customNotif);
            _customNotif = null;
        }

        if (_nodeJoined != null) {
            _cache.removeNodeJoinedListner(_nodeJoined);
            _nodeJoined = null;
        }
        if (_nodeLeft != null) {
            _cache.removeNodeLeftListner(_nodeLeft);
            _nodeLeft = null;
        }

        if (_blockClientActivity != null) {
            _cache.removeBlockActivity(_blockClientActivity);
            _blockClientActivity = null;
        }

        if (_unblockClientActivity != null) {
            _cache.removeUnBlockActivity(_unblockClientActivity);
            _unblockClientActivity = null;
        }


        if (this._hashmapChanged != null) {

            this._cache.removeHashmapChangedListner(_hashmapChanged);
            _hashmapChanged = null;
        }

    }

    /**
     * Called when an async operation is completed
     *
     * @param key key being used for async operation
     * @param callbackEntry callback entry being used for async operation
     */
    private void AsyncOperationCompleted(Object opCode, Object result, EventContext eventContext) {
        if (result instanceof Object[]) {
            if (_client != null) {
                AsyncCallbackInfo cbInfo = (AsyncCallbackInfo) ((((Object[]) result)[1] instanceof AsyncCallbackInfo) ? ((Object[]) result)[1] : null);
                if (cbInfo != null && !_client.getClientID().equals(cbInfo.getClient())) {
                    return;
                }

                synchronized (ConnectionManager.getCallbackQueue()) {
                    ConnectionManager.getCallbackQueue().offer(new AsyncOpCompletedCallback(opCode, result, _cacheId)); //cbEntry.ClientSocket, - cbEntry.CallerID,
                    Monitor.pulse(ConnectionManager.getCallbackQueue());

                    if (SocketServer.getIsServerCounterEnabled()) {
                        SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                    }
                }
            }
        }
    }

    public final EventStatus GetEventsStatus() {
        EventStatus eventStatus = new EventStatus();
        if (_cacheCleared != null) {
            eventStatus.setIsCacheClearedEvent(true);
        }
        if (_itemUpdated != null) {
            eventStatus.setIsItemUpdatedEvent(true);
        }
        if (_itemAdded != null) {
            eventStatus.setIsItemAddedEvent(true);
        }
        if (_itemRemoved != null) {
            eventStatus.setIsItemRemovedEvent(true);
        }
        return eventStatus;
    }

    /**
     * Called when item is updated
     *
     * @param key key of the item being updated
     * @param callbackEntry callback entry that contains the updated value
     */
    private void CustomUpdate(Object key, Object callbackInfo, EventContext eventContext) {
        if (_client != null) {
            CallbackInfo cbInfo = (CallbackInfo) ((callbackInfo instanceof CallbackInfo) ? callbackInfo : null);
            if (cbInfo != null && _client.getClientID().equals(cbInfo.getClient()))
            {
                synchronized (ConnectionManager.getCallbackQueue())
                {
                    ConnectionManager.getCallbackQueue().offer(new ItemUpdateCallback((Short) cbInfo.getCallback(), key, cbInfo.getClient(), eventContext,cbInfo.getDataFilter(), _cacheId));

                    Monitor.pulse(ConnectionManager.getCallbackQueue());

                    if (SocketServer.getIsServerCounterEnabled()) {
                        SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                    }
                }
            }
        }
    }

    /**
     *
     *
     * @param key
     * @param callbackEntry
     * @param reason
     */
    private void CustomRemove(Object key, Object value, ItemRemoveReason reason, BitSet Flag, EventContext eventContext) {
        if (_client != null) {
            Object[] args = (Object[]) ((value instanceof Object[]) ? value : null);
            if (args != null) {
                Object val = args[0];
                CallbackInfo cbInfo = (CallbackInfo) ((args[1] instanceof CallbackInfo) ? args[1] : null);

                if (cbInfo != null && _client.getClientID().equals(cbInfo.getClient()))
                {
                    synchronized (ConnectionManager.getCallbackQueue())
                    {
                        ConnectionManager.getCallbackQueue().offer(new ItemRemoveCallback((Short) cbInfo.getCallback(), key, val, reason, _client.getClientID(), Flag, eventContext,cbInfo.getDataFilter(), _cacheId));

                        Monitor.pulse(ConnectionManager.getCallbackQueue());

                        if (SocketServer.getIsServerCounterEnabled()) {
                            SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                        }
                    }
                }
            }
        }
    }


    /**
     * This function is called by ConfigurationModified callback
     *
     * @param hotConfig
     */
    private void OnConfigModified(HotConfig hotConfig) {
        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client != null) {
                ConnectionManager.getCallbackQueue().offer(new ConfigModifiedEvent(hotConfig, _cacheId, _client.getClientID()));
                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }
    }

    /**
     * Notify the connected client about logging information change
     *
     * @param enableErrorLogs
     * @param enableDetailedLogs
     * @param clientId
     */
    public void OnLoggingInfoModified(boolean enableErrorLogs, boolean enableDetailedLogs, String clientId) {
        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client != null) {
                ConnectionManager.getCallbackQueue().offer(new LoggingInfoModifiedEvent(enableErrorLogs, enableDetailedLogs, clientId));
                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }
    }

    /**
     * This function is called by ItemAddedCallback
     *
     * @param key
     */

    private void ItemAdded(Object key, EventContext eventContext)
    {
        synchronized (ConnectionManager.getCallbackQueue())
        {
            if (_client != null)
            {
                ConnectionManager.getCallbackQueue().offer(new ItemAddedEvent(key, _cacheId, _client.getClientID(), eventContext,this._addDataFilter));

                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }
    }

    /**
     * This function is called by ItemUpdatedCallback
     *
     * @param key
     */

    private void ItemUpdated(Object key, EventContext eventContext)
    {
        synchronized (ConnectionManager.getCallbackQueue())
        {
            if (_client != null)
            {
                ConnectionManager.getCallbackQueue().offer(new ItemUpdatedEvent(key, _cacheId, _client.getClientID(), eventContext,this._updateDataFilter));

                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }
    }

    /**
     * This function is called by ItemRemovedCallback
     *
     * @param key
     * @param value
     * @param reason
     */
    private void ItemRemoved(Object key, Object value, ItemRemoveReason reason, BitSet Flag, EventContext eventContext) {
        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client == null) {
                return;
            }
            if (value instanceof CallbackEntry) {
                value = ((CallbackEntry) value).getValue();
            }

            ConnectionManager.getCallbackQueue().offer(new ItemRemovedEvent(key, _cacheId, reason, (UserBinaryObject) value, _client.getClientID(), Flag, eventContext,this._removeDataFilter));

            Monitor.pulse(ConnectionManager.getCallbackQueue());

            if (SocketServer.getIsServerCounterEnabled()) {
                SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
            }
        }
    }

    /**
     * This function is called by cacheClearedCallback
     */
    private void CacheCleared(EventContext eventContext) {
        if (_client != null) {
            synchronized (ConnectionManager.getCallbackQueue()) {
                ConnectionManager.getCallbackQueue().offer(new CacheClearedEvent(_cacheId, _client.getClientID(), eventContext));
                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }
    }

    /**
     * This function is called by CacheStoppedCallback
     */
    private void CacheStopped() {
        if (_client != null) {

            synchronized (ConnectionManager.getCallbackQueue()) {
                ConnectionManager.getCallbackQueue().offer(new CacheStoppedEvent(_cacheId, _client.getClientID()));
                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }
    }

    private void TaskCallbackHandler(Object taskID, TaskCallbackInfo callbackInfo, EventContext eventContext) {
        if (_client != null) {            
            synchronized (ConnectionManager.getCallbackQueue()) {
                ConnectionManager.getCallbackQueue().offer(new TaskCallback(_client.getClientID(), (String)taskID, eventContext.getTaskStatus().getValue(),(Short)callbackInfo.getCallback()));
                Monitor.pulse(ConnectionManager.getCallbackQueue());
            }
            if (SocketServer.getIsServerCounterEnabled()) {
                SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
            }
        }
    }
    
    /**
     *
     *
     * @param notifId
     * @param value
     */
    private void CustomNotification(Object notifId, Object value, EventContext eventContext) {
        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client == null) {
                return;
            }
            ConnectionManager.getCallbackQueue().offer(new CustomEvent(_cacheId, (byte[]) notifId, (byte[]) value, _client.getClientID()));
            Monitor.pulse(ConnectionManager.getCallbackQueue());

            if (SocketServer.getIsServerCounterEnabled()) {
                SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
            }
        }
    }

    private void DataSourceUpdated(Object result, CallbackEntry cbEntry, OpCode operationCode) {
        if (cbEntry != null) {
            if (_client == null) {
                return;
            }
            Object tempVar = cbEntry.getWriteBehindOperationCompletedCallback();
            AsyncCallbackInfo asyncInfo = (AsyncCallbackInfo) ((tempVar instanceof AsyncCallbackInfo) ? tempVar : null);

            if (_client.getClientID().equals(asyncInfo.getClient()))
            {
                synchronized (ConnectionManager.getCallbackQueue())
                {
                    ConnectionManager.getCallbackQueue().offer(new DataSourceUpdatedCallbackTask((Short) asyncInfo.getCallback(), result, operationCode, _client.getClientID(), _cacheId));

                    Monitor.pulse(ConnectionManager.getCallbackQueue());

                    if (SocketServer.getIsServerCounterEnabled()) {
                        SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                    }
                }
            }
        }
    }

    /**
     * This function is called by NodeJoinedCallback
     *
     * @param notifId
     * @param value
     */
    private void NodeJoined(Object clusterAddress, Object serverAddress, boolean reconnect, EventContext eventContext) {
        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client == null) {
                return;
            }
            ConnectionManager.getCallbackQueue().offer(new NodeJoinedEvent(_cacheId, (com.alachisoft.tayzgrid.common.net.Address) ((clusterAddress instanceof com.alachisoft.tayzgrid.common.net.Address) ? clusterAddress : null), (com.alachisoft.tayzgrid.common.net.Address) ((serverAddress instanceof com.alachisoft.tayzgrid.common.net.Address) ? serverAddress : null), _client.getClientID(), reconnect));

            Monitor.pulse(ConnectionManager.getCallbackQueue());

            if (SocketServer.getIsServerCounterEnabled()) {
                SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
            }
        }
    }

    /**
     * This function is called by NodeLeftCallback
     *
     * @param notifId
     * @param value
     */
    private void NodeLeft(Object clusterAddress, Object serverAddress, EventContext eventContext) {
        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client == null) {
                return;
            }
            ConnectionManager.getCallbackQueue().offer(new NodeLeftEvent(_cacheId, (com.alachisoft.tayzgrid.common.net.Address) ((clusterAddress instanceof com.alachisoft.tayzgrid.common.net.Address) ? clusterAddress : null), (com.alachisoft.tayzgrid.common.net.Address) ((serverAddress instanceof com.alachisoft.tayzgrid.common.net.Address) ? serverAddress : null), _client.getClientID()));
            Monitor.pulse(ConnectionManager.getCallbackQueue());

            if (SocketServer.getIsServerCounterEnabled()) {
                SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
            }
        }
    }

    private void HashmapChanged(NewHashmap newmap, EventContext eventContext) {

        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client != null) {
                ConnectionManager.getCallbackQueue().offer(new HashmapChangedEvent(_cacheId, _client.getClientID(), newmap, this._isDotNetClient));
                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }

    }

    private void BlockClientActivity(String uniqueId, String serverIp, long timeoutInterval, int port) {

        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client != null) {
                ConnectionManager.getCallbackQueue().offer(new BlockActivityEvent(uniqueId, _cacheId, _client.getClientID(), serverIp, timeoutInterval, port));
                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }
    }

    private void UnBlockClientActivity(String uniqueId, String serverIp, int port) {

        synchronized (ConnectionManager.getCallbackQueue()) {
            if (_client != null) {
                ConnectionManager.getCallbackQueue().offer(new UnBlockActivityEvent(uniqueId, _cacheId, _client.getClientID(), serverIp, port));
                Monitor.pulse(ConnectionManager.getCallbackQueue());

                if (SocketServer.getIsServerCounterEnabled()) {
                    SocketServer.getPerfStatsColl().setEventQueueCount(ConnectionManager.getCallbackQueue().size());
                }
            }
        }
    }

    public String getID() {
        return _cacheId;
    }

    public void OnClientConnected(String clientID, String cacheId) throws OperationFailedException {

        if (_cache != null) {
            _cache.OnClientConnected(clientID, cacheId);
        }

    }

    /**
     * This function is called client is dis-connected
     *
     * @param clientID
     * @param cacheId
     */
    public void OnClientDisconnected(String clientID, String cacheId) throws OperationFailedException {

        if (_cache != null) {
            _cache.OnClientDisconnected(clientID, cacheId);
        }

    }

    public void MaxEventRequirement(EventDataFilter datafilter, NotificationsType eventType, short sequence) {
        switch (eventType) {
            case RegAddNotif:
                synchronized (sync_lock_AddDataFilter) {
                    if (_addSeq < sequence) {
                        _addDataFilter = datafilter;
                        _addSeq = sequence;
                    }
                }
                break;
            case RegRemoveNotif:
                synchronized (sync_lock_RemoveDataFilter) {
                    if (_removeSeq < sequence) {
                        _removeDataFilter = datafilter;
                        _removeSeq = sequence;
                    }
                }
                break;
            case RegUpdateNotif:
                synchronized (sync_lock_UpdateDataFilter) {
                    if (_updateSeq < sequence) {
                        _updateDataFilter = datafilter;
                        _updateSeq = sequence;
                    }
                }
                break;
        }
    }

    public void UpdateSocketServerStats(SocketServerStats stats) {
        if (_cache != null) {
            _cache.UpdateSocketServerStats(stats);
        }
    }

    public boolean IsCoordinator(String srcCacheID) {
       
        return false;
    }       
    public void RegisterNotification(NotificationsType type) {

        switch (type) {
            case RegAddNotif:

                if (this._itemAdded == null) {
                    _itemAdded = new NEventStart() {
                        @Override
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            ItemAdded(obj[0], (EventContext) obj[1]);
                            return null;
                        }
                    };
                    _cache.addItemAddedListner(_itemAdded, null);
                }

                break;

            case RegUpdateNotif:

                if (this._itemUpdated == null) {
                    this._itemUpdated = new NEventStart() {
                        @Override
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            ItemUpdated(obj[0], (EventContext) obj[1]);
                            return null;
                        }
                    };
                    _cache.addItemUpdatedListner(this._itemUpdated, null);
                }

                break;

            case RegRemoveNotif:

                if (this._itemRemoved == null) {
                    this._itemRemoved = new NEventStart() {
                        @Override
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            ItemRemoved(obj[0], obj[1], (ItemRemoveReason) obj[2], (BitSet) obj[3], (EventContext) obj[4]);
                            return null;
                        }
                    };
                    _cache.addItemRemovedListner(this._itemRemoved, null);
                }

                break;

            case RegClearNotif:

                if (this._cacheCleared == null) {
                    this._cacheCleared = new NEventStart() {
                        @Override
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            CacheCleared((EventContext) obj[0]);
                            return null;
                        }
                    };

                    _cache.addCacheClearedListner(this._cacheCleared, null);
                }

                break;
            case RegCustomNotif:

                if (this._customNotif == null) {
                    this._customNotif = new NEventStart() {
                        @Override
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            CustomNotification(obj[0], obj[1], (EventContext) obj[2]);
                            return null;
                        }
                    };

                    _cache.addCustomNotifListner(this._customNotif, null);
                }

                break;

            case RegNodeJoinedNotif:

                if (this._nodeJoined == null) {
                    this._nodeJoined = new NEventStart() {
                        @Override
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            NodeJoined(obj[0], obj[1], ((Boolean) obj[2]).booleanValue(), (EventContext) obj[3]);
                            return null;
                        }
                    };
                    _cache.addNodeJoinedListner(this._nodeJoined, null);
                }

                break;

            case RegNodeLeftNotif:

                if (this._nodeLeft == null) {
                    this._nodeLeft = new NEventStart() {
                        @Override
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            NodeLeft(obj[0], obj[1], (EventContext) obj[2]);
                            return null;
                        }
                    };

                    _cache.addNodeLeftListner(this._nodeLeft, null);
                }

                break;

            case RegCacheStoppedNotif:

                this._cacheStoppedEventRegistered = true;

                break;

            case RegHashmapChangedNotif:

                this._hashmapChanged = new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, Exception {
                        HashmapChanged((NewHashmap) obj[0], (EventContext) obj[1]);
                        return null;
                    }
                };
                this._cache.addHashmapChangedListner(this._hashmapChanged, null);

                break;

            case UnregAddNotif:

                if (this._itemAdded != null) {
                    _cache.removeItemAddListner(this._itemAdded);
                    this._itemAdded = null;
                    synchronized (sync_lock_AddDataFilter) {
                        _addDataFilter = EventDataFilter.None;
                        _addSeq = -1;
                    }
                }

                break;

            case UnregUpdateNotif:

                if (this._itemUpdated != null) {
                    _cache.removeItemUpdatedListner(this._itemUpdated);
                    this._itemUpdated = null;
                    synchronized (sync_lock_UpdateDataFilter = -1) {
                        _updateDataFilter = EventDataFilter.None;
                        _updateSeq = -1;
                    }
                }

                break;

            case UnregRemoveNotif:

                if (this._itemRemoved != null) {
                    _cache.removeItemRemovedListner(this._itemRemoved);
                    this._itemRemoved = null;
                    synchronized (sync_lock_RemoveDataFilter) {
                        _removeDataFilter = EventDataFilter.None;
                        _removeSeq = -1;
                    }
                }

                break;

            case UnregClearNotif:

                if (this._cacheCleared != null) {
                    _cache.removeCacheClearedListner(this._cacheCleared);
                    this._cacheCleared = null;
                }

                break;

            case UnregCustomNotif:

                if (this._customNotif != null) {
                    _cache.removeCustomNotifListner(this._customNotif);
                    this._customNotif = null;
                }

                break;

            case UnregNodeJoinedNotif:

                if (this._nodeJoined != null) {
                    _cache.removeNodeJoinedListner(this._nodeJoined);
                    this._nodeJoined = null;
                }

                break;

            case UnregNodeLeftNotif:

                if (this._nodeLeft != null) {
                    _cache.removeNodeLeftListner(this._nodeLeft);
                    this._nodeLeft = null;
                }

                break;

            case UnregCacheStoppedNotif:

                this._cacheStoppedEventRegistered = false;

                break;
            case UnregHashmapChangedNotif:

                if (this._hashmapChanged != null) {
                    this._cache.removeHashmapChangedListner(this._hashmapChanged);
                    this._hashmapChanged = null;
                }

                break;
        }
    }
}
