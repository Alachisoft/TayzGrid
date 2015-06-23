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

package com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy;

import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridTransactionalDataRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;

public class NonStrictReadWriteTayzGridRegionAccessStrategy extends TayzGridRegionAccessStrategy {

    public NonStrictReadWriteTayzGridRegionAccessStrategy(TayzGridTransactionalDataRegion region) {
        super(region);
    }

    /**
     * Attempt to retrieve an object from the cache. Mainly used in attempting
     * to resolve entities/collections from the second level cache.
     *
     * @param key The key of the item to be retrieved.
     * @param txTimestamp a timestamp prior to the transaction start time
     * @return the cached object or <tt>null</tt>
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public Object get(Object key, long txTimestamp) throws CacheException {
        return _region.getCache().get(key);
    }

    /**
     * Attempt to cache an object, after loading from the database.
     *
     * @param key The item key
     * @param value The item
     * @param txTimestamp a timestamp prior to the transaction start time
     * @param version the item version number
     * @return <tt>true</tt> if the object was successfully cached
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean putFromLoad(
            Object key,
            Object value,
            long txTimestamp,
            Object version) throws CacheException {
        _region.getCache().put(key, value);
        return true;
    }

    /**
     * Attempt to cache an object, after loading from the database, explicitly
     * specifying the minimalPut behavior.
     *
     * @param key The item key
     * @param value The item
     * @param txTimestamp a timestamp prior to the transaction start time
     * @param version the item version number
     * @param minimalPutOverride Explicit minimalPut flag
     * @return <tt>true</tt> if the object was successfully cached
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean putFromLoad(
            Object key,
            Object value,
            long txTimestamp,
            Object version,
            boolean minimalPutOverride) throws CacheException {
        if (minimalPutOverride && _region.getCache().contains(key)) {
            return false;
        } else {
            return this.putFromLoad(key, value, txTimestamp, version);
        }
    }

    /**
     * We are going to attempt to update/delete the keyed object. This method is
     * used by "asynchronous" concurrency strategies.
     * <p/>
     * The returned object must be passed back to {@link #unlockItem}, to
     * release the lock. Concurrency strategies which do not support
     * client-visible locks may silently return null.
     *
     * @param key The key of the item to lock
     * @param version The item's current version value
     * @return A representation of our lock on the item; or null.
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        return null;
    }

    /**
     * Called when we have finished the attempted update/delete (which may or
     * may not have been successful), after transaction completion. This method
     * is used by "asynchronous" concurrency strategies.
     *
     * @param key The item key
     * @param lock The lock previously obtained from {@link #lockItem}
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
    }
}
