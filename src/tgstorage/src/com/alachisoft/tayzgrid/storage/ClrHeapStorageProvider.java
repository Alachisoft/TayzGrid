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

import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.propagator.IAlertPropagator;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.ISizable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Implements the Heap cache storage option. Also implements ICacheStorage interface.
 */
public class ClrHeapStorageProvider extends StorageProviderBase
{

    /**
     * Storage Map
     */
    protected java.util.HashMap _itemDict;

    /**
     * Default constructor.
     */
    public ClrHeapStorageProvider()
    {

        _itemDict = new HashMap(DEFAULT_CAPACITY, 0.7f);
    }

    /**
     * Overloaded constructor. The passed in parameters specify the values for maxObjects and maxSizeMB.
     *
     * @param maxDataSize maximum size of data, in bytes, that store can contain.
     */
    public ClrHeapStorageProvider(long maxDataSize)
    {
        super(maxDataSize);
        _itemDict = new java.util.HashMap(DEFAULT_CAPACITY, 0.7f);
    }

    /**
     * Overloaded constructor. Takes the properties as a map.
     *
     * @param properties properties collection
     */
    public ClrHeapStorageProvider(java.util.Map properties, boolean evictionEnabled, ILogger NCacheLog, IAlertPropagator alertPropagator)
    {
        super(properties, evictionEnabled, NCacheLog, alertPropagator);
        _itemDict = new java.util.HashMap(DEFAULT_CAPACITY, 0.7f);
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    @Override
    public void dispose()
    {
        _itemDict.clear();
        _itemDict = null;
        super.dispose();
    }

    /**
     * returns the number of objects contained in the cache.
     */
    @Override
    public long getCount()
    {
        return _itemDict.size();
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void Clear()
    {

        _itemDict.clear();
        super.Cleared();

    }

    /**
     * Determines whether the store contains a specific key.
     *
     * @param key The key to locate in the store.
     * @return true if the store contains an element with the specified key; otherwise, false.
     */
    @Override
    public boolean Contains(Object key)
    {
        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("Store.Cont", "");
        }

        return _itemDict.containsKey(key);
    }

    /**
     * Provides implementation of Get method of the ICacheStorage interface. Get an object from the store, specified by the passed in key.      *
     * @param key key
     * @return object
     */
    @Override
    public Object Get(Object key)
    {
        try
        {
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("Store.Get", "");
            }

            return (Object) _itemDict.get(key);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Get the size of item stored in cache, specified by the passed in key
     *
     * @param key key
     * @return item size
     */
    @Override
    public int GetItemSize(Object key)
    {
        try
        {
            ISizable item = (ISizable) ((_itemDict.get(key) instanceof ISizable) ? _itemDict.get(key) : null);

            return item != null ? item.getInMemorySize() : 0;
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    /**
     * Provides implementation of Add method of the ICacheStorage interface. Add the key value pair to the store.      *
     * @param key key
     * @param item object
     * @return returns the result of operation.
     */
    @Override
    public StoreAddResult Add(Object key, Object item, boolean allowExtentedSize)
    {
        try
        {
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("Store.Add", "");
            }

            if (_itemDict.containsKey(key))
            {
                return StoreAddResult.KeyExists;
            }
            StoreStatus status = HasSpace((ISizable) item,allowExtentedSize);
            if (_reportCacheNearEviction)
            {
                CheckForStoreNearEviction();
            }
            if (status == StoreStatus.HasNotEnoughSpace)
            {
                return StoreAddResult.NotEnoughSpace;
            }


            _itemDict.put(key, item);
            super.Added((ISizable) ((item instanceof ISizable) ? item : null));

            if (status == StoreStatus.NearEviction)
            {
                return StoreAddResult.SuccessNearEviction;
            }
        }
        catch (OutOfMemoryError e)
        {
            return StoreAddResult.NotEnoughSpace;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        return StoreAddResult.Success;
    }

    /**
     * Provides implementation of Insert method of the ICacheStorage interface. Insert/Add the key value pair to the store.      *
     * @param key key
     * @param item object
     * @return returns the result of operation.
     */
    @Override
    public StoreInsResult Insert(Object key, Object item,boolean allowExtentedSize)
    {
        try
        {
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("Store.Insert", "");
            }

            Object oldItem = _itemDict.get(key);

            StoreStatus status = HasSpace((ISizable) ((oldItem instanceof ISizable) ? oldItem : null), (ISizable) item,allowExtentedSize);
            if (_reportCacheNearEviction)
            {
                CheckForStoreNearEviction();
            }

            if (status == StoreStatus.HasNotEnoughSpace)
            {
                return StoreInsResult.NotEnoughSpace;
            }


            _itemDict.put(key, item);
            super.Inserted((ISizable) ((oldItem instanceof ISizable) ? oldItem : null), (ISizable) ((item instanceof ISizable) ? item : null));

            if (status == StoreStatus.NearEviction)
            {
                //the store is almost full, need to evict.
                return oldItem != null ? StoreInsResult.SuccessOverwriteNearEviction : StoreInsResult.SuccessNearEviction;
            }

            return oldItem != null ? StoreInsResult.SuccessOverwrite : StoreInsResult.Success;
        }
        catch (OutOfMemoryError e)
        {
            return StoreInsResult.NotEnoughSpace;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
    }

    /**
     * Provides implementation of Remove method of the ICacheStorage interface. Removes an object from the store, specified by the passed in key
     *
     * @param key key
     * @return object
     */
    @Override
    public Object Remove(Object key)
    {
        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("Store.Remove", "");
        }

        Object e = Get(key);
        if (e != null)
        {

            _itemDict.remove(key);
            super.Removed((ISizable) ((e instanceof ISizable) ? e : null));

        }
        return e;
    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to iterate over the elements of the cache store.
     */
    @Override
    public Iterator GetEnumerator()
    {
        return _itemDict.entrySet().iterator();
    }

    /**
     * returns the keys
     */
    @Override
    public Object[] getKeys()
    {
        return _itemDict.keySet().toArray();
    }
}
