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

package com.alachisoft.tayzgrid.storage;

import com.alachisoft.tayzgrid.common.propagator.IAlertPropagator;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.ISizable;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.ISizableIndex;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is the base class of all Cache stores. Provides additional optional functions that can be overridden as well as default implementation of the some of the methods in the
 * ICacheStorage interface, wherever possible. Implements ICacheStorage.
 */
public class StorageProviderBase implements ICacheStorage
{

    /**
     * Staus of the Store.
     */
    protected enum StoreStatus
    {

        /**
         * Store has space.
         */
        HasSpace,
        /**
         * Store is almost full,but can accomadate some data.
         */
        NearEviction,
        /**
         * Store has no space to accomodate new data.
         */
        HasNotEnoughSpace;

        public int getValue()
        {
            return this.ordinal();
        }

        public static StoreStatus forValue(int value)
        {
            return values()[value];
        }
    }
    public static final int KB = 1024;
    public static final int MB = KB * KB;
    public static final int GB = MB * KB;
    private String _cacheContext;
    private boolean _virtualUnlimitedSpace = false;
    protected long _dataSize;
    
    /**
     * The default starting capacity of stores.
     */
    protected final int DEFAULT_CAPACITY = 25000;
    /**
     * The default percentage of the extra data which can be accomdated.
     */
    protected final double DEFAULT_EXTRA_ACCOMDATION_PERCENTAGE = 0.20f;
    /**
     * Maximam data size, in bytes, that store can hold
     */
    private long _maxSize;
    /**
     * Size of data which can be accomdated even after we reach max size.
     */
    private long _extraDataSize = 0;
    /**
     * Maximam number of object in cache
     */
    private long _maxCount;
    /**
     * Size of data, in bytes, stored in cache
     */
    
    /**
     * Reader, writer lock to be used for synchronization.
     */
    protected ReentrantReadWriteLock _syncObj;
    protected Lock readLock;

    ISizableIndex _queryIndexManager;
    ISizableIndex _expirationIndexManager;
    ISizableIndex _evictionIndexManager;
    
    protected long getTotalDataSize()
    {
        long temp = _dataSize;
        
        if(_queryIndexManager != null)
            temp += _queryIndexManager.getIndexInMemorySize();
        
        if(_expirationIndexManager != null)
            temp += _expirationIndexManager.getIndexInMemorySize();
        
        if(_evictionIndexManager != null)
            temp += _evictionIndexManager.getIndexInMemorySize();
        
        return temp;
    }
    protected void setTotalDataSize(long size)
    {
        _dataSize = size;
    }
    
    public Lock getReadLock()
    {
        return readLock;
    }

    public Lock getWriteLock()
    {
        return writeLock;
    }
    protected Lock writeLock;
    protected boolean _reportCacheNearEviction = false;
    protected int _evictionReportSize = 0;
    protected int _reportInterval = 5;
    protected Calendar _lastReportedTime = Calendar.getInstance();
    private ILogger _ncacheLog;

    public final ILogger getCacheLog()
    {
        return _ncacheLog;
    }
    protected IAlertPropagator _alertPropagator;

    /**
     * Default contructor.
     */
    public StorageProviderBase()
    {
        this(0);
    }

    /**
     * Overloaded constructor. Takes the max objects limit, and the listener as parameters.
     *
     * @param maxLimit maximum number of objects to contain.
     */
    public StorageProviderBase(long maxSize)
    {
        _syncObj = new ReentrantReadWriteLock();
        readLock = _syncObj.readLock();
        writeLock = _syncObj.writeLock();
        _maxSize = maxSize;
    }

    public StorageProviderBase(java.util.Map properties, boolean evictionEnabled)
    {
        this(properties, evictionEnabled, null, null);
    }

    /**
     * Overloaded constructor. Takes the properties as a map.
     *
     * @param properties property collection
     */
    public StorageProviderBase(java.util.Map properties, boolean evictionEnabled, ILogger NCacheLog, IAlertPropagator alertPropagator)
    {
        Initialize(properties, evictionEnabled);
        _ncacheLog = NCacheLog;


        _alertPropagator = alertPropagator;

        String tmp = "";
        if (tmp != null && !tmp.equals(""))
        {
            int size = Integer.parseInt(tmp);
            if (size > 0)
            {
                _evictionReportSize = size;
                _reportCacheNearEviction = true;
            }
        }
        tmp = "";
        if (tmp != null && !tmp.equals(""))
        {
            int interval = Integer.parseInt(tmp);
            if (interval > 5)
            {
                _reportInterval = interval;
            }
        }
    }
    
    /**
     *
     * @param isVirtualUnlimitedSpace
     * @return
     */
    @Override
    public void setVirtualUnlimitedSpace(boolean isVirtualUnlimitedSpace)
    {
        _virtualUnlimitedSpace = isVirtualUnlimitedSpace;
    }
    
