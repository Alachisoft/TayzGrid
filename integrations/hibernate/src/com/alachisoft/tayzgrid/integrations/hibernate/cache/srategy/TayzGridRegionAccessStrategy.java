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

import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

public abstract class TayzGridRegionAccessStrategy implements RegionAccessStrategy {
    TayzGridRegion _region;

    public TayzGridRegionAccessStrategy(TayzGridRegion region) {
        _region=region;
    }

    /**
     * Lock the entire region
     *
     * @return A representation of our lock on the item; or null.
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public SoftLock lockRegion() throws CacheException {
        return null;
    }

    /**
     * Called after we have finished the attempted invalidation of the entire
     * region
     *
     * @param lock The lock previously obtained from {@link #lockRegion}
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public void unlockRegion(SoftLock lock) throws CacheException {
    }

    /**
     * Called after an item has become stale (before the transaction completes).
     * This method is used by "synchronous" concurrency strategies.
     *
     * @param key The key of the item to remove
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public void remove(Object key) throws CacheException {
        _region.getCache().remove(key);
    }

    /**
     * Called to evict data from the entire region
     *
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public void removeAll() throws CacheException {
        _region.getCache().clear();
    }

    /**
     * Forcibly evict an item from the cache immediately without regard for
     * transaction isolation.
     *
     * @param key The key of the item to remove
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public void evict(Object key) throws CacheException {
        _region.getCache().remove(key);
    }

    /**
     * Forcibly evict all items from the cache immediately without regard for
     * transaction isolation.
     *
     * @throws org.hibernate.cache.CacheException Propogated from underlying
     * {@link org.hibernate.cache.spi.Region}
     */
    @Override
    public void evictAll() throws CacheException {
        _region.getCache().clear();
    }
}
