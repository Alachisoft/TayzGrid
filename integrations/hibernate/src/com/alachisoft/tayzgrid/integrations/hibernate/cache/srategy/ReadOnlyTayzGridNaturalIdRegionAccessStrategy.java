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

import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridNaturalIdRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

public final class ReadOnlyTayzGridNaturalIdRegionAccessStrategy extends ReadOnlyTayzGridRegionAccessStrategy implements NaturalIdRegionAccessStrategy {

    public ReadOnlyTayzGridNaturalIdRegionAccessStrategy(TayzGridNaturalIdRegion region) {
        super(region);
    }

    /**
     * Get the wrapped naturalId cache region
     *
     * @return The underlying region
     */
    @Override
    public NaturalIdRegion getRegion() {
        return (NaturalIdRegion) _region;
    }

    /**
     * Called after an item has been inserted (before the transaction
     * completes), instead of calling evict(). This method is used by
     * "synchronous" concurrency strategies.
     *
     * @param key The item key
     * @param value The item
     * @return Were the contents of the cache actual changed by this operation?
     * @throws CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean insert(Object key, Object value) throws CacheException {
        return false;
    }

    /**
     * Called after an item has been inserted (after the transaction completes),
     * instead of calling release(). This method is used by "asynchronous"
     * concurrency strategies.
     *
     * @param key The item key
     * @param value The item
     * @return Were the contents of the cache actual changed by this operation?
     * @throws CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean afterInsert(Object key, Object value) throws CacheException {
        _region.getCache().put(key, value);
        return true;
    }

    /**
     * Called after an item has been updated (before the transaction completes),
     * instead of calling evict(). This method is used by "synchronous"
     * concurrency strategies.
     *
     * @param key The item key
     * @param value The item
     * @return Were the contents of the cache actual changed by this operation?
     * @throws CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean update(Object key, Object value) throws CacheException {
        return false;
    }

    /**
     * Called after an item has been updated (after the transaction completes),
     * instead of calling release(). This method is used by "asynchronous"
     * concurrency strategies.
     *
     * @param key The item key
     * @param value The item
     * @param lock The lock previously obtained from {@link #lockItem}
     * @return Were the contents of the cache actual changed by this operation?
     * @throws CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public boolean afterUpdate(Object key, Object value, SoftLock clientLock) throws CacheException {
        throw new UnsupportedOperationException("Can't write to a readonly object");
    }
}
