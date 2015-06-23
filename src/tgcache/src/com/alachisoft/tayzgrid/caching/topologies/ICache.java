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

import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.DataSourceUpdateOptions;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.Iterator;

/**
 * This interface defines the basic set of operations a Cache is required to
 * implement.
 */
public interface ICache {

    /**
     * returns the number of objects contained in the cache.
     */
    long getCount() throws OperationFailedException, CacheException, GeneralFailureException;

    /**
     * Removes all entries from the cache.
     *
     * @param cbEntry callback entry for write behind
     * @param updateOptions data source update options
     */
    void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @param operationContext
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     * @throws OperationFailedException
     * @throws CacheException
     * @throws LockingException
     * @throws GeneralFailureException
     */
    boolean Contains(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    /**
     * Retrieve the object from the cache. A string key is passed as parameter.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    CacheEntry Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    /**
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     */
    CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, CacheException, SuspectedException, TimeoutException;

    /**
     * Adds a pair of key and value to the cache. If the specified key already
     * exists in the cache; it is updated, otherwise a new item is added to the
     * cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     */
    CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter. Moreover it take a removal reason and a boolean specifying if
     * a notification should be raised.
     *
     * @param key key of the entry.
     * @param notify boolean specifying to raise the event.
     * @return item value
     */
    CacheEntry Remove(Object key, ItemRemoveReason ir, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    /**
     * Determines whether the cache contains the specific keys.
     *
     * @param keys The keys to locate in the cache.
     * @return List of keys that are not found in the cache.
     */
    java.util.HashMap Contains(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    /**
     * Retrieve the objectd from the cache. An array of keys is passed as
     * parameter.
     *
     * @param keys keys of the entries.
     * @return key and value pairs
     */
    java.util.HashMap Get(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, ClassNotFoundException;

    /**
     * Adds key and value pairs to the cache. Throws an exception or returns a
     * list of keys that failed to add in the cache.
     *
     * @param keys keys of the entries.
     * @param cacheEntries the cache entries.
     * @return List of keys that are added or that alredy exists in the cache
     * and their status
     */
    java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;

    /**
     * Adds key and value pairs to the cache. If any of the specified key
     * already exists in the cache; it is updated, otherwise a new item is added
     * to the cache.
     *
     * @param keys keys of the entries.
     * @param cacheEntries the cache entries.
     * @return return successful keys and there status.
     */
    java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    /**
     * Removes key and value pairs from the cache. The keys are specified as
     * parameter. Moreover it take a removal reason and a boolean specifying if
     * a notification should be raised.
     *
     * @param keys keys of the entries.
     * @param notify boolean specifying to raise the event.
     * @return List of keys and values that are removed from cache
     */
    java.util.HashMap Remove(Object[] keys, ItemRemoveReason ir, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    //: Changing IDictionaryEnumerator to Iterator
    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     *
     * @return IDictionaryEnumerator enumerator.
     */
    Iterator GetEnumerator() throws OperationFailedException, CacheException, LockingException, GeneralFailureException;

    //: changing Array to Object[]
    /**
     * Return all the keys currently present in cache
     */
    Object[] getKeys();
}