    @Override
    public boolean getVirtualUnlimitedSpace()
    {
        return _virtualUnlimitedSpace;
    }

    protected final void CheckForStoreNearEviction()
    {
        if (_reportCacheNearEviction  && !getVirtualUnlimitedSpace())
        {
            _lastReportedTime.add(Calendar.MINUTE, _reportInterval);
            if (_lastReportedTime.getTimeInMillis() < System.currentTimeMillis())
            {
                if (_maxSize > 0 && _evictionReportSize > 0)
                {
                    double currentSizeInPerc = ((double) getTotalDataSize() / (double) _maxSize) * (double) 100;
                    if (currentSizeInPerc >= _evictionReportSize)
                    {
                        _lastReportedTime.setTimeInMillis(System.currentTimeMillis());
                    }
                }
            }
        }
    }

    private void InformCacheNearEviction(Object state)
    {
        try
        {
            long currentSizeInPerc = (getTotalDataSize() / _maxSize) * 100;
            if (currentSizeInPerc > 100)
            {
                currentSizeInPerc = 100;
            }
            //Cannot log system event. Should be replaced bty logger.
            EventLogger.LogEvent("TayzGrid", "Cache '" + _cacheContext + "' has exceeded " + _evictionReportSize + "% of allocated cache size", EventType.WARNING, EventCategories.Warning, EventID.CacheSizeWarning);
            if (_alertPropagator != null)
            {
                _alertPropagator.RaiseAlert(EventID.CacheSizeWarning, "NCache", "Cache '" + _cacheContext + "' has exceeded " + _evictionReportSize + "% of allocated cache size");
            }
            //-
            getCacheLog().CriticalInfo("CacheStore", "cache has exceeded " + _evictionReportSize + "% of allocated cache size");
        }
        catch (RuntimeException e)
        {
        }
    }

    /**
     * Initialize settings
     *
     * @param properties
     */
    public final void Initialize(java.util.Map properties, boolean evictionEnabled)
    {
        _syncObj = new ReentrantReadWriteLock();
        readLock = _syncObj.readLock();
        writeLock = _syncObj.writeLock();
        if (properties == null)
        {
            return;
        }
        if (properties.containsKey("max-size"))
        {
            try
            {
                _maxSize = ToBytes(Long.parseLong((String) properties.get("max-size")));
                
                if (properties.get("max-objects") != null)
                {
                    _maxCount = Long.parseLong((String) properties.get("max-objects"));
                }
                if (evictionEnabled)
                {
                    //we give user extra cution to add/insert data into the store even
                    //when you have reached the max limit. But if this limit is also reached
                    //then we reject the request.
                    _extraDataSize = (long) (_maxSize * DEFAULT_EXTRA_ACCOMDATION_PERCENTAGE);
                }
            }
            catch (Exception e)
            {
            }
        }
    }

