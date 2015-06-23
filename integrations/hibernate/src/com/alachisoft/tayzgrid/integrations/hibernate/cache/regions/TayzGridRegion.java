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

import com.alachisoft.tayzgrid.integrations.hibernate.cache.TayzGrid;
import java.util.Map;
import java.util.Properties;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.Region;

public class TayzGridRegion implements Region {

    private String _name;
    private TayzGrid _cache;

    public TayzGridRegion(String name, Properties properties) {
        _name = name;
        _cache = new TayzGrid(name, properties);
    }

    /**
     * Retrieve the name of this region.
     *
     * @return The region name
     */
    @Override
    public String getName() {
        return _cache.getRegionName();
    }

    /**
     * The "end state" contract of the region's lifecycle. Called during
     * {@link org.hibernate.SessionFactory#close()} to give the region a chance
     * to cleanup.
     *
     * @throws org.hibernate.cache.CacheException Indicates problem shutting
     * down
     */
    @Override
    public void destroy() throws CacheException {
        _cache.destroy();
    }

    /**
     * Determine whether this region contains data for the given key.
     * <p/>
     * The semantic here is whether the cache contains data visible for the
     * current call context. This should be viewed as a "best effort", meaning
     * blocking should be avoid if possible.
     *
     * @param key The cache key
     *
     * @return True if the underlying cache contains corresponding data; false
     * otherwise.
     */
    @Override
    public boolean contains(Object key) {
        return _cache.contains(key);
    }

    /**
     * The number of bytes is this cache region currently consuming in memory.
     *
     * @return The number of bytes consumed by this region; -1 if unknown or
     * unsupported.
     */
    @Override
    public long getSizeInMemory() {
        return _cache.getSizeInMemory();
    }

    /**
     * The count of entries currently contained in the regions in-memory store.
     *
     * @return The count of entries in memory; -1 if unknown or unsupported.
     */
    @Override
    public long getElementCountInMemory() {
        return _cache.getElementCountInMemory();
    }

    /**
     * The count of entries currently contained in the regions disk store.
     *
     * @return The count of entries on disk; -1 if unknown or unsupported.
     */
    @Override
    public long getElementCountOnDisk() {
        return _cache.getElementCountOnDisk();
    }

    /**
     * Get the contents of this region as a map.
     * <p/>
     * Implementors which do not support this notion should simply return an
     * empty map.
     *
     * @return The content map.
     */
    @Override
    public Map toMap() {
        return _cache.toMap();
    }

    @Override
    public long nextTimestamp() {
        return _cache.nextTimestamp();
    }

    @Override
    public int getTimeout() {
        return _cache.getTimeout();
    }

    public TayzGrid getCache() {
        return _cache;
    }
}
