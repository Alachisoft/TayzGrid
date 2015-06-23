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

import java.util.Iterator;

/**
 * Synchronized wrapper over cache stores. Provides internal as well as external
 * thread safety.
 */
public class StorageProviderSyncWrapper extends StorageProviderBase {

    /**
     * enwrapped cache store
     */
    protected StorageProviderBase _storage = null;

    /**
     * Default constructor.
     *
     * @param storageProvider The cache store to be wrapped.
     */
    public StorageProviderSyncWrapper(StorageProviderBase storageProvider) {
        if (storageProvider == null) {
            throw new IllegalArgumentException("storageProvider");
        }
        _storage = storageProvider;
        _syncObj = _storage.getSync();
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public void dispose() {
        if (_storage != null) {
            _storage.dispose();
            _storage = null;
        }
        super.dispose();
    }

    /**
     * get or set the maximam size of store, in bytes
     */
    @Override
    public long getMaxSize() {
        return _storage.getMaxSize();
    }

    @Override
    public void setMaxSize(long value) {
        _storage.setMaxSize(value);
    }

    @Override
    public long getMaxCount() {
        return super.getMaxCount();
    }

    @Override
    public void setMaxCount(long value) {
        super.setMaxCount(value);
    }

    /**
     * returns the number of objects contained in the cache.
     */
    @Override
    public long getCount() {
        getReadLock().lock();
        try {
            return _storage.getCount();
        } finally {
            getReadLock().unlock();
        }
    }

    @Override
    public long getSize() {
        getWriteLock().lock();
        try {
            return _storage.getSize();
        } finally {
            getWriteLock().unlock();
        }
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void Clear() {
        getWriteLock().lock();
        try {
            _storage.Clear();
        } finally {
            getWriteLock().unlock();
        }
    }

    /**
     * Determines whether the store contains a specific key.
     *
     * @param key The key to locate in the store.
     * @return true if the store contains an element with the specified key;
     * otherwise, false.
     */
    @Override
    public boolean Contains(Object key) {
        getReadLock().lock();
        try {
            return _storage.Contains(key);
        } finally {
            getReadLock().unlock();
        }
    }

    /**
     * Provides implementation of Get method of the ICacheStorage interface. Get
     * an object from the store, specified by the passed in key.
     *
     * @param key key
     * @return object
     */
    @Override
    public Object Get(Object key) {
        getReadLock().lock();
        try {
            return _storage.Get(key);
        } finally {
            getReadLock().unlock();
        }
    }

    @Override
    public int GetItemSize(Object key) {
        getReadLock().lock();
        try {
            return _storage.GetItemSize(key);
        } finally {
            getReadLock().unlock();
        }
    }

    /**
     * Provides implementation of Add method of the ICacheStorage interface. Add
     * the key value pair to the store.
     *
     * @param key key
     * @param item object
     * @return returns the result of operation.
     */
    @Override
    public StoreAddResult Add(Object key, Object item, boolean allowExtentedSize) {
        getWriteLock().lock();
        try {
             return _storage.Add(key, item,allowExtentedSize);
        } finally {
            getWriteLock().unlock();
        }
    }

    /**
     * Provides implementation of Insert method of the ICacheStorage interface.
     * Insert/Add the key value pair to the store.
     *
     * @param key key
     * @param item object
     * @return returns the result of operation.
     */
    @Override
    public StoreInsResult Insert(Object key, Object item, boolean allowExtentedSize) {
        getWriteLock().lock();
        try {
            return _storage.Insert(key, item, allowExtentedSize);
        } finally {
            getWriteLock().unlock();
        }
    }

    /**
     * Provides implementation of Remove method of the ICacheStorage interface.
     * Removes an object from the store, specified by the passed in key
     *
     * @param key key
     * @return object
     */
    @Override
    public Object Remove(Object key) {
        getWriteLock().lock();
        try {
            return _storage.Remove(key);
        } finally {
            getWriteLock().unlock();
        }
    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     */
    @Override
    public Iterator GetEnumerator() {
        return _storage.GetEnumerator();
    }

    /**
     * returns all the keys of a particular cache store..
     */
    @Override
    public Object[] getKeys() {
        getWriteLock().lock();
        try {
            return _storage.getKeys();
        } finally {
            getWriteLock().unlock();
        }
    }

}
