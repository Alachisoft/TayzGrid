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
package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.CompactCacheEntry;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.DataSourceReadOptions;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.cacheloader.CacheLoaderUtil;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.CacheConfigParams;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.InsertResult;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.ResourcePool;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.event.CacheListener;
import com.alachisoft.tayzgrid.event.CacheNotificationType;
import com.alachisoft.tayzgrid.event.CacheStatusEventListener;
import com.alachisoft.tayzgrid.event.CacheStatusNotificationType;
import com.alachisoft.tayzgrid.event.ClusterEvent;
import com.alachisoft.tayzgrid.event.CustomListener;
import com.alachisoft.tayzgrid.management.CacheConfig;
import com.alachisoft.tayzgrid.management.ICacheServer;
import com.alachisoft.tayzgrid.management.CacheRPCService;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.caching.CacheItemAttributes;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.TypeIndexNotDefined;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessorResult;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import com.alachisoft.tayzgrid.serialization.util.SerializationBitSetConstant;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import com.alachisoft.tayzgrid.web.events.EventCacheItem;
import com.alachisoft.tayzgrid.web.events.EventUtil;
import com.alachisoft.tayzgrid.web.mapreduce.MROutputOption;
import com.alachisoft.tayzgrid.web.mapreduce.TaskEnumerator;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import tangible.RefObject;

public final class InprocCache extends CacheImplBase implements Iterable {

    public com.alachisoft.tayzgrid.caching.Cache _tgCache;

    public CacheConfig _config;
    /**
     * Reference count of the cache.
     */
    public int _refCount;
    /**
     */
    private CompactCacheEntry _entry;
    /**
     * Serialization context (actually name of the cache.)used for Compact
     * Framework
     */
    private String _serializationContext;
    /**
     * Cache event listener object. Implements all events.
     */

    private ResourcePool _callbackIDsMap;

    private short _rcbInitialVal = 0;
    private short _ucbInitialVal = 1000;
    private short _aiacbInitialVal = 2000;
    private short _aiucbInitialVal = 3000;
    private short _aircbInitialVal = 4000;
    private short _acccbInitialVal = 5000;
    protected static final long forcedViewId = -5;
    private Cache _parent;

    ///#region    /                 --- Fields & Events ---           /
    /**
     * Used in the <paramref name="absoluteExpiration"/> parameter in an Insert
     * method call to indicate the item should never expire. This field is
     * read-only.
     *

     * When used, this field sets the <paramref name="absoluteExpiration"/> parameter equal to <see cref="DateTime.MaxValue"/>, which is a constant representing the largest
     * possible <see cref="DateTime"/> value, 12/31/9999 11:59:59 PM. <example>The following example demonstrates how to use the <see cref="NoAbsoluteExpiration"/> field to disable
     * absolute expirations when inserting an item in the <see cref="Cache"/>.
     * <code>

     * NCache.Cache.Insert("DSN", connectionString, null, Cache.DefaultAbsoluteExpiration, TimeSpan.FromSeconds(10));

     *</code> </example>
     */
    public static final java.util.Date NoAbsoluteExpiration = null;
    /**
     * Used as the <paramref name="slidingExpiration"/> parameter in an Insert
     * method call to disable sliding expirations. This field is read-only.
     *

     * When used, this field sets the <paramref name="slidingExpiration"/> parameter equal to the <see cref="TimeSpan.Zero"/> field, which has a constant value of zero. The cached
     * item then expires in accordance with the <paramref name="absoluteExpiration"/> parameter. <example>The following example demonstrates how to use the Insert method to add an
     * item to the <see cref="Cache"/> object using the <see cref="NoSlidingExpiration"/> field.
     * <code>

 NCache.Cache.Insert("DSN", connectionString, null, DateTime.Now.AddMinutes(2), Cache.DefaultSlidingExpiration);

 </code> </example>

     */
    public static final TimeSpan NoSlidingExpiration = null;
    private Vector callbackQue = new Vector();

    private TypeInfoMap _typeMap;
    private int _refAddCount;
    private boolean _clearNotifRegistered;
    private boolean _addNotifRegistered;
    private boolean _removeNotifRegistered;
    private boolean _updateNotifRegistered;
    private final InprocEventListener inprocEventListener;
    private final AsyncCallbacks asyncCallback;
    List<CacheStatusListenerWrapper> _memberJoined = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
    List<CacheStatusListenerWrapper> _memberLeft = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
    List<CacheStatusListenerWrapper> _cacheStopped = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
    private NEventStart _nodeJoinedEvent = null;
    private NEventStart _nodeLeftEvent = null;
    private NEventStart _cacheStoppedEvent = null;
    private Thread portPublisher;

    @Override
    protected TypeInfoMap getTypeMap() {
        return _typeMap;
    }

    @Override
    protected void setTypeMap(TypeInfoMap value) {
        _typeMap = value;
    }



    @Override
    protected boolean getSerializationEnabled() {
        return _tgCache.getSerializationEnabled();
        //return false;
    }

    @Override
    Vector getCallbackQueue() {
        return callbackQue;
    }

    /**
     * Initializes a new instance of the Cache class.
     *
     * @param objectCache
     * @param config
     */
    public InprocCache(com.alachisoft.tayzgrid.caching.Cache objectCache, CacheConfig config, Cache parent) {
        super();
        _tgCache = objectCache;

        this.setTypeMap(_tgCache.GetTypeInfoMap());

        inprocEventListener = new InprocEventListener(parent, _tgCache);
        asyncCallback = new AsyncCallbacks(parent, _tgCache);

        _config = config;
        _parent = parent;

        if (_tgCache != null) {

            try {
                _tgCache.OnClientConnected(getClientID(), _config.getCacheId());
            } catch (Exception e) {

            }
            _serializationContext = _tgCache.getName(); //Sets the serialization context.
        }

        addRef();
        startPortPublisher();
    }

      private void startPortPublisher(){
         if(portPublisher == null){
                    portPublisher = new Thread(new Runnable(){
                         @Override
                        public void run() {
                            while(true){
                                try{
                                     Thread.sleep(5000);
                                     sendSnmpPort();
                                }catch(ThreadDeath e){
                                    break;
                                }
                                catch(InterruptedException e){
                                    break;
                                }
                                catch(Exception e){
                                    
                                }
                            }
                        }
                        
                    });
                    
                    portPublisher.setDaemon(false);
                    portPublisher.setName("port_pubisher");
                    portPublisher.start();
                }
    }
    
    private void stopPortPublisher(){
        if(portPublisher != null && portPublisher.isAlive()){
            portPublisher.interrupt();
        }
    }
    
    /**
     * Finalizer for this object.
     */
    protected void finalize() throws Throwable {
        dispose(false);
    }

    public void addRef() {
        synchronized (this) {
            _refCount++;
        }
    }

