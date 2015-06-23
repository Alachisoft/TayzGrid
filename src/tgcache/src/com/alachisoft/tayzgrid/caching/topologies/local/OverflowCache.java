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

import com.alachisoft.tayzgrid.config.ConfigHelper;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.ResetableIterator;

/**
 * Combines two cache stores together and provides abstraction of a single
 * store. Also implements ICacheStore.
 */
public class OverflowCache extends LocalCacheBase {

    /**
     * Listener for the primary cache.
     */
    private static class PrimaryCacheListener implements ICacheEventsListener {

        /**
         * parent composite cache object.
         */
        private OverflowCache _parent = null;

        /**
         * Constructor.
         *
         * @param parent parent composite cache object
         */
        public PrimaryCacheListener(OverflowCache parent) {
            _parent = parent;
        }

        /**
         * Fired when an item is added to the cache.
         */
        @Override
        public void OnItemAdded(Object key, OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * handler for item updated event.
         */
        @Override
        public void OnItemUpdated(Object key, OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * Fire when the cache is cleared.
         */
        @Override
        public void OnCacheCleared(OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * Fired when an item is removed from the cache.
         */
        @Override
        public void OnItemRemoved(Object key, Object val, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {

        }

        /**
         * Fired when multiple items are removed from the cache.
         */
        @Override
        public final void OnItemsRemoved(Object[] key, Object[] val, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContext) {

        }

        /**
         *
         *
         * @param notifId
         * @param data
         */
        @Override
        public void OnCustomEvent(Object notifId, Object data, OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * Fire when hasmap changes when - new node joins - node leaves -
         * manual/automatic load balance
         *
         * @param newHashmap new hashmap
         */
        @Override
        public void OnHashmapChanged(NewHashmap newHashmap, boolean updateClientMap) {
        }

        /**
         *
         *
         * @param operationCode
         * @param result
         * @param cbEntry
         */
        @Override
        public void OnWriteBehindOperationCompletedCallback(OpCode operationCode, Object result, CallbackEntry cbEntry) {
        }

        @Override
        public final void OnCustomUpdateCallback(Object key, Object value, OperationContext operationContext, EventContext eventContext) {
        }

        @Override
        public final void OnCustomRemoveCallback(Object key, Object value, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {
        }

        @Override
        public void OnTaskCallback(Object taskID, Object value, OperationContext operationContext, EventContext eventContext) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /**
     * Listener for the secondary cache.
     */
    private static class SecondaryCacheListener implements ICacheEventsListener {

        /**
         * parent composite cache object.
         */
        private OverflowCache _parent = null;

        /**
         * Constructor.
         *
         * @param parent parent composite cache object
         */
        public SecondaryCacheListener(OverflowCache parent) {
            _parent = parent;
        }

        /**
         * Fired when an item is added to the cache.
         */
        public void OnItemAdded(Object key, OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * handler for item updated event.
         */
        public void OnItemUpdated(Object key, OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * Fire when the cache is cleared.
         */
        public void OnCacheCleared(OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * Fired when an item is removed from the cache.
         */
        public void OnItemRemoved(Object key, Object val, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) throws OperationFailedException, CacheException, CacheException, LockingException {
            if ((reason == ItemRemoveReason.Underused) && (_parent.getListener() != null)) {
                _parent.getListener().OnItemRemoved(key, val, reason, operationContext, eventContext);
            }
            ((IDisposable) val).dispose();
        }

        /**
         * Fired when multiple items are removed from the cache.
         */
        @Override
        public final void OnItemsRemoved(Object[] key, Object[] val, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContext) throws OperationFailedException, CacheException, LockingException {
            // SAL: change approved by 
            if (reason != ItemRemoveReason.Underused || (_parent.getListener() == null)) {
                return;
            }

            for (int i = 0; i < key.length; i++) {
                _parent.getListener().OnItemRemoved(key[i], val[i], reason, operationContext, eventContext[i]);
                ((IDisposable) val[i]).dispose();
            }
        }

        /**
         *
         *
         * @param notifId
         * @param data
         */
        @Override
        public void OnCustomEvent(Object notifId, Object data, OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * Fire when hasmap changes when - new node joins - node leaves -
         * manual/automatic load balance
         *
         * @param newHashmap new hashmap
         */
        @Override
        public void OnHashmapChanged(NewHashmap newHashmap, boolean updateClientMap) {
        }

        /**
         *
         *
         * @param operationCode
         * @param result
         * @param cbEntry
         */
        @Override
        public void OnWriteBehindOperationCompletedCallback(OpCode operationCode, Object result, CallbackEntry cbEntry) {
        }

        /**
         * Fired when an item which has CacheItemUpdateCallback is updated.
         *
         * @param key
         * @param value
         */
        @Override
        public final void OnCustomUpdateCallback(Object key, Object value, OperationContext operationContext, EventContext eventContext) {
        }

        /**
         * Fired when an item which has CacheItemRemoveCallback is removed.
         *
         * @param key
         * @param value
         * @param reason
         */
        @Override
        public final void OnCustomRemoveCallback(Object key, Object value, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {
        }

        @Override
        public void OnTaskCallback(Object taskID, Object value, OperationContext operationContext, EventContext eventContext) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    /**
     * the front cache store.
     */
    protected LocalCacheBase _primary = null;
    /**
     * the backing cache store.
     */
    protected LocalCacheBase _secondary = null;

    /**
     * Overloaded constructor. Takes the properties as a map.
     *
     * @param cacheSchemes collection of cache schemes (config properties).
     * @param properties properties collection for this cache.
     * @param listener listener for the cache
     * @param timeSched scheduler to use for periodic tasks
     */
    public OverflowCache(java.util.Map cacheClasses, CacheBase parentCache, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) throws ConfigurationException {
        super(properties, parentCache, listener, context);
        _stats.setClassName("overflow-cache");
        Initialize(cacheClasses, properties);

        CacheStatistics pstat = _primary.getStatistics(), sstat = _secondary.getStatistics();
        if (pstat.getMaxCount() == 0 || sstat.getMaxCount() == 0) {
            _stats.setMaxCount(0);
        } else {
            _stats.setMaxCount(pstat.getMaxCount() + sstat.getMaxCount());
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public void dispose() {
        if (_primary != null) {
            _primary.dispose();
            _primary = null;
        }
        if (_secondary != null) {
            _secondary.dispose();
            _secondary = null;
        }
        super.dispose();
    }

    /**
     * front cache store.
     */
    public final CacheBase getPrimary() {
        return _primary;
    }

    /**
     * Get the size of data in store, in bytes.
     */
    @Override
    public long getSize() {
        if (_primary == null || _secondary == null) {
            throw new UnsupportedOperationException();
        }

        long size = 0;
        size += _primary.getSize();
        size += _secondary.getSize();
        return size;
    }

    /**
     * backing cache store.
     */
    public final CacheBase getSecondary() {
        return _secondary;
    }

    /**
     * returns the number of objects contained in the cache.
     */
    @Override
    public long getCount() {
        return 0L;
    }

    /**
     * Method that allows the object to initialize itself. Passes the property
     * map down the object hierarchy so that other objects may configure
     * themselves as well..
     *
     * @param cacheSchemes collection of cache schemes (config properties).
     * @param properties properties collection for this cache.
     */
    @Override
    protected void Initialize(java.util.Map cacheClasses, java.util.Map properties) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            super.Initialize(cacheClasses, properties);
            {
                java.util.Map schemeProps = ConfigHelper.GetCacheScheme(cacheClasses, properties, "primary-cache");
                String cacheType = String.valueOf(schemeProps.get("type")).toLowerCase();
                if (cacheType.compareTo("local-cache") == 0) {
                    // very important to note that the perf collector is not passed further down.
                    _primary = CreateLocalCache(this, cacheClasses, schemeProps);
                    _primary._allowAsyncEviction = false; //do not evict item asynchronously.
                } else if (cacheType.compareTo("overflow-cache") == 0) {
                    _primary = CreateOverflowCache(cacheClasses, schemeProps);
                } else {
                    throw new ConfigurationException("invalid or non-local cache class specified in composite cache");
                }
            }
            {
                java.util.Map schemeProps = ConfigHelper.GetCacheScheme(cacheClasses, properties, "secondary-cache");
                String cacheType = String.valueOf(schemeProps.get("type")).toLowerCase();
                if (cacheType.compareTo("local-cache") == 0) {
                    _secondary = CreateLocalCache(_parentCache, cacheClasses, schemeProps);
                    _secondary._allowAsyncEviction = true;
                } else if (cacheType.compareTo("overflow-cache") == 0) {
                    _secondary = CreateOverflowCache(cacheClasses, schemeProps);
                } else {
                    throw new ConfigurationException("invalid or non-local cache class specified in composite cache");
                }
            }
            _primary.setListener(new PrimaryCacheListener(this));
            _secondary.setListener(new SecondaryCacheListener(this));
        } catch (ConfigurationException e) {
            if (_context != null) {
                _context.getCacheLog().Error("OverflowCache.Initialize()", e.getMessage());
            }
            dispose();
            throw e;
        } catch (Exception e) {
            if (_context != null) {
                _context.getCacheLog().Error("OverflowCache.Initialize()", e.getMessage());
            }
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    protected LocalCacheBase CreateLocalCache(CacheBase parentCache, java.util.Map cacheClasses, java.util.Map schemeProps) throws ConfigurationException {
        return new LocalCache(cacheClasses, parentCache, schemeProps, null, _context);
    }

    protected LocalCacheBase CreateOverflowCache(java.util.Map cacheClasses, java.util.Map schemeProps) throws ConfigurationException {
        return new OverflowCache(cacheClasses, this, schemeProps, null, _context);
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void ClearInternal() {
        if (_primary == null || _secondary == null) {
            throw new UnsupportedOperationException();
        }

        _secondary.ClearInternal();
        _primary.ClearInternal();
    }

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    @Override
    public boolean ContainsInternal(Object key) throws StateTransferException, CacheException {
        if (_primary == null || _secondary == null) {
            throw new UnsupportedOperationException();
        }

        return _primary.ContainsInternal(key) || _secondary.ContainsInternal(key);
    }

    /**
     * Provides implementation of Get method of the ICacheStore interface. Get
     * an object from the store, specified by the passed in key.
     *
     * @param key key
     * @return object
     */
    @Override
    public CacheEntry GetInternal(Object key, boolean isUserOperation, OperationContext operationContext) {
        return null;
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
    public CacheAddResult AddInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation) throws StateTransferException, CacheException, CacheException {
        if (_primary == null || _secondary == null) {
            throw new UnsupportedOperationException();
        }

        // If the secondary has it then we are bound to return error
        if (_secondary.ContainsInternal(key)) {
            return CacheAddResult.KeyExists;
        }

        // If the call succeeds there might be some eviction, which is handled by
        // the primary listener. Otherwise there is some error so we may try the second
        // instance.
        return _primary.AddInternal(key, cacheEntry, false);
    }

    @Override
    public boolean AddInternal(Object key, ExpirationHint eh, OperationContext operationContext) {
        return false;
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
    public CacheInsResult InsertInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation, CacheEntry oldEntry, OperationContext operationContext) {
        return CacheInsResult.Failure;
    }

    /**
     * remove item from the primary cache and move items to the secondary cache
     * if items are being evicted from the primary cache.
     *
     * @param keys
     * @param reason
     * @param notify
     * @return
     */
    @Override
    public Object RemoveSync(Object[] keys, ItemRemoveReason reason, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {
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
    @Override
    public CacheEntry RemoveInternal(Object key, ItemRemoveReason removalReason, boolean isUserOperation, OperationContext operationContext) {
        return null;
    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     */
    @Override
    public ResetableIterator GetEnumerator() throws OperationFailedException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Evicts items from the store.
     *
     * @return
     */
    @Override
    public void Evict() {
        if (_primary == null || _secondary == null) {
            throw new UnsupportedOperationException();
        }

        _primary.Evict();
    }
}