    private long ToBytes(long mbytes)
    {
        return mbytes * 1024 * 1024;
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    public void dispose()
    {
        _syncObj = null;
        this.Cleared();
    }

    /**
     * get or set the maximam size of store, in bytes
     */
    public long getMaxSize()
    {
        return _maxSize;
    }

    public void setMaxSize(long value)
    {
        _maxSize = value;
    }

    /**
     * get or set the maximam number of objects
     */
    public long getMaxCount()
    {
        return _maxCount;
    }

    public void setMaxCount(long value)
    {
        _maxCount = value;
    }

    /**
     * Gets/Sets the cache context used for the Compact serialization framework.
     */
    public final String getCacheContext()
    {
        return _cacheContext;
    }

    public final void setCacheContext(String value)
    {
        _cacheContext = value;
    }

    /**
     * get the synchronization object for this store.
     */
    public final ReentrantReadWriteLock getSync()
    {
        return _syncObj;
    }

    /**
     * returns the number of objects contained in the cache.
     */
    public long getCount()
    {
        return 0;
    }

    /**
     * returns the size of data, in bytes, stored in cache
     */
    public long getSize()
    {
        return getTotalDataSize();
    }

    public Object[] getKeys()
    {
        return null;
    }

    /**
     * Removes all entries from the store.
     */
    public void Clear()
    {
    }

    /**
     * Determines whether the store contains a specific key.
     *
     * @param key The key to locate in the store.
     * @return true if the store contains an element with the specified key; otherwise, false.
     */
    public boolean Contains(Object key)
    {
        return false;
    }

    /**
     * Get an object from the store, specified by the passed in key. Must be implemented by cache stores.
     *
     *
     * @param key key
     * @return cache entry.
     */
    public Object Get(Object key)
    {
        return null;
    }

    /**
     * Get the size of item stored in store
     *
     * @param key The key whose items size to get
     * @return Items size
     */
    public int GetItemSize(Object key)
    {
        return 0;
    }

    /**
     * Add the key value pair to the store. Must be implemented by cache stores.
     *
     * @param key key
     * @param item object
     * @param allowExtentedSize boolean
     * @return returns the result of operation.
     */
    public StoreAddResult Add(Object key, Object item, boolean allowExtentedSize)
    {
        return StoreAddResult.Failure;
    }

    /**
     * Insert the key value pair to the store. Must be implemented by cache stores.
     *
     * @param key key
     * @param item object
     * @param allowExtentedSize boolean
     * @return returns the result of operation.
     */
    public StoreInsResult Insert(Object key, Object item, boolean allowExtentedSize)
    {
        return StoreInsResult.Failure;
    }

    /**
     * Removes an object from the store, specified by the passed in key. Must be implemented by cache stores.
     *
     * @param key key
     * @return cache entry.
     */
    public Object Remove(Object key)
    {
        return null;
    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to iterate over the elements of the cache store.
     *
     * @return IDictionaryEnumerator enumerator.
     */
    public Iterator GetEnumerator()
    {
        return null;
    }

 
    
    /**
     * Check if store has enough space to add new item
     *
     * @param item item to be added
     * @param allowExtentedSize boolean
     * @return true is store has space, else false
     */
    protected final StoreStatus HasSpace(ISizable item, boolean allowExtentedSize)
    {
        if(getVirtualUnlimitedSpace())
            return StoreStatus.HasSpace;
        
        long maxSize = _maxSize;
        
        if(!allowExtentedSize)
            maxSize = (long)(_maxSize * 0.95);    

         
        long nextSize = getTotalDataSize() + item.getInMemorySize();


        StoreStatus status = StoreStatus.HasSpace;

        if (nextSize > maxSize)
        {
            if (nextSize > (maxSize + _extraDataSize))
            {
                status = StoreStatus.HasNotEnoughSpace;
            }
            else
            {
                status = StoreStatus.NearEviction;
            }
        }


        return status;
    }

    /**
     * Check if store has enough space to add new item
     *
     * @param oldItem old item
     * @param newItem new item to be inserted
     * @return true is store has space, else false
     */
    protected final StoreStatus HasSpace(ISizable oldItem, ISizable newItem, boolean allowExtendedSize)
    {
        
        if(getVirtualUnlimitedSpace())
            return StoreStatus.HasSpace;
        
        long maxSize = _maxSize;
        
        if(!allowExtendedSize)
            maxSize = (long)(_maxSize*0.95);
        

        long nextSize = getTotalDataSize() + newItem.getInMemorySize()- (oldItem == null ? 0 : oldItem.getInMemorySize());


        StoreStatus status = StoreStatus.HasSpace;
            
        if (nextSize > maxSize)
        {
            if (nextSize > (maxSize + _extraDataSize))
            {
                return StoreStatus.HasNotEnoughSpace;
            }
            return StoreStatus.NearEviction;
        }
        return status;
    }

    /**
     * Increments the data size in cache, after item is Added
     *
     * @param itemSize item added
     */
    protected final void Added(ISizable item)
    {
        _dataSize += (item.getInMemorySize());
    }

    /**
     * Increments the data size in cache, after item is inserted
     *
     * @param oldItem old item
     * @param newItem new item to be inserted
     */
    protected final void Inserted(ISizable oldItem, ISizable newItem)
    {
        _dataSize += newItem.getInMemorySize() - (oldItem == null ? 0 : oldItem.getInMemorySize());
    }

    /**
     * Decrement the data size in cache, after item is removed
     *
     * @param itemSize item removed
     */
    protected final void Removed(ISizable item)
    {
        _dataSize -= (item.getInMemorySize( ));
    }

    /**
     * Reset data size when cache is cleared
     */
    protected final void Cleared()
    {
        setTotalDataSize(0);
    }

    /**
     * Returns the thread safe synchronized wrapper over cache store.
     *
     * @param storageProvider
     * @return
     */
    public static StorageProviderBase Synchronized(StorageProviderBase cacheStorage)
    {
        return new StorageProviderSyncWrapper(cacheStorage);
    }

    
    @Override
    public void setISizableQueryIndexManager(ISizableIndex i) {
        _queryIndexManager = i;
    }

    @Override
    public void setISizableExpirationIndexManager(ISizableIndex i) {
        _expirationIndexManager = i;
    }

    @Override
    public void setISizableEvictionIndexManager(ISizableIndex i) {
        _evictionIndexManager = i;
    }
    
    @Override
    public ISizableIndex getISizableQueryIndexManager() {
       return _queryIndexManager;
    }

    @Override
    public ISizableIndex getISizableExpirationIndexManager() {
        return _expirationIndexManager;
    }

    @Override
    public ISizableIndex getISizableEvictionIndexManager() {
        return _evictionIndexManager;
    }
}
