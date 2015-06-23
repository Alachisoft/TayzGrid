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

package com.alachisoft.tayzgrid.integrations.spring;

import com.alachisoft.tayzgrid.integrations.spring.configuration.SpringCacheConfiguration;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.web.caching.CacheItem;
import com.alachisoft.tayzgrid.web.caching.TayzGrid;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.cache.Cache;

public class TayzGridCache implements Cache {

    private SpringCacheConfiguration cacheConfig = null;
    private com.alachisoft.tayzgrid.web.caching.Cache cache = null;
    private String cacheName = null;

    public TayzGridCache(String cacheName, SpringCacheConfiguration cacheConfig) throws CacheException, GeneralFailureException, Exception {
        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;
        cache = TayzGrid.initializeCache(cacheConfig.getTayzgridInstanceName());
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public Object getNativeCache() {
        return cache;
    }

    @Override
    public ValueWrapper get(Object key) {
        CacheItem item = null;
        try {
            item = cache.getCacheItem(getKey(key));
        } catch (Exception ex) {
            Logger.getLogger(TayzGridCache.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        if (item != null) {
            return new CacheObject(item.getValue());
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        CacheItem item = null;
        try {
            item = cache.getCacheItem(getKey(key));
        } catch (Exception ex) {
            Logger.getLogger(TayzGridCache.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        if (item == null) {
            return null;
        }
        return type.cast(item.getValue());

    }

    @Override
    public void put(Object key, Object value) {
        try {
            cache.insert(getKey(key), getCacheItem(value));
        } catch (Exception ex) {
            Logger.getLogger(TayzGridCache.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    @Override
    public void evict(Object key) {
        try {
            cache.remove(getKey(key));
        } catch (Exception ex) {
            Logger.getLogger(TayzGridCache.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    @Override
    public void clear() {
        try {
            cache.removeByTag(new Tag(cacheName));
        } catch (Exception ex) {
            Logger.getLogger(TayzGridCache.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    private String getKey(Object key) {
        return cacheName + ":" + key.toString();
    }

    private CacheItem getCacheItem(Object value) {
        CacheItem cItem = new CacheItem(value);
        cItem.setTags(new Tag[]{new Tag(cacheName)});
        if (cacheConfig != null) {
            if (cacheConfig.getExpirationType().equalsIgnoreCase("sliding")) {
                cItem.setSlidingExpiration(new TimeSpan(0, 0, cacheConfig.getExpirationPeriod()));
            } else if (cacheConfig.getExpirationType().equalsIgnoreCase("absolute")) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.SECOND, cacheConfig.getExpirationPeriod());
                cItem.setAbsoluteExpiration(c.getTime());
            }
        }
        return cItem;
    }

    public void dispose() throws GeneralFailureException, OperationFailedException, ConfigurationException {
        cache.dispose();
    }
}