    @Override
    public void registerGeneralNotification(EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter, short sequenceNumber) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        inprocEventListener.registerCacheNotification(eventEnumSet);
    }

    @Override
    public boolean getEncryptionEnabled() {
        return false;
    }

    @Override
    public void setEncryptionEnabled(boolean value) {

    }

    @Override
    public java.util.HashMap GetEncryptionInfo() {
        return null;
    }

    ///#region    /                 --- IDisposable ---           /
    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     * @param disposing The behavior of this method depends upon the cache's
     * sharing mode (inproc/outproc) specified in the configuration.
     * <p>
     * If the application uses the cache in
     * <b>inproc</b> mode, calling Dispose closes the cache and releases the
     * resources used by it. If in a cluster the calling node leaves the cluster
     * as well. </p>
     * <p>
     * If the application uses the cache in <b>outproc</b> mode, calling Dispose
     * releases the reference to the cache object. The cache itself remains
     * operational and cluster remains intact. </p>
     *
     */
    private void disposeInternal(boolean disposing) {
        synchronized (this) {
            _refCount--;
            if (_refCount > 0) {
                return;
            } else if (_refCount < 0) {
                _refCount = 0;
            }

            // remove from the cache table.
            synchronized (TayzGrid.getCaches()) {
                if (_config != null) {
                    TayzGrid.getCaches().removeCache(_config.getCacheId());
                }
            }

            if (_config != null && _config.getUseInProc()) {
                if (_tgCache != null) {
                    _tgCache.dispose();
                }
            }

            _tgCache = null;
            if (disposing) {

            }
            stopPortPublisher();
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Events">
    @Override
    protected void registerCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        inprocEventListener.registerCustomListener(listener);
    }

    @Override
    protected void registerCacheEventlistener(final CacheListener listener, EnumSet<CacheNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {

    }

    @Override
    protected void registerCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
    
         boolean joined = false, left = false, stopped = false;

        final CacheStatusListenerWrapper wrapper = new CacheStatusListenerWrapper(listener);
        if (registerAgainst.contains(CacheStatusNotificationType.ALL)) {
            if (!_memberJoined.contains(wrapper)) {
                _memberJoined.add(wrapper);
                joined = true;
            }

            if (!_memberLeft.contains(wrapper)) {
                _memberLeft.add(wrapper);
                left = true;
            }

            if (!_cacheStopped.contains(wrapper)) {
                _cacheStopped.add(wrapper);
                stopped = true;
            }
        } else {
            if (registerAgainst.contains(CacheStatusNotificationType.MemberJoined) && !_memberJoined.contains(wrapper)) {
                _memberJoined.add(wrapper);
                joined = true;
            }

            if (registerAgainst.contains(CacheStatusNotificationType.MemberLeft) && !_memberLeft.contains(wrapper)) {
                _memberLeft.add(wrapper);
                left = true;
            }

            if (registerAgainst.contains(CacheStatusNotificationType.CacheStopped) && !_cacheStopped.contains(wrapper)) {
                _cacheStopped.add(wrapper);
                stopped = true;
            }
        }

        if (_memberJoined.size() == 1 && joined && registerAgainst.contains(CacheStatusNotificationType.MemberJoined)
                || registerAgainst.contains(CacheStatusNotificationType.ALL)) {
            
            //registerNotification(NotificationType.REGISTER_MEMBER_JOINED, true);
            //this.memberjoinedFlag = true;
            
              this._nodeJoinedEvent = new NEventStart() {
                        @Override
                        public Object hanleEvent(final Object... obj) throws SocketException, Exception {
                             
                int length = _memberJoined.size();
                for (int i = 0; i < length; i++) {
                    final CacheStatusEventListener eventlistener = _memberJoined.get(i).getClusterEvent();
                    com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                        public void run() {
                            StringBuilder sb = new StringBuilder(((Address)obj[0]).getIpAddress().toString());
                            sb.deleteCharAt(0);
                            ClusterEvent eventArgs = new ClusterEvent(_parent, ClusterEvent.EventType.JOINED, sb.toString(), ((Address)obj[0]).getPort(), _tgCache.getName());
                            eventlistener.memberJoined(eventArgs);
                        }
                    });

                }
                return null;  
                        }
              };
              _tgCache.addNodeJoinedListner(this._nodeJoinedEvent, null);
                    
        }
        if (_memberLeft.size() == 1 && left && registerAgainst.contains(CacheStatusNotificationType.MemberLeft)
                || registerAgainst.contains(CacheStatusNotificationType.ALL)) {
            //registerNotification(NotificationType.REGISTER_MEMBER_LEFT, true);
            //this.memberleftFlag = true;
             this._nodeLeftEvent = new NEventStart() {
                        @Override
                        public Object hanleEvent(final Object... obj) throws SocketException, Exception {
                             
                int length = _memberLeft.size();
                for (int i = 0; i < length; i++) {
                    final CacheStatusEventListener listener = _memberLeft.get(i).getClusterEvent();
                    com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                        public void run() {
                            StringBuilder sb = new StringBuilder(((Address)obj[0]).getIpAddress().toString());
                            sb.deleteCharAt(0);
                            ClusterEvent eventArgs = new ClusterEvent(_parent, ClusterEvent.EventType.LEFT,sb.toString(), ((Address)obj[0]).getPort(), _tgCache.getName());
                            listener.memberLeft(eventArgs);
                        }
                    });

                }
                return null;  
                        }
              };
              _tgCache.addNodeLeftListner(this._nodeLeftEvent, null);
        }

        if (_cacheStopped.size() == 1 && stopped && registerAgainst.contains(CacheStatusNotificationType.CacheStopped)
                || registerAgainst.contains(CacheStatusNotificationType.ALL)) {
            //registerNotification(NotificationType.REGISTER_CACHE_STOPPED, true);
            //this.cachestoppedFlag = true;
            this._cacheStoppedEvent = new NEventStart() {
                        @Override
                        public Object hanleEvent(final Object... obj) throws SocketException, Exception {
                             
                int length = _cacheStopped.size();
                for (int i = 0; i < length; i++) {
                    final CacheStatusEventListener listener = _cacheStopped.get(i).getClusterEvent();
                    com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                        public void run() {
                            ClusterEvent eventArgs = new ClusterEvent(_parent, ClusterEvent.EventType.STOPPED, _tgCache.getName());
                            listener.cacheStopped(eventArgs);
                        }
                    });

                }
                return null;  
                        }
              };
              _tgCache.addCacheStoppedListner(this._cacheStoppedEvent, null);
        }

        
    }

    @Override
    protected void unregisterCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        inprocEventListener.unregisterCacheEventlistener(listener, unregisterAgainst);
    }

    @Override
    protected void unregisterCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
    
        if (unregisterAgainst.contains(CacheStatusNotificationType.ALL)) {
            removeIfCacheListenerExists(_memberJoined, listener);
            removeIfCacheListenerExists(_memberLeft, listener);
            removeIfCacheListenerExists(_cacheStopped, listener);
        } else {
            if (unregisterAgainst.contains(CacheStatusNotificationType.MemberJoined)) {
                removeIfCacheListenerExists(_memberJoined, listener);
            }

            if (unregisterAgainst.contains(CacheStatusNotificationType.MemberLeft)) {
                removeIfCacheListenerExists(_memberLeft, listener);
            }

            if (unregisterAgainst.contains(CacheStatusNotificationType.CacheStopped)) {
                removeIfCacheListenerExists(_cacheStopped, listener);
            }
        }

        if (_memberJoined.size() == 1 && unregisterAgainst.contains(CacheStatusNotificationType.MemberJoined)) {
            //unregisterNotifications(NotificationType.REGISTER_MEMBER_JOINED);
            //this.memberjoinedFlag = false;
            if (this._nodeJoinedEvent != null) {
                    _tgCache.removeNodeJoinedListner(this._nodeJoinedEvent);
                    this._nodeJoinedEvent = null;
                }
        }

        if (_memberLeft.size() == 1 && unregisterAgainst.contains(CacheStatusNotificationType.MemberLeft)) {
            //unregisterNotifications(NotificationType.REGISTER_MEMBER_LEFT);
            //this.memberleftFlag = false;
            if (this._nodeLeftEvent != null) {
                    _tgCache.removeNodeLeftListner(this._nodeLeftEvent);
                    this._nodeLeftEvent = null;
                }
        }

        if (_cacheStopped.size() == 1 && unregisterAgainst.contains(CacheStatusNotificationType.CacheStopped)) {
            //unregisterNotifications(NotificationType.REGISTER_CACHE_STOPPED);
            //this.cachestoppedFlag = false;
             if (this._cacheStoppedEvent != null) {
                _tgCache.removeCacheStoppedListner(this._cacheStoppedEvent);
                this._cacheStoppedEvent = null;
                }
        }
    }

    private boolean removeIfCacheListenerExists(List list, java.util.EventListener listener) {
        Iterator ite = list.iterator();
        int indexOf = -1;

        if (listener instanceof CacheListener) {
            while (ite.hasNext()) {
                CacheListenerWrapper wrapper = (CacheListenerWrapper) ite.next();
                if (wrapper.verifyListenerInstance((CacheListener) listener)) {
                    indexOf = list.indexOf(wrapper);
                    break;
                }
            }
        }

        if (indexOf > -1) {
            list.remove(indexOf);
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    protected void unregisterCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        inprocEventListener.unregisterCustomEventListener(listener);
    }

    //</editor-fold>
    /**
     * Decerements the reference count of the cache and performs
     * application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     *
     * The behavior of this method depends upon the cache's sharing mode
     * (inproc/outproc) specified in the configuration.
     * <p>
     * If the application uses the cache in <b>inproc</b>
     * mode, calling Dispose closes the cache and releases the resources used by
     * it. If in a cluster the calling node leaves the cluster as well. </p>
     * <p>
     * If the application uses the cache in <b>outproc</b> mode, calling Dispose
     * releases the reference to the cache object. The cache itself remains
     * operational and cluster remains intact. </p>
     *
     */
    @Override
    public void dispose(boolean disposing) {
        disposeInternal(true);
    }

    /**
     * Acquire a lock on an item in cache.
     *
     * @param key key of cached item to be locked.
     * @param lockTimeout TimeSpan after which the lock is automatically
     * released.
     * @param lockHandle An instance of <see cref="Lockhandle"/> that will be
     * filled in with the lock information if lock is acquired successfully.
     * @return Whether or not lock was acquired successfully. <example>
     * Following example demonstrates how to lock a cached item.      <code>
     * ...
     * LockHandle lockHandle = new LockHandle();
     * bool locked = theCache.lock("cachedItemKey", new TimeSpan(0,0,10), out lockHandle);
     * ...
     * </code> </example>
     */
    @Override
    public boolean lock(Object key, TimeSpan lockTimeout, LockHandle lockHandle) throws OperationFailedException, LockingException, GeneralFailureException, CacheException, Exception
    {

        Object lockId = null;
        java.util.Date lockDate = new java.util.Date();

        boolean result = false;
        if (_tgCache != null) {
            tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
            tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
            try {
                result = _tgCache.Lock(key, lockTimeout, tempRef_lockId, tempRef_lockDate, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            } catch (Exception e) {
                throw e;
            }
            lockId = tempRef_lockId.argvalue;
            lockDate = tempRef_lockDate.argvalue;
            lockHandle.setLockId(lockId == null ? null : (String) ((lockId instanceof String) ? lockId : null));
            lockHandle.setLockDate(lockDate);

            return result;
        }
        return false;
    }

    @Override
    public boolean isLocked(Object key, LockHandle lockHandle) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, Exception
    {
        if (_tgCache != null)
        {

            Object lockId = null;
            java.util.Date lockDate = new java.util.Date(0);
            boolean result = false;
            if (lockHandle == null) {
                lockHandle = new LockHandle();
            }
            tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
            tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
            try {
                result = _tgCache.IsLocked(key, tempRef_lockId, tempRef_lockDate, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            } catch (Exception e) {
                throw e;
            }
            lockId = tempRef_lockId.argvalue;
            lockDate = tempRef_lockDate.argvalue;
            lockHandle.setLockId(lockId == null ? "" : (String) ((lockId instanceof String) ? lockId : null));
            lockHandle.setLockDate(lockDate);
            return result;
        }
        return false;
    }

    /**
     * Forcefully unlocks a locked cached item.
     *
     * @param key key of a cached item to be unlocked <example> Following
     * example demonstrates how to unlock a cached item.      <code>
     * ...
     * theCache.Unlock("cachedItemKey");
     * ...
     * </code> </example>
     */
    @Override

    public void unlock(Object key) throws Exception
    {
        if (_tgCache != null)
        {


            _tgCache.Unlock(key, null, true, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

        }
    }

    /**
     * Unlocks a locked cached item if the correct lock-id is specified.
     *
     * @param key key of a cached item to be unlocked
     * @param lockHandle An instance of <see cref="LockHandle"/> that was
     * generated when lock was acquired. <example> Following example
     * demonstrates how to unlock a cached item.      <code>
     * ...
     * theCache.Unlock("cachedItemKey", lockHandle);
     * ...
     * </code> </example>
     */
    @Override

    public void unlock(Object key, String lockId) throws Exception
    {
        if (_tgCache != null)
        {


            _tgCache.Unlock(key, lockId, false, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

        }
    }

    ///#region    /                 --- Count ---           /
    /**
     * Gets the number of items stored in the cache.
     *
     * This property can be useful when monitoring your application's
     * performance or when using ASP.NET tracing functionality.
     * <p>
     * <b>Note:</b> In a partitioned cluster this operation is an expensive one
     * as it might result in network calls. It is therefore advised to use this
     * property only when required. </p>
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <value>The number of items stored in the cache.</value>
     */
    @Override
    public long getCount() throws Exception {
        if (_tgCache != null) {
            return _tgCache.getCount();
        }
        return 0;
    }

    /**
     * Gets the name of the cache.
     */
    @Override
    public String getName() {
        if (_tgCache != null) {
            return _tgCache.getName();
        }
        return null;
    }

    ///#region    /                 --- Clear ---           /
    /**
     * Removes all elements from the <see cref="Cache"/>.
     *
     * In most of the cases this method's implementation is close to O(1).
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how to check for containment
     * of an item in the <see cref="Cache"/>.      <code>
     *
     * NCache.Cache.Clear();
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Clear();
     *
     * </code> </example>
     */
    @Override
    public void clear(BitSet flagMap, short onDsClearedCallback, boolean isAsync, String providerName) throws OperationFailedException {
        if (_tgCache == null) {
            return;
        }

        CallbackEntry cbEntry = null;
        if (onDsClearedCallback != -1) {
            cbEntry = new CallbackEntry(getClientID(), -1, null, (short) -1, (short) -1, (short) -1, onDsClearedCallback, flagMap, EventDataFilter.None, EventDataFilter.None);
        }
        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        operationContext.Add(OperationContextFieldName.ReadThruProviderName, providerName);
        try {
            _tgCache.Clear(flagMap, cbEntry, operationContext);
        } catch (OperationFailedException e) {
            throw e;
        }
    }

    /**
     * Removes all elements from the <see cref="Cache"/> asynchronously.
     *
     *
     * This is similar to <see cref="Clear"/> except that the operation is
     * performed asynchronously. A <see cref="CacheCleared"/> event is fired
     * upon successful completion of this method.It is not possible to determine
     * if the actual operation has failed, therefore use this operation for the
     * cases when it does not matter much.
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how to clear the <see
     * cref="Cache"/>.      <code>
     *
     * NCache.Cache.ClearAsync();
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.ClearAsync();
     *
     * </code> </example>
     */
    @Override
    public void clearAsync(BitSet flagMap, short onDsClearedCallback, boolean isAsync, String providerName) {
        if (_tgCache == null) {
            return;
        }

        CallbackEntry cbEntry = null;
        if (onDsClearedCallback != -1) {
            cbEntry = new CallbackEntry(getClientID(), -1, null, (short) -1, (short) -1, (short) -1, onDsClearedCallback, flagMap, EventDataFilter.None, EventDataFilter.None);
        }
        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        operationContext.Add(OperationContextFieldName.ReadThruProviderName, providerName);
        _tgCache.ClearAsync(flagMap, cbEntry, operationContext);
    }

    /**
     * Removes all elements from the <see cref="Cache"/> asynchronously.
     *
     *
     * This is similar to <see cref="Clear"/> except that the operation is
     * performed asynchronously. A <see cref="CacheCleared"/> event is fired
     * upon successful completion of this method.It is not possible to determine
     * if the actual operation has failed, therefore use this operation for the
     * cases when it does not matter much.
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * @param onAsyncCacheClearedCallback A delegate that, that can be used to
     * get the result of the Asynchronous Clear operation.
     *
     * <example>The following example demonstrates how to clear the <see
     * cref="Cache"/>.      <code>
     *
     * void OnAsyncCacheCleared(object result)
     * {
     * ...
     * }
     *
     * NCache.Cache.ClearAsync(new AsyncCacheClearedCallback(OnAsyncCacheCleared));
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.ClearAsync(new AsyncCacheClearedCallback(OnAsyncCacheCleared));
     *
     * </code> </example>
     */
    @Override
    public void clearAsync(BitSet flagMap, short onAsyncCacheClearCallback, short onDsClearedCallback, boolean isAsync, String providerName) {
        if (_tgCache == null) {
            return;
        }

        CallbackEntry cbEntry = null;
        if (onAsyncCacheClearCallback != -1 || onDsClearedCallback != -1) {
            cbEntry = new CallbackEntry(getClientID(), -1, null, (short) -1, (short) -1, onAsyncCacheClearCallback, onDsClearedCallback, flagMap, EventDataFilter.None, EventDataFilter.None);
        }
        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        operationContext.Add(OperationContextFieldName.ReadThruProviderName, providerName);
        _tgCache.ClearAsync(flagMap, cbEntry, operationContext);
    }

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param key The key to locate in the <see cref="Cache"/>.
     * @return <b>true</b> if the <see cref="Cache"/> contains an element with
     * the specified key; otherwise, <b>false</b>. In most of the cases this
     * method's implementation is close to O(1).
     * <p>
     * <b>Note:</b> In a partitioned cluster this operation is an expensive one
     * as it might result in network calls. It is therefore advised to use this
     * property only when required. </p>
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     * @throws ArgumentNullException <paramref name="key"/> contains a null
     * reference (Nothing in Visual Basic).
     * @throws ArgumentException <paramref name="key"/> is not serializable.
     * <example>The following example demonstrates how to check for containment
     * of an item in the <see cref="Cache"/>.      <code>
     *
     * if(NCache.Cache.Contains("MyTextBox.Value"))
     * {
     * Response.Write("Item found!");
     * }
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * if(Cache.Contains("MyTextBox.Value"))
     * {
     * Response.Write("Item found!");
     * }
     *
     * </code> </example>
     */
    @Override

    public boolean contains(Object key) throws OperationFailedException
    {
        if (key == null)
        {

            throw new IllegalArgumentException("key");
        }


        try
        {

            return _tgCache.Contains(key, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
        } catch (OperationFailedException e) {
            throw e;
        }

    }
    
    @Override
    public CacheConfigParams getCacheConfiguration() throws GeneralFailureException, OperationFailedException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConfigurationException, ConnectionException
    {
        try {
            return _tgCache.getCacheConfiguration();
        }
        catch (OperationFailedException e) {
            throw e;
        }
    }

    /**
     * Broadcasts a custom application defined event.
     *
     * @param notifId Application specific notification code/id
     * @param data Application specific data In most of the cases this method's
     * implementation is close to O(1).
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     * <p>
     * <b>Note:</b> Custom event notifications can not be disabled through
     * configuration.</p>
     *
     * <example>The following example demonstrates how to raise a custom
     * application defined event.      <code>
     *
     * NCache.Cache.RaiseCustomEvent(MyNotificationCodes.ConsumeItem,
     * new ItemData(DateTime.Now));
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.RaiseCustomEvent(MyNotificationCodes.ConsumeItem,
     * new ItemData(DateTime.Now));
     *
     * </code> </example>
     */
    @Override
    public void raiseCustomEvent(Object notifId, Object data) throws Exception {

        _tgCache.SendNotification(notifId, data);

    }

    /**
     *
     *
     * @param key
     * @param value
     */

    private void validateKeyValue(Object key, Object value)
    {

        java.lang.Class type = ICompactSerializable.class;
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }
        if (!(key.getClass() instanceof java.io.Serializable) && !type.isAssignableFrom(key.getClass())) {
            throw new IllegalArgumentException("key is not serializable");
        }
        if (!(value.getClass() instanceof java.io.Serializable) && !type.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("value is not serializable");
        }
    }

    /**
     * Create a CompactEntry object
     *
     * @param key
     * @param value
     * @param absoluteExpiration
     * @param slidingExpiration
     * @param priority
     * @param onRemoveCallback
     * @param isResyncExpiredItems
     * @param group
     * @param subGroup
     * @return
     */

    private Object makeCompactEntry(Object key, Object value,   java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onAsyncOperationCompleteCallback, short DsCallback, boolean isResyncExpiredItems, String group, String subGroup, java.util.HashMap queryInfo, BitSet Flag, Object lockId, long version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName, String resyncProviderName) throws Exception
    {

        //we create a user binary object.
        if (this.getSerializationEnabled()) {
            value = UserBinaryObject.CreateUserBinaryObject((byte[]) value);
        }

        if ((short) onRemoveCallback != -1 || (short) onUpdateCallback != -1 || (short) onAsyncOperationCompleteCallback != -1 || (short) DsCallback != -1) {
            value = new CallbackEntry(getClientID(), -1, value, onRemoveCallback, onUpdateCallback, onAsyncOperationCompleteCallback, DsCallback, Flag, EventDataFilter.None, EventDataFilter.None);
        }

        TimeSpan slidingExp = slidingExpiration == TimeSpan.ZERO ? null : slidingExpiration;
        int expType = CacheLoaderUtil.EvaluateExpirationParameters(absoluteExpiration, slidingExp);
        int options = 0;

        if (expType < 2) {
            options = expType;
        }

        if (isResyncExpiredItems) {
            int isResync = isResyncExpiredItems ? 1 : 0;
            isResync = isResync << 1;
            options = options | isResync;
        }

        int prio = (Integer) priority.value();
        prio += 2;
        prio = (prio << 2);
        options = options | prio;
        long absExp = absoluteExpiration != null ? HelperFxn.getUTCTicks(absoluteExpiration) : 0;
        long sldExp = slidingExpiration != null ? slidingExpiration.getTotalTicks() : 0;

        long expiration = expType == 1 ? absExp : sldExp;

      

        Object entry = new CompactCacheEntry(key, value,  expiration, (byte) options, null, group, subGroup, queryInfo, Flag, lockId, version, accessType, providerName, resyncProviderName);
        return entry;
    }

    @Override

    public boolean setAttributes(Object key, CacheItemAttributes attribute) throws Exception
    {
        if (key == null)
        {
            throw new IllegalArgumentException("key");
        }

        if (attribute == null)
        {

            throw new IllegalArgumentException("attributes");
        }

        ExpirationHint hint = com.alachisoft.tayzgrid.caching.autoexpiration.DependencyHelper.GetExpirationHint( attribute.getAbsoluteExpiration(), Cache.DefaultSlidingExpiration);
        return _tgCache.AddExpirationHint(key, hint, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

    }



    
    @Override
    public Object SafeSerialize(Object serializableObject, String serializationContext, BitSet flag, CacheImplBase cacheImpl, RefObject<Long> size)
    {
        Object retVal=null;
        try
        {
            retVal = super.SafeSerialize(serializableObject, serializationContext, flag, cacheImpl, size);
        } 
        catch (GeneralFailureException ex)
        {
            Logger.getLogger(InprocCache.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (_tgCache.getSerializationEnabled())
        {
            //flag.UnsetBit((byte)SerializationBitSetConstant.Flattened);
            return retVal;
        }
        else
        {
            flag.UnsetBit((byte)SerializationBitSetConstant.Flattened);
            return serializableObject;
        }
				//            try
				//            {
				//                java.lang.Class type = serializableObject.getClass();
				//
				//                if (byte[].class.equals(type) && flag != null)
				//                {
				//                    flag.SetBit((byte) BitSetConstants.BinaryData);
				//                    size.argvalue = (long) ((byte[]) serializableObject).length;
				//                    return serializableObject;
				//                }
				//
				//                Object seralized = CompactBinaryFormatter.toByteBuffer(serializableObject, serializationContext);
				//
				//                long value = seralized instanceof byte[] ? Long.valueOf(((byte[]) seralized).length) : Long.valueOf(0);
				//                size.argvalue = value;
				//
				//            } catch (IOException ex)
				//            {
				//                Logger.getLogger(InprocCache.class.getName()).log(Level.SEVERE, null, ex);
				//            }
    }


    /**
     * Function that choose the appropriate function of TGCache's Cache, that
     * need to be called according to the data provided to it.
     */
    @Override

    public Object add(Object key, Object value, java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onDsItemAddedCallback, short asyncItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, java.util.HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, long size) throws Exception
    {

        
        queryInfo = SetDateForQuery(queryInfo);

        try {
            Object entry = makeCompactEntry(key, value,   absoluteExpiration, slidingExpiration, priority, onRemoveCallback, onUpdateCallback, (short) -1, onDsItemAddedCallback, isResyncExpiredItems, group, subGroup, queryInfo, flagMap, null, 0, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, providerName, resyncProviderName);

            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            
            operationContext.Add(OperationContextFieldName.ValueDataSize, size);

            _tgCache.AddEntry(entry, operationContext);

        } catch (Exception e) {
            throw e;
        }
        return value;

    }

    public java.util.HashMap SetDateForQuery(HashMap queryInfo) {
        java.util.HashMap queryInfoDic = (java.util.HashMap) ((queryInfo.get("query-info") instanceof java.util.HashMap) ? queryInfo.get("query-info") : null);
        if (queryInfoDic != null) {
            Iterator queryInfoEnum = queryInfoDic.entrySet().iterator();
            Map.Entry keyVal;
            while (queryInfoEnum.hasNext()) {
                keyVal = (Map.Entry) queryInfoEnum.next();
                java.util.ArrayList valuesEnum = (java.util.ArrayList) keyVal.getValue();
                for (int i = 0; i < valuesEnum.size(); i++) {
                    if (valuesEnum.get(i) instanceof java.util.Date) {

                        Long ticks = (HelperFxn.getTicks((java.util.Date) valuesEnum.get(i)));
                        String str = ticks.toString();
                        valuesEnum.set(i, str);
                    }
                }
            }
        }
        return queryInfo;

    }

    /**
     * Add array of <see cref="CacheItem"/> to the cache.
     *
     * @param keys The cache keys used to reference the items.
     * @param items The items that are to be stored
     * @param group The data group of the item
     * @param subGroup Sub group of the group
     * @return keys that are added or that alredy exists in the cache and their
     * status. If CacheItem contains invalid values the related exception is
     * thrown. See <see cref="CacheItem"/> for invalid property values and
     * related exceptions <example>The following example demonstrates how to add
     * items to the cache with an absolute expiration 2 minutes from now, a
     * priority of high, and that notifies the application when the item is
     * removed from the cache.
     *
     * First create a CacheItems.      <code>
     * string keys = {"ORD_23", "ORD_67"};
     * CacheItem items = new CacheItem[2]
     * items[0] = new CacheItem(new Order());
     * items[0].AbsoluteExpiration = DateTime.Now.AddMinutes(2);
     * items[0].Priority = CacheItemPriority.High;
     * items[0].ItemRemoveCallback = onRemove;
     *
     * items[1] = new CacheItem(new Order());
     * items[1].AbsoluteExpiration = DateTime.Now.AddMinutes(2);
     * items[1].Priority = CacheItemPriority.Low;
     * items[1].ItemRemoveCallback = onRemove;
     * </code>
     *
     * Then add CacheItem to the cache      <code>
     *
     * NCache.Cache.Add(keys, items, "Customer", "Orders");
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Add(keys, items, "Customer", "Orders");
     *
     * </code> </example>
     */
    @Override

    public java.util.HashMap add(Object[] keys, CacheItem[] items, int[] removeCallbackIds, int[] updateCallbackIds, short onDataSourceItemsAdded, String providerName, long[] sizes) throws Exception
    {
        if (_tgCache == null)
        {

            return null;
        }

       
        Object[] entries = new Object[items.length];

        short itemRemovedCallback = -1;
        short itemUpdatedCallback = -1;

        try
        {
            for (int i = 0; i < items.length; i++)
            {
                Object key = keys[i];

                CacheItem item = items[i];

               
                itemRemovedCallback = (short) removeCallbackIds[i];
                itemUpdatedCallback = (short) updateCallbackIds[i];;

                java.util.HashMap queryInfo = SetDateForQuery(item.getQueryInfo());
                item.setQueryInfo(queryInfo);

                entries[i] = makeCompactEntry(key, item.getValue(),   item.getAbsoluteExpiration(), item.getSlidingExpiration(), item.getPriority(), itemRemovedCallback, itemUpdatedCallback, (short) -1, onDataSourceItemsAdded, item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(), item.getQueryInfo(), item.getFlag(), null, 0, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, providerName, item.getResyncProviderName());
            }

            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            
            operationContext.Add(OperationContextFieldName.ValueDataSize, sizes);

            return getHashMap((HashMap) _tgCache.AddEntries(entries, operationContext));
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Function that choose the appropriate function of TGCache's Cache, that
     * need to be called according to the data provided to it.
     */
    @Override

    public Object addAsync(Object key, Object value,  java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onAsyncItemAddCallback, short dsItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, java.util.HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, long size) throws Exception
    {
        if (_tgCache == null)
        {

            return null;
        }

       
        java.util.HashMap queryInfoDic = (java.util.HashMap) ((queryInfo.get("query-info") instanceof java.util.HashMap) ? queryInfo.get("query-info") : null);
        if (queryInfoDic != null) {
            Iterator queryInfoEnum = queryInfoDic.entrySet().iterator();
            Map.Entry keyVal;
            while (queryInfoEnum.hasNext()) {
                keyVal = (Map.Entry) queryInfoEnum.next();
                java.util.ArrayList valuesEnum = (java.util.ArrayList) keyVal.getValue();
                for (int i = 0; i < valuesEnum.size(); i++) {
                    if (valuesEnum.get(i) instanceof java.util.Date) {
                        valuesEnum.set(i, ((NCDateTime) valuesEnum.get(i)).getTicks());
                    }
                }
            }
        }

        Object entry = makeCompactEntry(key, value,  absoluteExpiration, slidingExpiration, priority, onRemoveCallback, onUpdateCallback, onAsyncItemAddCallback, dsItemAddedCallback, isResyncExpiredItems, group, subGroup, queryInfo, flagMap, null, 0, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, providerName, resyncProviderName);

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        
        operationContext.Add(OperationContextFieldName.ValueDataSize, size);

        _tgCache.AddAsyncEntry(entry, operationContext);

        return value;
    }

    ///#region    /                 --- Get ---           /
    /**
     * Retrieves the specified item from the Cache object.
     *
     * @param key The identifier for the cache item to retrieve.
     * @return The retrieved cache item, or a null reference (Nothing in Visual
     * Basic) if the key is not found.
     * @exception ArgumentNullException <paramref name="key"/> contains a null
     * reference (Nothing in Visual Basic).
     * @exception ArgumentException <paramref name="key"/> is not serializable.
     *
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how to retrieve the value
     * cached for an ASP.NET text box server control.      <code>
     *
     * NCache.Cache.Get("MyTextBox.Value");
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Get("MyTextBox.Value");
     *
     * </code> </example>
     */
    //public override CompressedValueEntry Get(string key, BitSet flagMap)
    //{
    //    return Get(key, flagMap, null, null);
    //}
    /**
     * Retrieves the specified item from the Cache object. If the object is read
     * thru the data source, put is against the given group and sub group.
     *
     * @param key The identifier for the cache item to retrieve.
     * @param group Group of the object.
     * @param subGroup Sub group of the group.
     * @return The retrieved cache item, or a null reference (Nothing in Visual
     * Basic) if the key is not found.
     * @throws ArgumentNullException <paramref name="key"/> contains a null
     * reference (Nothing in Visual Basic).
     * @throws ArgumentException <paramref name="key"/> is not serializable.
     * @throws ArgumentNullException <paramref name="group"/> contains a null
     * reference (Nothing in Visual Basic).
     *
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     * <p>
     * Note: The group and subGroup parameters are used only if the object is
     * read thru the data source. Otherwise the object will be returned from the
     * cache whether it belongs to the given group and sub group or not. </p>
     *
     * <example>The following example demonstrates how to retrieve the value
     * cached for an ASP.NET text box server control.      <code>
     *
     * NCache.Cache.Get("MyTextBox.Value", "Customer", null);
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Get("MyTextBox.Value", "Customer", null);
     *
     * </code> </example>
     */
    @Override
    public com.alachisoft.tayzgrid.caching.CompressedValueEntry get(Object key, BitSet flagMap, String group, String subGroup, CacheItemVersion version, LockHandle lockHandle, TimeSpan lockTimeout, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName) throws OperationFailedException
    {

        Object lockId = lockHandle == null ? null : lockHandle.getLockId();
        try {
            NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
            java.util.Date lockDate = time.getDate();
            com.alachisoft.tayzgrid.caching.CompressedValueEntry cmpEntry = null;

            long itemVersion = version == null ? 0 : version.getVersion();

            if (_tgCache != null) {

                tangible.RefObject<Long> tempRef_itemVersion = new tangible.RefObject<Long>(itemVersion);
                tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
                tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
                try {
                    cmpEntry = _tgCache.GetGroup(key, flagMap, group, subGroup, tempRef_itemVersion, tempRef_lockId, tempRef_lockDate, lockTimeout, accessType, providerName, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                } catch (OperationFailedException e) {
                    throw e;
                }
                itemVersion = tempRef_itemVersion.argvalue;
                lockId = tempRef_lockId.argvalue;
                lockDate = tempRef_lockDate.argvalue;
                if (cmpEntry != null && cmpEntry.Value != null) {
                    if (cmpEntry.Value instanceof UserBinaryObject) {
                        UserBinaryObject ubObject = (UserBinaryObject) ((cmpEntry.Value instanceof UserBinaryObject) ? cmpEntry.Value : null);
                        cmpEntry.Value = ubObject.GetFullObject();                        
                    }
                }

                if (lockHandle == null) {
                    lockHandle = new LockHandle();
                }
                lockHandle.setLockId(lockId == null ? null : (String) ((lockId instanceof String) ? lockId : null));
                lockHandle.setLockDate(lockDate);
                version.setVersion(itemVersion);
                return cmpEntry;
            }
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage());

        }
        return null;
    }

    @Override
    public java.util.HashMap getByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comparisonType) throws Exception {
        if (_tgCache == null) {
            return null;
        }

        OperationContext operationContext =  new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
        java.util.HashMap items = getHashMap(_tgCache.GetByTag(tagsToStringArray(tags), comparisonType, operationContext));

        if (items != null) {
            Iterator ide = items.entrySet().iterator();
            Map.Entry keyVal;
            while (ide.hasNext()) {
                keyVal = (Map.Entry) ide.next();
                com.alachisoft.tayzgrid.caching.CompressedValueEntry cmpEntry = (CompressedValueEntry) ((keyVal.getValue() instanceof CompressedValueEntry) ? keyVal.getValue() : null);
                if (cmpEntry != null && cmpEntry.Value != null) {
                    if (cmpEntry.Value instanceof UserBinaryObject) {
                        UserBinaryObject ubObject = (UserBinaryObject) ((cmpEntry.Value instanceof UserBinaryObject) ? cmpEntry.Value : null);
                        cmpEntry.Value = ubObject.GetFullObject();
                    }
                }
            }
        }

        return items;
    }

    @Override
    public void removeByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comarisonType) throws Exception {
        if (_tgCache == null) {
            return;
        }

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        
        operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
        _tgCache.RemoveByTag(tagsToStringArray(tags), comarisonType, operationContext);
    }

    @Override
    public java.util.Collection getKeysByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comparisonType) throws Exception {
        if (_tgCache == null) {
            return null;
        }

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
        return _tgCache.GetKeysByTag(tagsToStringArray(tags), comparisonType, operationContext);
    }

    /**
     * Retrieves the keys of items in a group or sub group.
     *
     * @param group The group whose keys are to be returned.
     * @param subGroup The sub group of the group foe which keys are to be
     * returned.
     * @return The list of keys of a group or a sub group.
     * @throws ArgumentNullException <paramref name="group"/> contains a null
     * reference (Nothing in Visual Basic).
     *
     * <p>
     * If only group is specified, keys for the group and all the sub groups of
     * the group are returned. If both the group and sub group are specified.
     * Only the keys related to the sub group are returned. </p>
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how to retrieve the value
     * cached for an ASP.NET text box server control.      <code>
     *
     * ArrayList list = NCache.Cache.Get("Customer", "Orders");
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * ArrayList list = Cache.Get("Customer", "Orders");
     *
     * </code> </example>
     */
    @Override
    public java.util.ArrayList getGroupKeys(String group, String subGroup) throws OperationFailedException {
        if (_tgCache != null) {
            try {
                OperationContext operationContext =  new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
                return _tgCache.GetGroupKeys(group, subGroup,operationContext );
            } catch (OperationFailedException e) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Retrieves the key and value pairs in a group or sub group.
     *
     * @param group The group whose data is to be returned.
     * @param subGroup The sub group of the group for which data is to be
     * returned.
     * @return The list of key and value pairs of a group or a sub group.
     * @throws ArgumentNullException <paramref name="group"/> contains a null
     * reference (Nothing in Visual Basic).
     *
     * <p>
     * If only group is specified, data for the group and all the sub groups of
     * the group are returned. If both the group and sub group are specified.
     * Only the data related to the sub group are returned. </p>
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how to retrieve the value
     * cached for an ASP.NET text box server control.      <code>
     *
     * HashMap table = NCache.Cache.Get("Customer", "Orders");
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * HashMap table = Cache.Get("Customer", "Orders");
     *
     * </code> </example>
     */
    @Override
    public java.util.HashMap getGroupData(String group, String subGroup) throws OperationFailedException {
        java.util.HashMap items = null;
        if (_tgCache != null) {
            try {
                OperationContext operationContext =  new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation); 
                operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
                items = getHashMap(_tgCache.GetGroupData(group, subGroup,operationContext));
            } catch (OperationFailedException e) {
                throw e;
            }
            if (items != null) {
                Iterator ide = items.entrySet().iterator();
                Map.Entry keyVal;
                while (ide.hasNext()) {
                    keyVal = (Map.Entry) ide.next();
                    CompressedValueEntry cmpEntry = (CompressedValueEntry) ((keyVal.getValue() instanceof CompressedValueEntry) ? keyVal.getValue() : null);
                    if (cmpEntry != null && cmpEntry.Value != null) {
                        if (cmpEntry.Value instanceof UserBinaryObject) {
                            UserBinaryObject ubObject = (UserBinaryObject) ((cmpEntry.Value instanceof UserBinaryObject) ? cmpEntry.Value : null);
                            cmpEntry.Value = ubObject.GetFullObject();
                        }
                    }
                }
            }
        }
        return items;
    }
    
    public void asyncGet(Object[] keys, BitSet flagMap,String providerName, OperationContext operationContext) {
        if(_tgCache == null) {
            return;
        }
        short onAsyncOperationCallback =(Short) operationContext.GetValueByField(OperationContextFieldName.LoadAllNotificationId);
        if(onAsyncOperationCallback != -1) {
            AsyncCallbackInfo onAsyncOperationCompleteCallback = new AsyncCallbackInfo(-1, getClientID(),onAsyncOperationCallback);
            operationContext.Add(OperationContextFieldName.LoadAllNotificationId, onAsyncOperationCompleteCallback);
        }
        _tgCache.AsyncGetBulk(keys, flagMap, providerName, operationContext);
    }

    /**
     * Retrieves the object from the cache for the given keys as key value pairs
     *
     * @param keys The keys against which items are to be fetched.
     * @return The retrieved cache items.
     * @throws ArgumentNullException <paramref name="keys"/> contains a null
     * reference (Nothing in Visual Basic).
     * @throws ArgumentException <paramref name="keys"/> is not serializable.
     *
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how to retrieve the value
     * cached for an ASP.NET text box server control.      <code>
     *
     * NCache.Cache.Get(keys);
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Get(keys);
     *
     * </code> </example>
     */
    @Override

    public java.util.Map get(Object[] keys, BitSet flagMap, String providerName , short jCacheCompletionListener, boolean replaceExistingValues, boolean isAsync) throws OperationFailedException
    {
        if (_tgCache != null)
        {

            java.util.Map items = null;

            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            if(isAsync) {
                operationContext.Add(OperationContextFieldName.IsAsync,isAsync);
                operationContext.Add(OperationContextFieldName.ReplaceExistingValues, replaceExistingValues);
                operationContext.Add(OperationContextFieldName.LoadAllNotificationId, jCacheCompletionListener);
                operationContext.Add(OperationContextFieldName.JCacheLoader, true);
            }
            
            
            try
            {
                if(!isAsync) {
                    items = getHashMap((java.util.HashMap) _tgCache.GetBulk(keys, flagMap, providerName, operationContext));
                }
                else
                    asyncGet(keys, flagMap, providerName, operationContext);
            }
            catch (OperationFailedException e)
            {

                throw e;
            }
            if (items != null) {
                Iterator ide = items.entrySet().iterator();
                Map.Entry keyVal;
                while (ide.hasNext()) {
                    keyVal = (Map.Entry) ide.next();
                    CompressedValueEntry cmpEntry = (CompressedValueEntry) ((keyVal.getValue() instanceof com.alachisoft.tayzgrid.caching.CompressedValueEntry) ? keyVal.getValue() : null);
                    if (cmpEntry != null && cmpEntry.Value != null) {
                        if (cmpEntry.Value instanceof UserBinaryObject) {
                            UserBinaryObject ubObject = (UserBinaryObject) ((cmpEntry.Value instanceof UserBinaryObject) ? cmpEntry.Value : null);
                            cmpEntry.Value = ubObject.GetFullObject();
                        }
                    }
                }
            }

            return items;
        }
        return null;
    }

    @Override
    public Object getCacheItem(Object key, BitSet flagMap, String group, String subGroup, DSReadOption dsReadOption, CacheItemVersion version, LockHandle lockHandle, TimeSpan lockTimeout, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, Exception
    {
        if (lockHandle == null)
        {
            lockHandle = new LockHandle();
        }
        if (version == null) {
            version = new CacheItemVersion();
        }
        Object lockId = lockHandle.getLockId();
        java.util.Date lockDate = new java.util.Date(0);
        DataSourceReadOptions readOptions = DataSourceReadOptions.None;
        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
     //   operationContext.Add(OperationContextFieldName.ReadThru, flagMap.IsBitSet((byte) BitSetConstants.ReadThru));
        operationContext.Add(OperationContextFieldName.GenerateQueryInfo, true);
        operationContext.Add(OperationContextFieldName.ReaderBitsetEnum, flagMap);
        if (providerName != null) {
            operationContext.Add(OperationContextFieldName.ReadThruProviderName, providerName);
        }

        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version != null ? version.getVersion() : 0);
        Object tempVar = _tgCache.GetCacheEntry(key, group, subGroup, tempRef_lockId, tempRef_lockDate, lockTimeout, tempRef_version, accessType, operationContext);
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        version.setVersion(tempRef_version.argvalue);
        CacheEntry entry = (CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null);

        if (entry != null) {
            if (entry.getValue() instanceof CallbackEntry) {
                CallbackEntry cbEntry = (CallbackEntry) ((entry.getValue() instanceof CallbackEntry) ? entry.getValue() : null);
                if (cbEntry.getValue() instanceof UserBinaryObject) {
                    cbEntry.setValue(((UserBinaryObject) cbEntry.getValue()).GetFullObject());
                }
            } else {
                if (entry.getValue() instanceof UserBinaryObject) {
                    entry.setValue(((UserBinaryObject) entry.getValue()).GetFullObject());
                }
            }

        }

        lockHandle.setLockId(lockId == null ? null : (String) ((lockId instanceof String) ? lockId : null));
        lockHandle.setLockDate(lockDate);

        return entry;
    }

    ///#region    /                 --- Insert ---           /
    /**
     * Function that choose the appropriate function of TGCache's Cache, that
     * need to be called according to the data provided to it.
     */
    @Override
    public InsertResult insert(Object key, Object value,  java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onDsItemAddedCallback, short asyncItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, java.util.HashMap queryInfo, BitSet flagMap, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName, String resyncProviderName, EventDataFilter itemAddedDataFilter, EventDataFilter itemRemovedDataFilter, long size, InsertParams options) throws Exception
    {
      
        queryInfo = SetDateForQuery(queryInfo);
        try {
            Object entry = makeCompactEntry(key, value,   absoluteExpiration, slidingExpiration, priority, onRemoveCallback, onUpdateCallback, (short) -1, onDsItemAddedCallback, isResyncExpiredItems, group, subGroup, queryInfo, flagMap, lockId, version
                    == null ? 0 : version.getVersion(), accessType, providerName, resyncProviderName);

            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            
            operationContext.Add(OperationContextFieldName.ValueDataSize, size);
            if(options!=null)
                operationContext.Add(OperationContextFieldName.InsertParams, options);


            return _tgCache.InsertEntry(entry, operationContext);
        }
        catch (Exception e)
        {

            throw e;
        }

    }

    /**
     * Insert list of <see cref="CacheItem"/> to the cache
     *
     * @param keys The cache keys used to reference the items.
     * @param items The items that are to be stored
     * @param group The group with which this object is associated
     * @param subGroup The subGroup of the group
     * @return returns keys that are added or updated successfully and their
     * status. If CacheItem contains invalid values the related exception is
     * thrown. See <see cref="CacheItem"/> for invalid property values and
     * related exceptions <example>The following example demonstrates how to
     * assign an item high priority when you insert it into your application's
     * <see cref="Cache"/> object.
     * <p>
     * <b>Note: </b>For more information about how to use this method with the
     * <see cref="CacheItemRemovedCallback"/> delegate, see <see
     * cref="CacheItemRemovedCallback"/>. </p> First create CacheItems.      <code>
     * string[] keys = {"SQLDSN", "ORADSN"};
     * CacheItem items[] = new CacheItem[2];
     * items[0] = new CacheItem(sqlConnectionString);
     * item.AbsoluteExpiration = DateTime.Now.AddMinutes(2);
     * item.SlidingExpiration = TimeSpan.Zero;
     * item.Priority = CacheItemPriority.High;
     * item.ItemRemoveCallback = onRemove;
     *
     * items[1] = new CacheItem(oraConnectionString);
     * item.AbsoluteExpiration = DateTime.Now.AddMinutes(1);
     * item.SlidingExpiration = TimeSpan.Zero;
     * item.Priority = CacheItemPriority.Low;
     * item.ItemRemoveCallback = onRemove;
     * </code>
     *
     * Then insert CacheItems to the cache      <code>
     *
     * NCache.Cache.Insert(keys, items, "Connection", null);
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Insert(keys, items, "Connection", null);
     *
     * </code> </example>
     */
    @Override

    public java.util.HashMap insert(Object[] keys, CacheItem[] items, int[] removeCallbackIds, int[] updateCallbackIds, short onDsItemsUpdatedCallback, String providerName, long[] sizes) throws Exception
    {


        if (_tgCache == null) {
            return null;
        }

        Object[] entries = new Object[items.length];

     
        short itemRemovedCallback = -1;
        short itemUpdatedCallback = -1;

        try
        {
            for (int i = 0; i < items.length; i++)
            {
                Object key = keys[i];

                CacheItem item = items[i];

              
                itemRemovedCallback = -1;
                itemUpdatedCallback = -1;

                java.util.HashMap queryInfo = SetDateForQuery(item.getQueryInfo());
                item.setQueryInfo(queryInfo);

                entries[i] = makeCompactEntry(key, item.getValue(),  item.getAbsoluteExpiration(), item.getSlidingExpiration(), item.getPriority(), (short) removeCallbackIds[i], (short) updateCallbackIds[i], (short) -1, onDsItemsUpdatedCallback, item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(), item.getQueryInfo(), item.getFlag(), null, 0, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, providerName, item.getResyncProviderName());
            }

            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            
            operationContext.Add(OperationContextFieldName.ValueDataSize, sizes);
            return getHashMap((HashMap) _tgCache.InsertEntries(entries, operationContext));
        } catch (Exception e) {
            throw e;
        }

    }

    /**
     * Function that choose the appropriate function of TGCache's Cache, that
     * need to be called according to the data provided to it.
     */
    @Override
    public void insertAsync(Object key, Object value, java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onAsyncItemUpdateCallback, short onDsItemUpdatedCallback, boolean isResyncExpiredItems, String group, String subGroup, java.util.HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemoveDataFilter, long size) throws Exception
    {

     
        java.util.HashMap queryInfoDic = (java.util.HashMap) ((queryInfo.get("query-info") instanceof java.util.HashMap) ? queryInfo.get("query-info") : null);
        if (queryInfoDic != null) {
            Iterator queryInfoEnum = queryInfoDic.entrySet().iterator();
            Map.Entry keyVal;
            while (queryInfoEnum.hasNext()) {
                keyVal = (Map.Entry) queryInfoEnum.next();

                java.util.ArrayList valuesEnum = (java.util.ArrayList) keyVal.getValue();
                for (int i = 0; i < valuesEnum.size(); i++) {
                    if (valuesEnum.get(i) instanceof java.util.Date) {
                        valuesEnum.set(i, ((NCDateTime) valuesEnum.get(i)).getTicks());
                    }
                }
            }
        }

        try
        {
            Object entry = makeCompactEntry(key, value,  absoluteExpiration, slidingExpiration, priority, onRemoveCallback, onUpdateCallback, onAsyncItemUpdateCallback, onDsItemUpdatedCallback, isResyncExpiredItems, group, subGroup, queryInfo, flagMap, null, 0, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, providerName, resyncProviderName);


            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            
            operationContext.Add(OperationContextFieldName.ValueDataSize, size);
            _tgCache.InsertAsyncEntry(entry, operationContext);
        } catch (Exception e) {
            throw e;
        }

    }

    ///#region    /                 --- Remove ---           /
    /**
     * Removes the object from the <see cref="Cache"/>.
     *
     * @param key The cache key used to reference the item.
     * @return The item removed from the Cache. If the value in the key
     * parameter is not found, returns a null reference (Nothing in Visual
     * Basic).
     * @throws ArgumentNullException <paramref name="key"/> contains a null
     * reference (Nothing in Visual Basic).
     * @throws ArgumentException <paramref name="key"/> is not serializable.
     *
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how you can remove an item
     * from your application's <see cref="Cache"/> object.      <code>
     *
     * NCache.Cache.Remove("timestamp");
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Remove("timestamp");
     *
     * </code> </example>
     */
    @Override

    public com.alachisoft.tayzgrid.caching.CompressedValueEntry remove(Object key, BitSet flagMap, short asyncItemRemovedCallback, short dsItemRemovedCallbackId, boolean isAsync, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String ProviderName) throws Exception
    {

        com.alachisoft.tayzgrid.caching.CompressedValueEntry cmpEntry = null;
        if (_tgCache != null) {
            CallbackEntry cbEntry = null;
            if (dsItemRemovedCallbackId != -1) {
                cbEntry = new CallbackEntry(getClientID(), (short) -1, null, (short) -1, (short) -1, (short) -1, dsItemRemovedCallbackId, flagMap, EventDataFilter.None, EventDataFilter.None);
            }

            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            
            try {
                if (!isAsync) {
                    cmpEntry = _tgCache.Remove(key, flagMap, cbEntry, lockId, version == null ? 0 : version.getVersion(), accessType, ProviderName, operationContext);
                } else {
                    this.removeAsync(key, flagMap, asyncItemRemovedCallback, dsItemRemovedCallbackId, ProviderName);
                }

            } catch (OperationFailedException e) {
                throw e;
            }

            if (cmpEntry != null && cmpEntry.Value != null) {
                if (cmpEntry.Value instanceof UserBinaryObject) {
                    UserBinaryObject ubObject = (UserBinaryObject) ((cmpEntry.Value instanceof UserBinaryObject) ? cmpEntry.Value : null);
                    cmpEntry.Value = ubObject.GetFullObject();
                }
            }

            return cmpEntry;
        }
        return null;
    }

    /**
     * Removes the object from the <see cref="Cache"/>.
     *
     * @param key The cache key used to reference the item.
     * @throws ArgumentNullException <paramref name="key"/> contains a null
     * reference (Nothing in Visual Basic).
     * @throws ArgumentException <paramref name="key"/> is not serializable.
     *
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how you can remove an item
     * from your application's <see cref="Cache"/> object.      <code>
     *
     * NCache.Cache.Remove("timestamp");
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Remove("timestamp");
     *
     * </code> </example>
     */
    @Override
    public boolean delete(Object key, BitSet flagMap, short asyncItemRemovedCallback, short dsItemRemovedCallbackId, boolean isAsync, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String ProviderName, DeleteParams deleteParams) throws Exception
    {
        if (_tgCache != null)
        {

            CallbackEntry cbEntry = null;
            if (dsItemRemovedCallbackId != -1) {
                cbEntry = new CallbackEntry(getClientID(), (short) -1, null, (short) -1, (short) -1, (short) -1, dsItemRemovedCallbackId, flagMap, EventDataFilter.None, EventDataFilter.None);
            }

            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            

            if(deleteParams!=null)
                operationContext.Add(OperationContextFieldName.DeleteParams, deleteParams);
            try
            {
               return _tgCache.Delete(key, flagMap, cbEntry, lockId, version == null ? 0 : version.getVersion(), accessType, ProviderName, operationContext);


            } catch (OperationFailedException e) {
                throw e;
            }
        }
        return false;
    }

    /**
     * Removes the objects from the <see cref="Cache"/>.
     *
     * @param keys The cache keys used to reference the item.
     * @return The items removed from the Cache. If the value in the keys
     * parameter is not found, returns a null reference (Nothing in Visual
     * Basic).
     * @throws ArgumentNullException <paramref name="keys"/> contains a null
     * reference (Nothing in Visual Basic).
     * @throws ArgumentException <paramref name="keys"/> is not serializable.
     *
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how you can remove an item
     * from your application's <see cref="Cache"/> object.      <code>
     *
     * NCache.Cache.Remove(keys);
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.Remove(keys);
     *
     * </code> </example>
     */
    @Override

    public java.util.HashMap remove(Object[] keys, BitSet flagMap, String providerName, short onDsItemsRemovedCallback) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        CallbackEntry cbEntry = null;
        java.util.Map items = null;
        if (onDsItemsRemovedCallback != -1) {
            cbEntry = new CallbackEntry(getClientID(), (short) -1, null, (short) -1, (short) -1, (short) -1, onDsItemsRemovedCallback, flagMap, EventDataFilter.None, EventDataFilter.None);
        }

        Object[] baseKeys = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            baseKeys[i] = keys[i];
        }

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        
        try {
            items = getHashMap((HashMap) _tgCache.Remove(baseKeys, flagMap, cbEntry, providerName, operationContext));

        } catch (OperationFailedException e) {
            throw e;
        }
        if (items != null) {
            Iterator ide = items.entrySet().iterator();
            Map.Entry keyVal;
            while (ide.hasNext()) {
                keyVal = (Map.Entry) ide.next();
                com.alachisoft.tayzgrid.caching.CompressedValueEntry cmpEntry = (com.alachisoft.tayzgrid.caching.CompressedValueEntry) ((keyVal.getValue() instanceof CompressedValueEntry) ? keyVal.getValue() : null);
                if (cmpEntry != null && cmpEntry.Value != null) {
                    if (cmpEntry.Value instanceof UserBinaryObject) {
                        UserBinaryObject ubObject = (UserBinaryObject) ((cmpEntry.Value instanceof UserBinaryObject) ? cmpEntry.Value : null);
                        cmpEntry.Value = ubObject.GetFullObject();
                    }
                }
            }
        }

        return (HashMap) items;
    }

    @Override

    public void delete(Object[] keys, BitSet flagMap, String providerName, short onDsItemsRemovedCallback) throws OperationFailedException
    {

        CallbackEntry cbEntry = null;
        if (onDsItemsRemovedCallback != -1) {
            cbEntry = new CallbackEntry(getClientID(), (short) -1, null, (short) -1, (short) -1, (short) -1, onDsItemsRemovedCallback, flagMap, EventDataFilter.None, EventDataFilter.None);
        }

        Object[] baseKeys = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            baseKeys[i] = keys[i];
        }

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        
        try {
            _tgCache.Delete(baseKeys, flagMap, cbEntry, providerName, operationContext);

        } catch (OperationFailedException e) {
            throw e;
        }
    }

    /**
     * Removes the object from the <see cref="Cache"/> asynchronously.
     *
     * @param key The cache key used to reference the item.
     * @return The item removed from the Cache. If the value in the key
     * parameter is not found, returns a null reference (Nothing in Visual
     * Basic).
     * @throws ArgumentNullException <paramref name="key"/> contains a null
     * reference (Nothing in Visual Basic).
     * @throws ArgumentException <paramref name="key"/> is not serializable.
     *
     * This is similar to <see cref="Remove"/> except that the operation is
     * performed asynchronously. A <see cref="ItemRemoved"/> event is fired upon
     * successful completion of this method.It is not possible to determine if
     * the actual operation has failed, therefore use this operation for the
     * cases when it does not matter much.
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how you can remove an item
     * from your application's <see cref="Cache"/> object.      <code>
     *
     * NCache.Cache.RemoveAsync("timestamp");
     *
     * </code> Or simply in a class deriving from <see
     * cref="Alachisoft.NCache.Web.UI.NPage"/> or <see
     * cref="Alachisoft.NCache.Web.UI.NUserControl"/>.      <code>
     *
     * Cache.RemoveAsync("timestamp");
     *
     * </code> </example>
     */
    @Override

    public void removeAsync(Object key, BitSet flagMap, short onDsItemRemovedCallback)
    {
        if (key == null)
        {

            throw new IllegalArgumentException("key");
        }
        if (_tgCache == null) {
            return;
        }

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        

        if (onDsItemRemovedCallback != -1) {
            CallbackEntry entry = new CallbackEntry(getClientID(), (short) -1, null, (short) -1, (short) -1, (short) -1, onDsItemRemovedCallback, flagMap, EventDataFilter.None, EventDataFilter.None);
            _tgCache.RemoveAsync(new Object[]{
                key, flagMap, entry, null
            }, operationContext);
        } else {
            _tgCache.RemoveAsync(new Object[]{
                key, flagMap, null, null
            }, operationContext);
        }
    }

    /**
     * Removes the object from the <see cref="Cache"/> asynchronously.
     *
     * @param key The cache key used to reference the item.
     * @param onAsyncItemRemoveCallback The delegate that can be used by the
     * client application to get the result of the Asynchronous Remove
     * operation.
     * @return The item removed from the Cache. If the value in the key
     * parameter is not found, returns a null reference (Nothing in Visual
     * Basic).
     * @throws ArgumentNullException <paramref name="key"/> contains a null
     * reference (Nothing in Visual Basic).
     * @throws ArgumentException <paramref name="key"/> is not serializable.
     *
     * This is similar to <see cref="Remove"/> except that the operation is
     * performed asynchronously. A <see cref="ItemRemoved"/> event is fired upon
     * successful completion of this method.It is not possible to determine if
     * the actual operation has failed, therefore use this operation for the
     * cases when it does not matter much.
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * <example>The following example demonstrates how you can remove an item
     * from your application's <see cref="Cache"/> object.      <code>
     *
     * OnAsyncItemRemoved(string key, object result)
     * {
     * ...
     * }
     *
     * NCache.Cache.RemoveAsync("timestamp", new AsyncItemRemovedCallback(OnAsyncItemRemoved));
     *
     * Cache.RemoveAsync("timestamp", new AsyncItemRemovedCallback(OnAsyncItemRemoved));
     *
     * </code> </example>
     */
    @Override

    public void removeAsync(Object key, BitSet flagMap, short onAsyncItemRemoveCallback, short onDsItemRemovedCallback, String providerName)
    {

        CallbackEntry cbEntry = null;

        if (onAsyncItemRemoveCallback != -1 || onDsItemRemovedCallback != -1) {
            cbEntry = new CallbackEntry(getClientID(), (short) -1, null, (short) -1, (short) -1, onAsyncItemRemoveCallback, onDsItemRemovedCallback, flagMap, EventDataFilter.None, EventDataFilter.None);
        }

        if (_tgCache == null) {
            return;
        }

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        

        _tgCache.RemoveAsync(new Object[]{
            key, flagMap, cbEntry, providerName
        }, operationContext);
    }

    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     */
    @Override
    public void remove(String group, String subGroup) throws OperationFailedException {
        if (group == null) {
            throw new IllegalArgumentException("group");
        }
        if (_tgCache == null) {
            return;
        }

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
        try {
            _tgCache.Remove(group, subGroup, operationContext);
        } catch (OperationFailedException e) {
            throw e;
        }
        return;
    }

    /**
     * Remove on the <see cref="Cache"/> based on the query specified.
     *
     * @param query simple SQL like query syntax t oquery objects from cache
     * @return Returns a dictionary containing removed cache keys and associated
     * objects
     * <example>
     * These operators are supported by NCache Queries. 1. Comparison Operators
     * = , == , != , <> , < , > , <=, >=, IN 2. Logical Operators AND , OR , NOT
     * 3. Miscellaneous () , DateTime.Now , DateTime("any date time compatible
     * string")
     *
     * <code>
     *
     * "delete Test.Application.Employee where this.Name = 'Paul Jones'"
     * "delete Test.Application.Employee where this.Salary > 2000"
     * "delete Test.Application.Employee where this.Name = 'Paul Jones' and this.Salary > 2000"
     * "delete Test.Application.Employee where Not(this.Name = 'Paul Jones' and this.Salary > 2000)"
     *
     * </code>
     * </example>
     */
    @Override
    public int executeNonQuery(String query, HashMap values)
            throws OperationFailedException, Exception {
        if (_tgCache == null) {
            return 0;
        }

        java.util.HashMap tempValues = getValues(values);

        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        operationContext.Add(OperationContextFieldName.RemoveQueryOperation, true);
        operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
        int affectedkeys = _tgCache.RemoveQuery(query, tempValues, operationContext);

        return affectedkeys;
    }

    /**
     * Performs search on the <see cref="Cache"/> based on the query specified.
     *
     * @param query simple SQL like query syntax t oquery objects from cache
     * @param values The IDictionary of atribute names and values.
     * @return Returns a list of cache keys <example> These operators are
     * supported by NCache Queries. 1. Comparison Operators = , == , != , <> , <
     * , > , <=, >=, IN 2. Logical Operators AND , OR , NOT 3. Miscellaneous ()
     * , DateTime.Now , DateTime("any date time compatible string")
     *
     * <code>
     *
     * HashMap values = new HashMap();
     * values.add("Name", "Paul Jones");
     * "select Test.Application.Employee where this.Name = ?"
     *
     * values.add("Salary", 2000);
     * "select Test.Application.Employee where this.Salary > ?"
     *
     * values.Add("Name", "Paul jones");
     * values.Add("Salary", 2000);
     * "select Test.Application.Employee where this.Name = ? and this.Salary > ?"
     *
     * values.Add("Name", "Paul Jones");
     * values.Add("Salary", 2000);
     * "select Test.Application.Employee where Not(this.Name = 'Paul Jones' and this.Salary > 2000)"
     *
     * </code> </example>
     */
    @Override
    public com.alachisoft.tayzgrid.caching.queries.QueryResultSet search(String query, java.util.HashMap values) throws OperationFailedException, TypeIndexNotDefined {
        if (_tgCache == null) {
            return null;
        }
        com.alachisoft.tayzgrid.caching.queries.QueryResultSet resultSet = null;
        java.util.HashMap tempValues = getValues(values);
        try {
            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
            resultSet = _tgCache.Search(query, tempValues, operationContext);
        } catch (OperationFailedException e) {
            if (e.getMessage().contains("Index is not defined for")) {
                throw new com.alachisoft.tayzgrid.runtime.exceptions.TypeIndexNotDefined(e.getMessage());
            }
            if (e.getMessage().contains("ClassCastException")) {
                throw new OperationFailedException(e.getCause().getMessage());
            }
            throw e;
        }
        return resultSet;
    }

    /**
     * Performs search on the <see cref="Cache"/> based on the query specified.
     *
     * @param query simple SQL like query syntax t oquery objects from cache
     * @param values The IDictionary of atribute names and values.
     * @return Returns a dictionary containing cache keys and associated objects
     * <example> These operators are supported by NCache Queries. 1. Comparison
     * Operators = , == , != , <>
     * , < , > , <=, >=, IN 2. Logical Operators AND , OR , NOT 3. Miscellaneous
     * () , DateTime.Now , DateTime("any date time compatible string")
     *
     * <code>
     *
     * HashMap values = new HashMap();
     * values.add("Name", "Paul Jones");
     * "select Test.Application.Employee where this.Name = ?"
     *
     * values.add("Salary", 2000);
     * "select Test.Application.Employee where this.Salary > ?"
     *
     * values.Add("Name", "Paul jones");
     * values.Add("Salary", 2000);
     * "select Test.Application.Employee where this.Name = ? and this.Salary > ?"
     *
     * values.Add("Name", "Paul Jones");
     * values.Add("Salary", 2000);
     * "select Test.Application.Employee where Not(this.Name = 'Paul Jones' and this.Salary > 2000)"
     *
     * </code> </example>
     */
    @Override
    public com.alachisoft.tayzgrid.caching.queries.QueryResultSet searchEntries(String query, java.util.HashMap values) throws Exception {
        if (_tgCache == null) {
            return null;
        }
        com.alachisoft.tayzgrid.caching.queries.QueryResultSet resultSet = null;
        java.util.HashMap tempValues = getValues(values);
        try {
            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
            resultSet = _tgCache.SearchEntries(query, tempValues, operationContext );
        } catch (OperationFailedException e) {
            if (e.getMessage().contains("Index is not defined for")) {
                throw new TypeIndexNotDefined(e.getMessage());
            }
            throw e;
        }
        return resultSet;
    }

    ///#region    /                 --- GetEnumerator ---           /
    /**
     * Retrieves a dictionary enumerator used to iterate through the key
     * settings and their values contained in the cache.
     *
     *
     * If items are added or removed from the cache while enumerating through
     * the items the behavior is not predictable. It is therefore advised not to
     * update the cache keys while enumerating.
     * <p>
     * <b>Note:</b> Just like <see cref="Cache.Count"/> in a cluster especially
     * partitioned this operation is an expensive one and may require network
     * calls. It is therefore advised to use this method only when required.
     * </p>
     * <p>
     * <b>Note:</b> If exceptions are enabled through the <see
     * cref="NCache.ExceptionsEnabled"/> setting, this property throws exception
     * incase of failure.</p>
     *
     * @return An enumerator to iterate through the <see cref="Cache"/> object.
     */
    @Override
    public java.util.Iterator iterator() {

        return null;
    }

    @Override
    public EnumerationDataChunk getNextChunk(EnumerationPointer pointer) throws OperationFailedException {
        EnumerationDataChunk nextChunk = null;

        if (_tgCache != null) {
            try {
                nextChunk = _tgCache.GetNextChunk(pointer, new OperationContext());
            } catch (OperationFailedException e) {
                throw e;
            }
        }

        return nextChunk;
    }

    ///#region /               --- Key based notification registration ---     /
    /**
     * Registers the CacheItemUpdatedCallback and/or CacheItemRemovedCallback
     * for the specified key.
     *
     *
     * <see cref="CacheItemUpdatedCallback"/> and/or <see
     * cref="CacheItemRemovedCallback"/> provided this way are very useful
     * because a client application can show interest in any item already
     * present in the cache. As soon as the item is updated or removed from the
     * cache, the client application is notified and actions can be taken
     * accordingly.
     *
     * @param key The cache key used to reference the cache item.
     * @param updateCallback The CacheItemUpdatedCallback that is invoked if the
     * item with the specified key is updated in the cache.
     * @param removeCallback The CacheItemRemovedCallback is invoked when the
     * item with the specified key is removed from the cache.
     */
    @Override
    public void registerKeyNotificationCallback(Object key, short updateCallbackid, short removeCallbackid, boolean notifyOnItemExpiration) throws OperationFailedException
    {
        if (_tgCache != null)
        {
            try
            {

                inprocEventListener.registerKeyNotificationCallback(key, updateCallbackid, removeCallbackid, notifyOnItemExpiration);
            } catch (OperationFailedException e) {
                throw e;
            }
        }
    }

    @Override
    public void registerKeyNotificationCallback(Object key, short update, short remove, EventDataFilter datafilter, boolean notifyOnItemExpiration) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException
    {

        CallbackInfo cbUpdate = null;
        CallbackInfo cbRemove = null;

        cbUpdate = new CallbackInfo(getClientID(), update, datafilter);
        cbRemove = new CallbackInfo(getClientID(), remove, datafilter, notifyOnItemExpiration);

        inprocEventListener.registerKeyNotificationCallback(key, cbUpdate, cbRemove);
    }

    /**
     * Unregisters the <see cref="CacheItemUpdatedCallback"/> and/or <see
     * cref="CacheItemRemovedCallback"/> already registered for the specified
     * key.
     *
     * @param key The cache key used to reference the cache item.
     * @param updateCallback CacheItemUpdatedCallback that is invoked when the
     * item with the specified key is updated in the cache.
     * @param removeCallback CacheItemRemovedCallback that is invoked when the
     * item with the key is removed from the cache.
     */
    @Override
    public void unRegisterKeyNotificationCallback(Object key, short updateCallbackid, short removeCallbackid) throws OperationFailedException
    {
        if (_tgCache != null)
        {
            try
            {

                _tgCache.UnregisterKeyNotificationCallback(key, new CallbackInfo(getClientID(), updateCallbackid, EventDataFilter.None), new CallbackInfo(getClientID(), removeCallbackid, EventDataFilter.DataWithMetaData), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            } catch (OperationFailedException e) {
                throw e;
            }
        }
    }

    @Override
    public void unRegisterKeyNotificationCallback(Object key, short update, short remove, EventType eventType) throws OperationFailedException     
    {

        this.unRegisterKeyNotificationCallback(key, update, remove);
    }

    @Override
    public void unRegisterKeyNotificationCallback(Object[] key, short update, short remove, EventType eventType) throws OperationFailedException
    {

        this.unRegisterKeyNotificationCallback(key, update, remove);
    }

    @Override
    public void unRegisterKeyNotificationCallback(Object[] key, short update, short remove) throws OperationFailedException
    {
        
        if (_tgCache != null)
            _tgCache.UnregisterKeyNotificationCallback(key, new CallbackInfo(getClientID(), update, EventDataFilter.None), new CallbackInfo(getClientID(), remove, EventDataFilter.DataWithMetaData), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

    }

    /**
     * Registers the CacheItemUpdatedCallback and/or CacheItemRemovedCallback
     * for the list of specified keys.
     *
     *
     * <see cref="CacheItemUpdatedCallback"/> and/or <see
     * cref="CacheItemRemovedCallback"/> provided this way are very useful
     * because a client application can show interest in any item already
     * present in the cache. As soon as the item is updated or removed from the
     * cache, the client application is notified and actions can be taken
     * accordingly.
     *
     * @param keys The list of the cache keys used to reference the cache items.
     * @param updateCallback The CacheItemUpdatedCallback that is invoked if the
     * item with the specified key is updated in the cache.
     * @param removeCallback The CacheItemRemovedCallback is invoked when the
     * item with the specified key is removed from the cache.
     */
    @Override
    public void registerKeyNotificationCallback(Object[] keys, short updateCallbackid, short removeCallbackid) throws OperationFailedException
    {
        if (_tgCache != null)
        {
            try
            {

                inprocEventListener.registerKeyNotificationCallback(keys, updateCallbackid, removeCallbackid);
            } catch (OperationFailedException e) {
                throw e;
            }
        }

    }

    @Override
    public void registerKeyNotificationCallback(Object[] key, short update, short remove, EventDataFilter datafilter, boolean notifyOnItemExpiration) throws OperationFailedException
    {
        CallbackInfo cbUpdate = null;
        CallbackInfo cbRemove = null;

        cbUpdate = new CallbackInfo(getClientID(), update, datafilter);
        cbRemove = new CallbackInfo(getClientID(), remove, datafilter, notifyOnItemExpiration);
        
        if (_tgCache != null)
        {
            try
            {
                inprocEventListener.registerKeyNotificationCallback(key, cbUpdate, cbRemove);
            }
            catch (OperationFailedException e)
            {

                throw e;
            }
        }

    }

    ///#region/          ---Serialization Compact Framework---         /
    /**
     * Initializes the Compact Serialization Framework.
     *
     * @throws CacheArgumentException
     * @throws CompactSerializationException
     * @throws Exception
     */
    public final void InitializeCompactFramework() throws CacheArgumentException, Exception {
        FormatterServices.getDefault().registerKnownTypes(CallbackEntry.class, ((Integer) 107).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.net.Address.class, ((Integer) 110).shortValue());
       
    }

    private java.util.HashMap getHashMap(HashMap table) {
        HashMap hashMap = new HashMap();
        Iterator entries = table.entrySet().iterator();
        Map.Entry entry;

        while (entries.hasNext()) {
            entry = (Map.Entry) entries.next();
            hashMap.put(entry.getKey(), entry.getValue());
        }

        return hashMap;
    }




    // <editor-fold desc=" ----- MapReduce ----- ">
    
	/**
     *
     * @param task
     * @param taskId
     * @param outputOption
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException
     */
        
    @Override
    public void executeMapReduceTask(MapReduceTask task, String taskId, MROutputOption outputOption, short callbackId) throws GeneralFailureException, OperationFailedException
    {
        try {
            //throw new UnsupportedOperationException("Operation not supported");			
            _tgCache.submitMapReduceTask(task, taskId, new TaskCallbackInfo(getClientID(), callbackId), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
        }
        catch (Exception ex) {
            throw new GeneralFailureException(ex.getMessage());
        }
    }
    
    /**
     *
     * @param taskId
     * @param cancelAll
     */
    @Override
    public void cancelTask(String taskId) throws OperationFailedException
    {
        try {
            //throw new UnsupportedOperationException("Operation not supported");
            _tgCache.cancelMapReduceTask(taskId, false);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
    }
    
    /**
     * Cancels all tasks, running as well as waiting
     */
    @Override
    public void cancelAllTasks() throws OperationFailedException
    {
        try {
            //throw new UnsupportedOperationException("Operation not supported");
            _tgCache.cancelMapReduceTask(null, true);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
    }
    
    /**
     * Get List of taskIDs of Running Tasks
     * @return
     */
    @Override
    public java.util.ArrayList getRunningTasks() throws GeneralFailureException
    {
        return _tgCache.getRunningTasks();
    }
    
    @Override
    public TaskStatus getTaskProgress(String taskId) throws GeneralFailureException
    {
        return _tgCache.getTaskProgress(taskId);
    }
    
    @Override
    public TaskEnumerator getTaskEnumerator(String taskId, short callbackId) throws OperationFailedException {
        TaskEnumerator mrResultEnumerator = null;
        try
        {
            OperationContext opContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            opContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
            ArrayList<TaskEnumeratorResult> list = (ArrayList)_tgCache.getTaskEnumerator(new TaskEnumeratorPointer(getClientID(), taskId, callbackId), opContext);
            mrResultEnumerator = new TaskEnumerator(list, this);
            return mrResultEnumerator;
        }
        catch(Exception ex)
        {
            throw new OperationFailedException(ex.getMessage());
        }
    }
    
    @Override
    public TaskEnumeratorResult getNextRecord(String serverAddress, TaskEnumeratorPointer pointer) throws OperationFailedException {
        try
        {
            OperationContext opContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            opContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
            return _tgCache.getTaskNextRecord(pointer, opContext);
        }
        catch(Exception ex)
        {
            throw new OperationFailedException(ex.getMessage());
        }
    }
    
    // </editor-fold>


    @Override
    public java.util.ArrayList<EnumerationDataChunk> getNextChunk(java.util.ArrayList<EnumerationPointer> pointers) throws OperationFailedException {
        EnumerationPointer pointer = null;
        java.util.ArrayList<EnumerationDataChunk> chunks = new java.util.ArrayList<EnumerationDataChunk>();
        if (pointers.size() > 0) {
            pointer = pointers.get(0);
            try {
                chunks.add(_tgCache.GetNextChunk(pointer, new OperationContext()));
            } catch (OperationFailedException e) {
                throw e;
            }
        }
        return chunks;
    }

    private String[] tagsToStringArray(Tag[] tags) throws ArgumentNullException {
        if (tags == null) {
            throw new ArgumentNullException("Tag");
        }

        String[] sTags = new String[tags.length];

        for (int i = 0; i < tags.length; i++) {
            if (sTags == null) {
                throw new ArgumentNullException("Tag is null");
            }

            sTags[i] = tags[i].getTagName();
        }
        return sTags;
    }

    private java.util.HashMap getValues(java.util.Map values) {
        Iterator dicEnum = values.entrySet().iterator();
        java.util.HashMap tempValues = new java.util.HashMap();
        Map.Entry keyVal;
        while (dicEnum.hasNext()) {
            keyVal = (Map.Entry) dicEnum.next();

            try {
                if (keyVal.getValue() instanceof java.util.ArrayList) {
                    java.util.ArrayList list = new java.util.ArrayList((java.util.ArrayList) keyVal.getValue());

                    for (int i = 0; i < list.size(); i++) {
                        if (!(list.get(i) == null)) {
                            if (list.get(i) instanceof String) {
                                list.set(i, (Object) (list.get(i).toString().toLowerCase()));
                            }
                        } else {
                            throw new IllegalArgumentException("NCache query does not support null values", (RuntimeException) null);

                        }
                    }
                    tempValues.put(keyVal.getKey(), new java.util.ArrayList(list));
                } else {
                    if (!(keyVal.getValue() == null)) {
                        if (keyVal.getValue() instanceof String) {
                            tempValues.put(keyVal.getKey(), (Object) (keyVal.getValue().toString().toLowerCase()));
                        } else {
                            tempValues.put(keyVal.getKey(), keyVal.getValue());
                        }
                    } else {

                        throw new IllegalArgumentException("NCache query does not support null values", (Exception) null);
                    }
                }
            } catch (IllegalArgumentException ane) {
                throw ane;
            }
        }

        return tempValues;
    }

    private void sendSnmpPort() {
        ICacheServer cs = null;
        try {
            ServicePropValues.loadServiceProp();
            CacheRPCService _ncacheService = new CacheRPCService("");
            _ncacheService.setServerName(ServicePropValues.BIND_ToCLUSTER_IP);
            _ncacheService.setPort(Integer.parseInt(ServicePropValues.CACHE_MANAGEMENT_PORT));
            cs = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
            if (cs != null) {
                cs.setSnmpPort(_config.getCacheId(), PortPool.getInstance().getSNMPMap());
            }
        } catch (Exception ex) {
        }
        
          if (cs != null) {
                cs.dispose();
            }
    }

    class InprocEventListener implements CacheItemUpdatedCallback, CacheItemRemovedCallback {

        public void itemUpdated(String key) {
            throw new UnsupportedOperationException("Not supported yet.");
        }


        public void itemUpdated(Object key)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void itemRemoved(Object key, Object value, CacheItemRemovedReason reason)
        {

            throw new UnsupportedOperationException("Not supported yet.");
        }
        List<CacheListenerWrapper> _cacheCleared = Collections.synchronizedList(new ArrayList<CacheListenerWrapper>());
        List<CacheListenerWrapper> _cacheItemAdded = Collections.synchronizedList(new ArrayList<CacheListenerWrapper>());
        List<CacheListenerWrapper> _cacheItemUpdated = Collections.synchronizedList(new ArrayList<CacheListenerWrapper>());
        List<CacheListenerWrapper> _cacheItemRemoved = Collections.synchronizedList(new ArrayList<CacheListenerWrapper>());
        List<CacheStatusListenerWrapper> _memberJoined = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
        List<CacheStatusListenerWrapper> _memberLeft = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
        List<CacheStatusListenerWrapper> _cacheStopped = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
        List<CustomListenerWrapper> _customListener = Collections.synchronizedList(new ArrayList<CustomListenerWrapper>());

        NEventStart _itemAdded = null;
        NEventStart _itemUpdated = null;
        NEventStart _itemRemoved = null;
        NEventStart _cacheClearedEvent = null;
        NEventStart _customEvent = null;
        NEventStart _keyUpdated = null;
        NEventStart _keyRemoved = null;
        NEventStart _MRTask = null;
        NEventStart _activeQueryCallback = null;
        NEventStart _asyncOperationCompleted = null;
        NEventStart _onDSUpdated = null;

        private com.alachisoft.tayzgrid.caching.Cache _nCache;
        private boolean _clearNotifRegistered;
        private boolean _addNotifRegistered;
        private boolean _removeNotifRegistered;
        private boolean _updateNotifRegistered;
        private ExecutorService pool = Executors.newCachedThreadPool();

        com.alachisoft.tayzgrid.web.caching.Cache _parent;
        CacheEventsListener _listener;
        CacheAsyncEventsListener _asyncListener;

        public InprocEventListener(Cache parent, com.alachisoft.tayzgrid.caching.Cache cache) {
            this._nCache = cache;

            this._parent = parent;
            this._listener = _parent.getCacheEventListener();
            this._asyncListener = _parent.getCacheAsyncEventListener();

            this._keyUpdated = new NEventStart() {
                //<editor-fold defaultstate="collapsed" desc="Anonymous">

                public Object hanleEvent(Object... obj) throws SocketException, Exception
                {
                    onCustomUpdateCallback(obj[0], obj[1], (EventContext)obj[2]);

                    return null;
                }
                //</editor-fold>
            };
            this._nCache.addCustomUpdateNotifListner(_keyUpdated, null);

            this._keyRemoved = new NEventStart() {
                //<editor-fold defaultstate="collapsed" desc="Anonymous">

                public Object hanleEvent(Object... obj) throws SocketException, Exception
                {
                    onCustomRemoveCallback(obj[0], obj[1], (ItemRemoveReason) obj[2], (BitSet)obj[3], (EventContext)obj[4]);

                    return null;
                }
                //</editor-fold>
            };
            this._nCache.addCustomRemoveNotifListner(_keyRemoved, null);

            
            this._MRTask = new NEventStart() {

                @Override
                public Object hanleEvent(Object... obj) throws SocketException, Exception {
                   
                    onTaskCompletion(obj[0],obj[1],obj[2]);
                    return null;
                }
            };
            
            this._nCache.addTaskNotifListener(_MRTask, null);
            
        }

        public void onTaskCompletion(Object taskId,Object taskStatus,Object callbackId)
        {
            _listener.OnMapReduceTaskCompleteCallback(taskId.toString(), ((EventContext)callbackId).getTaskStatus().getValue(), new Short(((TaskCallbackInfo)taskStatus).getCallback().toString()));
        }
        

        //<editor-fold defaultstate="collapsed" desc="CustomCallbacks">

        public void onCustomUpdateCallback(Object key, Object value, EventContext eventContext)
        {
            EventCacheItem item = EventUtil.ConvertToItem(eventContext.getItem());
            EventCacheItem oldItem = EventUtil.ConvertToItem(eventContext.getOldItem());
            _listener.OnCustomUpdateCallback(key, value, true, item, oldItem, null);            
        }

        public void onCustomRemoveCallback(Object key, Object value, ItemRemoveReason reason, BitSet flag, EventContext eventContext)
        {
            EventCacheItem item = EventUtil.ConvertToItem(eventContext.getItem());
            CacheItemRemovedReason removeReason = EventUtil.ConvertToCIRemoveReason(reason);
            _listener.OnCustomRemoveCallback(key, value, removeReason, flag, true, item);  

        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="CacheItemCallbacks">
        public void cacheCleared(EventContext eventContext) {
            try {
                if (_listener != null) {
                    _listener.OnCacheCleared(true);
                }
            } catch (Exception ex) {

            }

        }

        public void cacheItemAdded(Object key, EventContext eventContext) {
            try {
                BitSet flag = new BitSet();
                EventCacheItem item = EventUtil.ConvertToItem(eventContext.getItem());

				if (_listener != null)
                    _listener.OnItemAdded((Object)key, true, item, flag);
					
            } catch (Exception ex) {

            }

        }

        public void cacheItemRemoved(Object key, Object value, ItemRemoveReason reason, EventContext eventContext) {
            EventCacheItem item = null;
            try {
                BitSet flag = new BitSet((byte) 0);
                if (eventContext != null && eventContext.getItem() != null && eventContext.getItem().getFlags() != null) {
                    flag = eventContext.getItem().getFlags();
                }

                if (eventContext != null && eventContext.getItem() != null) {
                    item = EventUtil.ConvertToItem(eventContext.getItem());
                }


                if (_listener != null)
                    _listener.OnItemRemoved((Object)key,
                        value,
                        reason,
                        flag, true, item);


            } catch (Exception ex) {
            }

        }

        public void cacheItemUpdated(Object key, EventContext eventContext) {
            EventCacheItem oldItem = null;
            EventCacheItem item = null;

            try {
                BitSet flag = new BitSet((byte) 0);
                if (eventContext != null) {
                    if (eventContext.getOldItem() != null) {
                        oldItem = EventUtil.ConvertToItem(eventContext.getOldItem());
                    }
                    if (eventContext.getItem() != null) {
                        item = EventUtil.ConvertToItem(eventContext.getItem());
                    }


                    if (_listener != null)
                        _listener.OnItemUpdated((Object)key, true, item, oldItem, flag);

                }
            } catch (Exception ex) {
            }

        }

        public void customEventOccured(Object notifId, Object data, EventContext eventContext) {
            try {
                if (_listener != null) {
                    _listener.OnCustomNotification(notifId, data, true);
                }
            } catch (Exception ex) {
            }

        }


        public void registerKeyNotificationCallback(Object key, short updateCallback, short removeCallback, boolean notifyOnItemExpiration) throws OperationFailedException
        {
            _nCache.RegisterKeyNotificationCallback(key, new CallbackInfo(getClientID(), updateCallback, EventDataFilter.None), new CallbackInfo(getClientID(), removeCallback, EventDataFilter.DataWithMetaData), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
        }

        public void registerKeyNotificationCallback(Object[] key, short updateCallback, short removeCallback) throws OperationFailedException
        {
            _nCache.RegisterKeyNotificationCallback(key, new CallbackInfo(getClientID(), updateCallback, EventDataFilter.None), new CallbackInfo(getClientID(), removeCallback, EventDataFilter.DataWithMetaData), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
        }
        
        public void registerKeyNotificationCallback(Object key, CallbackInfo updateCallbackInfo, CallbackInfo removeCallbackInfo) throws OperationFailedException {

            if (_nCache != null) {
                _nCache.RegisterKeyNotificationCallback(key, updateCallbackInfo, removeCallbackInfo,
                        new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            }
        }

        
        public void registerKeyNotificationCallback(Object[] key, CallbackInfo updateCallbackInfo, CallbackInfo removeCallbackInfo) throws OperationFailedException     
        {
            if(_nCache != null) {

                _nCache.RegisterKeyNotificationCallback(key,
                        updateCallbackInfo,
                        removeCallbackInfo,
                        new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            }
        }

        public void registerCacheNotification(EnumSet<EventType> registerAgainst) {

            if (registerAgainst.contains(EventType.CacheCleared)) {
                if (_cacheClearedEvent == null) {
                    this._cacheClearedEvent = new NEventStart() {
                        //<editor-fold defaultstate="collapsed" desc="Anonymous">
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            cacheCleared((EventContext) obj[0]);
                            return null;
                        }
                        //</editor-fold>
                    };
                     _nCache.addCacheClearedListner(_cacheClearedEvent, null);
                    this._clearNotifRegistered = true;
                }
                
            }

            if (registerAgainst.contains(EventType.ItemAdded)) {
                if (_itemAdded == null) {
                    this._itemAdded = new NEventStart() {
                        //<editor-fold defaultstate="collapsed" desc="Anonymous">
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            
                            cacheItemAdded((String) obj[0], (EventContext) obj[1]);
                            return null;
                        } 
                        //</editor-fold>
                    }; 
                    _nCache.addItemAddedListner(_itemAdded, null);
                    this._addNotifRegistered = true;
                }
                
            }

            if (registerAgainst.contains(EventType.ItemRemoved)) {
                if (_itemRemoved == null) {
                    this._itemRemoved = new NEventStart() {
                        //<editor-fold defaultstate="collapsed" desc="Anonymous">
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {
                            cacheItemRemoved((String) obj[0], obj[1], (ItemRemoveReason) obj[2], (EventContext) obj[3]);
                            return null;
                        }
                        //</editor-fold>
                    };
                    _nCache.addItemRemovedListner(_itemRemoved, null);
                    this._removeNotifRegistered = true;
                }
                
            }

            if (registerAgainst.contains(EventType.ItemUpdated)) {
                if (_itemUpdated == null) {
                    this._itemUpdated = new NEventStart() {
                        //<editor-fold defaultstate="collapsed" desc="Anonymous">
                        public Object hanleEvent(Object... obj) throws SocketException, Exception {

                            cacheItemUpdated(obj[0], (EventContext) obj[1]);
                            return null;
                        }
                        //</editor-fold>
                    };
                    _nCache.addItemUpdatedListner(_itemUpdated, null);
                    this._updateNotifRegistered = true;
                }
                
            }

        }

        public void registerCustomListener(CustomListener listener) {
            if (listener != null) {
                _customListener.add(new CustomListenerWrapper(listener));
                if (_customListener.size() == 1) {
                    if (_customEvent == null) {
                        this._customEvent = new NEventStart() {
                            //<editor-fold defaultstate="collapsed" desc="Anonymous">
                            public Object hanleEvent(Object... obj) throws SocketException, Exception {
                                customEventOccured(obj[0], obj[1], (EventContext) obj[2]);
                                return null;
                            }
                            //</editor-fold>
                        };
                    }
                    _nCache.addCustomNotifListner(_customEvent, null);
                }

            }
        }

        public void unregisterCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
            {
                if (unregisterAgainst.contains(CacheNotificationType.CacheCleared)
                        || unregisterAgainst.contains(CacheNotificationType.ALL)) {
                    removeIfCacheListenerExists(_cacheCleared, listener);
                }

                if (unregisterAgainst.contains(CacheNotificationType.ItemAdded)
                        || unregisterAgainst.contains(CacheNotificationType.ALL)) {
                    removeIfCacheListenerExists(_cacheItemAdded, listener);
                }

                if (unregisterAgainst.contains(CacheNotificationType.ItemRemoved)
                        || unregisterAgainst.contains(CacheNotificationType.ALL)) {
                    removeIfCacheListenerExists(_cacheItemRemoved, listener);
                }

                if (unregisterAgainst.contains(CacheNotificationType.ItemUpdated)
                        || unregisterAgainst.contains(CacheNotificationType.ALL)) {
                    removeIfCacheListenerExists(_cacheItemUpdated, listener);
                }
            }

            if (_cacheCleared.isEmpty() && unregisterAgainst.contains(CacheNotificationType.CacheCleared)
                    || unregisterAgainst.contains(CacheNotificationType.ALL)) {
                if (_cacheClearedEvent != null) {
                    _nCache.removeCacheClearedListner(this._cacheClearedEvent);
                    this._clearNotifRegistered = false;
                }
            }

            if (_cacheItemAdded.isEmpty() && unregisterAgainst.contains(CacheNotificationType.ItemAdded)
                    || unregisterAgainst.contains(CacheNotificationType.ALL)) {
                if (_itemAdded != null) {
                    _nCache.removeItemAddListner(this._itemAdded);
                    this._addNotifRegistered = false;
                }
            }

            if (_cacheItemRemoved.isEmpty() && unregisterAgainst.contains(CacheNotificationType.ItemRemoved)
                    || unregisterAgainst.contains(CacheNotificationType.ALL)) {
                if (_itemRemoved != null) {
                    _nCache.removeItemRemovedListner(this._itemRemoved);
                    this._removeNotifRegistered = false;
                }
            }

            if (_cacheItemUpdated.size() == 0 && unregisterAgainst.contains(CacheNotificationType.ItemUpdated)
                    || unregisterAgainst.contains(CacheNotificationType.ALL)) {
                if (_itemUpdated != null) {
                    _nCache.removeItemUpdatedListner(this._itemUpdated);
                    this._updateNotifRegistered = false;
                }
            }

        }

        public void unregisterCustomEventListener(CustomListener listener) {
            if (listener != null) {
                if (!removeIfCustomListenerExists(_customListener, listener)) {
                    return;
                }

                if (_customListener.isEmpty()) {
                    _nCache.removeCustomNotifListner(this._customEvent);
                }
            }

        }

        private boolean removeIfCacheListenerExists(List list, java.util.EventListener listener) {
            Iterator ite = list.iterator();
            int indexOf = -1;

            if (listener instanceof CacheListener) {
                while (ite.hasNext()) {
                    CacheListenerWrapper wrapper = (CacheListenerWrapper) ite.next();
                    if (wrapper.verifyListenerInstance((CacheListener) listener)) {
                        indexOf = list.indexOf(wrapper);
                        break;
                    }
                }
            }

            if (indexOf > -1) {
                list.remove(indexOf);
                return true;
            } else {
                return false;
            }
        }

        private boolean removeIfClusterListenerExists(List list, java.util.EventListener listener) {
            Iterator ite = list.iterator();
            int indexOf = -1;

            if (listener instanceof CacheStatusEventListener) {
                while (ite.hasNext()) {
                    CacheStatusListenerWrapper wrapper = (CacheStatusListenerWrapper) ite.next();
                    if (wrapper.verifyListenerInstance((CacheStatusEventListener) listener)) {
                        indexOf = list.indexOf(wrapper);
                        break;
                    }
                }
            }

            if (indexOf > -1) {
                list.remove(indexOf);
                return true;
            } else {
                return false;
            }
        }

        private boolean removeIfCustomListenerExists(List list, java.util.EventListener listener) {
            Iterator ite = list.iterator();
            int indexOf = -1;

            if (listener instanceof CustomListener) {
                while (ite.hasNext()) {
                    CustomListenerWrapper wrapper = (CustomListenerWrapper) ite.next();
                    if (wrapper.verifyListenerInstance((CustomListener) listener)) {
                        indexOf = list.indexOf(wrapper);
                        break;
                    }
                }
            }

            if (indexOf > -1) {
                list.remove(indexOf);
                return true;
            } else {
                return false;
            }
        }
    }
    
        // <editor-fold defaultstate="collapsed" desc="Entry Processor Code">  
    
    /**
     *
     * @param key
     * @param entryProcessor
     * @param defaultReadThru
     * @param defaultWriteThru
     * @param arguments
     * @return
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.SecurityException
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.AggregateException
     */
        
    @Override
    public java.util.Map<Object,EntryProcessorResult>  invokeEntryProcessor(Object[] key,
           com.alachisoft.tayzgrid.runtime.processor.EntryProcessor entryProcessor,String defaultReadThru,String defaultWriteThru,
           Object... arguments) throws GeneralFailureException,OperationFailedException,ConnectionException,AggregateException
    {
        return _tgCache.invokeEntryProcessor(key, entryProcessor, arguments, defaultReadThru, defaultWriteThru, null);        
    }
    
     // </editor-fold>

    class AsyncCallbacks {

        public AsyncCallbacks(Cache parent, com.alachisoft.tayzgrid.caching.Cache nCache) {
            final CacheAsyncEventsListener _asyncListener = parent.getCacheAsyncEventListener();

            nCache.addAsyncOperationCallbackListner(new NEventStart() {
                public Object hanleEvent(Object... obj) throws SocketException, Exception {

                    _asyncListener.OnAsyncOperationCompleted(obj[0], obj[1], true);
                    return null;
                }
            }, null);

            nCache.addDsUpdatedListner(new NEventStart() {
                @Override
                //<editor-fold defaultstate="collapsed" desc="Anonymous">
                public Object hanleEvent(Object... obj) throws SocketException, Exception {

                    Object[] args = (Object[]) obj;
                    Object result = args[0];
                    CallbackEntry cbEntry = (CallbackEntry) args[1];
                    OpCode operationCode = (OpCode) args[2];

                    AsyncCallbackInfo info = (AsyncCallbackInfo) cbEntry.getWriteBehindOperationCompletedCallback();
                    if (info != null && ((Short) info.getCallback()) != -1) {
                        java.util.Hashtable resTbl = (java.util.Hashtable) ((result instanceof java.util.Hashtable) ? result : null);
                        java.util.Hashtable newRes = null;
                        if (resTbl != null) {
                            newRes = new java.util.Hashtable();
                            Iterator ide = resTbl.entrySet().iterator();
                            while (ide.hasNext()) {
                                Object val = ide.next();
                                Map.Entry entry = (Map.Entry) ide.next();

                                if (entry.getValue() != null && entry.getValue() instanceof String) {
                                    DataSourceOpResult.valueOf((String) entry.getValue());
                                    newRes.put(entry.getKey(), DataSourceOpResult.valueOf((String) entry.getValue()));
                                } else {
                                    newRes.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }
                        _asyncListener.OnDataSourceUpdated((Short) info.getCallback(), newRes, operationCode, true);
                    }
                    return null;
                }
                //</editor-fold>
            }, null);
        }

        //<editor-fold defaultstate="collapsed" desc="Legacy Code - Async Operations">
        public void onAsyncAddCompleted(Object[] obj) {
            AsyncItemAddedCallback async;

            int id = ((Short) ((CallbackInfo) obj[1]).getCallback()).intValue();
            async = (AsyncItemAddedCallback) getCallbackQueue().get(id);
            int x = 0;

            final Object callback = callbackQue.get(id);
            if (callback != null) {

                async.asyncItemAdded((String) obj[0], obj[2]);

            }
        }

        public void onAsyncUpdateCompleted(Object[] obj) {
            AsyncItemUpdatedCallback async;

            int id = ((Short) ((CallbackInfo) obj[1]).getCallback()).intValue();
            async = (AsyncItemUpdatedCallback) getCallbackQueue().get(id);
            int x = 0;

            final Object callback = callbackQue.get(id);
            if (callback != null) {

                async.asyncItemUpdated((String) obj[0], obj[2]);

            }
        }

        public void onAsyncRemoveCompleted(Object[] obj) {
            AsyncItemRemovedCallback async;

            int id = ((Short) ((CallbackInfo) obj[1]).getCallback()).intValue();
            async = (AsyncItemRemovedCallback) getCallbackQueue().get(id);
            int x = 0;

            final Object callback = callbackQue.get(id);
            if (callback != null) {

                async.asyncItemRemoved((String) obj[0], obj[2]);

            }
        }

        public void onAsyncClearCompleted(Object[] obj) {
            AsyncCacheClearedCallback async;

            int id = ((Short) ((CallbackInfo) obj[1]).getCallback()).intValue();
            async = (AsyncCacheClearedCallback) getCallbackQueue().get(id);

            final Object callback = callbackQue.get(id);
            if (callback != null) {

                async.asyncCacheCleared(obj[2]);

            }
        }
        //</editor-fold>
    }
}
