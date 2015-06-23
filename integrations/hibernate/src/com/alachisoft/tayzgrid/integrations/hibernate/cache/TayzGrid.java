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

package com.alachisoft.tayzgrid.integrations.hibernate.cache;

import com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy.util.Timestamper;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.configuration.ConfigurationManager;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.configuration.RegionConfiguraton;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.web.caching.CacheItem;
import com.alachisoft.tayzgrid.web.caching.DSWriteOption;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.cache.CacheException;

public class TayzGrid {

    private static final Logger _log = LogManager.getLogger("TayzGrid");
    private static HashMap<String, CacheHandler> _caches = new HashMap<String, CacheHandler>();
    private static String _tagPrefix = "TGHibernate.";
    private CacheHandler _cacheHandler;
    private RegionConfiguraton _regionConfig = null;
    private String _regionName;
    private String _connectionString;

    public TayzGrid(String regionName, Properties properties) {
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Initializing TayzGrid with region : " + regionName);
            }
            if (properties.containsKey("tayzgrid.application_id")) {
                System.setProperty("tayzgrid.application_id", properties.getProperty("tayzgrid.application_id"));
            }

            _regionName = regionName;
            _regionConfig = ConfigurationManager.getInstance().getRegionConfiguration(regionName);

            synchronized (_caches) {
                if (_caches.containsKey(_regionConfig.getCacheName())) {
                    _cacheHandler = _caches.get(_regionConfig.getCacheName());
                    _cacheHandler.incrementRefCount();
                } else {
                    _cacheHandler = new CacheHandler(_regionConfig.getCacheName(), ConfigurationManager.getInstance().isExceptionEnabled());
                    _caches.put(_regionConfig.getCacheName(), _cacheHandler);
                }
            }

            if (properties.containsKey("hibernate.connection.url")) {
                _connectionString = properties.getProperty("hibernate.connection.url").toString();
            }

        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Failed to initialize TayzGrid. " + e.getMessage());
            }
            throw new CacheException("Failed to initialize TayzGrid. " + e.getMessage(), e);
        }

    }

    public Object get(Object key) throws CacheException {
        try {
            if (key == null) {
                return null;
            }

            String cacheKey = ConfigurationManager.getInstance().getCacheKey(key);
            if (_log.isDebugEnabled()) {
                _log.debug("Fetching object from cache with key = " + cacheKey);
            }
            return _cacheHandler.getCache().get(cacheKey);
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Get operation failed. " + e.getMessage());
            }
            throw new CacheException("Get operation failed. " + e.getMessage(), e);
        }
    }

    public void put(Object key, Object value) throws CacheException {
        try {
            if (key == null) {
                throw new ArgumentException("null key not allowed.");
            }
            if (value == null) {
                throw new ArgumentException("null value not allowed.");
            }

            String cacheKey = ConfigurationManager.getInstance().getCacheKey(key);
            CacheItem cItem = new CacheItem(value);
            cItem.setPriority(_regionConfig.getCacheItemPriority());
            cItem.setTags(new Tag[]{new Tag(_tagPrefix + _regionName)});


            if (_regionConfig.getExpirationType().equalsIgnoreCase("sliding")) {
                cItem.setSlidingExpiration(new TimeSpan(0, 0, _regionConfig.getExpirationPeriod()));
            } else if (_regionConfig.getExpirationType().equalsIgnoreCase("absolute")) {
                cItem.setAbsoluteExpiration(Calendar.getInstance().getTime());
            }

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("Inserting: key={0}&value={1}", key, value.toString()));
            }
            _cacheHandler.getCache().insert(cacheKey, cItem);
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Put operation failed. " + e.getMessage());
            }
            throw new CacheException("Put operation failed. " + e.getMessage(), e);
        }
    }

    public void remove(Object key) throws CacheException {
        try {
            String cacheKey = ConfigurationManager.getInstance().getCacheKey(key);

            if (_log.isDebugEnabled()) {
                _log.debug("Removing item with key: " + cacheKey);
            }
            _cacheHandler.getCache().remove(cacheKey);
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Remove operation failed. " + e.getMessage());
            }
            throw new CacheException("Remove operation failed. " + e.getMessage(), e);
        }
    }

    public void clear() throws CacheException {
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Clearing Cache : " + _cacheHandler + "   with Region: " + _regionName);
            }
            _cacheHandler.getCache().removeByTag(new Tag(_tagPrefix + _regionName));
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Clear operation failed. " + e.getMessage());
            }
            throw new CacheException("Clear operation failed. " + e.getMessage(), e);
        }
    }

    public void destroy() throws CacheException {
        try {
            synchronized (_caches) {
                if (_cacheHandler != null) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Destroying Cache : " + _cacheHandler);
                    }
                    if (_cacheHandler.decrementRefCount() == 0) {
                        _caches.remove(_cacheHandler.toString());
                        _cacheHandler.disposeCache();
                    }
                }
            }
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Destroy operation failed. " + e.getMessage());
            }
            throw new CacheException("Destroy operation failed. " + e.getMessage(), e);
        }
    }

    public void lock(Object key) throws CacheException {
    }

    public void unlock(Object key) throws CacheException {
    }

    public long nextTimestamp() {
        return Timestamper.next();
    }

    public int getTimeout() {
        return Timestamper.ONE_MS * 60000;
    }

    public String getRegionName() {
        return _regionName;
    }

    public long getSizeInMemory() {
        return -1;
    }

    public long getElementCountInMemory() {
        try {
            return _cacheHandler.getCache().getCount();
        } catch (Exception ex) {
        }
        return 0;
    }

    public long getElementCountOnDisk() {
        return 0;
    }

    public Map toMap() {
        return null;
    }

    public boolean contains(Object key) {
        try {
            if (key == null) {
                throw new ArgumentException("null key not allowed.");
            }

            String cacheKey = ConfigurationManager.getInstance().getCacheKey(key);
            return _cacheHandler.getCache().contains(cacheKey);
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("contains operation failed. " + e.getMessage());
            }
            throw new CacheException("contains operation failed. " + e.getMessage(), e);
        }
    }
}
