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

package com.alachisoft.tayzgrid.integrations.hibernate.cache.regions;

import java.util.Properties;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.GeneralDataRegion;

public class TayzGridGeneralDataRegion extends TayzGridRegion implements GeneralDataRegion {

    public TayzGridGeneralDataRegion(String name, Properties properties) {
        super(name, properties);
    }

    /**
     * Get an item from the cache.
     *
     * @param key The key of the item to be retrieved.
     * @return the cached object or <tt>null</tt>
     * @throws org.hibernate.cache.CacheException Indicates a problem accessing
     * the item or region.
     */
    @Override
    public Object get(Object key) throws CacheException {
        return super.getCache().get(key);
    }

    /**
     * Put an item into the cache.
     *
     * @param key The key under which to cache the item.
     * @param value The item to cache.
     * @throws CacheException Indicates a problem accessing the region.
     */
    @Override
    public void put(Object key, Object value) throws CacheException {
        super.getCache().put(key, value);
    }

    /**
     * Evict an item from the cache immediately (without regard for transaction
     * isolation).
     *
     * @param key The key of the item to remove
     * @throws CacheException Indicates a problem accessing the item or region.
     */
    @Override
    public void evict(Object key) throws CacheException {
        super.getCache().remove(key);
    }

    /**
     * Evict all contents of this particular cache region (without regard for
     * transaction isolation).
     *
     * @throws CacheException Indicates problem accessing the region.
     */
    @Override
    public void evictAll() throws CacheException {
        super.getCache().clear();
    }
}
