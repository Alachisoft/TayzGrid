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

import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridEntityRegion;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy.util.Item;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy.util.Lock;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy.util.Lockable;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

public final class ReadWriteTayzGridEntityRegionAccessStrategy extends ReadWriteTayzGridRegionAccessStrategy implements EntityRegionAccessStrategy {

    public ReadWriteTayzGridEntityRegionAccessStrategy(TayzGridEntityRegion region) {
        super(region);
    }

    /**
     * Get the wrapped naturalId cache region
     *
     * @return The underlying region
     */
    @Override
    public EntityRegion getRegion() {
        return (EntityRegion) _region;
    }

    /**
     * Called after an item has been inserted (before the transaction
     * completes), instead of calling evict(). This method is used by
     * "synchronous" concurrency strategies.
     *
     * @param key The item key
     * @param value The item
     * @param version The item's version value
     * @return Were the contents of the cache actual changed by this operation?
     * @throws CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean insert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    /**
     * Called after an item has been inserted (after the transaction completes),
     * instead of calling release(). This method is used by "asynchronous"
     * concurrency strategies.
     *
     * @param key The item key
     * @param value The item
     * @param version The item's version value
     * @return Were the contents of the cache actual changed by this operation?
     * @throws CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        try {
            _region.getCache().lock(key);

            Lockable lockable = (Lockable) _region.getCache().get(key);
            if (lockable == null) {
                _region.getCache().put(key, new Item(value, version, _region.getCache().nextTimestamp()));
                return true;
            }
            return false;
        } finally {
            _region.getCache().unlock(key);
        }
    }

    /**
     * Called after an item has been updated (before the transaction completes),
     * instead of calling evict(). This method is used by "synchronous"
     * concurrency strategies.
     *
     * @param key The item key
     * @param value The item
     * @param currentVersion The item's current version value
     * @param previousVersion The item's previous version value
     * @return Were the contents of the cache actual changed by this operation?
     * @throws CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
        return false;
    }

    /**
     * Called after an item has been updated (after the transaction completes),
     * instead of calling release(). This method is used by "asynchronous"
     * concurrency strategies.
     *
     * @param key The item key
     * @param value The item
     * @param currentVersion The item's current version value
     * @param previousVersion The item's previous version value
     * @param lock The lock previously obtained from {@link #lockItem}
     * @return Were the contents of the cache actual changed by this operation?
     * @throws CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock clientLock) throws CacheException {
        try {
            _region.getCache().lock(key);

            Lockable lockable = (Lockable) _region.getCache().get(key);
            Lock lock;
            if (isUnlockable(clientLock, lockable)) {
                lock = (Lock) lockable;
                if (lock.wasLockedConcurrently()) {
                    decrementLock(key, lock);
                    return false;
                }

                _region.getCache().put(key, new Item(value, currentVersion, _region.getCache().nextTimestamp()));
                return true;
            }

            handleLockExpiry(key);
            return false;
        } finally {
            _region.getCache().unlock(key);
        }
    }
}
